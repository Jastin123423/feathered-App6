package com.unera.social.data

import com.unera.social.model.Event
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * EventsApiService
 *
 * Professional API service for Feathered Events matching web App.tsx /api/events:
 * - GET /api/events — Fetch all live events
 * - POST /api/events — Create live event in database (events table)
 * - POST /api/events/{id}/rsvp — RSVP Going or Interested
 */
object EventsApiService {
    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * GET /api/events
     */
    suspend fun fetchLiveEvents(currentUserId: Long? = null): List<Event> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Event>()
        try {
            val url = URL("$BASE_URL/api/events")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                if (currentUserId != null && currentUserId > 0) {
                    setRequestProperty("x-user-id", currentUserId.toString())
                }
            }

            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (text.startsWith("{")) {
                    val root = JSONObject(text)
                    val arr = root.optJSONArray("events") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseEventJson(obj, currentUserId)?.let { result.add(it) }
                        }
                    }
                } else if (text.startsWith("[")) {
                    val arr = JSONArray(text)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseEventJson(obj, currentUserId)?.let { result.add(it) }
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
     * POST /api/events
     * Creates an event in the events database table.
     */
    suspend fun createEvent(
        title: String,
        description: String,
        eventDateISO: String,
        eventTime: String,
        location: String,
        visibility: String,
        coverUrl: String,
        creator: User,
        groupId: Long? = null
    ): Event? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/events")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", creator.id.toString())
            }

            val payload = JSONObject().apply {
                put("title", title.trim())
                put("description", description.trim())
                put("event_date", eventDateISO)
                put("event_time", eventTime.ifBlank { "12:00" })
                put("location", location.trim())
                put("visibility", visibility.ifBlank { "worldwide" })
                put("cover_url", coverUrl)
                put("image", coverUrl)
                put("creator_id", creator.id)
                put("creator_name", creator.name)
                put("creator_avatar", creator.profileImageUrl)
                if (groupId != null && groupId > 0) {
                    put("group_id", groupId)
                } else {
                    put("group_id", JSONObject.NULL)
                }
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (text.startsWith("{")) {
                    val root = JSONObject(text)
                    val evObj = root.optJSONObject("event") ?: root
                    return@withContext parseEventJson(evObj, creator.id) ?: Event(
                        id = evObj.optLong("id", System.currentTimeMillis()),
                        title = title,
                        date = eventDateISO,
                        location = location,
                        imageUrl = coverUrl,
                        description = description,
                        organizer = creator,
                        time = eventTime,
                        visibility = visibility,
                        groupId = groupId
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
     * Parses an event JSON object from /api/events or feed
     */
    fun parseEventJson(obj: JSONObject, currentUserId: Long? = null): Event? {
        if (obj.optInt("is_deleted", 0) == 1) return null

        val id = obj.optLong("id", obj.optLong("event_id", 0L))
        if (id <= 0L) return null

        val title = obj.optString("title", "").ifBlank { "Event" }
        val description = obj.optString("description", "")
        val rawDate = obj.optString("event_date", "").ifBlank { obj.optString("date", "Oct 24, 2026") }
        val time = obj.optString("event_time", "").ifBlank { obj.optString("time", "") }
        val location = obj.optString("location", "").ifBlank { "Dar es Salaam, Tanzania" }

        val cover = obj.optString("cover_url").ifBlank {
            obj.optString("image", obj.optString("imageUrl", "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=800"))
        }

        val creatorId = obj.optLong("creator_id", obj.optLong("organizerId", obj.optLong("user_id", 1L)))
        val creatorName = obj.optString("creator_name", obj.optString("name", "Organizer"))
        val creatorAvatar = obj.optString("creator_avatar", obj.optString("profile_image_url", ""))

        val organizer = User(
            id = creatorId,
            name = creatorName,
            username = "@${creatorName.lowercase().replace(" ", "")}",
            profileImageUrl = creatorAvatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800" },
            isVerified = creatorId == 1L
        )

        val attendeesArray = obj.optJSONArray("attendees")
        val interestedArray = obj.optJSONArray("interested_ids") ?: obj.optJSONArray("interested")

        val attendingCount = obj.optInt("attending_count", obj.optInt("going_count", attendeesArray?.length() ?: 0))
        val interestedCount = obj.optInt("interested_count", interestedArray?.length() ?: 0)

        var isGoing = false
        var isInterested = false
        if (currentUserId != null) {
            if (attendeesArray != null) {
                for (i in 0 until attendeesArray.length()) {
                    if (attendeesArray.optLong(i) == currentUserId) isGoing = true
                }
            }
            if (interestedArray != null) {
                for (i in 0 until interestedArray.length()) {
                    if (interestedArray.optLong(i) == currentUserId) isInterested = true
                }
            }
        }

        val myRsvp = obj.optString("my_rsvp_status", "")
        if (myRsvp.equals("going", ignoreCase = true)) isGoing = true
        if (myRsvp.equals("interested", ignoreCase = true)) isInterested = true

        val visibility = obj.optString("visibility", "worldwide")
        val groupId = if (obj.has("group_id") && !obj.isNull("group_id") && obj.optLong("group_id") > 0) {
            obj.optLong("group_id")
        } else null
        val groupName = obj.optString("group_name").takeIf { it.isNotBlank() && it != "null" }
        val createdAt = obj.optString("created_at", "Just now")

        return Event(
            id = id,
            title = title,
            date = rawDate,
            location = location,
            imageUrl = cover,
            description = description,
            interestedCount = interestedCount,
            goingCount = attendingCount,
            isInterested = isInterested,
            isGoing = isGoing,
            organizer = organizer,
            time = time,
            visibility = visibility,
            groupId = groupId,
            groupName = groupName,
            createdAt = createdAt
        )
    }
}
