package com.unera.social.data

import com.unera.social.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * VideosApiService - Direct network interactions for native VideosPage parity with web.
 *
 * Implements:
 * 1. GET /api/ads/feeds (Direct fetch for sponsored ads)
 * 2. GET /api/feeds?userId={meId}&limit=50 (Primary video source, fallback GET /api/posts)
 * 3. POST /api/posts/{id}/react (React to feed item: like, love, haha, wow, sad, angry)
 * 4. GET /api/posts/{id}/comments?viewerId={meId} (Fetch post discussions)
 * 5. POST /api/posts/{id}/comments (Submit new comment)
 * 6. POST /api/post-comments/{commentId}/like (Like a comment)
 * 7. PATCH /api/comments/{commentId} (Edit existing comment)
 * 8. DELETE /api/post-comments/{commentId}/delete?user_id={meId} (Delete comment)
 * 9. POST /api/posts/{id}/share (Share post to feed, profile, copy, chat, group)
 * 10. POST /api/user-follows (Follow / unfollow author)
 * 11. GET /api/users?id={userId} (Fetch uncached user profile)
 * 12. POST /api/stories/{id}/view (Record story ring view)
 */
object VideosApiService {
    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * Fetch video posts from primary GET /api/feeds?userId={meId}&limit=50,
     * falling back to GET /api/posts.
     * Filters for media_type == 'video' || type == 'video' || resolvable video URL.
     */
    suspend fun fetchVideos(userId: Long? = null, limit: Int = 50): List<Post> = withContext(Dispatchers.IO) {
        val videoPosts = mutableListOf<Post>()

        // 1. Primary: GET /api/feeds?userId={meId}&limit=50
        try {
            val feedPosts = PostsApiService.fetchFeedPosts(userId = userId, limit = limit)
            val extracted = feedPosts.filter { post ->
                post.postType == "video" ||
                        post.mediaType == "video" ||
                        !post.videoUrl.isNullOrBlank() ||
                        post.mediaUrl?.let { isVideoUrl(it) } == true
            }.map { post ->
                ensureVideoPostContract(post)
            }
            videoPosts.addAll(extracted)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback to GET /api/posts if empty
        if (videoPosts.isEmpty()) {
            try {
                val publicPosts = PostsApiService.fetchPublicPosts()
                val extracted = publicPosts.filter { post ->
                    post.postType == "video" ||
                            post.mediaType == "video" ||
                            !post.videoUrl.isNullOrBlank() ||
                            post.mediaUrl?.let { isVideoUrl(it) } == true
                }.map { post ->
                    ensureVideoPostContract(post)
                }
                videoPosts.addAll(extracted)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        videoPosts
    }

    /**
     * 1. GET /api/ads/feeds
     * Direct fetch for sponsored ads to mix into the video stream.
     */
    suspend fun fetchFeedAds(): List<SponsoredFeedAd> = withContext(Dispatchers.IO) {
        val adsList = mutableListOf<SponsoredFeedAd>()
        try {
            val url = URL("$BASE_URL/api/ads/feeds")
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
                    val arr = root.optJSONArray("ads") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseFeedAd(obj)?.let { adsList.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseFeedAd(obj)?.let { adsList.add(it) }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        adsList
    }

    /**
     * 2. POST /api/posts/{id}/react
     * Body: { type, user_id, post_id }
     * Types: like, love, haha, wow, sad, angry
     */
    suspend fun reactToPost(postId: Long, userId: Long, type: String): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/posts/$postId/react")
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
                put("type", type)
                put("user_id", userId)
                put("post_id", postId)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            var newCount = 0
            if (code in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    newCount = root.optInt("reactions_count", root.optInt("likes_count", 0))
                }
            }
            conn.disconnect()
            Pair(code in 200..299, newCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, 0)
        }
    }

    /**
     * 3. GET /api/posts/{id}/comments?viewerId={meId}
     * Fetch comments sheet data.
     */
    suspend fun fetchComments(postId: Long, viewerId: Long): List<Comment> = withContext(Dispatchers.IO) {
        val commentsList = mutableListOf<Comment>()
        try {
            val url = URL("$BASE_URL/api/posts/$postId/comments?viewerId=$viewerId")
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
                            parseCommentJson(obj, viewerId)?.let { commentsList.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseCommentJson(obj, viewerId)?.let { commentsList.add(it) }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        commentsList
    }

    /**
     * 4. POST /api/posts/{id}/comments
     * Body: { user_id, text, parent_comment_id, image_url }
     */
    suspend fun postComment(
        postId: Long,
        userId: Long,
        text: String,
        parentCommentId: Long? = null,
        imageUrl: String? = null
    ): Comment? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/posts/$postId/comments")
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
                if (parentCommentId != null && parentCommentId > 0) {
                    put("parent_comment_id", parentCommentId)
                }
                if (!imageUrl.isNullOrBlank()) {
                    put("image_url", imageUrl)
                }
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                conn.disconnect()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val commentObj = root.optJSONObject("comment") ?: root.optJSONObject("data") ?: root
                    return@withContext parseCommentJson(commentObj, userId)
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 5. POST /api/post-comments/{commentId}/like
     * Body: { user_id }
     */
    suspend fun likeComment(commentId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/post-comments/$commentId/like")
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
     * 6. PATCH /api/comments/{commentId}
     * Body: { user_id, text, image_url }
     */
    suspend fun editComment(commentId: Long, userId: Long, text: String, imageUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/comments/$commentId")
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
                put("text", text)
                if (!imageUrl.isNullOrBlank()) {
                    put("image_url", imageUrl)
                }
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
     * 7. DELETE /api/post-comments/{commentId}/delete?user_id={meId}
     */
    suspend fun deleteComment(commentId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/post-comments/$commentId/delete?user_id=$userId")
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
     * 8. POST /api/posts/{id}/share
     * Body: { destination, user_id }
     * Destinations: feed, profile, copy, chat, group
     */
    suspend fun sharePost(postId: Long, userId: Long, destination: String = "feed"): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/posts/$postId/share")
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
                put("destination", destination)
                put("user_id", userId)
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
     * 9. POST /api/user-follows
     * Body: { follower_id, following_id }
     */
    suspend fun followUser(followerId: Long, followingId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/user-follows")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", followerId.toString())
            }

            val payload = JSONObject().apply {
                put("follower_id", followerId)
                put("following_id", followingId)
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
     * 10. GET /api/users?id={userId}
     * Fetch user details if not found in local cache.
     */
    suspend fun fetchUser(userId: Long): User? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/users?id=$userId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                conn.disconnect()
                if (responseText.startsWith("{")) {
                    val obj = JSONObject(responseText)
                    val id = obj.optLong("id", userId)
                    val name = obj.optString("name").takeIf { it.isNotBlank() && it != "null" }
                        ?: obj.optString("username").takeIf { it.isNotBlank() && it != "null" }
                        ?: "User $id"
                    val username = obj.optString("username").takeIf { it.isNotBlank() && it != "null" } ?: "@user$id"
                    val avatar = obj.optString("profile_image_url").takeIf { it.isNotBlank() && it != "null" }
                        ?: obj.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
                        ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800"
                    val isVerified = obj.optBoolean("is_verified", false) || obj.optInt("is_verified", 0) == 1
                    val role = obj.optString("role", "user")

                    return@withContext User(
                        id = id,
                        name = name,
                        username = username,
                        profileImageUrl = avatar,
                        isVerified = isVerified,
                        role = role
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
     * 11. POST /api/stories/{id}/view
     * Record a view on author's story ring.
     */
    suspend fun recordStoryView(storyId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext StoriesApiService.recordView(storyId, userId)
    }

    // Helper functions

    private fun isVideoUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val clean = url.substringBefore("?").substringBefore("#").trim().lowercase()
        val lower = url.lowercase()
        return clean.endsWith(".mp4") || clean.endsWith(".mov") || clean.endsWith(".webm") ||
                clean.endsWith(".m4v") || clean.endsWith(".ogg") || clean.endsWith(".m3u8") ||
                clean.endsWith(".mkv") ||
                lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".webm") ||
                lower.contains(".m4v") || lower.contains(".m3u8") ||
                lower.contains("/videos/") || lower.contains("/video/") ||
                lower.contains("reel") || lower.contains("1080p") || lower.contains("720p") || lower.contains("original")
    }

    private fun ensureVideoPostContract(post: Post): Post {
        val videoUrl = post.videoUrl?.takeIf { it.isNotBlank() }
            ?: post.getDefaultPlayableVideoUrl().takeIf { it.isNotBlank() }
            ?: post.mediaUrl ?: ""
        val resolvedMeta = if (post.mediaMeta.isNotEmpty()) {
            post.mediaMeta
        } else if (videoUrl.isNotBlank()) {
            listOf(
                MediaMetaItem(
                    feed = videoUrl,
                    full = post.getHdPlayableVideoUrl().ifBlank { videoUrl },
                    resolution720 = videoUrl,
                    resolution1080 = post.getHdPlayableVideoUrl().ifBlank { videoUrl },
                    thumb = post.getPosterUrl(),
                    thumbnailUrl = post.getPosterUrl(),
                    type = "video"
                )
            )
        } else {
            emptyList()
        }
        return post.copy(
            postType = "video",
            mediaType = "video",
            videoUrl = videoUrl,
            mediaUrl = videoUrl,
            mediaUrls = if (post.mediaUrls.isNotEmpty()) post.mediaUrls else if (videoUrl.isNotBlank()) listOf(videoUrl) else emptyList(),
            mediaMeta = resolvedMeta,
            userId = post.userId.takeIf { it > 0 } ?: post.author.id,
            createdAt = post.createdAt ?: post.timestamp
        )
    }

    private fun parseFeedAd(obj: JSONObject): SponsoredFeedAd? {
        val id = obj.optLong("id", obj.optLong("ad_id", System.currentTimeMillis()))
        val title = obj.optString("title").takeIf { it != "null" } ?: ""
        val headline = obj.optString("headline").takeIf { it != "null" }
            ?: obj.optString("caption").takeIf { it != "null" }
            ?: title
        val description = obj.optString("description").takeIf { it != "null" } ?: ""
        val imageUrl = obj.optString("image_url").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("media_url").takeIf { it.isNotBlank() && it != "null" }
        val videoUrl = obj.optString("video_url").takeIf { it.isNotBlank() && it != "null" }
        val ctaText = obj.optString("cta_text").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("cta").takeIf { it.isNotBlank() && it != "null" }
            ?: "Learn More"
        val ctaUrl = obj.optString("cta_url").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("link").takeIf { it.isNotBlank() && it != "null" }
            ?: ""
        val sponsorName = obj.optString("brand_name").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("sponsor_name").takeIf { it.isNotBlank() && it != "null" }
            ?: "Sponsored"
        val sponsorAvatar = obj.optString("brand_logo").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("sponsor_avatar").takeIf { it.isNotBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1572021335469-31706a17aaef?w=400"
        val location = obj.optString("location", "Global")
        val brandId = if (obj.has("brand_id") && !obj.isNull("brand_id")) obj.optLong("brand_id") else null
        val isVideo = videoUrl != null || obj.optString("type") == "video" || obj.optString("media_type") == "video"

        return SponsoredFeedAd(
            id = id,
            title = title,
            headline = headline,
            description = description,
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            ctaText = ctaText,
            ctaUrl = ctaUrl,
            sponsorName = sponsorName,
            sponsorAvatar = sponsorAvatar,
            location = location,
            brandId = brandId,
            isVideo = isVideo
        )
    }

    private fun parseCommentJson(obj: JSONObject, viewerId: Long): Comment? {
        val id = obj.optLong("id", obj.optLong("comment_id", System.currentTimeMillis()))
        val text = obj.optString("text").takeIf { it != "null" }
            ?: obj.optString("content").takeIf { it != "null" }
            ?: ""
        val userId = obj.optLong("user_id", 1L)
        val userObj = obj.optJSONObject("user") ?: obj.optJSONObject("author")

        val authorName = userObj?.optString("name")?.takeIf { it.isNotBlank() }
            ?: userObj?.optString("username")?.takeIf { it.isNotBlank() }
            ?: obj.optString("author_name").takeIf { it.isNotBlank() && it != "null" }
            ?: if (userId == 1L) "Jastin Beda" else "User $userId"

        val username = userObj?.optString("username")?.takeIf { it.isNotBlank() }
            ?: "@${authorName.lowercase().replace(" ", "")}"

        val avatar = userObj?.optString("profile_image_url")?.takeIf { it.isNotBlank() && it != "null" }
            ?: userObj?.optString("avatar_url")?.takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("author_avatar").takeIf { it.isNotBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800"

        val isVerified = userObj?.optBoolean("is_verified", false) ?: (userId == 1L)
        val author = User(
            id = userId,
            name = authorName,
            username = username,
            profileImageUrl = avatar,
            isVerified = isVerified
        )

        val rawDate = obj.optString("created_at").takeIf { it.isNotBlank() && it != "null" }
        val timestamp = rawDate ?: "Just now"
        val likesCount = obj.optInt("likes_count", obj.optInt("likes", 0))
        val isLiked = obj.optBoolean("is_liked", false) || obj.optInt("liked_by_me", 0) == 1
        val imageUrl = obj.optString("image_url").takeIf { it.isNotBlank() && it != "null" }
        val parentCommentId = if (obj.has("parent_comment_id") && !obj.isNull("parent_comment_id")) {
            obj.optLong("parent_comment_id")
        } else null

        return Comment(
            id = id,
            author = author,
            text = text,
            timestamp = timestamp,
            likesCount = likesCount,
            isLiked = isLiked,
            imageUrl = imageUrl,
            parentCommentId = parentCommentId
        )
    }
}
