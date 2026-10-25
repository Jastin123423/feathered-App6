package com.unera.social.data

import com.unera.social.model.Comment
import com.unera.social.model.LocationSearchResult
import com.unera.social.model.Product
import com.unera.social.model.User
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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object MarketplaceApiService {
    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * 1. GET /api/products
     * List all products from live API, overlaying active sponsored ads from GET /api/ads/feeds
     */
    suspend fun fetchLiveProducts(viewerCountry: String = "TZ"): List<Product> = withContext(Dispatchers.IO) {
        val fetchedList = mutableListOf<Product>()
        val sponsoredProductIds = mutableSetOf<Long>()

        // Fetch sponsored ads from /api/ads/feeds (fallback /api/ads/feed) to mark isSponsored
        try {
            val adsUrl = URL("$BASE_URL/api/ads/feeds")
            val conn = (adsUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (text.startsWith("{")) {
                    val root = JSONObject(text)
                    val adsArr = root.optJSONArray("ads") ?: root.optJSONArray("data")
                    if (adsArr != null) {
                        for (i in 0 until adsArr.length()) {
                            val ad = adsArr.optJSONObject(i) ?: continue
                            val pid = ad.optLong("product_id", 0L)
                            val sourceId = ad.optLong("source_id", 0L)
                            val sourceType = ad.optString("source_type", "")
                            if (pid > 0) sponsoredProductIds.add(pid)
                            if (sourceType == "product" && sourceId > 0) sponsoredProductIds.add(sourceId)
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
            // Optional sponsored overlay check fallback
        }

        // Fetch live products
        try {
            val url = URL("$BASE_URL/api/products")
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
                    val array = JSONArray(responseText)
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseProductJson(obj, sponsoredProductIds)?.let { fetchedList.add(it) }
                    }
                } else if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val array = root.optJSONArray("products") ?: root.optJSONArray("data")
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            parseProductJson(obj, sponsoredProductIds)?.let { fetchedList.add(it) }
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to feeds endpoint if products list is empty
        if (fetchedList.isEmpty()) {
            try {
                val url = URL("$BASE_URL/api/feeds?limit=100")
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
                    if (responseText.startsWith("{")) {
                        val root = JSONObject(responseText)
                        val feed = root.optJSONArray("feed") ?: root.optJSONArray("data")
                        if (feed != null) {
                            for (i in 0 until feed.length()) {
                                val item = feed.optJSONObject(i) ?: continue
                                val isProduct = item.optString("source") == "product" ||
                                        item.optString("item_type") == "product" ||
                                        item.has("product_id") || item.has("product_id2")
                                if (isProduct) {
                                    parseProductJson(item, sponsoredProductIds)?.let { fetchedList.add(it) }
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Deduplicate by ID
        val uniqueProducts = fetchedList.distinctBy { it.id }
        uniqueProducts
    }

    /**
     * 2. GET /api/products?id={pid}
     * Single product details fallback
     */
    suspend fun fetchProductById(id: Long): Product? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/products?id=$id")
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
                    val root = JSONObject(responseText)
                    val prodObj = root.optJSONObject("product") ?: root
                    return@withContext parseProductJson(prodObj)
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    if (arr.length() > 0) {
                        val obj = arr.optJSONObject(0)
                        if (obj != null) return@withContext parseProductJson(obj)
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 3. POST /api/products
     * Create new listing - populates both images and canonical image_variants
     */
    suspend fun publishProduct(product: Product): Product? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/products")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", product.seller.id.toString())
            }

            val json = JSONObject().apply {
                put("seller_id", product.seller.id)
                put("seller_name", product.seller.name)
                put("seller_avatar", product.seller.profileImageUrl)
                put("title", product.title.trim())
                put("category", product.category.lowercase().replace(" ", "_"))
                put("description", product.description.trim())
                put("country", product.country)
                put("address", product.location.trim())
                put("location", product.location.trim())
                put("main_price", product.price)
                put("price", product.price)
                put("currency", product.currency)
                put("currency_symbol", product.currency)
                if (product.discountPrice != null && product.discountPrice > 0) {
                    put("discount_price", product.discountPrice)
                } else {
                    put("discount_price", JSONObject.NULL)
                }
                put("quantity", product.quantity)
                put("phone_number", product.phoneNumber)

                val imagesArray = JSONArray()
                val imageVariantsArray = JSONArray()
                val allImgs = if (product.images.isNotEmpty()) product.images else listOf(product.imageUrl)
                allImgs.forEach { imgUrl ->
                    imagesArray.put(imgUrl)
                    val variant = JSONObject().apply {
                        put("thumb", imgUrl)
                        put("feed", imgUrl)
                        put("full", imgUrl)
                        put("type", "image")
                    }
                    imageVariantsArray.put(variant)
                }

                put("images", imagesArray)
                put("image_variants", imageVariantsArray)
                put("status", "active")
                put("views", 0)
                put("ratings", JSONArray())
                put("comments", JSONArray())
                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
                put("created_at", now)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(json.toString())
                it.flush()
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                conn.disconnect()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val prodObj = root.optJSONObject("product") ?: root
                    return@withContext parseProductJson(prodObj) ?: product
                }
                return@withContext product
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 4. PATCH /api/products/{pid}
     * Edit product
     */
    suspend fun updateProduct(productId: Long, fields: Map<String, Any?>): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/products/$productId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val payload = JSONObject()
            fields.forEach { (k, v) ->
                payload.put(k, v ?: JSONObject.NULL)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 5. DELETE /api/products/{pid}
     * Delete product listing
     */
    suspend fun deleteProduct(productId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/products/$productId?user_id=$userId")
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
     * 6. POST /api/products/{pid}/react
     * React to product listing
     */
    suspend fun reactToProduct(productId: Long, userId: Long, type: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/products/$productId/react")
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
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 7. POST /api/products/{pid}/review
     * Add review / comment to product
     */
    suspend fun addReview(
        productId: Long,
        userId: Long,
        text: String,
        rating: Int = 5,
        parentCommentId: Long? = null,
        imageUrl: String? = null
    ): Comment? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/products/$productId/review")
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
                put("rating", rating)
                put("text", text)
                put("parent_comment_id", parentCommentId ?: JSONObject.NULL)
                put("image_url", imageUrl ?: JSONObject.NULL)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                conn.disconnect()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val cObj = root.optJSONObject("review") ?: root.optJSONObject("comment") ?: root
                    return@withContext parseReviewJson(cObj, userId)
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * 8. GET /api/products/{pid}/reviews?viewerId=
     * Fetch reviews / comments for product
     */
    suspend fun fetchProductReviews(productId: Long, viewerId: Long = 101L): List<Comment> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Comment>()
        try {
            val url = URL("$BASE_URL/api/products/$productId/reviews?viewerId=$viewerId")
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
                    val arr = root.optJSONArray("reviews") ?: root.optJSONArray("comments") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseReviewJson(obj, viewerId)?.let { result.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseReviewJson(obj, viewerId)?.let { result.add(it) }
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
     * 9. POST /api/products/{pid}/share
     * Record share for product
     */
    suspend fun shareProduct(productId: Long, userId: Long, destination: String = "feed"): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/products/$productId/share")
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
                put("product_id", productId)
                put("user_id", userId)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 10. POST /api/messages/send
     * Send product inquiry to seller
     */
    suspend fun sendProductInquiry(
        recipientId: Long,
        productId: Long,
        productTitle: String,
        userId: Long = 101L
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/messages/send")
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

            val textContent = "$productTitle: Hello, I am interested in this product. Is it still available? [product:$productId]"
            val payload = JSONObject().apply {
                put("recipient_id", recipientId)
                put("text_content", textContent)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 11. Location search via Nominatim
     * GET https://nominatim.openstreetmap.org/search?format=json&q={query}&addressdetails=1&limit=5
     */
    suspend fun searchLocation(query: String): List<LocationSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LocationSearchResult>()
        if (query.trim().length < 2) return@withContext results
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&addressdetails=1&limit=5")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "FeatheredAndroidApp/1.0 (contact@featheredsocial.site)")
            }

            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (text.startsWith("[")) {
                    val arr = JSONArray(text)
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val displayName = item.optString("display_name", "")
                        val lat = item.optString("lat", "")
                        val lon = item.optString("lon", "")
                        val addr = item.optJSONObject("address")
                        val code = addr?.optString("country_code", "")?.uppercase()
                            ?: detectCountryFromText(displayName)
                        if (displayName.isNotBlank()) {
                            results.add(
                                LocationSearchResult(
                                    displayName = displayName,
                                    countryCode = code.ifBlank { "TZ" },
                                    lat = lat,
                                    lon = lon
                                )
                            )
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    /**
     * 12. Upload image to POST /api/upload
     */
    suspend fun uploadMarketplaceImage(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String = "image/webp"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val boundary = "==Boundary-${UUID.randomUUID()}=="
            val url = URL("$BASE_URL/api/upload")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val dos = DataOutputStream(conn.outputStream)
            // thumbnail field
            dos.writeBytes("--$boundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"thumbnail\"; filename=\"thumb_$fileName\"\r\n")
            dos.writeBytes("Content-Type: $mimeType\r\n\r\n")
            dos.write(fileBytes)
            dos.writeBytes("\r\n")

            // feed field
            dos.writeBytes("--$boundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"feed\"; filename=\"feed_$fileName\"\r\n")
            dos.writeBytes("Content-Type: $mimeType\r\n\r\n")
            dos.write(fileBytes)
            dos.writeBytes("\r\n")

            // original field (same as feed file per spec)
            dos.writeBytes("--$boundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"original\"; filename=\"$fileName\"\r\n")
            dos.writeBytes("Content-Type: $mimeType\r\n\r\n")
            dos.write(fileBytes)
            dos.writeBytes("\r\n")

            dos.writeBytes("--$boundary--\r\n")
            dos.flush()
            dos.close()

            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                val root = JSONObject(text)
                if (root.has("url")) return@withContext root.optString("url")
                val uploaded = root.optJSONObject("uploaded")
                if (uploaded != null) {
                    val feed = uploaded.optJSONObject("feed")?.optString("url")
                    val orig = uploaded.optJSONObject("original")?.optString("url")
                    val thumb = uploaded.optJSONObject("thumbnail")?.optString("url")
                    return@withContext feed ?: orig ?: thumb
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Country detection from address text
     */
    fun detectCountryFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("tanzania") || lower.contains("dar es salaam") || lower.contains("zanzibar") || lower.contains("arusha") -> "TZ"
            lower.contains("kenya") || lower.contains("nairobi") || lower.contains("mombasa") -> "KE"
            lower.contains("uganda") || lower.contains("kampala") -> "UG"
            lower.contains("rwanda") || lower.contains("kigali") -> "RW"
            lower.contains("burundi") || lower.contains("bujumbura") -> "BI"
            lower.contains("nigeria") || lower.contains("lagos") || lower.contains("abuja") -> "NG"
            lower.contains("south africa") || lower.contains("johannesburg") || lower.contains("cape town") -> "ZA"
            lower.contains("ghana") || lower.contains("accra") -> "GH"
            lower.contains("ethiopia") || lower.contains("addis ababa") -> "ET"
            lower.contains("egypt") || lower.contains("cairo") -> "EG"
            lower.contains("united states") || lower.contains("usa") -> "US"
            lower.contains("united kingdom") || lower.contains("uk") || lower.contains("london") -> "GB"
            lower.contains("china") -> "CN"
            lower.contains("india") -> "IN"
            lower.contains("uae") || lower.contains("dubai") -> "AE"
            else -> "TZ"
        }
    }

    /**
     * Ranking ("for_you" mode) scoring & anti-repetition interleave
     */
    fun rankForYou(products: List<Product>, viewerCountry: String = "TZ"): List<Product> {
        if (products.isEmpty()) return emptyList()

        val scored = products.map { p ->
            var score = 0.0

            // 1. Freshness scoring
            val daysAgo = calculateDaysAgo(p.createdAt)
            score += when {
                daysAgo <= 2 -> 70.0
                daysAgo <= 7 -> 55.0
                daysAgo <= 14 -> 30.0
                daysAgo <= 30 -> 18.0
                daysAgo <= 90 -> 8.0
                else -> 2.0
            }

            // 2. Locality scoring
            val isLocal = p.country.equals(viewerCountry, ignoreCase = true)
            score += if (isLocal) 34.0 else 8.0

            // 3. Quality scoring
            val imgCount = p.images.size.coerceAtLeast(if (p.imageUrl.isNotBlank()) 1 else 0)
            if (imgCount >= 1) score += 12.0
            if (imgCount >= 3) score += 5.0
            if (p.location.length >= 5) score += 8.0
            if (p.description.length >= 20) score += 8.0
            if (p.price > 0) score += 8.0
            if (p.quantity > 0) score += 4.0

            // 4. Engagement
            score += (p.views * 0.05).coerceAtMost(12.0)
            score += (p.sharesCount * 0.6).coerceAtMost(8.0)

            // 5. Deterministic jitter
            val hash = (p.id.toString() + ":" + p.seller.id.toString()).hashCode()
            val jitter = ((hash % 1000).toDouble() / 1000.0).coerceIn(0.0, 1.0) * 6.0
            score += jitter

            // 6. Sponsored boost
            if (p.isSponsored) score += 50.0

            Pair(p, score)
        }

        // Split into 4 pools: localFresh, localOlder, worldFresh, worldOlder
        val localFresh = mutableListOf<Product>()
        val localOlder = mutableListOf<Product>()
        val worldFresh = mutableListOf<Product>()
        val worldOlder = mutableListOf<Product>()

        scored.sortedByDescending { it.second }.forEach { (p, _) ->
            val isLocal = p.country.equals(viewerCountry, ignoreCase = true)
            val isFresh = calculateDaysAgo(p.createdAt) <= 7
            when {
                isLocal && isFresh -> localFresh.add(p)
                isLocal && !isFresh -> localOlder.add(p)
                !isLocal && isFresh -> worldFresh.add(p)
                else -> worldOlder.add(p)
            }
        }

        // Interleave pulling with anti-seller-repeat heuristic
        val interleaved = mutableListOf<Product>()
        val lastSellerIds = ArrayDeque<Long>()

        fun pickNextFrom(pool: MutableList<Product>): Product? {
            if (pool.isEmpty()) return null
            val idx = pool.indexOfFirst { it.seller.id !in lastSellerIds }
            val chosen = if (idx >= 0) pool.removeAt(idx) else pool.removeAt(0)
            lastSellerIds.addLast(chosen.seller.id)
            if (lastSellerIds.size > 2) lastSellerIds.removeFirst()
            return chosen
        }

        while (localFresh.isNotEmpty() || worldFresh.isNotEmpty() || localOlder.isNotEmpty() || worldOlder.isNotEmpty()) {
            pickNextFrom(localFresh)?.let { interleaved.add(it) }
            pickNextFrom(localFresh)?.let { interleaved.add(it) }
            pickNextFrom(worldFresh)?.let { interleaved.add(it) }
            pickNextFrom(localOlder)?.let { interleaved.add(it) }
            pickNextFrom(worldOlder)?.let { interleaved.add(it) }
        }

        return interleaved
    }

    private fun calculateDaysAgo(createdAt: String): Int {
        if (createdAt.isBlank()) return 1
        return try {
            val clean = createdAt.replace("Z", "+0000")
            val format = if (clean.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            } else {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            }
            val date = format.parse(clean.substringBefore("."))
            if (date != null) {
                val diffMs = System.currentTimeMillis() - date.time
                (diffMs / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            } else 3
        } catch (_: Exception) {
            3
        }
    }

    // ==========================================
    // JSON PARSERS
    // ==========================================

    fun parseProductJson(obj: JSONObject, sponsoredIds: Set<Long> = emptySet()): Product? {
        val id = obj.optLong("id", obj.optLong("product_id", 0L))
        val title = obj.optString("title", "").ifBlank { return null }
        val mainPrice = obj.optDouble("main_price", obj.optDouble("price", 0.0))
        val discountPrice = if (obj.has("discount_price") && !obj.isNull("discount_price")) {
            obj.optDouble("discount_price", 0.0).takeIf { it > 0 }
        } else null

        val category = obj.optString("category", "General")
        val address = obj.optString("address", obj.optString("location", "Tanzania"))
        val country = obj.optString("country", detectCountryFromText(address))
        val description = obj.optString("description", "")
        val phone = obj.optString("phone_number", obj.optString("phone", ""))
        val isSponsored = obj.optBoolean("is_sponsored", false) ||
                obj.optBoolean("sponsored", false) ||
                obj.optBoolean("is_ad", false) ||
                (id in sponsoredIds && id > 0)

        val imagesList = mutableListOf<String>()

        // Canonical: image_variants
        val imageVariants = obj.optJSONArray("image_variants")
        if (imageVariants != null) {
            for (j in 0 until imageVariants.length()) {
                val variantObj = imageVariants.optJSONObject(j)
                if (variantObj != null) {
                    val feedUrl = variantObj.optString("feed", "").ifBlank {
                        variantObj.optString("full", "").ifBlank {
                            variantObj.optString("thumb", "")
                        }
                    }
                    if (feedUrl.isNotBlank() && !imagesList.contains(feedUrl)) {
                        imagesList.add(feedUrl)
                    }
                }
            }
        }

        // Legacy fallback: images
        val imagesJson = obj.optJSONArray("images")
        if (imagesJson != null) {
            for (j in 0 until imagesJson.length()) {
                val img = imagesJson.optString(j, "").trim()
                if (img.isNotBlank() && !imagesList.contains(img)) imagesList.add(img)
            }
        }

        val imageUrl = obj.optString("imageUrl", obj.optString("image_url", obj.optString("media_url", "")))
        if (imageUrl.isNotBlank() && !imagesList.contains(imageUrl)) {
            imagesList.add(0, imageUrl)
        }

        val primaryImage = imagesList.firstOrNull() ?: "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800"

        val sellerId = obj.optLong("seller_id", obj.optLong("user_id", 101L))
        val sellerName = obj.optString("seller_name", obj.optString("seller_username", obj.optString("username", obj.optString("name", "Seller"))))
        val sellerAvatar = obj.optString("seller_avatar", obj.optString("profile_image_url", obj.optString("avatar_url", "")))
            .ifBlank { "https://ui-avatars.com/api/?name=${sellerName.replace(" ", "+")}&background=1877F2&color=FFFFFF" }

        val seller = User(
            id = sellerId,
            name = sellerName,
            username = obj.optString("seller_username", sellerName.lowercase().replace(" ", "_")),
            profileImageUrl = sellerAvatar,
            location = address,
            isVerified = obj.optBoolean("seller_is_verified", obj.optBoolean("is_verified", false)) || obj.optInt("seller_is_verified", 0) == 1
        )

        val views = obj.optInt("views", obj.optInt("views_count", 0))
        val likesCount = obj.optInt("likes_count", obj.optInt("reactions_count", 0))
        val commentsCount = obj.optInt("comments_count", obj.optInt("reviews_count", 0))
        val sharesCount = obj.optInt("shares_count", obj.optInt("shares", 0))
        val myReaction = obj.optString("my_reaction").takeIf { it.isNotBlank() && it != "null" }
        val isLiked = myReaction != null || obj.optBoolean("is_liked", false)
        val createdAt = obj.optString("created_at", "")
        val isFresh = calculateDaysAgo(createdAt) <= 7

        return Product(
            id = if (id != 0L) id else System.currentTimeMillis() + (Math.random() * 1000).toLong(),
            title = title,
            price = mainPrice,
            discountPrice = discountPrice,
            currency = if (country.equals("TZ", ignoreCase = true)) "TZS" else if (country.equals("KE", ignoreCase = true)) "KES" else "USD",
            category = category,
            location = address,
            country = country,
            imageUrl = primaryImage,
            images = if (imagesList.isNotEmpty()) imagesList else listOf(primaryImage),
            description = description,
            seller = seller,
            phoneNumber = phone,
            isSponsored = isSponsored,
            isFresh = isFresh,
            quantity = obj.optInt("quantity", 1),
            createdAt = createdAt,
            views = views,
            likesCount = likesCount,
            commentsCount = commentsCount,
            sharesCount = sharesCount,
            isLiked = isLiked,
            myReaction = myReaction
        )
    }

    private fun parseReviewJson(obj: JSONObject, currentUserId: Long): Comment? {
        val id = obj.optLong("id", System.currentTimeMillis())
        val userId = obj.optLong("user_id", obj.optLong("author_id", 1L))
        val text = obj.optString("text", obj.optString("content", "")).ifBlank { return null }
        val rating = obj.optInt("rating", 5)
        val imageUrl = obj.optString("image_url").takeIf { it.isNotBlank() && it != "null" }
        val parentId = if (obj.has("parent_comment_id") && !obj.isNull("parent_comment_id")) obj.optLong("parent_comment_id") else null
        val createdAt = obj.optString("created_at", "Just now")
        val likesCount = obj.optInt("likes_count", 0)
        val isLiked = obj.optBoolean("is_liked", false)

        val authorName = obj.optString("user_name", obj.optString("author_name", obj.optString("username", "User $userId")))
        val authorAvatar = obj.optString("user_avatar", obj.optString("profile_image_url", obj.optString("avatar_url", "")))
            .ifBlank { "https://ui-avatars.com/api/?name=${authorName.replace(" ", "+")}&background=1877F2&color=FFFFFF" }

        val author = User(
            id = userId,
            name = authorName,
            username = authorName.lowercase().replace(" ", "_"),
            profileImageUrl = authorAvatar
        )

        return Comment(
            id = id,
            author = author,
            text = text,
            timestamp = createdAt,
            likesCount = likesCount,
            isLiked = isLiked,
            imageUrl = imageUrl,
            parentCommentId = parentId
        )
    }
}
