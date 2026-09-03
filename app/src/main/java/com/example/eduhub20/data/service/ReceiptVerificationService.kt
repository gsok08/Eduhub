package com.example.eduhub20.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ReceiptData(
    val isValid: Boolean,
    val rawText: String,
    val amount: String = "",
    val receiver: String = "",
    val dateTime: String = "",
    val status: String = "",
    val remark: String = "",
    val errorMessage: String? = null
)

object ReceiptVerificationService {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun verifyReceiptFromUri(
        context: Context,
        uri: Uri,
        expectedReceiver: String = "CHONG YI JIE",
        expectedAmount: String = "7.00"
    ): ReceiptData {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) {
                return ReceiptData(false, "", errorMessage = "Unable to decode image file.")
            }
            verifyReceiptBitmap(bitmap, expectedReceiver, expectedAmount)
        } catch (e: Exception) {
            ReceiptData(false, "", errorMessage = "Failed to load image: ${e.message}")
        }
    }

    suspend fun verifyReceiptBitmap(
        bitmap: Bitmap,
        expectedReceiver: String = "CHONG YI JIE",
        expectedAmount: String = "7.00"
    ): ReceiptData = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                val parsed = parseAndValidateReceiptText(fullText, expectedReceiver, expectedAmount)
                continuation.resume(parsed)
            }
            .addOnFailureListener { e ->
                continuation.resume(
                    ReceiptData(false, "", errorMessage = "OCR recognition failed: ${e.message}")
                )
            }
    }

    fun parseAndValidateReceiptText(
        text: String,
        expectedReceiver: String,
        expectedAmount: String
    ): ReceiptData {
        // 1. Status Check
        val statusFound = text.contains("Transferred", ignoreCase = true) ||
                text.contains("Successful", ignoreCase = true) ||
                text.contains("Berjaya", ignoreCase = true) ||
                text.contains("Completed", ignoreCase = true)
        val extractedStatus = if (statusFound) "Transferred (Success)" else "Unknown Status"

        // 2. Amount Check (e.g. RM 7.00, 7.00, RM7.00)
        val amountRegex = Regex("""(?:RM|MYR)?\s*(\d+\.\d{2})""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(text)
        val extractedAmount = amountMatch?.groupValues?.get(1) ?: ""

        val isAmountValid = extractedAmount == expectedAmount ||
                text.contains("RM $expectedAmount", ignoreCase = true) ||
                text.contains("RM$expectedAmount", ignoreCase = true) ||
                text.contains(expectedAmount)

        // 3. Receiver Check (case-insensitive)
        val cleanExpectedReceiver = expectedReceiver.trim().uppercase()
        val isReceiverValid = text.uppercase().contains(cleanExpectedReceiver) ||
                text.uppercase().contains("CHONG") ||
                text.uppercase().contains("YI JIE")

        // 4. Date & Time Check
        val dateRegex = Regex("""(\d{2}[/-]\d{2}[/-]\d{2,4})""")
        val timeRegex = Regex("""(\d{1,2}:\d{2}(?::\d{2})?)""")
        val dateMatch = dateRegex.find(text)?.value ?: ""
        val timeMatch = timeRegex.find(text)?.value ?: ""
        val extractedDateTime = if (dateMatch.isNotBlank()) "$dateMatch $timeMatch".trim() else "N/A"

        // 5. Remark Check
        val extractedRemark = if (text.contains("Fund Transfer", ignoreCase = true)) "Fund Transfer" else "EduHub Pro Subscription"

        val errors = mutableListOf<String>()
        if (!statusFound) errors.add("Payment status 'Transferred' or 'Successful' was not found.")
        if (!isAmountValid) errors.add("Amount does not match required RM $expectedAmount (found: ${if (extractedAmount.isNotBlank()) "RM $extractedAmount" else "none"}).")
        if (!isReceiverValid) errors.add("Receiver name does not match '$expectedReceiver'.")

        val isValid = errors.isEmpty()
        return ReceiptData(
            isValid = isValid,
            rawText = text,
            amount = if (extractedAmount.isNotBlank()) "RM $extractedAmount" else "RM $expectedAmount",
            receiver = if (isReceiverValid) expectedReceiver else "Unknown",
            dateTime = extractedDateTime,
            status = extractedStatus,
            remark = extractedRemark,
            errorMessage = if (isValid) null else errors.joinToString("\n• ", prefix = "• ")
        )
    }
}
