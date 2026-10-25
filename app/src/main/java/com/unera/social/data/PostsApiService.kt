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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object PostsApiService {
    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun isValidMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (trimmed == "0" || trimmed == "1" || trimmed == "null" || trimmed == "undefined" || trimmed == "false") return false
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("file://")
    }

    fun isValidVideoUrl(url: String?): Boolean {
        if (!isValidMediaUrl(url)) return false
        val u = url!!.trim().lowercase()
        return u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".mov") || u.endsWith(".m4v") ||
                u.endsWith(".mkv") || u.contains("/video/") || u.contains("video_") || u.contains(".mp4?")
    }

    fun isImageUrl(url: String?): Boolean {
        if (!isValidMediaUrl(url)) return false
        val u = url!!.trim().lowercase()
        return u.endsWith(".jpg") || u.endsWith(".jpeg") || u.endsWith(".png") || u.endsWith(".webp") ||
                u.endsWith(".gif") || u.contains("/image/") || u.contains("image_") || u.contains(".jpg?")
    }

    /**
     * Fetch feed posts from api/feeds (used when user is logged in)
     */
    suspend fun fetchFeedPosts(userId: Long? = null, limit: Int = 50): List<Post> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Post>()
        try {
            val url = if (userId != null && userId > 0) {
                URL("$BASE_URL/api/feeds?userId=$userId&limit=$limit")
            } else {
                URL("$BASE_URL/api/feeds?limit=$limit")
            }
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                if (userId != null && userId > 0) {
                    setRequestProperty("x-user-id", userId.toString())
                }
            }

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.use { it.readText() }.trim()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val feedArray = root.optJSONArray("feed") ?: root.optJSONArray("data")
                    if (feedArray != null) {
                        for (i in 0 until feedArray.length()) {
                            val obj = feedArray.optJSONObject(i) ?: continue
                            parseFeedItemToPost(obj)?.let { result.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseFeedItemToPost(obj)?.let { result.add(it) }
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
     * Fetch public posts from api/posts (used for non-logged-in users)
     */
    suspend fun fetchPublicPosts(): List<Post> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Post>()
        try {
            val url = URL("$BASE_URL/api/posts")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.use { it.readText() }.trim()
                if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parsePostJson(obj)?.let { result.add(it) }
                    }
                } else if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val arr = root.optJSONArray("posts") ?: root.optJSONArray("data") ?: root.optJSONArray("feed")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parsePostJson(obj)?.let { result.add(it) }
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
     * Fetch user posts from api/post/by-user or api/posts/by-user (used for user profiles)
     */
    suspend fun fetchUserPosts(userId: Long, limit: Int = 50): List<Post> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Post>()
        try {
            val url = URL("$BASE_URL/api/posts/by-user?user_id=$userId&limit=$limit")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.use { it.readText() }.trim()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val postsArray = root.optJSONArray("posts") ?: root.optJSONArray("data") ?: root.optJSONArray("feed")
                    if (postsArray != null) {
                        for (i in 0 until postsArray.length()) {
                            val obj = postsArray.optJSONObject(i) ?: continue
                            parseFeedItemToPost(obj)?.let { result.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseFeedItemToPost(obj)?.let { result.add(it) }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to /api/posts?user_id=$userId if empty
        if (result.isEmpty()) {
            try {
                val url = URL("$BASE_URL/api/posts?user_id=$userId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                if (conn.responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseText = reader.use { it.readText() }.trim()
                    if (responseText.startsWith("[")) {
                        val arr = JSONArray(responseText)
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parsePostJson(obj)?.let { result.add(it) }
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        result
    }

    /**
     * Create post via POST /api/posts — single endpoint for text, images, and videos
     */
    suspend fun createPost(
        userId: Long,
        content: String,
        mediaUrl: String? = null,
        mediaUrls: List<String> = emptyList(),
        mediaType: String? = null,
        mediaTypes: List<String> = emptyList(),
        videoUrl: String? = null,
        thumbnailUrl: String? = null,
        mediaMeta: List<MediaMetaItem> = emptyList(),
        feeling: String? = null,
        location: String? = null,
        background: String? = null,
        taggedUsers: List<Long> = emptyList(),
        type: String? = null,
        visibility: String = "public"
    ): Post? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/posts")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            val body = JSONObject().apply {
                put("user_id", userId)
                put("content", content.trim())
                if (!mediaUrl.isNullOrBlank()) put("media_url", mediaUrl)
                if (mediaUrls.isNotEmpty()) {
                    put("media_urls", JSONArray(mediaUrls))
                }
                if (!mediaType.isNullOrBlank()) put("media_type", mediaType)
                if (mediaTypes.isNotEmpty()) {
                    put("media_types", JSONArray(mediaTypes))
                }
                if (!videoUrl.isNullOrBlank()) put("video_url", videoUrl)
                val resolvedThumb = thumbnailUrl ?: mediaMeta.firstOrNull()?.thumbnailUrl ?: mediaMeta.firstOrNull()?.thumb
                if (!resolvedThumb.isNullOrBlank()) {
                    put("thumbnail_url", resolvedThumb)
                    put("thumb_url", resolvedThumb)
                    put("thumb", resolvedThumb)
                }
                if (mediaMeta.isNotEmpty()) {
                    val metaArray = JSONArray()
                    for (item in mediaMeta) {
                        val mObj = JSONObject().apply {
                            if (!item.original.isNullOrBlank()) put("original", item.original)
                            if (!item.resolution1080.isNullOrBlank()) put("1080", item.resolution1080)
                            if (!item.resolution720.isNullOrBlank()) put("720", item.resolution720)
                            val th = item.thumbnailUrl ?: item.thumb
                            if (!th.isNullOrBlank()) {
                                put("thumbnail_url", th)
                                put("thumb", th)
                            }
                            if (!item.feed.isNullOrBlank()) put("feed", item.feed)
                            if (!item.full.isNullOrBlank()) put("full", item.full)
                            val itemType = if (!item.type.isNullOrBlank()) item.type else (if (mediaType == "video") "video" else "image")
                            put("type", itemType)
                            if (item.width != null) put("width", item.width)
                            if (item.height != null) put("height", item.height)
                            if (item.duration != null) put("duration", item.duration)
                        }
                        metaArray.put(mObj)
                    }
                    put("media_meta", metaArray)
                }
                val postType = type ?: (if (mediaType == "video") "video" else if (mediaType == "image") "image" else "text")
                put("type", postType)
                put("feed_key", "post:${System.currentTimeMillis()}")
                put("visibility", visibility)
                if (!feeling.isNullOrBlank()) put("feeling", feeling)
                if (!location.isNullOrBlank()) put("location", location)
                if (!background.isNullOrBlank()) {
                    put("background", background)
                    put("colours", background)
                    put("background_style", background)
                }
                if (taggedUsers.isNotEmpty()) {
                    put("tagged_users", JSONArray(taggedUsers))
                }
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                if (text.startsWith("{")) {
                    val root = JSONObject(text)
                    val postObj = root.optJSONObject("post") ?: root
                    return@withContext parseFeedItemToPost(postObj)
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun parseFeedItemToPost(obj: JSONObject): Post? {
        if (obj.optInt("is_deleted", 0) == 1) return null

        val id = obj.optLong("id", obj.optLong("post_id", System.currentTimeMillis()))
        val userId = obj.optLong("user_id", obj.optLong("owner_id", 1L))
        val authorObj = obj.optJSONObject("author") ?: obj.optJSONObject("user")

        val authorName = authorObj?.optString("name")?.takeIf { it.isNotBlank() }
            ?: obj.optString("name").takeIf { it.isNotBlank() && it != "null" }
            ?: authorObj?.optString("username")?.takeIf { it.isNotBlank() }
            ?: obj.optString("username").takeIf { it.isNotBlank() && it != "null" }
            ?: if (userId == 1L) "Jastin Beda" else "User $userId"

        val username = authorObj?.optString("username")?.takeIf { it.isNotBlank() }
            ?: obj.optString("username").takeIf { it.isNotBlank() && it != "null" }
            ?: "@${authorName.lowercase().replace(" ", "")}"

        val avatar = authorObj?.optString("profile_image_url")?.takeIf { it.isNotBlank() && it != "null" }
            ?: authorObj?.optString("avatar_url")?.takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("profile_image_url").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800"

        val isVerified = authorObj?.optBoolean("is_verified", false)
            ?: obj.optBoolean("is_verified", obj.optBoolean("verified", false))
        val role = authorObj?.optString("role") ?: obj.optString("role", "user")

        val author = User(
            id = userId,
            name = authorName,
            username = username,
            profileImageUrl = avatar,
            isVerified = isVerified,
            role = role
        )

        val content = obj.optString("content").takeIf { it != "null" }
            ?: obj.optString("description").takeIf { it != "null" }
            ?: ""

        val mediaUrls = extractMediaUrls(obj)
        val mediaTypes = extractMediaTypes(obj)
        val mediaMeta = extractMediaMeta(obj)
        val mediaUrl = obj.optString("media_url").takeIf { it.isNotBlank() && it != "null" }
            ?: mediaUrls.firstOrNull()

        val backgroundStyle = obj.optString("colours").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("background_style").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("background").takeIf { it.isNotBlank() && it != "null" }

        val groupId = if (obj.has("group_id") && !obj.isNull("group_id") && obj.optLong("group_id") > 0) {
            obj.optLong("group_id")
        } else null

        val groupName = obj.optString("group_name").takeIf { it.isNotBlank() && it != "null" }
        val groupImage = obj.optString("group_image").takeIf { it.isNotBlank() && it != "null" }

        val rawDate = obj.optString("created_at").takeIf { it.isNotBlank() && it != "null" }
        val timestamp = formatTimestamp(rawDate)

        val likesCount = obj.optInt("reactions_count", obj.optInt("likesCount", obj.optInt("likes_count", 0)))
        val commentsCount = obj.optInt("comments_count", obj.optInt("commentsCount", obj.optInt("reviews_count", 0)))
        val sharesCount = obj.optInt("shares", obj.optInt("shares_count", 0))

        val source = obj.optString("source").takeIf { it.isNotBlank() && it != "null" } ?: ""
        val itemType = obj.optString("item_type").takeIf { it.isNotBlank() && it != "null" } ?: ""
        val postTypeRaw = obj.optString("post_type").takeIf { it.isNotBlank() && it != "null" } ?: ""
        val feedKey = obj.optString("feed_key", "")

        val metaObj = obj.optJSONObject("meta")
        val metaKind = metaObj?.optString("kind", "")?.takeIf { it.isNotBlank() }
            ?: metaObj?.optString("type", "")?.takeIf { it.isNotBlank() }
            ?: ""

        // Strict source classification: posts must NEVER be classified as events, products, or songs
        val isSourcePost = source == "post" || itemType == "post" || feedKey.startsWith("post:") || feedKey.startsWith("ad_post:")

        // 1. EVENT DETECTION: only true if item is genuinely an event from feeds.ts
        val isEvent = !isSourcePost && (
            source == "event" || itemType == "event" || postTypeRaw == "event" ||
            feedKey.startsWith("event:") || metaKind == "event" ||
            (!obj.isNull("event_id") && obj.optLong("event_id", 0L) > 0)
        )

        val event = if (isEvent) {
            val evObj = metaObj?.optJSONObject("event") ?: obj.optJSONObject("event") ?: obj
            val evId = evObj.optLong("id", obj.optLong("event_id", obj.optLong("id", id)))
            val evTitle = evObj.optString("title").ifBlank { obj.optString("title", content) }
            val evDate = evObj.optString("event_date").ifBlank {
                evObj.optString("date", obj.optString("event_date", obj.optString("date", "Oct 24, 2026")))
            }
            val evLoc = evObj.optString("location", obj.optString("location", "Tanzania"))
            val evImg = evObj.optString("cover_url").ifBlank {
                evObj.optString("image", obj.optString("cover_url", mediaUrl ?: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1500&q=80"))
            }
            val evDesc = evObj.optString("description").ifBlank {
                obj.optString("event_description", obj.optString("description", content))
            }
            val evInterested = obj.optInt("interested_count", evObj.optInt("interested_count", 0))
            val evGoing = obj.optInt("attending_count", evObj.optInt("going_count", 0))
            val myRsvp = obj.optString("my_rsvp_status", "")

            Event(
                id = evId,
                title = evTitle,
                date = evDate,
                location = evLoc,
                imageUrl = evImg,
                description = evDesc,
                interestedCount = evInterested,
                goingCount = evGoing,
                isGoing = myRsvp == "going",
                isInterested = myRsvp == "interested",
                organizer = author
            )
        } else null

        // 2. PRODUCT DETECTION: only true if item is genuinely a product from feeds.ts
        val isProduct = !isSourcePost && !isEvent && (
            source == "product" || itemType == "product" || postTypeRaw == "product" || postTypeRaw == "marketplace" ||
            feedKey.startsWith("product:") || metaKind == "product" ||
            (!obj.isNull("product_id2") && obj.optLong("product_id2", 0L) > 0) ||
            (!obj.isNull("product_id") && obj.optLong("product_id", 0L) > 0)
        )

        val product = if (isProduct) {
            val pObj = metaObj?.optJSONObject("marketplace") ?: metaObj?.optJSONObject("product") ?: obj.optJSONObject("product") ?: obj
            val pId = pObj.optLong("product_id", pObj.optLong("id", obj.optLong("id", id)))
            val pTitle = pObj.optString("title").ifBlank { obj.optString("title", content) }
            val pMainPrice = pObj.optDouble("main_price", pObj.optDouble("price", obj.optDouble("main_price", 0.0)))
            val pDiscPrice = if (pObj.has("discount_price") && !pObj.isNull("discount_price")) {
                pObj.optDouble("discount_price", 0.0).takeIf { it > 0 }
            } else null
            val pLoc = pObj.optString("location", pObj.optString("address", obj.optString("location", "Tanzania")))
            val pDesc = pObj.optString("description", obj.optString("description", content))
            val pImages = extractMediaUrls(pObj).ifEmpty { mediaUrls }
            val pPrimaryImg = pImages.firstOrNull() ?: mediaUrl ?: "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800"

            Product(
                id = pId,
                title = pTitle,
                price = pMainPrice,
                discountPrice = pDiscPrice,
                currency = pObj.optString("currency", "TZS"),
                category = pObj.optString("category", "General"),
                location = pLoc,
                country = pObj.optString("country", "TZ"),
                imageUrl = pPrimaryImg,
                images = pImages,
                description = pDesc,
                seller = author,
                views = obj.optInt("views", 0),
                likesCount = likesCount,
                commentsCount = commentsCount,
                sharesCount = sharesCount,
                myReaction = obj.optString("my_reaction").takeIf { it.isNotBlank() && it != "null" },
                isLiked = obj.optBoolean("is_liked", false)
            )
        } else null

        // 3. MUSIC / SONG DETECTION
        val isMusic = !isSourcePost && !isEvent && !isProduct && (
            source == "song" || itemType == "song" || postTypeRaw == "song" || postTypeRaw == "music" ||
            feedKey.startsWith("song:") || feedKey.startsWith("music:") || metaKind == "music" ||
            (!obj.isNull("song_id2") && obj.optLong("song_id2", 0L) > 0)
        )

        val song = if (isMusic) {
            val sObj = metaObj?.optJSONObject("song") ?: obj.optJSONObject("song") ?: obj
            Song(
                id = sObj.optString("id", obj.optString("song_id2", id.toString())),
                title = sObj.optString("title", obj.optString("song_title", "Audio Track")),
                artist = sObj.optString("artist_name", obj.optString("song_artist_name", author.name)),
                album = sObj.optString("album_name", obj.optString("song_album_name", "Single")),
                cover = sObj.optString("cover_image_url", obj.optString("song_cover_image_url", mediaUrl ?: "https://media.unera.social/task_01kftb3024ed7bm84gy6j485fh_1769336848_img_0.webp")),
                audioUrl = sObj.optString("audio_url", ""),
                durationSeconds = sObj.optInt("duration_seconds", obj.optInt("song_duration_seconds", 204)),
                genre = sObj.optString("genre", obj.optString("song_genre", "Afrobeats"))
            )
        } else null

        // 4. STORY DETECTION
        val isStory = !isSourcePost && (
            source == "story" || itemType == "story" || postTypeRaw == "story" ||
            feedKey.startsWith("story:") || metaKind == "story"
        )
        val story = if (isStory) {
            val stObj = obj.optJSONObject("story") ?: obj
            val stId = stObj.optLong("story_id", stObj.optLong("id", id))
            val stType = stObj.optString("type", if (mediaUrl?.contains("mp4") == true) "video" else "image")
            Story(
                id = stId,
                author = author,
                mediaUrl = mediaUrl ?: "",
                type = stType,
                textContent = content,
                backgroundStyle = backgroundStyle ?: "linear-gradient(45deg, #1877F2, #0055FF)",
                viewsCount = obj.optInt("views_count", obj.optInt("views", 0)),
                reactionsCount = likesCount,
                commentsCount = commentsCount,
                sharesCount = sharesCount
            )
        } else null

        // 5. SHARED ITEM DETECTION: product_share, event_share, song_share, story_share, share
        val isProductShare = source == "product_share" || itemType == "product_share" ||
                feedKey.startsWith("product_share:") || (!obj.isNull("shared_product") && obj.has("shared_product"))

        val isEventShare = source == "event_share" || itemType == "event_share" ||
                feedKey.startsWith("event_share:") || (!obj.isNull("shared_event") && obj.has("shared_event"))

        val isSongShare = source == "song_share" || itemType == "song_share" ||
                feedKey.startsWith("song_share:") || (!obj.isNull("shared_song") && obj.has("shared_song"))

        val isStoryShare = source == "story_share" || itemType == "story_share" ||
                feedKey.startsWith("story_share:") || (!obj.isNull("shared_story") && obj.has("shared_story"))

        val isPostShare = source == "share" || itemType == "share" || feedKey.startsWith("share:") ||
                (!obj.isNull("shared_post") && obj.has("shared_post") && !isProductShare && !isEventShare && !isSongShare && !isStoryShare)

        val sharedPost: Post? = when {
            isProductShare -> {
                val spObj = obj.optJSONObject("shared_product") ?: obj.optJSONObject("shared_post")
                if (spObj != null) {
                    val p = MarketplaceApiService.parseProductJson(spObj) ?: Product(
                        id = spObj.optLong("product_id", spObj.optLong("id", 0L)),
                        title = spObj.optString("title", "Marketplace Item"),
                        price = spObj.optDouble("price", spObj.optDouble("main_price", 0.0)),
                        imageUrl = spObj.optString("image_url", spObj.optString("imageUrl", "")),
                        category = spObj.optString("category", "General"),
                        location = spObj.optString("location", spObj.optString("address", "Tanzania")),
                        description = spObj.optString("description", ""),
                        seller = author
                    )
                    Post(
                        id = p.id,
                        author = p.seller,
                        content = p.description.ifBlank { p.title },
                        product = p,
                        postType = "product",
                        timestamp = timestamp
                    )
                } else null
            }
            isEventShare -> {
                val seObj = obj.optJSONObject("shared_event") ?: obj.optJSONObject("shared_post")
                if (seObj != null) {
                    val ev = Event(
                        id = seObj.optLong("id", 0L),
                        title = seObj.optString("title", "Event"),
                        date = seObj.optString("event_date", "Oct 24, 2026"),
                        location = seObj.optString("location", "Tanzania"),
                        imageUrl = seObj.optString("cover_url", ""),
                        description = seObj.optString("description", ""),
                        organizer = author
                    )
                    Post(
                        id = ev.id,
                        author = author,
                        content = ev.title,
                        event = ev,
                        postType = "event",
                        timestamp = timestamp
                    )
                } else null
            }
            isSongShare -> {
                val ssObj = obj.optJSONObject("shared_song") ?: obj.optJSONObject("shared_post")
                if (ssObj != null) {
                    val s = Song(
                        id = ssObj.optString("id", "0"),
                        title = ssObj.optString("title", "Track"),
                        artist = ssObj.optString("artist_name", author.name),
                        cover = ssObj.optString("cover_image_url", ""),
                        audioUrl = ssObj.optString("audio_url", ""),
                        durationSeconds = ssObj.optInt("duration_seconds", 204),
                        genre = ssObj.optString("genre", "Afrobeats")
                    )
                    Post(
                        id = s.id.toLongOrNull() ?: 0L,
                        author = author,
                        content = s.title,
                        song = s,
                        postType = "music",
                        timestamp = timestamp
                    )
                } else null
            }
            isStoryShare -> {
                val stObj = obj.optJSONObject("shared_story") ?: obj.optJSONObject("shared_post")
                if (stObj != null) {
                    val st = Story(
                        id = stObj.optLong("id", stObj.optLong("story_id", 0L)),
                        author = author,
                        mediaUrl = stObj.optString("media_url", ""),
                        type = stObj.optString("type", "image"),
                        textContent = stObj.optString("text_content", ""),
                        backgroundStyle = stObj.optString("background_style", "")
                    )
                    Post(
                        id = st.id,
                        author = author,
                        content = st.textContent,
                        story = st,
                        postType = "story",
                        timestamp = timestamp
                    )
                } else null
            }
            isPostShare -> {
                val spObj = obj.optJSONObject("shared_post") ?: obj.optJSONObject("sharedPost")
                spObj?.let { parseFeedItemToPost(it) }
            }
            else -> {
                val spObj = obj.optJSONObject("shared_post") ?: obj.optJSONObject("sharedPost")
                spObj?.let { parseFeedItemToPost(it) }
            }
        }

        // Sponsored / Ad detection from feeds.ts
        val sponsoredMeta = obj.optJSONObject("sponsored_meta") ?: metaObj?.optJSONObject("sponsored_meta")
        val isSponsored = feedKey.startsWith("ad_post:") ||
                obj.optBoolean("is_sponsored", false) || obj.optBoolean("sponsored", false) ||
                metaObj?.optInt("is_sponsored", 0) == 1 || sponsoredMeta != null
        val sponsoredHeadline = sponsoredMeta?.optString("headline") ?: obj.optString("headline", "")
        val sponsoredCtaText = sponsoredMeta?.optString("cta_text") ?: obj.optString("cta_text", "Learn More")
        val sponsoredCtaUrl = sponsoredMeta?.optString("cta_url") ?: obj.optString("cta_url", "")

        // Reactions list
        val reactionsList = mutableListOf<UserReaction>()
        val reactionsArr = obj.optJSONArray("reactions") ?: obj.optJSONArray("reactions_preview")
        if (reactionsArr != null) {
            for (i in 0 until reactionsArr.length()) {
                val rObj = reactionsArr.optJSONObject(i) ?: continue
                val rUid = rObj.optLong("user_id", rObj.optLong("userId", 0L))
                val rType = rObj.optString("type", "like")
                val rName = rObj.optString("name", rObj.optString("username", ""))
                reactionsList.add(UserReaction(userId = rUid, userName = rName, type = rType))
            }
        }

        val feedUrl = obj.optString("feed_url").takeIf { isValidMediaUrl(it) }
        val fullUrl = obj.optString("full_url").takeIf { isValidMediaUrl(it) }
        val thumbUrl = obj.optString("thumb_url").takeIf { isValidMediaUrl(it) }

        val mediaTypeRaw = obj.optString("media_type").takeIf { it.isNotBlank() && it != "null" }
        val typeRaw = obj.optString("type").takeIf { it.isNotBlank() && it != "null" }
        val rawVideo = obj.optString("video_url").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("video").takeIf { it.isNotBlank() && it != "null" }
        val videoUrlRaw = rawVideo?.takeIf { isValidVideoUrl(it) }

        val isVideoExt = !mediaUrl.isNullOrBlank() && isValidVideoUrl(mediaUrl)
        val isImageExt = !mediaUrl.isNullOrBlank() && isImageUrl(mediaUrl)

        val isGroupPost = (groupId != null && groupId > 0) || source == "group_post" || itemType == "group_post"

        // Strict classification matching feeds.ts getFeedItemType:
        // Group posts, marketplace, event, music, story, and share cards are never classified as standalone videos!
        val isVideo = !isEvent && !isProduct && !isMusic && !isGroupPost && !isStory &&
                !isProductShare && !isEventShare && !isSongShare && !isStoryShare && (
            (mediaTypeRaw == "video" && (videoUrlRaw != null || isVideoExt || (!isImageExt && mediaUrls.isEmpty()))) ||
            (typeRaw == "video" && (videoUrlRaw != null || isVideoExt || (!isImageExt && mediaUrls.isEmpty()))) ||
            postTypeRaw == "video" ||
            postTypeRaw == "reel" ||
            videoUrlRaw != null ||
            (isVideoExt && !isImageExt) ||
            (mediaMeta.firstOrNull()?.type == "video" && isValidVideoUrl(mediaMeta.firstOrNull()?.resolution720 ?: mediaMeta.firstOrNull()?.original))
        )

        val resolvedMediaMeta = if (mediaMeta.isNotEmpty()) {
            mediaMeta
        } else if (feedUrl != null || fullUrl != null || thumbUrl != null) {
            listOf(
                MediaMetaItem(
                    feed = feedUrl,
                    full = fullUrl,
                    resolution720 = feedUrl,
                    resolution1080 = fullUrl,
                    thumb = thumbUrl,
                    thumbnailUrl = thumbUrl,
                    type = if (isVideo) "video" else "image"
                )
            )
        } else {
            emptyList()
        }

        val resolvedMediaUrls = if (mediaUrls.isNotEmpty()) {
            mediaUrls.filter { isValidMediaUrl(it) }
        } else {
            listOfNotNull(fullUrl, feedUrl, thumbUrl).filter { isValidMediaUrl(it) }
        }

        val finalPostType = when {
            isProductShare -> "product_share"
            isEventShare -> "event_share"
            isSongShare -> "song_share"
            isStoryShare -> "story_share"
            isPostShare || sharedPost != null -> "shared_post"
            isEvent -> "event"
            isProduct -> "product"
            isMusic -> "music"
            isStory -> "story"
            isGroupPost -> "group_post"
            isSponsored -> "sponsored"
            isVideo -> "video"
            else -> "post"
        }
        val finalVideoUrl = videoUrlRaw ?: if (isVideo && isVideoExt) mediaUrl else null
        val finalMediaType = if (isVideo) "video" else if (mediaTypeRaw != null && mediaTypeRaw != "video") mediaTypeRaw else if (resolvedMediaUrls.isNotEmpty()) "image" else null
        val myReaction = obj.optString("my_reaction").takeIf { it.isNotBlank() && it != "null" }
        val isLiked = myReaction != null || obj.optBoolean("is_liked", false) || obj.optInt("liked_by_me", 0) == 1

        return Post(
            id = id,
            author = author,
            content = content,
            mediaUrl = mediaUrl ?: feedUrl ?: fullUrl,
            mediaUrls = resolvedMediaUrls,
            mediaTypes = mediaTypes,
            mediaMeta = resolvedMediaMeta,
            timestamp = timestamp,
            likesCount = likesCount,
            commentsCount = commentsCount,
            sharesCount = sharesCount,
            isLiked = isLiked,
            isSaved = false,
            myReaction = myReaction,
            reactions = reactionsList,
            backgroundStyle = backgroundStyle,
            groupId = groupId,
            groupName = groupName,
            groupImageUrl = groupImage,
            postType = finalPostType,
            product = product,
            event = event,
            song = song,
            story = story,
            sharedPost = sharedPost,
            isSponsored = isSponsored,
            sponsoredHeadline = sponsoredHeadline,
            sponsoredCtaText = sponsoredCtaText,
            sponsoredCtaUrl = sponsoredCtaUrl,
            videoUrl = finalVideoUrl,
            mediaType = finalMediaType,
            createdAt = rawDate,
            userId = userId
        )
    }

    private fun parsePostJson(obj: JSONObject): Post? {
        if (obj.optInt("is_deleted", 0) == 1) return null

        val id = obj.optLong("id", System.currentTimeMillis())
        val userId = obj.optLong("user_id", 1L)

        val authorName = if (userId == 1L) "Jastin Beda" else "User $userId"
        val username = if (userId == 1L) "Jastin Beda" else "@user$userId"
        val avatar = if (userId == 1L) {
            "https://media.featheredsocial.site/uploads/images/1791201400713-120d7c17f3d2e8_original.jpg?t=1791201404884"
        } else {
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800"
        }

        val author = User(
            id = userId,
            name = authorName,
            username = username,
            profileImageUrl = avatar,
            isVerified = userId == 1L,
            role = if (userId == 1L) "admin" else "user"
        )

        val content = obj.optString("content").takeIf { it != "null" } ?: ""
        val mediaUrls = extractMediaUrls(obj)
        val mediaTypes = extractMediaTypes(obj)
        val mediaMeta = extractMediaMeta(obj)
        val mediaUrl = obj.optString("media_url").takeIf { it.isNotBlank() && it != "null" }
            ?: mediaUrls.firstOrNull()

        val backgroundStyle = obj.optString("colours").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("background_style").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("background").takeIf { it.isNotBlank() && it != "null" }

        val groupId = if (obj.has("group_id") && !obj.isNull("group_id") && obj.optLong("group_id") > 0) {
            obj.optLong("group_id")
        } else null

        val rawDate = obj.optString("created_at").takeIf { it.isNotBlank() && it != "null" }
        val timestamp = formatTimestamp(rawDate)

        val sharesCount = obj.optInt("shares", obj.optInt("shares_count", 0))

        val feedUrl = obj.optString("feed_url").takeIf { isValidMediaUrl(it) }
        val fullUrl = obj.optString("full_url").takeIf { isValidMediaUrl(it) }
        val thumbUrl = obj.optString("thumb_url").takeIf { isValidMediaUrl(it) }

        val mediaTypeRaw = obj.optString("media_type").takeIf { it.isNotBlank() && it != "null" }
        val typeRaw = obj.optString("type").takeIf { it.isNotBlank() && it != "null" }
        val rawVideo = obj.optString("video_url").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("video").takeIf { it.isNotBlank() && it != "null" }
        val videoUrlRaw = rawVideo?.takeIf { isValidVideoUrl(it) }

        val isVideoExt = !mediaUrl.isNullOrBlank() && isValidVideoUrl(mediaUrl)
        val isImageExt = !mediaUrl.isNullOrBlank() && isImageUrl(mediaUrl)

        val isVideo = (videoUrlRaw != null) ||
                (isVideoExt && !isImageExt) ||
                (mediaTypeRaw == "video" && (videoUrlRaw != null || isVideoExt || (!isImageExt && mediaUrls.isEmpty()))) ||
                (typeRaw == "video" && (videoUrlRaw != null || isVideoExt || (!isImageExt && mediaUrls.isEmpty()))) ||
                (mediaMeta.firstOrNull()?.type == "video" && isValidVideoUrl(mediaMeta.firstOrNull()?.resolution720 ?: mediaMeta.firstOrNull()?.original))

        val resolvedMediaMeta = if (mediaMeta.isNotEmpty()) {
            mediaMeta
        } else if (feedUrl != null || fullUrl != null || thumbUrl != null) {
            listOf(
                MediaMetaItem(
                    feed = feedUrl,
                    full = fullUrl,
                    resolution720 = feedUrl,
                    resolution1080 = fullUrl,
                    thumb = thumbUrl,
                    thumbnailUrl = thumbUrl,
                    type = if (isVideo) "video" else "image"
                )
            )
        } else {
            emptyList()
        }

        val resolvedMediaUrls = if (mediaUrls.isNotEmpty()) {
            mediaUrls.filter { isValidMediaUrl(it) }
        } else {
            listOfNotNull(fullUrl, feedUrl, thumbUrl).filter { isValidMediaUrl(it) }
        }

        val finalVideoUrl = videoUrlRaw ?: if (isVideo && isVideoExt) mediaUrl else null
        val finalMediaType = if (isVideo) "video" else if (mediaTypeRaw != null && mediaTypeRaw != "video") mediaTypeRaw else if (resolvedMediaUrls.isNotEmpty()) "image" else null

        return Post(
            id = id,
            author = author,
            content = content,
            mediaUrl = mediaUrl ?: feedUrl ?: fullUrl,
            mediaUrls = resolvedMediaUrls,
            mediaTypes = mediaTypes,
            mediaMeta = resolvedMediaMeta,
            timestamp = timestamp,
            likesCount = 0,
            commentsCount = 0,
            sharesCount = sharesCount,
            isLiked = false,
            isSaved = false,
            backgroundStyle = backgroundStyle,
            groupId = groupId,
            postType = if (isVideo) "video" else "post",
            videoUrl = finalVideoUrl,
            mediaType = finalMediaType,
            createdAt = rawDate,
            userId = userId
        )
    }

    private fun extractMediaUrls(obj: JSONObject): List<String> {
        val list = mutableListOf<String>()
        val mediaObj = obj.opt("media_urls")
        if (mediaObj is JSONArray) {
            for (i in 0 until mediaObj.length()) {
                val u = mediaObj.optString(i)
                if (isValidMediaUrl(u)) list.add(u)
            }
        } else if (mediaObj is String && mediaObj.startsWith("[")) {
            try {
                val arr = JSONArray(mediaObj)
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i)
                    if (isValidMediaUrl(u)) list.add(u)
                }
            } catch (_: Exception) {}
        }

        val mediaArray = obj.optJSONArray("media")
        if (mediaArray != null && list.isEmpty()) {
            for (i in 0 until mediaArray.length()) {
                val m = mediaArray.optJSONObject(i) ?: continue
                val u = m.optString("feed").takeIf { isValidMediaUrl(it) }
                    ?: m.optString("full").takeIf { isValidMediaUrl(it) }
                    ?: m.optString("original").takeIf { isValidMediaUrl(it) }
                    ?: m.optString("720").takeIf { isValidMediaUrl(it) }
                    ?: m.optString("thumb").takeIf { isValidMediaUrl(it) }
                if (u != null) list.add(u)
            }
        }

        val single = obj.optString("media_url").takeIf { isValidMediaUrl(it) }
            ?: obj.optString("feed_url").takeIf { isValidMediaUrl(it) }
            ?: obj.optString("full_url").takeIf { isValidMediaUrl(it) }
            ?: obj.optString("thumb_url").takeIf { isValidMediaUrl(it) }
        if (single != null && !list.contains(single)) {
            list.add(0, single)
        }

        return list
    }

    fun formatRelativeTime(raw: String?): String {
        if (raw.isNullOrBlank() || raw.equals("null", ignoreCase = true)) return "Just now"
        val trimmed = raw.trim()

        if (trimmed.equals("Just now", ignoreCase = true)) return "Just now"
        if (Regex("""^\d+\s*(hr|hrs|week|weeks|Month|Months|Year|Years)$""").matches(trimmed)) return trimmed

        val epochMillis = when {
            trimmed.toLongOrNull() != null -> {
                val num = trimmed.toLong()
                if (num > 100_000_000_000L) num else num * 1000L
            }
            else -> parseDateToMillis(trimmed)
        }

        if (epochMillis == null || epochMillis <= 0L) {
            val agoMatch = Regex("""(\d+)\s*([mhdwMy])\s*(?:ago)?""", RegexOption.IGNORE_CASE).find(trimmed)
            if (agoMatch != null) {
                val amount = agoMatch.groupValues[1].toIntOrNull() ?: 1
                val unit = agoMatch.groupValues[2].lowercase()
                return when (unit) {
                    "m" -> "${amount}m"
                    "h" -> if (amount == 1) "1hr" else "${amount}hrs"
                    "d" -> "${amount}d"
                    "w" -> if (amount == 1) "1week" else "${amount}weeks"
                    "y" -> if (amount == 1) "1 Year" else "${amount} Years"
                    else -> trimmed
                }
            }
            return trimmed
        }

        val now = System.currentTimeMillis()
        val diffMillis = (now - epochMillis).coerceAtLeast(0L)

        val seconds = diffMillis / 1000L
        val minutes = seconds / 60L
        val hours = minutes / 60L
        val days = hours / 24L
        val weeks = days / 7L
        val months = days / 30L
        val years = days / 365L

        return when {
            minutes < 1 -> "Just now"
            hours < 1 -> "${minutes}m"
            hours < 24 -> if (hours == 1L) "1hr" else "${hours}hrs"
            days < 7 -> "${days}d"
            days < 30 -> if (weeks <= 1L) "1week" else "${weeks}weeks"
            days < 365 -> if (months <= 1L) "1 Month" else "${months} Months"
            else -> if (years <= 1L) "1 Year" else "${years} Years"
        }
    }

    private fun parseDateToMillis(str: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(str)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun formatTimestamp(raw: String?): String {
        return formatRelativeTime(raw)
    }

    private fun extractMediaTypes(obj: JSONObject): List<String> {
        val list = mutableListOf<String>()
        val typesObj = obj.opt("media_types")
        if (typesObj is JSONArray) {
            for (i in 0 until typesObj.length()) {
                val t = typesObj.optString(i)
                if (t.isNotBlank() && t != "null") list.add(t)
            }
        } else if (typesObj is String && typesObj.startsWith("[")) {
            try {
                val arr = JSONArray(typesObj)
                for (i in 0 until arr.length()) {
                    val t = arr.optString(i)
                    if (t.isNotBlank() && t != "null") list.add(t)
                }
            } catch (_: Exception) {}
        }
        return list
    }

    private fun extractMediaMeta(obj: JSONObject): List<MediaMetaItem> {
        val list = mutableListOf<MediaMetaItem>()
        val metaObj = obj.opt("media_meta")
        if (metaObj is JSONArray) {
            for (i in 0 until metaObj.length()) {
                val m = metaObj.optJSONObject(i) ?: continue
                parseMediaMetaItem(m)?.let { list.add(it) }
            }
        } else if (metaObj is JSONObject) {
            parseMediaMetaItem(metaObj)?.let { list.add(it) }
        } else if (metaObj is String && metaObj.startsWith("[")) {
            try {
                val arr = JSONArray(metaObj)
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    parseMediaMetaItem(m)?.let { list.add(it) }
                }
            } catch (_: Exception) {}
        } else if (metaObj is String && metaObj.startsWith("{")) {
            try {
                val m = JSONObject(metaObj)
                parseMediaMetaItem(m)?.let { list.add(it) }
            } catch (_: Exception) {}
        }

        // Also fallback to "media" array if media_meta is empty
        if (list.isEmpty()) {
            val mediaArr = obj.optJSONArray("media")
            if (mediaArr != null) {
                for (i in 0 until mediaArr.length()) {
                    val m = mediaArr.optJSONObject(i) ?: continue
                    parseMediaMetaItem(m)?.let { list.add(it) }
                }
            }
        }

        // Also fallback to direct feed_url, full_url, thumb_url from feeds.ts
        if (list.isEmpty()) {
            val feedUrl = obj.optString("feed_url").takeIf { isValidMediaUrl(it) }
            val fullUrl = obj.optString("full_url").takeIf { isValidMediaUrl(it) }
            val thumbUrl = obj.optString("thumb_url").takeIf { isValidMediaUrl(it) }
            if (feedUrl != null || fullUrl != null || thumbUrl != null) {
                val candidate = (fullUrl ?: feedUrl ?: thumbUrl ?: "")
                val isVid = isValidVideoUrl(candidate)
                list.add(
                    MediaMetaItem(
                        feed = feedUrl,
                        full = fullUrl,
                        resolution720 = feedUrl,
                        resolution1080 = fullUrl,
                        thumb = thumbUrl,
                        thumbnailUrl = thumbUrl,
                        type = if (isVid) "video" else "image"
                    )
                )
            }
        }
        return list
    }

    private fun parseMediaMetaItem(m: JSONObject): MediaMetaItem? {
        val original = m.optString("original").takeIf { it.isNotBlank() && it != "null" }
        val res1080 = m.optString("1080").takeIf { it.isNotBlank() && it != "null" }
            ?: m.optString("full").takeIf { it.isNotBlank() && it != "null" }
        val res720 = m.optString("720").takeIf { it.isNotBlank() && it != "null" }
            ?: m.optString("feed").takeIf { it.isNotBlank() && it != "null" }
        val thumbUrl = m.optString("thumbnail_url").takeIf { it.isNotBlank() && it != "null" }
            ?: m.optString("thumb").takeIf { it.isNotBlank() && it != "null" }
        val thumb = m.optString("thumb").takeIf { it.isNotBlank() && it != "null" }
            ?: thumbUrl
        val feed = m.optString("feed").takeIf { it.isNotBlank() && it != "null" }
            ?: res720
        val full = m.optString("full").takeIf { it.isNotBlank() && it != "null" }
            ?: res1080
        val type = m.optString("type").takeIf { it.isNotBlank() && it != "null" }
        val width = if (m.has("width") && !m.isNull("width")) m.optInt("width") else null
        val height = if (m.has("height") && !m.isNull("height")) m.optInt("height") else null
        val duration = if (m.has("duration") && !m.isNull("duration")) m.optDouble("duration") else null

        if (original == null && res1080 == null && res720 == null && thumb == null && feed == null && full == null) {
            return null
        }

        return MediaMetaItem(
            original = original,
            resolution1080 = res1080,
            resolution720 = res720,
            thumbnailUrl = thumbUrl,
            thumb = thumb,
            feed = feed,
            full = full,
            type = type,
            width = width,
            height = height,
            duration = duration
        )
    }
}
