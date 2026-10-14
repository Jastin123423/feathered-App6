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

data class GroupMemberInfo(
    val userId: Long,
    val name: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val role: String = "member", // "admin", "moderator", "member"
    val postingDisabled: Boolean = false,
    val isVerified: Boolean = false
)

object GroupsApiService {
    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    // 1. Fetch All Groups for Viewer
    suspend fun fetchGroups(viewerId: Long): List<Group> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Group>()
        try {
            val url = URL("$BASE_URL/api/groups?viewerId=$viewerId")
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
                        parseGroupJson(obj, viewerId)?.let { result.add(it) }
                    }
                } else if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val arr = root.optJSONArray("groups") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseGroupJson(obj, viewerId)?.let { result.add(it) }
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

    // 2. Fetch Single Group Details + Member IDs
    suspend fun fetchGroupDetails(groupId: Long, viewerId: Long): Pair<Group?, List<Long>> = withContext(Dispatchers.IO) {
        var group: Group? = null
        val memberIds = mutableListOf<Long>()
        try {
            val url = URL("$BASE_URL/api/groups?id=$groupId&viewerId=$viewerId")
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
                    val groupObj = root.optJSONObject("group")
                    if (groupObj != null) {
                        group = parseGroupJson(groupObj, viewerId)
                    }
                    val membersArr = root.optJSONArray("members")
                    if (membersArr != null) {
                        for (i in 0 until membersArr.length()) {
                            val m = membersArr.optJSONObject(i) ?: continue
                            val uid = m.optLong("user_id")
                            if (uid > 0) memberIds.add(uid)
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Pair(group, memberIds)
    }

    // 3. Create Group
    suspend fun createGroup(
        name: String,
        description: String,
        type: String,
        category: String,
        profileImage: String,
        coverImage: String,
        adminId: Long
    ): Group? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/groups")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", adminId.toString())
            }

            val payload = JSONObject().apply {
                put("name", name)
                put("description", description)
                put("type", type)
                put("category", category)
                put("profile_image", profileImage)
                put("cover_image", coverImage)
                put("admin_id", adminId)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                val root = JSONObject(responseText)
                val groupObj = root.optJSONObject("group") ?: root
                return@withContext parseGroupJson(groupObj, adminId)
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // 4. Update Group Settings / Images
    suspend fun updateGroup(
        groupId: Long,
        name: String? = null,
        description: String? = null,
        coverImage: String? = null,
        profileImage: String? = null,
        memberPostingAllowed: Boolean? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/groups?id=$groupId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val payload = JSONObject().apply {
                if (name != null) put("name", name)
                if (description != null) put("description", description)
                if (coverImage != null) put("cover_image", coverImage)
                if (profileImage != null) put("profile_image", profileImage)
                if (memberPostingAllowed != null) put("member_posting_allowed", memberPostingAllowed)
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

    // 5. Delete Group
    suspend fun deleteGroup(groupId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/groups?id=$groupId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // 6. Group Suggestions (Discover)
    suspend fun fetchGroupSuggestions(userId: Long, limit: Int = 12): List<Group> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Group>()
        try {
            val url = URL("$BASE_URL/api/group-suggestions?user_id=$userId&limit=$limit")
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
                    val arr = root.optJSONArray("groups")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseGroupJson(obj, userId)?.let { result.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseGroupJson(obj, userId)?.let { result.add(it) }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    // 7. Group Members List
    suspend fun fetchGroupMembers(groupId: Long): List<GroupMemberInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<GroupMemberInfo>()
        try {
            val url = URL("$BASE_URL/api/group-members?group_id=$groupId")
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
                    val arr = root.optJSONArray("members")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseMemberInfo(obj)?.let { result.add(it) }
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

    // 8. Join Group
    suspend fun joinGroup(groupId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-members")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val payload = JSONObject().apply {
                put("group_id", groupId)
                put("user_id", userId)
                put("role", "member")
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

    // 9. Leave or Remove Member
    suspend fun leaveOrRemoveMember(groupId: Long, userId: Long, actorId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-members?group_id=$groupId&user_id=$userId&actor_id=$actorId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // 10. Member Role & Posting Actions (PATCH)
    suspend fun patchMemberAction(action: String, payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-members?action=$action")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
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

    // 11. Group Posts (Discussions)
    suspend fun fetchGroupPosts(groupId: Long, viewerId: Long, limit: Int = 30, offset: Int = 0): List<GroupPost> = withContext(Dispatchers.IO) {
        val result = mutableListOf<GroupPost>()
        try {
            val url = URL("$BASE_URL/api/group-posts?group_id=$groupId&viewerId=$viewerId&limit=$limit&offset=$offset")
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
                    val arr = root.optJSONArray("posts")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseGroupPostJson(obj, viewerId)?.let { result.add(it) }
                        }
                    }
                } else if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseGroupPostJson(obj, viewerId)?.let { result.add(it) }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    // 12. Create Group Post
    suspend fun createGroupPost(
        groupId: Long,
        userId: Long,
        content: String,
        mediaUrls: List<String> = emptyList(),
        price: Double? = null,
        currency: String = "USD",
        condition: String? = null,
        location: String? = null,
        status: String = "available",
        jobTitle: String? = null,
        company: String? = null,
        jobType: String? = null,
        salary: String? = null,
        applicationType: String? = null,
        applicationValue: String? = null,
        expiryDate: String? = null
    ): GroupPost? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-posts")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val payload = JSONObject().apply {
                put("group_id", groupId)
                put("user_id", userId)
                put("content", content)
                if (mediaUrls.isNotEmpty()) {
                    put("media_url", mediaUrls.first())
                    put("media_type", "image")
                    val arr = JSONArray()
                    mediaUrls.forEach { arr.put(it) }
                    put("media_urls", arr)
                }
                if (price != null) {
                    put("price", price)
                    put("currency", currency)
                    if (condition != null) put("condition", condition)
                    if (location != null) put("location", location)
                    put("status", status)
                }
                if (jobTitle != null) {
                    put("job_title", jobTitle)
                    if (company != null) put("company", company)
                    if (jobType != null) put("job_type", jobType)
                    if (salary != null) put("salary", salary)
                    if (applicationType != null) put("application_type", applicationType)
                    if (applicationValue != null) put("application_value", applicationValue)
                    if (expiryDate != null) put("expiry_date", expiryDate)
                }
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                val root = JSONObject(responseText)
                val postObj = root.optJSONObject("post") ?: root
                return@withContext parseGroupPostJson(postObj, userId)
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // 13. Delete Group Post
    suspend fun deleteGroupPost(postId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-posts?post_id=$postId&user_id=$userId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // 14. Like / React to Group Post
    suspend fun likeGroupPost(postId: Long, userId: Long, type: String = "like"): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-post-likes")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("post_id", postId)
                put("type", type)
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

    // 15. Comments on Group Post
    suspend fun fetchGroupPostComments(postId: Long, viewerId: Long): List<Comment> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Comment>()
        try {
            val url = URL("$BASE_URL/api/group-post-comments?post_id=$postId&viewerId=$viewerId")
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
                    val arr = root.optJSONArray("comments")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseCommentJson(obj)?.let { result.add(it) }
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

    suspend fun addGroupPostComment(postId: Long, userId: Long, text: String): Comment? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-post-comments")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("post_id", postId)
                put("text", text)
                put("image_url", JSONObject.NULL)
                put("parent_comment_id", JSONObject.NULL)
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

    // 16. Group Events
    suspend fun fetchGroupEvents(groupId: Long, viewerId: Long): List<Event> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Event>()
        try {
            val url = URL("$BASE_URL/api/groups/$groupId/events?viewerId=$viewerId")
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
                    val arr = root.optJSONArray("events")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseEventJson(obj)?.let { result.add(it) }
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

    // 17. Group Invites
    suspend fun fetchGroupInvites(userId: Long): List<GroupInvite> = withContext(Dispatchers.IO) {
        val result = mutableListOf<GroupInvite>()
        try {
            val url = URL("$BASE_URL/api/group-invites?user_id=$userId")
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
                    val arr = root.optJSONArray("invites")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseInviteJson(obj, userId)?.let { result.add(it) }
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

    suspend fun sendGroupInvite(groupId: Long, inviterId: Long, inviteeIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-invites")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val payload = JSONObject().apply {
                put("group_id", groupId)
                put("inviter_id", inviterId)
                val arr = JSONArray()
                inviteeIds.forEach { arr.put(it) }
                put("invitee_ids", arr)
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

    suspend fun respondGroupInvite(inviteId: Long, groupId: Long, userId: Long, status: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/group-invites?id=$inviteId&group_id=$groupId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val payload = JSONObject().apply {
                put("status", status)
                put("user_id", userId)
                put("group_id", groupId)
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

    // ==========================================
    // PARSERS
    // ==========================================

    private fun parseGroupJson(obj: JSONObject, viewerId: Long): Group? {
        val id = obj.optLong("id")
        if (id <= 0) return null
        val adminId = obj.optLong("admin_id", obj.optLong("adminId", 1L))
        val name = obj.optString("name", "Group")
        val desc = obj.optString("description", "")
        val type = obj.optString("type", "public")
        val isPrivate = type.equals("private", ignoreCase = true)
        val cover = obj.optString("cover_image", obj.optString("coverImageUrl", ""))
        val profile = obj.optString("profile_image", obj.optString("profileImageUrl", ""))
        val membersCount = obj.optInt("members_count", obj.optInt("membersCount", 0))
        val isMember = obj.optInt("is_member", -1) == 1 || obj.optBoolean("is_member", false) || adminId == viewerId
        val category = obj.optString("category", "general")
        val memberPostingAllowed = obj.optInt("member_posting_allowed", 1) == 1 && obj.optBoolean("member_posting_allowed", true)
        val createdAt = obj.optString("created_at", "")

        val memberIds = mutableListOf<Long>()
        if (adminId > 0) memberIds.add(adminId)
        val memArr = obj.optJSONArray("members")
        if (memArr != null) {
            for (i in 0 until memArr.length()) {
                val m = memArr.optJSONObject(i) ?: continue
                val uid = m.optLong("user_id")
                if (uid > 0 && !memberIds.contains(uid)) memberIds.add(uid)
            }
        }

        return Group(
            id = id,
            adminId = adminId,
            name = name,
            description = desc,
            coverImageUrl = cover,
            profileImageUrl = profile,
            membersCount = membersCount,
            isMember = isMember,
            isPrivate = isPrivate,
            category = category,
            memberPostingAllowed = memberPostingAllowed,
            createdAt = createdAt,
            members = memberIds
        )
    }

    private fun parseGroupPostJson(obj: JSONObject, viewerId: Long): GroupPost? {
        val id = obj.optLong("id")
        if (id <= 0) return null
        val groupId = obj.optLong("group_id")
        val userId = obj.optLong("user_id", obj.optLong("shared_by_user_id", 1L))

        val authorName = obj.optString("name").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("username").takeIf { it.isNotBlank() && it != "null" }
            ?: if (userId == 1L) "Jastin Beda" else "User $userId"

        val username = obj.optString("username").takeIf { it.isNotBlank() && it != "null" }
            ?: "@${authorName.lowercase().replace(" ", "")}"

        val avatar = obj.optString("profile_image_url").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("profile_image").takeIf { it.isNotBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800"

        val isVerified = obj.optInt("is_verified", 0) == 1 || obj.optBoolean("is_verified", false)

        val author = User(
            id = userId,
            name = authorName,
            username = username,
            profileImageUrl = avatar,
            isVerified = isVerified,
            role = obj.optString("role", "member")
        )

        val content = obj.optString("content", "")

        val mediaUrls = mutableListOf<String>()
        val mediaUrlsObj = obj.opt("media_urls")
        if (mediaUrlsObj is JSONArray) {
            for (i in 0 until mediaUrlsObj.length()) {
                val u = mediaUrlsObj.optString(i)
                if (u.isNotBlank() && u != "null") mediaUrlsListAdd(mediaUrls, u)
            }
        }
        val imagesArr = obj.optJSONArray("images")
        if (imagesArr != null) {
            for (i in 0 until imagesArr.length()) {
                val u = imagesArr.optString(i)
                if (u.isNotBlank() && u != "null") mediaUrlsListAdd(mediaUrls, u)
            }
        }
        val single = obj.optString("media_url").takeIf { it.isNotBlank() && it != "null" }
        if (single != null) mediaUrlsListAdd(mediaUrls, single)

        val price = if (obj.has("price") && !obj.isNull("price")) obj.optDouble("price") else null
        val currency = obj.optString("currency", "USD")
        val condition = obj.optString("condition").takeIf { it.isNotBlank() && it != "null" }
        val location = obj.optString("location").takeIf { it.isNotBlank() && it != "null" }
        val status = obj.optString("status", "available")

        val jobTitle = obj.optString("job_title").takeIf { it.isNotBlank() && it != "null" }
        val company = obj.optString("company").takeIf { it.isNotBlank() && it != "null" }
        val jobType = obj.optString("job_type").takeIf { it.isNotBlank() && it != "null" }
        val salary = obj.optString("salary").takeIf { it.isNotBlank() && it != "null" }
        val applicationType = obj.optString("application_type").takeIf { it.isNotBlank() && it != "null" }
        val applicationValue = obj.optString("application_value").takeIf { it.isNotBlank() && it != "null" }
        val expiryDate = obj.optString("expiry_date").takeIf { it.isNotBlank() && it != "null" }

        val likesCount = obj.optInt("reactions_count", obj.optInt("likes_count", 0))
        val commentsCount = obj.optInt("comments_count", 0)
        val sharesCount = obj.optInt("shares", 0)
        val myReaction = obj.optString("my_reaction").takeIf { it.isNotBlank() && it != "null" }
        val isLiked = myReaction != null

        return GroupPost(
            id = id,
            groupId = groupId,
            author = author,
            content = content,
            imageUrls = mediaUrls,
            createdAt = obj.optString("created_at", "Just now"),
            likesCount = likesCount,
            commentsCount = commentsCount,
            sharesCount = sharesCount,
            isLiked = isLiked,
            myReaction = myReaction,
            price = price,
            currency = currency,
            condition = condition,
            location = location,
            status = status,
            jobTitle = jobTitle,
            company = company,
            jobType = jobType,
            salary = salary,
            applicationType = applicationType,
            applicationValue = applicationValue,
            expiryDate = expiryDate
        )
    }

    private fun mediaUrlsListAdd(list: MutableList<String>, url: String) {
        if (!list.contains(url)) list.add(url)
    }

    private fun parseMemberInfo(obj: JSONObject): GroupMemberInfo? {
        val uid = obj.optLong("user_id")
        if (uid <= 0) return null
        val name = obj.optString("name").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("username", "Member")
        val username = obj.optString("username", "member")
        val avatar = obj.optString("profile_image_url").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("profile_image", "")
        val role = obj.optString("group_role", obj.optString("role", "member"))
        val disabled = obj.optInt("posting_disabled", 0) == 1 || obj.optBoolean("posting_disabled", false)
        val isVerified = obj.optInt("is_verified", 0) == 1 || obj.optBoolean("is_verified", false)

        return GroupMemberInfo(
            userId = uid,
            name = name,
            username = username,
            profileImageUrl = avatar,
            role = role,
            postingDisabled = disabled,
            isVerified = isVerified
        )
    }

    private fun parseCommentJson(obj: JSONObject): Comment? {
        val id = obj.optLong("id")
        if (id <= 0) return null
        val userId = obj.optLong("user_id", 1L)
        val authorName = obj.optString("name").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("username", "User $userId")
        val username = obj.optString("username", "user$userId")
        val avatar = obj.optString("profile_image_url").takeIf { it.isNotBlank() && it != "null" }
            ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800"

        val author = User(
            id = userId,
            name = authorName,
            username = username,
            profileImageUrl = avatar,
            isVerified = obj.optInt("is_verified", 0) == 1 || obj.optBoolean("is_verified", false)
        )

        return Comment(
            id = id,
            author = author,
            text = obj.optString("text", ""),
            timestamp = obj.optString("created_at", "Just now"),
            likesCount = obj.optInt("likes_count", 0),
            isLiked = obj.optBoolean("liked_by_me", false)
        )
    }

    private fun parseEventJson(obj: JSONObject): Event? {
        val id = obj.optLong("id")
        if (id <= 0) return null
        val title = obj.optString("title", "Event")
        val desc = obj.optString("description", "")
        val date = obj.optString("event_date", "")
        val loc = obj.optString("location", "")
        val cover = obj.optString("cover_url", "")
        val attending = obj.optInt("attending_count", obj.optInt("going_count", 0))
        val interested = obj.optInt("interested_count", 0)
        val myStatus = obj.optString("my_status", "")
        val isGoing = myStatus == "going" || myStatus == "attend"
        val isInterested = myStatus == "interested"
        val creatorId = obj.optLong("creator_id", 1L)
        val creatorName = obj.optString("creator_name", "Organizer")
        val creatorAvatar = obj.optString("creator_avatar", "")
        val groupId = if (obj.has("group_id") && !obj.isNull("group_id")) obj.optLong("group_id") else null

        val organizer = User(
            id = creatorId,
            name = creatorName,
            username = "user$creatorId",
            profileImageUrl = creatorAvatar
        )

        return Event(
            id = id,
            title = title,
            description = desc,
            date = date,
            location = loc,
            imageUrl = cover,
            interestedCount = interested,
            goingCount = attending,
            isInterested = isInterested,
            isGoing = isGoing,
            organizer = organizer,
            groupId = groupId
        )
    }

    private fun parseInviteJson(obj: JSONObject, viewerId: Long): GroupInvite? {
        val id = obj.optLong("id")
        val gid = obj.optLong("group_id")
        val inviterId = obj.optLong("inviter_id")
        val groupObj = obj.optJSONObject("group")
        val group = if (groupObj != null) {
            parseGroupJson(groupObj, viewerId) ?: Group(id = gid, name = "Group $gid")
        } else Group(id = gid, name = "Group $gid")

        val inviter = User(
            id = inviterId,
            name = "Inviter $inviterId",
            username = "user$inviterId",
            profileImageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800"
        )

        return GroupInvite(
            id = id,
            groupId = gid,
            group = group,
            inviter = inviter,
            inviteeId = viewerId,
            createdAt = "Pending"
        )
    }
}
