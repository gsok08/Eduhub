package com.example.eduhub20.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.example.eduhub20.MainActivity
import com.example.eduhub20.R
import com.example.eduhub20.data.SupabaseClientProvider
import com.example.eduhub20.data.local.EduHubLocalStorage
import com.example.eduhub20.data.model.StudyGroupMember
import com.example.eduhub20.data.repository.ChatMessageDto
import com.example.eduhub20.data.repository.GroupMemberDto
import com.example.eduhub20.data.repository.StudyGroupDto
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GroupNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var monitorJob: Job? = null

    companion object {
        private const val TAG = "GroupNotifService"
        const val CHANNEL_SYNC_ID = "eduhub_group_sync_channel"
        const val CHANNEL_MESSAGES_ID = "eduhub_group_messages_channel"
        private const val SYNC_NOTIFICATION_ID = 9001
        private const val PREFS_NOTIF = "eduhub_group_notif_prefs"
        private const val KEY_SEEN_MESSAGES = "seen_message_ids"

        /**
         * Tracks which chat group is currently open on-screen so we don't
         * duplicate notifications if the user is already viewing the chat room.
         */
        @Volatile
        var activeChatGroupId: String? = null

        fun start(context: Context) {
            try {
                val intent = Intent(context, GroupNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "GroupNotificationService start requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start GroupNotificationService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, GroupNotificationService::class.java)
                context.stopService(intent)
                Log.d(TAG, "GroupNotificationService stop requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop GroupNotificationService: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startAsForeground()
        startMessagePolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob == null || monitorJob?.isActive != true) {
            startMessagePolling()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        Log.d(TAG, "GroupNotificationService destroyed")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return

            // 1. Background Sync Ongoing Channel (completely silent, hidden from shade)
            // Delete & recreate to force IMPORTANCE_NONE on devices that had the old IMPORTANCE_MIN channel cached.
            notificationManager.deleteNotificationChannel(CHANNEL_SYNC_ID)
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC_ID,
                "EduHub Background Service",
                NotificationManager.IMPORTANCE_NONE   // completely hidden from notification shade
            ).apply {
                description = "Keeps EduHub listening for study group messages while app is closed"
                setShowBadge(false)
                setSound(null, null)                  // no sound
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(syncChannel)

            // 2. Group Messages High Priority Alert Channel
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Study Group Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming study group messages"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(messageChannel)
        }
    }

    private fun startAsForeground() {
        val ongoingNotification = NotificationCompat.Builder(this, CHANNEL_SYNC_ID)
            .setContentTitle("EduHub Group Sync")
            .setContentText("Listening for group updates")
            .setSmallIcon(R.drawable.ic_school)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // hide from lock screen
            .setSilent(true)                                     // no sound, no vibration
            .setOngoing(true)
            .setShowWhen(false)
            // Android 12+: defer showing the notification so it stays hidden if service
            // finishes quickly or is not user-initiated
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
                }
            }
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    SYNC_NOTIFICATION_ID,
                    ongoingNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(SYNC_NOTIFICATION_ID, ongoingNotification)
            }
            // After promoting to foreground, immediately dismiss the notification from the
            // shade — the service stays alive but the notification card disappears.
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(SYNC_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}")
        }
    }

    private fun isNetworkOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun startMessagePolling() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val authPrefs = getSharedPreferences("eduhub_auth_prefs", Context.MODE_PRIVATE)
            val notifPrefs = getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)

            // Load previously seen message IDs
            val seenMessageIds = (notifPrefs.getStringSet(KEY_SEEN_MESSAGES, null) ?: emptySet()).toMutableSet()
            var isInitialized = seenMessageIds.isNotEmpty()

            Log.d(TAG, "Starting group message polling loop. Initialized: $isInitialized, Seeded IDs: ${seenMessageIds.size}")

            while (isActive) {
                try {
                    // Check Remember Me condition
                    val rememberMe = authPrefs.getBoolean("remember_me", false)
                    val userId = authPrefs.getString("saved_user_id", null)
                    val userName = authPrefs.getString("saved_user_name", "") ?: ""

                    // Condition: If user logged out or Remember Me is false, stop immediately
                    if (!rememberMe || userId.isNullOrBlank()) {
                        Log.d(TAG, "User logged out or remember_me is false. Stopping service.")
                        stopSelf()
                        break
                    }

                    // Condition: If user / device is offline, skip this cycle silently
                    if (!isNetworkOnline()) {
                        delay(4000L)
                        continue
                    }

                    // Obtain all groups the user has joined
                    val joinedGroupIds = mutableSetOf<String>()
                    joinedGroupIds.addAll(EduHubLocalStorage.loadJoinedGroupIds(userId))

                    // Also fetch remote memberships to ensure multi-device consistency
                    try {
                        val remoteMemberships = SupabaseClientProvider.postgrest
                            .from("group_members")
                            .select { filter { eq("user_id", userId) } }
                            .decodeList<GroupMemberDto>()
                        joinedGroupIds.addAll(remoteMemberships.map { it.groupId })
                    } catch (_: Exception) {}


                    Log.d(TAG, "Poll cycle: user=$userName, joined=${joinedGroupIds.size} groups")

                    if (joinedGroupIds.isNotEmpty()) {
                        val remoteGroups = try {
                            SupabaseClientProvider.postgrest.from("study_groups").select().decodeList<StudyGroupDto>()
                        } catch (_: Exception) { emptyList() }
                        val localGroups = EduHubLocalStorage.loadGroups()
                        val groupMap = (localGroups.map { it.id to it.name } + remoteGroups.map { it.id to it.name }).toMap()

                        for (groupId in joinedGroupIds) {
                            if (!isActive) break

                            try {
                                val messages = SupabaseClientProvider.postgrest
                                    .from("chat_messages")
                                    .select {
                                        filter { eq("group_id", groupId) }
                                    }
                                    .decodeList<ChatMessageDto>()

                                if (!isInitialized) {
                                    // First time startup: seed all existing historical messages so user isn't spammed
                                    for (msg in messages) {
                                        seenMessageIds.add(msg.id)
                                    }
                                } else {
                                    // Check for new incoming messages from others
                                    for (msg in messages) {
                                        if (!seenMessageIds.contains(msg.id)) {
                                            seenMessageIds.add(msg.id)

                                            val isFromMe = (!msg.senderId.isNullOrBlank() && msg.senderId == userId) ||
                                                    msg.senderName.equals(userName, ignoreCase = true) ||
                                                    msg.isFromMe

                                            // Only notify if NOT from me and user is not currently in this chat room
                                            if (!isFromMe && activeChatGroupId != groupId) {
                                                val groupName = groupMap[groupId] ?: "Study Group"
                                                showIncomingMessageNotification(
                                                    messageId = msg.id,
                                                    groupId = groupId,
                                                    groupName = groupName,
                                                    senderName = msg.senderName,
                                                    messageText = msg.message
                                                )
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error fetching messages for group $groupId: ${e.message}")
                            }
                        }

                        // Mark initialized and save seen IDs
                        if (!isInitialized) {
                            isInitialized = true
                        }
                        notifPrefs.edit {
                            // Keep max 500 recent IDs to avoid unbound growth
                            val trimmed = if (seenMessageIds.size > 500) {
                                seenMessageIds.toList().takeLast(500).toSet()
                            } else {
                                seenMessageIds.toSet()
                            }
                            putStringSet(KEY_SEEN_MESSAGES, trimmed)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Loop error: ${e.message}")
                }

                delay(3500L)
            }
        }
    }

    private fun showIncomingMessageNotification(
        messageId: String,
        groupId: String,
        groupName: String,
        senderName: String,
        messageText: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_GROUP_ID", groupId)
            putExtra("OPEN_GROUP_NAME", groupName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            groupId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.drawable.ic_school)
            .setContentTitle("$senderName • $groupName")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(messageId.hashCode(), notification)
        Log.d(TAG, "🔔 Prompted notification for message $messageId in group '$groupName' from '$senderName'")
    }
}
