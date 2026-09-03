package com.example.eduhub20.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.eduhub20.data.SupabaseClientProvider
import com.example.eduhub20.data.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object AvatarRepository {
    private const val AVATAR_BUCKET = "avatars"
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB


     //Upload avatar image to Supabase Storage
     //Returns the public URL of the uploaded image
    suspend fun uploadAvatar(
         context: Context,
         imageUri: Uri,
         userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("AvatarRepository", "Starting upload for user: $userId")
            deleteOldAvatar(userId)
            // Validate file size
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.use { it.readBytes() }

            if (bytes == null || bytes.isEmpty()) {
                return@withContext Result.failure(Exception("Failed to read image file"))
            }

            if (bytes.size > MAX_FILE_SIZE) {
                return@withContext Result.failure(Exception("Image size exceeds 5MB limit. Please choose a smaller image."))
            }

            // Generate a unique filename
            val fileName = "${userId}_${System.currentTimeMillis()}.jpg"

            // Compress image to reduce size (optional but recommended)
            val compressedBytes = compressImage(context, imageUri, bytes)

            // Upload to Supabase Storage
            SupabaseClientProvider.storage.from(AVATAR_BUCKET)
                .upload(fileName, compressedBytes) {
                    upsert = true
                }

            // Get the public URL
            val publicUrl = "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/public/$AVATAR_BUCKET/$fileName"

            Log.d("AvatarRepository", "✅ Avatar uploaded successfully: $publicUrl")
            Result.success(publicUrl)

        } catch (e: Exception) {
            Log.e("AvatarRepository", "❌ Failed to upload avatar: ${e.message}")
            Result.failure(Exception("Failed to upload avatar: ${e.message}"))
        }
    }


     //Compress image to reduce file size
    private suspend fun compressImage(context: Context, imageUri: Uri, originalBytes: ByteArray): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    // Resize to max 512x512 to reduce size
                    val maxSize = 512
                    val width = bitmap.width
                    val height = bitmap.height

                    val scale = if (width > height) {
                        maxSize.toFloat() / width
                    } else {
                        maxSize.toFloat() / height
                    }

                    val newWidth = (width * scale).toInt()
                    val newHeight = (height * scale).toInt()

                    if (newWidth < width && newHeight < height) {
                        val resizedBitmap = Bitmap.createScaledBitmap(
                            bitmap,
                            newWidth,
                            newHeight,
                            true
                        )

                        val outputStream = ByteArrayOutputStream()
                        resizedBitmap.compress(
                            android.graphics.Bitmap.CompressFormat.JPEG,
                            80,  // Quality: 80%
                            outputStream
                        )
                        resizedBitmap.recycle()
                        return@withContext outputStream.toByteArray()
                    }

                    bitmap.recycle()
                }
            } catch (e: Exception) {
                Log.e("AvatarRepository", "Failed to compress image: ${e.message}")
            }
            // Return original bytes if compression fails
            originalBytes
        }
    }
    private suspend fun deleteOldAvatar(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // List all files in the avatars bucket
            val files = SupabaseClientProvider.storage.from(AVATAR_BUCKET)
                .list("")

            // Find and delete files that start with the user ID
            var deletedCount = 0
            files.forEach { file ->
                if (file.name.startsWith(userId)) {
                    SupabaseClientProvider.storage.from(AVATAR_BUCKET)
                        .delete(file.name)
                    deletedCount++
                    Log.d("AvatarRepository", " Deleted old avatar: ${file.name}")
                }
            }

            if (deletedCount > 0) {
                Log.d("AvatarRepository", "✅ Deleted $deletedCount old avatar(s) for user $userId")
            } else {
                Log.d("AvatarRepository", "No old avatar found for user $userId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AvatarRepository", "❌ Failed to delete old avatar: ${e.message}")
            // Don't fail the upload if deletion fails - just log the error
            Result.success(Unit)
        }
    }



    //Delete avatar from Supabase Storage
    @Suppress("unused")
    suspend fun deleteAvatar(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // List all files for this user
            val files = SupabaseClientProvider.storage.from(AVATAR_BUCKET)
                .list("")

            // Find and delete the user's avatar
            files.forEach { file ->
                if (file.name.startsWith(userId)) {
                    SupabaseClientProvider.storage.from(AVATAR_BUCKET)
                        .delete(file.name)
                    Log.d("AvatarRepository", "Deleted avatar: ${file.name}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AvatarRepository", "Failed to delete avatar: ${e.message}")
            Result.failure(Exception("Failed to delete avatar: ${e.message}"))
        }
    }


    //Get public URL for avatar
    @Suppress("unused")
    fun getAvatarUrl(fileName: String): String {
        return "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/public/$AVATAR_BUCKET/$fileName"
    }
}
