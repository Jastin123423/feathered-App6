package com.unera.social.data

import com.unera.social.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object StoriesApiService {
    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * 1. GET /api/stories?viewerId={meId}
     * List all active stories for viewer
     */
    suspend fun fetchStories(viewerId: Long): List<Story> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Story>()
        try {
            val url = URL("$BASE_URL/api/stories?viewerId=$viewerId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", viewerId.toString())
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseStoryJson(obj, viewerId)?.let { result.add(it) }
                    }
                } else if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val arr = root.optJSONArray("stories") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseStoryJson(obj, viewerId)?.let { result.add(it) }
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    /**
     * 2. POST /api/stories
     * Create new story (image / video / text)
     */
    suspend fun createStory(
        userId: Long,
        type: String, // "image", "video", "text"
        textContent: String? = null,
        mediaUrl: String? = null,
        mediaUrls: List<String> = emptyList(),
        backgroundStyle: String? = null,
        musicUrl: String? = null,
        musicTitle: String? = null,
        musicStart: Int = 0,
        musicEnd: Int? = null,
        musicDuration: Int = 0,
        effectId: String = "none",
        duration: Int = 90
    ): Story? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("type", type)
                if (!textContent.isNullOrBlank()) put("text_content", textContent)
                if (!mediaUrl.isNullOrBlank()) put("media_url", mediaUrl)
                if (mediaUrls.isNotEmpty()) {
                    val arr = JSONArray()
                    mediaUrls.forEach { arr.put(it) }
                    put("media_urls", arr)
                }
                if (!backgroundStyle.isNullOrBlank()) put("background_style", backgroundStyle)
                if (!musicUrl.isNullOrBlank()) put("music_url", musicUrl)
                if (!musicTitle.isNullOrBlank()) put("music_title", musicTitle)
                put("music_start", musicStart)
                if (musicEnd != null) put("music_end", musicEnd)
                put("music_duration", musicDuration)
                put("effect_id", effectId)
                put("duration", duration)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                val root = JSONObject(responseText)
                val sObj = root.optJSONObject("story") ?: root
                return@withContext parseStoryJson(sObj, userId)
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 3. DELETE /api/stories/{storyId}
     * Delete own story
     */
    suspend fun deleteStory(storyId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId?user_id=$userId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply { put("user_id", userId) }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 4. POST /api/stories/{storyId}/view
     * Record a view
     */
    suspend fun recordView(storyId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/view")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply { put("user_id", userId) }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 5. GET /api/stories/{storyId}/viewers?limit=200
     * List everyone who viewed the story
     */
    suspend fun fetchViewers(storyId: Long, limit: Int = 200): List<StoryViewer> = withContext(Dispatchers.IO) {
        val result = mutableListOf<StoryViewer>()
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/viewers?limit=$limit")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                val root = JSONObject(responseText)
                val arr = root.optJSONArray("viewers")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val v = parseViewerJson(obj)
                        if (v != null) result.add(v)
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    /**
     * 6. GET /api/stories/{storyId}/reactions?viewerId={meId}
     * Fetch reactions + counts
     */
    suspend fun fetchReactions(storyId: Long, viewerId: Long): StoryReactionsData? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/reactions?viewerId=$viewerId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", viewerId.toString())
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val reactionsList = mutableListOf<StoryReactionItem>()
                    val rArr = root.optJSONArray("reactions")
                    if (rArr != null) {
                        for (i in 0 until rArr.length()) {
                            val rObj = rArr.optJSONObject(i) ?: continue
                            val uObj = rObj.optJSONObject("user")
                            val uName = uObj?.optString("name").takeIf { !it.isNullOrBlank() }
                                ?: uObj?.optString("username", "User") ?: "User"
                            val uAvatar = uObj?.optString("profile_image_url") ?: ""
                            reactionsList.add(
                                StoryReactionItem(
                                    userId = rObj.optLong("user_id"),
                                    type = rObj.optString("type", "like"),
                                    name = uName,
                                    profileImageUrl = uAvatar
                                )
                            )
                        }
                    }

                    val countsMap = mutableMapOf<String, Int>()
                    val cObj = root.optJSONObject("counts")
                    if (cObj != null) {
                        val keys = cObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            countsMap[k] = cObj.optInt(k, 0)
                        }
                    }

                    val myReaction = root.optString("my_reaction").takeIf { it.isNotBlank() && it != "null" }
                    val commentsCount = root.optInt("comments_count", 0)

                    return@withContext StoryReactionsData(
                        reactions = reactionsList,
                        counts = countsMap,
                        myReaction = myReaction,
                        commentsCount = commentsCount
                    )
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 7. POST /api/stories/{storyId}/react
     * React / unreact to a story
     */
    suspend fun reactToStory(storyId: Long, userId: Long, type: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/react")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("type", type)
                put("reaction", type)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 8. GET /api/stories/{storyId}/comments?viewerId={meId}
     * Fetch all comments
     */
    suspend fun fetchComments(storyId: Long, viewerId: Long): List<StoryComment> = withContext(Dispatchers.IO) {
        val result = mutableListOf<StoryComment>()
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/comments?viewerId=$viewerId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", viewerId.toString())
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val arr = root.optJSONArray("comments") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseCommentJson(obj)?.let { result.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseCommentJson(obj)?.let { result.add(it) }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    /**
     * 9. POST /api/stories/{storyId}/comments
     * Add comment / reply
     */
    suspend fun addComment(
        storyId: Long,
        userId: Long,
        content: String,
        imageUrl: String? = null,
        parentId: Long? = null
    ): StoryComment? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/comments")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("content", content)
                put("image_url", imageUrl ?: JSONObject.NULL)
                put("parent_id", parentId ?: JSONObject.NULL)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                val root = JSONObject(responseText)
                val cObj = root.optJSONObject("comment") ?: root
                return@withContext parseCommentJson(cObj)
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 10. PATCH /api/stories/{storyId}/comments
     * Hide / unhide comment
     */
    suspend fun toggleHideComment(storyId: Long, userId: Long, commentId: Long, action: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/comments")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("comment_id", commentId)
                put("action", action) // "hide" or "unhide"
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 11. DELETE /api/stories/{storyId}/comments?comment_id={cid}&user_id={meId}
     * Delete comment
     */
    suspend fun deleteComment(storyId: Long, commentId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/comments?comment_id=$commentId&user_id=$userId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 12. POST /api/stories/comments/{commentId}/like
     * Like / unlike comment
     */
    suspend fun likeComment(commentId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/comments/$commentId/like")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply { put("user_id", userId) }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 13. POST /api/stories/{storyId}/reply
     * Send DM-style reply to story
     */
    suspend fun replyToStory(storyId: Long, userId: Long, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/reply")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("text", text)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 14. POST /api/stories/{storyId}/share
     * Record a share
     */
    suspend fun shareStory(storyId: Long, userId: Long, destination: String, message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/share")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("destination", destination)
                put("message", message)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 15. GET /api/stories/{storyId}/analytics
     * Owner-side insights
     */
    suspend fun fetchAnalytics(storyId: Long): StoryAnalytics? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/stories/$storyId/analytics")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val breakdown = mutableMapOf<String, Int>()
                    val bObj = root.optJSONObject("reaction_breakdown")
                    if (bObj != null) {
                        val keys = bObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            breakdown[k] = bObj.optInt(k, 0)
                        }
                    }

                    return@withContext StoryAnalytics(
                        totalViews = root.optInt("total_views", 0),
                        uniqueViewers = root.optInt("unique_viewers", 0),
                        viewsWithReactions = root.optInt("views_with_reactions", 0),
                        reactionBreakdown = breakdown,
                        completionRate = root.optDouble("completion_rate", 0.0),
                        averageViewTime = root.optDouble("average_view_time", 0.0)
                    )
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 16. Multipart upload to POST /api/upload
     */
    suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        folder: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val boundary = "==Boundary-${UUID.randomUUID()}=="
            val urlString = if (!folder.isNullOrBlank()) "$BASE_URL/api/upload?folder=$folder" else "$BASE_URL/api/upload"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val dos = DataOutputStream(conn.outputStream)
            dos.writeBytes("--$boundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            dos.writeBytes("Content-Type: $mimeType\r\n\r\n")
            dos.write(fileBytes)
            dos.writeBytes("\r\n")
            dos.writeBytes("--$boundary--\r\n")
            dos.flush()
            dos.close()

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                val root = JSONObject(responseText)
                if (root.has("url")) return@withContext root.optString("url")
                val uploaded = root.optJSONObject("uploaded")
                if (uploaded != null) {
                    val feed = uploaded.optJSONObject("feed")?.optString("url")
                    val orig = uploaded.optJSONObject("original")?.optString("url")
                    return@withContext feed ?: orig
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // ==========================================
    // PARSERS
    // ==========================================

    private fun parseStoryJson(obj: JSONObject, viewerId: Long): Story? {
        val id = obj.optLong("id")
        if (id <= 0) return null

        val userId = obj.optLong("user_id", obj.optLong("author_id", 1L))
        val authorName = obj.optString("author_name").takeIf { !it.isNullOrBlank() && it != "null" }
            ?: obj.optString("author_full_name").takeIf { !it.isNullOrBlank() && it != "null" }
            ?: obj.optString("username", "User $userId")

        val authorUsername = obj.optString("author_username").takeIf { !it.isNullOrBlank() && it != "null" }
            ?: obj.optString("username", "user$userId")

        val authorAvatar = obj.optString("author_image").takeIf { !it.isNullOrBlank() && it != "null" }
            ?: obj.optString("profile_image_url").takeIf { !it.isNullOrBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"

        val authorIsVerified = obj.optInt("author_is_verified", 0) == 1
            || obj.optBoolean("author_is_verified", false)

        // Nested author/user object fallback
        val userObj = obj.optJSONObject("user") ?: obj.optJSONObject("author")
        val finalUser = if (userObj != null) {
            val uId = userObj.optLong("id", userId)
            val uName = userObj.optString("name").takeIf { !it.isNullOrBlank() } ?: authorName
            val uUsername = userObj.optString("username").takeIf { !it.isNullOrBlank() } ?: authorUsername
            val uAvatar = userObj.optString("profile_image_url").takeIf { !it.isNullOrBlank() } ?: authorAvatar
            val uVerified = userObj.optBoolean("is_verified", authorIsVerified)
            User(id = uId, name = uName, username = uUsername, profileImageUrl = uAvatar, isVerified = uVerified)
        } else {
            User(id = userId, name = authorName, username = authorUsername, profileImageUrl = authorAvatar, isVerified = authorIsVerified)
        }

        val type = obj.optString("type", "image")
        val mediaUrl = obj.optString("media_url", "")
        val textContent = obj.optString("text_content", "")
        val backgroundStyle = obj.optString("background_style", "linear-gradient(45deg, #1877F2, #0055FF)")
        val musicTitle = obj.optString("music_title").takeIf { !it.isNullOrBlank() && it != "null" }
        val musicUrl = obj.optString("music_url").takeIf { !it.isNullOrBlank() && it != "null" }

        val viewsCount = obj.optInt("views_count", 0)
        val reactionsCount = obj.optInt("reactions_count", obj.optInt("likes_count", 0))
        val commentsCount = obj.optInt("comments_count", 0)
        val sharesCount = obj.optInt("shares_count", 0)
        val myReaction = obj.optString("my_reaction").takeIf { !it.isNullOrBlank() && it != "null" }
        val isLiked = myReaction != null || obj.optInt("liked_by_me", 0) == 1 || obj.optBoolean("liked_by_me", false)
        val isViewed = obj.optInt("viewed_by_me", 0) == 1 || obj.optBoolean("viewed_by_me", false)

        val duration = obj.optInt("duration", 90)
        val musicStart = obj.optInt("music_start", 0)
        val musicEnd = if (obj.has("music_end") && !obj.isNull("music_end")) obj.optInt("music_end") else null
        val musicDuration = obj.optInt("music_duration", 0)
        val effectId = obj.optString("effect_id", "none")

        val mediaUrls = mutableListOf<String>()
        val mUrlsRaw = obj.opt("media_urls")
        if (mUrlsRaw is JSONArray) {
            for (i in 0 until mUrlsRaw.length()) {
                val u = mUrlsRaw.optString(i)
                if (u.isNotBlank() && u != "null") mediaUrls.add(u)
            }
        } else if (mUrlsRaw is String && mUrlsRaw.startsWith("[")) {
            try {
                val arr = JSONArray(mUrlsRaw)
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i)
                    if (u.isNotBlank() && u != "null") mediaUrls.add(u)
                }
            } catch (e: Exception) { }
        }
        if (mediaUrls.isEmpty() && mediaUrl.isNotBlank()) {
            mediaUrls.add(mediaUrl)
        }

        return Story(
            id = id,
            author = finalUser,
            mediaUrl = mediaUrl,
            timestamp = obj.optString("created_at", "Just now"),
            isViewed = isViewed,
            type = type,
            textContent = textContent,
            backgroundStyle = backgroundStyle,
            musicTitle = musicTitle,
            musicUrl = musicUrl,
            viewsCount = viewsCount,
            reactionsCount = reactionsCount,
            commentsCount = commentsCount,
            sharesCount = sharesCount,
            myReaction = myReaction,
            isLiked = isLiked,
            duration = duration,
            musicStart = musicStart,
            musicEnd = musicEnd,
            musicDuration = musicDuration,
            effectId = effectId,
            mediaUrls = mediaUrls
        )
    }

    private fun parseViewerJson(obj: JSONObject): StoryViewer? {
        val uid = obj.optLong("user_id")
        if (uid <= 0) return null
        val id = obj.optLong("id", uid)
        val sId = obj.optLong("story_id")
        val viewedAt = obj.optString("viewed_at", "Just now")
        val reaction = obj.optString("reaction").takeIf { !it.isNullOrBlank() && it != "null" }
        val name = obj.optString("name").takeIf { !it.isNullOrBlank() }
            ?: obj.optString("username", "User")
        val username = obj.optString("username", "user")
        val avatar = obj.optString("profile_image_url").takeIf { !it.isNullOrBlank() } ?: ""
        val isVerified = obj.optInt("is_verified", 0) == 1 || obj.optBoolean("is_verified", false)
        val role = obj.optString("role", "user")

        return StoryViewer(
            id = id,
            storyId = sId,
            userId = uid,
            viewedAt = viewedAt,
            reaction = reaction,
            name = name,
            username = username,
            profileImageUrl = avatar,
            isVerified = isVerified,
            role = role
        )
    }

    private fun parseCommentJson(obj: JSONObject): StoryComment? {
        val id = obj.optLong("id")
        if (id <= 0) return null
        val storyId = obj.optLong("story_id")
        val userId = obj.optLong("user_id")
        val parentId = if (obj.has("parent_id") && !obj.isNull("parent_id")) obj.optLong("parent_id") else null
        val content = obj.optString("content", "")
        val imageUrl = obj.optString("image_url").takeIf { !it.isNullOrBlank() && it != "null" }
        val createdAt = obj.optString("created_at", "Just now")
        val likesCount = obj.optInt("likes_count", 0)
        val likedByMe = obj.optBoolean("liked_by_me", false)
        val repliesCount = obj.optInt("replies_count", 0)
        val name = obj.optString("name").takeIf { !it.isNullOrBlank() } ?: "User $userId"
        val username = obj.optString("username", "user$userId")
        val avatar = obj.optString("profile_image_url").takeIf { !it.isNullOrBlank() } ?: ""
        val isHidden = obj.optInt("is_hidden", 0) == 1 || obj.optBoolean("is_hidden", false)

        return StoryComment(
            id = id,
            storyId = storyId,
            userId = userId,
            parentId = parentId,
            content = content,
            imageUrl = imageUrl,
            createdAt = createdAt,
            likesCount = likesCount,
            likedByMe = likedByMe,
            repliesCount = repliesCount,
            name = name,
            username = username,
            profileImageUrl = avatar,
            isHidden = isHidden
        )
    }
}
