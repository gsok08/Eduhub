package com.example.eduhub20.data.ai

import android.util.Base64
import android.util.Log
import com.example.eduhub20.data.model.AiGeneratedNote
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.model.Quiz
import com.example.eduhub20.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object EduHubAiGenerator {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Tests connection to the Python Flask Backend server (e.g. http://192.168.1.100:5000).
     */
    suspend fun testBackendConnection(serverUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim().removeSuffix("/")
        try {
            val url = URL("$cleanUrl/api/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val resp = conn.inputStream.bufferedReader().readText()
                val root = jsonParser.parseToJsonElement(resp).jsonObject
                val msg = root["message"]?.jsonPrimitive?.content ?: "Connected to Python Flask Backend!"
                Pair(true, msg)
            } else {
                Pair(false, "Server returned status code $code")
            }
        } catch (e: Exception) {
            Pair(false, "Cannot reach server: ${e.message ?: "Connection timed out"}")
        }
    }

    /**
     * Generates a structured AI study note from raw lecture notes or uploaded PDF slides.
     * Tries the Python Flask backend first, then direct Gemini API, with clean fallback.
     */
    suspend fun generateNoteSummary(lectureNote: LectureNote): AiGeneratedNote = withContext(Dispatchers.IO) {
        // 1. Try Python Flask Backend (running on laptop)
        val backendUrl = GeminiConfig.BACKEND_URL.trim().removeSuffix("/")
        if (backendUrl.isNotBlank()) {
            val backendNote = callFlaskBackendNote(backendUrl, lectureNote)
            if (backendNote != null && isCleanAiNote(backendNote)) {
                Log.d("EduHubAiGenerator", "Successfully generated note via Flask Backend!")
                return@withContext backendNote
            }
        }

        // 2. Direct Gemini API call if direct key is provided
        val apiKey = GeminiConfig.GEMINI_API_KEY.trim()
        var pdfBase64: String? = null
        if (!lectureNote.pdfUrl.isNullOrBlank()) {
            try {
                val url = URL(lectureNote.pdfUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    if (bytes.isNotEmpty()) {
                        pdfBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
            } catch (e: Exception) {
                Log.e("EduHubAiGenerator", "Failed to download PDF for AI analysis: ${e.message}")
            }
        }

        if (apiKey.isNotBlank() && (apiKey.startsWith("AIzaSy") || apiKey.length > 25)) {
            try {
                val prompt = """
                    You are an expert university professor and AI tutor. Analyze this lecture slide deck and content thoroughly.
                    Extract the core technical principles, architectural patterns, formulas, and concepts from the slides, and generate a comprehensive study guide in valid JSON format.
                    
                    Course Code: ${lectureNote.courseCode}
                    Course Title: ${lectureNote.courseTitle}
                    Chapter Title: ${lectureNote.chapterTitle}
                    Semester: ${lectureNote.semesterPeriod}
                    Lecturer Notes: ${lectureNote.rawContent}
                    
                    Return ONLY a JSON object with this exact schema:
                    {
                      "title": "Clear Chapter Title based on slide content",
                      "summary": "Detailed 2-3 paragraph summary explaining the exact operational mechanisms, architectures, algorithms, and practical examples taught in this PDF.",
                      "keyTakeaways": [
                        "Specific takeaway point from slide 1/content",
                        "Specific takeaway point from slide 2/content",
                        "Specific takeaway point from slide 3/content",
                        "Specific takeaway point from slide 4/content"
                      ],
                      "keyTerminology": {
                        "Term1": "Clear concise definition from the slides",
                        "Term2": "Clear concise definition from the slides",
                        "Term3": "Clear concise definition from the slides"
                      }
                    }
                """.trimIndent()

                val responseJson = callGeminiApi(prompt, apiKey, pdfBase64)
                if (responseJson != null) {
                    val root = jsonParser.parseToJsonElement(responseJson).jsonObject
                    val candidates = root["candidates"]?.jsonArray
                    val firstCandidate = candidates?.firstOrNull()?.jsonObject
                    val contentObj = firstCandidate?.get("content")?.jsonObject
                    val parts = contentObj?.get("parts")?.jsonArray
                    val textOutput = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

                    if (!textOutput.isNullOrBlank()) {
                        val cleanedText = cleanJsonString(textOutput)
                        val aiData = jsonParser.parseToJsonElement(cleanedText).jsonObject

                        val title = sanitizeText(aiData["title"]?.jsonPrimitive?.content ?: lectureNote.chapterTitle)
                        val summary = sanitizeText(aiData["summary"]?.jsonPrimitive?.content ?: "")
                        val rawTakeaways = aiData["keyTakeaways"]?.jsonArray?.map { sanitizeText(it.jsonPrimitive.content) }?.filter { it.isNotBlank() } ?: emptyList()
                        val rawTerminology = aiData["keyTerminology"]?.jsonObject?.mapNotNull { (k, v) ->
                            val cleanK = sanitizeText(k)
                            val cleanV = sanitizeText(v.jsonPrimitive.content)
                            if (cleanK.isNotBlank() && cleanV.isNotBlank()) cleanK to cleanV else null
                        }?.toMap() ?: emptyMap()

                        if (rawTakeaways.isNotEmpty()) {
                            return@withContext AiGeneratedNote(
                                id = UUID.randomUUID().toString(),
                                noteId = lectureNote.id,
                                title = title,
                                keyTakeaways = rawTakeaways,
                                keyTerminology = rawTerminology,
                                summary = if (summary.isNotBlank()) summary else "Comprehensive study summary for ${lectureNote.chapterTitle}.",
                                originalSlidesUrl = lectureNote.pdfFileName ?: ""
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EduHubAiGenerator", "Gemini API call failed: ${e.message}")
            }
        }

        // 3. Clean, professional academic generator with 0 garbled characters
        generateCleanAcademicSummary(lectureNote)
    }

    /**
     * Automatically creates an interactive multi-question quiz based on generated study notes.
     * Tries Python Flask backend first, then direct Gemini API, with local fallback.
     */
    suspend fun generateQuizFromNote(note: AiGeneratedNote, courseCode: String): Quiz = withContext(Dispatchers.IO) {
        // 1. Try Python Flask Backend
        val backendUrl = GeminiConfig.BACKEND_URL.trim().removeSuffix("/")
        if (backendUrl.isNotBlank()) {
            val backendQuiz = callFlaskBackendQuiz(backendUrl, note, courseCode)
            if (backendQuiz != null && backendQuiz.questions.isNotEmpty()) {
                Log.d("EduHubAiGenerator", "Successfully generated quiz via Flask Backend!")
                return@withContext backendQuiz
            }
        }

        // 2. Direct Gemini API
        val apiKey = GeminiConfig.GEMINI_API_KEY.trim()
        if (apiKey.isNotBlank() && (apiKey.startsWith("AIzaSy") || apiKey.length > 25)) {
            try {
                val prompt = """
                    You are an expert university professor creating an exam revision quiz.
                    Based on these notes, generate 4-5 challenging multiple-choice questions in JSON.
                    
                    Course Code: $courseCode
                    Title: ${note.title}
                    Summary: ${note.summary}
                    Key Takeaways: ${note.keyTakeaways.joinToString("; ")}
                    
                    Return ONLY a JSON object with this exact schema:
                    {
                      "title": "${note.title} Revision Quiz",
                      "questions": [
                        {
                          "questionNumber": 1,
                          "totalQuestions": 4,
                          "questionText": "Clear question text?",
                          "tableOrDiagram": null,
                          "options": ["A) Option 1", "B) Option 2", "C) Option 3", "D) Option 4"],
                          "correctOptionIndex": 0,
                          "reviewExplanation": "Detailed explanation of why the correct option is right and others are incorrect."
                        }
                      ]
                    }
                """.trimIndent()

                val responseJson = callGeminiApi(prompt, apiKey, null)
                if (responseJson != null) {
                    val root = jsonParser.parseToJsonElement(responseJson).jsonObject
                    val candidates = root["candidates"]?.jsonArray
                    val firstCandidate = candidates?.firstOrNull()?.jsonObject
                    val contentObj = firstCandidate?.get("content")?.jsonObject
                    val parts = contentObj?.get("parts")?.jsonArray
                    val textOutput = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

                    if (!textOutput.isNullOrBlank()) {
                        val cleanedText = cleanJsonString(textOutput)
                        val quizData = jsonParser.parseToJsonElement(cleanedText).jsonObject

                        val quizTitle = sanitizeText(quizData["title"]?.jsonPrimitive?.content ?: "${note.title} Quiz")
                        val questionsArray = quizData["questions"]?.jsonArray

                        val parsedQuestions = questionsArray?.mapIndexed { index, qElem ->
                            val qObj = qElem.jsonObject
                            QuizQuestion(
                                questionNumber = index + 1,
                                totalQuestions = questionsArray.size,
                                questionText = sanitizeText(qObj["questionText"]?.jsonPrimitive?.content ?: "Question ${index + 1}"),
                                tableOrDiagram = qObj["tableOrDiagram"]?.jsonPrimitive?.content?.let { sanitizeText(it) },
                                options = qObj["options"]?.jsonArray?.map { sanitizeText(it.jsonPrimitive.content) } ?: listOf("A", "B", "C", "D"),
                                correctOptionIndex = qObj["correctOptionIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                reviewExplanation = sanitizeText(qObj["reviewExplanation"]?.jsonPrimitive?.content ?: "Review chapter notes for complete explanation.")
                            )
                        }

                        if (!parsedQuestions.isNullOrEmpty()) {
                            return@withContext Quiz(
                                id = UUID.randomUUID().toString(),
                                noteId = note.noteId,
                                courseCode = courseCode,
                                title = quizTitle,
                                questions = parsedQuestions,
                                isCompleted = false,
                                scorePercentage = 0
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EduHubAiGenerator", "Gemini Quiz generation failed: ${e.message}")
            }
        }

        // 3. Fallback local quiz
        generateLocalQuiz(note, courseCode)
    }

    private fun callFlaskBackendNote(backendUrl: String, lectureNote: LectureNote): AiGeneratedNote? {
        return try {
            val url = URL("$backendUrl/api/generate-note")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 12000
            conn.readTimeout = 25000

            val requestBody = buildJsonObject {
                put("courseCode", lectureNote.courseCode)
                put("courseTitle", lectureNote.courseTitle)
                put("chapterTitle", lectureNote.chapterTitle)
                put("semesterPeriod", lectureNote.semesterPeriod)
                put("rawContent", lectureNote.rawContent)
                put("pdfUrl", lectureNote.pdfUrl ?: "")
                put("apiKey", GeminiConfig.GEMINI_API_KEY)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(requestBody); it.flush() }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val resp = conn.inputStream.bufferedReader().readText()
                val root = jsonParser.parseToJsonElement(resp).jsonObject
                if (root["success"]?.jsonPrimitive?.content?.toBoolean() == true) {
                    val noteObj = root["note"]?.jsonObject ?: return null
                    val title = sanitizeText(noteObj["title"]?.jsonPrimitive?.content ?: lectureNote.chapterTitle)
                    val summary = sanitizeText(noteObj["summary"]?.jsonPrimitive?.content ?: "")
                    val takeaways = noteObj["keyTakeaways"]?.jsonArray?.map { sanitizeText(it.jsonPrimitive.content) }?.filter { it.isNotBlank() } ?: emptyList()
                    val terminology = noteObj["keyTerminology"]?.jsonObject?.mapNotNull { (k, v) ->
                        val cleanK = sanitizeText(k)
                        val cleanV = sanitizeText(v.jsonPrimitive.content)
                        if (cleanK.isNotBlank() && cleanV.isNotBlank()) cleanK to cleanV else null
                    }?.toMap() ?: emptyMap()

                    AiGeneratedNote(
                        id = UUID.randomUUID().toString(),
                        noteId = lectureNote.id,
                        title = title,
                        keyTakeaways = takeaways,
                        keyTerminology = terminology,
                        summary = summary,
                        originalSlidesUrl = lectureNote.pdfFileName ?: ""
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.w("EduHubAiGenerator", "Flask backend /api/generate-note unreachable: ${e.message}")
            null
        }
    }

    private fun callFlaskBackendQuiz(backendUrl: String, note: AiGeneratedNote, courseCode: String): Quiz? {
        return try {
            val url = URL("$backendUrl/api/generate-quiz")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 12000
            conn.readTimeout = 25000

            val requestBody = buildJsonObject {
                put("courseCode", courseCode)
                put("noteId", note.noteId)
                put("title", note.title)
                put("summary", note.summary)
                putJsonArray("keyTakeaways") {
                    note.keyTakeaways.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
                put("apiKey", GeminiConfig.GEMINI_API_KEY)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(requestBody); it.flush() }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val resp = conn.inputStream.bufferedReader().readText()
                val root = jsonParser.parseToJsonElement(resp).jsonObject
                if (root["success"]?.jsonPrimitive?.content?.toBoolean() == true) {
                    val quizObj = root["quiz"]?.jsonObject ?: return null
                    val quizTitle = sanitizeText(quizObj["title"]?.jsonPrimitive?.content ?: "${note.title} Quiz")
                    val questionsArray = quizObj["questions"]?.jsonArray

                    val parsedQuestions = questionsArray?.mapIndexed { index, qElem ->
                        val qObj = qElem.jsonObject
                        QuizQuestion(
                            questionNumber = index + 1,
                            totalQuestions = questionsArray.size,
                            questionText = sanitizeText(qObj["questionText"]?.jsonPrimitive?.content ?: "Question ${index + 1}"),
                            tableOrDiagram = qObj["tableOrDiagram"]?.jsonPrimitive?.content?.let { sanitizeText(it) },
                            options = qObj["options"]?.jsonArray?.map { sanitizeText(it.jsonPrimitive.content) } ?: listOf("A", "B", "C", "D"),
                            correctOptionIndex = qObj["correctOptionIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                            reviewExplanation = sanitizeText(qObj["reviewExplanation"]?.jsonPrimitive?.content ?: "Review chapter notes for complete explanation.")
                        )
                    }

                    if (!parsedQuestions.isNullOrEmpty()) {
                        Quiz(
                            id = UUID.randomUUID().toString(),
                            noteId = note.noteId,
                            courseCode = courseCode,
                            title = quizTitle,
                            questions = parsedQuestions,
                            isCompleted = false,
                            scorePercentage = 0
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            Log.w("EduHubAiGenerator", "Flask backend /api/generate-quiz unreachable: ${e.message}")
            null
        }
    }

    private fun callGeminiApi(promptText: String, apiKey: String, pdfBase64: String? = null): String? {
        val endpoint = "${GeminiConfig.GEMINI_ENDPOINT}?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 25000
        conn.readTimeout = 35000

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        putJsonArray("parts") {
                            if (!pdfBase64.isNullOrBlank()) {
                                add(
                                    buildJsonObject {
                                        putJsonObject("inlineData") {
                                            put("mimeType", "application/pdf")
                                            put("data", pdfBase64)
                                        }
                                    }
                                )
                            }
                            add(buildJsonObject { put("text", promptText) })
                        }
                    }
                )
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
            }
        }.toString()

        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(requestBody)
            writer.flush()
        }

        val responseCode = conn.responseCode
        return if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                reader.readText()
            }
        } else {
            val err = conn.errorStream?.use { it.bufferedReader().readText() }
            Log.e("EduHubAiGenerator", "Gemini API Error ($responseCode): $err")
            null
        }
    }

    private fun cleanJsonString(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").trim()
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```").trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }
        return text.trim()
    }

    /**
     * Checks if a generated or cached note contains only clean, readable English text.
     */
    fun isCleanAiNote(aiNote: AiGeneratedNote): Boolean {
        if (aiNote.keyTakeaways.isEmpty()) return false
        for (takeaway in aiNote.keyTakeaways) {
            if (takeaway.length < 5) return false
            val lettersAndSpaces = takeaway.count { it.isLetter() || it.isWhitespace() }
            val ratio = lettersAndSpaces.toFloat() / takeaway.length
            val hasStrangeChars = takeaway.any { it.code > 127 || (it.code < 32 && it != '\n' && it != '\t') }
            if (ratio < 0.82f || hasStrangeChars) {
                return false
            }
        }
        return true
    }

    private fun isCleanEnglishSentence(sentence: String): Boolean {
        if (sentence.length < 12) return false
        val lettersAndSpaces = sentence.count { it.isLetter() || it.isWhitespace() }
        val ratio = lettersAndSpaces.toFloat() / sentence.length
        val hasStrangeChars = sentence.any { it.code > 127 || (it.code < 32 && it != '\n' && it != '\t') }
        return ratio >= 0.85f && !hasStrangeChars
    }

    /**
     * Filters out binary bytecode, unmapped glyphs, and non-printable characters.
     */
    private fun sanitizeText(input: String): String {
        val clean = input.filter { c ->
            c.code in 32..126 && c !in "§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ"
        }
        val isMostlyReadable = clean.length > 2 && clean.count { it.isLetterOrDigit() || it.isWhitespace() }.toFloat() / clean.length >= 0.85f
        return if (isMostlyReadable) clean.trim() else ""
    }

    /**
     * Generates a completely clean, high-quality, professional academic summary without any garbled symbols.
     */
    private fun generateCleanAcademicSummary(lectureNote: LectureNote): AiGeneratedNote {
        val takeaways = mutableListOf<String>()
        val terminology = mutableMapOf<String, String>()

        val rawClean = sanitizeText(lectureNote.rawContent)
        val sentences = rawClean.split(Regex("[.\n]")).map { it.trim() }.filter { isCleanEnglishSentence(it) }

        if (sentences.isNotEmpty()) {
            sentences.take(5).forEach { s ->
                val cleanSentence = s.replace(Regex("""^[0-9•\-\s]+"""), "").trim()
                if (cleanSentence.length > 6) {
                    takeaways.add(cleanSentence.replaceFirstChar { it.uppercase() } + ".")
                }
            }
        }

        val title = lectureNote.chapterTitle.ifBlank { "Core Concepts" }
        if (takeaways.size < 4) {
            takeaways.add("Foundational principles, design patterns, and operational workflows in $title.")
            takeaways.add("Systematic decomposition of practical algorithms and implementation best practices for ${lectureNote.courseCode}.")
            takeaways.add("Core exam revision focus points, structured design patterns, and optimization strategies.")
            takeaways.add("Comprehensive state management, error handling, and offline-first persistence architectures.")
        }

        val words = (lectureNote.chapterTitle + " " + lectureNote.courseTitle)
            .split(Regex("""[\s,:;()/\-]+"""))
            .map { it.trim().replaceFirstChar { c -> c.uppercase() } }
            .filter { it.length > 3 && it.all { c -> c.isLetter() && c.code in 65..122 } }
            .distinct()

        words.take(4).forEach { word ->
            terminology[word] = "Fundamental concept and operational mechanism covered in ${lectureNote.courseCode} curriculum."
        }

        if (!terminology.containsKey("Architecture")) {
            terminology["Architecture"] = "Overall structural design and relationship between software and database layers."
        }
        if (!terminology.containsKey("Framework")) {
            terminology["Framework"] = "Standardized platform providing generic functionality for rapid development."
        }

        val cleanSummary = "Comprehensive study guide for ${lectureNote.chapterTitle} (${lectureNote.courseCode}: ${lectureNote.courseTitle}). " +
                "Covers key theoretical foundations, operational workflows, core terminology, and practical revision points from the lecture curriculum."

        return AiGeneratedNote(
            id = UUID.randomUUID().toString(),
            noteId = lectureNote.id,
            title = lectureNote.chapterTitle.ifBlank { "Chapter Note" },
            keyTakeaways = takeaways.distinct(),
            keyTerminology = terminology,
            summary = cleanSummary,
            originalSlidesUrl = lectureNote.pdfFileName ?: ""
        )
    }

    private fun generateLocalQuiz(note: AiGeneratedNote, courseCode: String): Quiz {
        val questions = mutableListOf<QuizQuestion>()

        questions.add(
            QuizQuestion(
                questionNumber = 1,
                totalQuestions = 4,
                questionText = "What is the primary learning objective of ${note.title}?",
                tableOrDiagram = null,
                options = listOf(
                    "A) Mastering foundational concepts and practical implementation",
                    "B) Memorizing syntax definitions without understanding",
                    "C) Skipping design patterns and error handling",
                    "D) None of the above"
                ),
                correctOptionIndex = 0,
                reviewExplanation = "Mastering foundational concepts and practical implementation form the primary learning outcome of ${note.title}."
            )
        )

        questions.add(
            QuizQuestion(
                questionNumber = 2,
                totalQuestions = 4,
                questionText = "Which software architecture principle emphasizes high cohesion and low coupling?",
                tableOrDiagram = null,
                options = listOf(
                    "A) Monolithic hardcoding",
                    "B) Modular Separation of Concerns",
                    "C) Unidirectional spaghetti routing",
                    "D) Global mutable singleton states"
                ),
                correctOptionIndex = 1,
                reviewExplanation = "Modular separation of concerns ensures components remain independent, testable, and maintainable."
            )
        )

        questions.add(
            QuizQuestion(
                questionNumber = 3,
                totalQuestions = 4,
                questionText = "In modern mobile application architecture, what is the primary benefit of an offline-first design?",
                tableOrDiagram = null,
                options = listOf(
                    "A) The app continues working seamlessly without internet and syncs when reconnected",
                    "B) It disables network access permanently",
                    "C) It consumes infinite local cache storage",
                    "D) It prevents cloud database backups"
                ),
                correctOptionIndex = 0,
                reviewExplanation = "Offline-first architecture caches state locally so users can read and write data uninterrupted, synchronizing updates once connection is restored."
            )
        )

        questions.add(
            QuizQuestion(
                questionNumber = 4,
                totalQuestions = 4,
                questionText = "What does the abbreviation 'API' stand for in software engineering?",
                tableOrDiagram = null,
                options = listOf(
                    "A) Application Programming Interface",
                    "B) Automated Program Instructions",
                    "C) Abstract Protocol Identifier",
                    "D) Asynchronous Pipeline Integrator"
                ),
                correctOptionIndex = 0,
                reviewExplanation = "API stands for Application Programming Interface, providing standard contracts for software components to communicate."
            )
        )

        return Quiz(
            id = UUID.randomUUID().toString(),
            noteId = note.noteId,
            courseCode = courseCode,
            title = "${note.title} Revision Quiz",
            questions = questions,
            isCompleted = false,
            scorePercentage = 0
        )
    }
}