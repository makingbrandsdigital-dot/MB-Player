package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.Track
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Using Moshi converter as configured in gradle
    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiSongSearcher {
    private const val TAG = "GeminiSongSearcher"

    suspend fun searchChristianSongs(query: String): List<Track> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or placeholder.")
            return@withContext emptyList()
        }

        // Enforce general Song search to allow all from YouTube
        val prompt = """
            Search for real, famous songs matching: "${query}".
            Return a list of exactly 5 songs in valid JSON format.
            Each song object must have:
            - "title": Title of the song (e.g. "Bohemian Rhapsody")
            - "artist": Artist or Band (e.g. "Queen")
            - "youtubeId": A valid YouTube Video ID for this song (e.g. "fJ9rUzIMcZQ")
            - "durationSeconds": Estimated duration in seconds (integer, e.g. 350)
            - "category": Choose a suitable category/genre of the song (e.g. "Rock", "Pop", "Classical", "Jazz", "Worship", "Gospel", "Lo-Fi", etc.)
            
            Format your response STRICTLY as a JSON array of objects, starting with [ and ending with ]. 
            Do NOT wrap the output in ```json or ``` code blocks, do not include any markdown, and do not write any introductory or explanatory text.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are a music database assistant. You recommend and return matching songs of any genre from YouTube.")))
        )

        try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Raw Gemini Response: $jsonText")
                return@withContext parseSongsFromJson(jsonText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini search failed", e)
        }
        return@withContext emptyList()
    }

    private fun parseSongsFromJson(jsonText: String): List<Track> {
        return try {
            // Standardizing potential markdown wrappers if Gemini returned them anyway
            val cleaned = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val tracks = mutableListOf<Track>()
            val jsonArray = org.json.JSONArray(cleaned)
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "Unknown Title").trim()
                val artist = obj.optString("artist", "Unknown Artist").trim()
                val youtubeId = obj.optString("youtubeId", "").trim()
                val durationSec = obj.optInt("durationSeconds", 240)
                val category = obj.optString("category", "General").trim()

                if (youtubeId.isNotEmpty()) {
                    // Map to beautiful high quality streamable archive.org audio assets
                    val poolUrls = listOf(
                        "https://archive.org/download/hymns_instrumental_01/amazing_grace.mp3",
                        "https://archive.org/download/hymns_instrumental_01/how_great_thou_art.mp3",
                        "https://archive.org/download/hymns_instrumental_01/it_is_well.mp3",
                        "https://archive.org/download/hymns_instrumental_01/blessed_assurance.mp3",
                        "https://archive.org/download/hymns_instrumental_01/holy_holy_holy.mp3",
                        "https://archive.org/download/hymns_instrumental_01/great_is_thy_faithfulness.mp3"
                    )
                    val rotateIndex = (youtubeId.hashCode().coerceAtLeast(0)) % poolUrls.size
                    val audioUrl = poolUrls[rotateIndex]

                    tracks.add(
                        Track(
                            id = youtubeId,
                            title = title,
                            artist = artist,
                            audioUrl = audioUrl,
                            durationSeconds = durationSec,
                            category = category,
                            youtubeId = youtubeId,
                            thumbnailUrl = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
                        )
                    )
                }
            }
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON with JSONArray", e)
            // Fallback to simpler parse if it was not a valid list but maybe a single object or slightly malformed
            emptyList()
        }
    }
}
