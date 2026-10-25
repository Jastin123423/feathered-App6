package com.unera.social.data

import com.unera.social.model.User
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
 * UserApiService
 *
 * Dedicated live network service for user profile data, People You May Know (suggestions),
 * and follow/unfollow interactions matching Feathered web App.tsx.
 */
object UserApiService {

    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) Feathered/1.0"

    /**
     * GET /api/suggestions?user_id={userId}&limit={limit}
     * Fetches live "People You May Know" suggestions from the database.
     */
    suspend fun fetchSuggestions(userId: Long, limit: Int = 20): List<User> = withContext(Dispatchers.IO) {
        val result = mutableListOf<User>()
        try {
            val url = URL("$BASE_URL/api/suggestions?user_id=$userId&limit=$limit")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val arr = root.optJSONArray("suggestions") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseSuggestionJson(obj)?.let { result.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseSuggestionJson(obj)?.let { result.add(it) }
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
     * GET /api/users?limit={limit}
     * Fetches real registered users from the database.
     */
    suspend fun fetchLiveUsers(limit: Int = 50): List<User> = withContext(Dispatchers.IO) {
        val result = mutableListOf<User>()
        try {
            val url = URL("$BASE_URL/api/users?limit=$limit")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseUserJson(obj)?.let { result.add(it) }
                    }
                } else if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val arr = root.optJSONArray("users") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseUserJson(obj)?.let { result.add(it) }
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
     * POST /api/user-follows
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
            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
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
     * DELETE /api/user-follows?follower_id={followerId}&following_id={followingId}
     */
    suspend fun unfollowUser(followerId: Long, followingId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/user-follows?follower_id=$followerId&following_id=$followingId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", followerId.toString())
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
     * GET /api/user-follows/list?userId={userId}
     * Fetches exact live followers and following user ID arrays.
     */
    suspend fun fetchUserFollowData(userId: Long): FollowDataResult? = withContext(Dispatchers.IO) {
        if (userId <= 0L) return@withContext null
        try {
            val url = URL("$BASE_URL/api/user-follows/list?userId=$userId")
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
                    val followersList = mutableListOf<Long>()
                    val followingList = mutableListOf<Long>()

                    val followersArr = root.optJSONArray("followers")
                    if (followersArr != null) {
                        for (i in 0 until followersArr.length()) {
                            followersList.add(followersArr.optLong(i))
                        }
                    }
                    val followingArr = root.optJSONArray("following")
                    if (followingArr != null) {
                        for (i in 0 until followingArr.length()) {
                            followingList.add(followingArr.optLong(i))
                        }
                    }
                    conn.disconnect()
                    return@withContext FollowDataResult(
                        userId = root.optLong("userId", userId),
                        followers = followersList,
                        following = followingList
                    )
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    fun parseSuggestionJson(obj: JSONObject): User? {
        val id = obj.optLong("id", obj.optLong("user_id", 0L))
        if (id <= 0L) return null

        val username = obj.optString("username").trim().ifBlank { "user$id" }
        val rawName = obj.optString("name").trim()
        val displayName = if (rawName.isNotBlank() && rawName != "null") rawName else username

        val avatar = obj.optString("profile_image_url").trim().takeIf { it.isNotBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400"

        val isVerified = obj.optBoolean("is_verified", false) || obj.optInt("is_verified", 0) == 1
        val role = obj.optString("role", "user")
        val isFollowing = obj.optBoolean("is_following", false)

        return User(
            id = id,
            name = displayName,
            username = if (username.startsWith("@")) username else "@$username",
            profileImageUrl = avatar,
            isVerified = isVerified,
            role = role,
            isFollowing = isFollowing
        )
    }

    fun parseUserJson(obj: JSONObject): User? {
        val id = obj.optLong("id", 0L)
        if (id <= 0L) return null

        val username = obj.optString("username").trim().ifBlank { "user$id" }
        val rawName = obj.optString("name").trim()
        val displayName = if (rawName.isNotBlank() && rawName != "null") rawName else username

        val avatar = obj.optString("profile_image_url").trim().takeIf { it.isNotBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400"

        val cover = obj.optString("cover_image_url").trim().takeIf { it.isNotBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1200"

        val bio = obj.optString("bio").trim().takeIf { it.isNotBlank() && it != "null" } ?: ""
        val location = obj.optString("location").trim().takeIf { it.isNotBlank() && it != "null" } ?: ""
        val isVerified = obj.optBoolean("is_verified", false) || obj.optInt("is_verified", 0) == 1
        val role = obj.optString("role", "user")
        val isFollowing = obj.optBoolean("is_following", false)

        return User(
            id = id,
            name = displayName,
            username = if (username.startsWith("@")) username else "@$username",
            profileImageUrl = avatar,
            coverImageUrl = cover,
            bio = bio,
            location = location,
            isVerified = isVerified,
            role = role,
            isFollowing = isFollowing
        )
    }
}

data class FollowDataResult(
    val userId: Long,
    val followers: List<Long>,
    val following: List<Long>
)

