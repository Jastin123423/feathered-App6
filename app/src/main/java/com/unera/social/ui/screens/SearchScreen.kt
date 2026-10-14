package com.unera.social.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.*
import com.unera.social.ui.components.VerifiedBadge
import org.json.JSONArray
import org.json.JSONObject

private const val STORAGE_KEY = "unera_recent_searches_cache"
private const val PREFS_KEY_ITEMS = "items"

/**
 * RecentSearchItem - Model representing a recent search entry matching web specification.
 */
data class RecentSearchItem(
    val id: String,
    val query: String,
    val type: String, // "query", "user", "group", "reel"
    val targetId: Long? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val avatar: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * SharedPreferences storage helper for caching recent searches.
 */
private object RecentSearchCache {
    fun load(context: Context): List<RecentSearchItem> {
        val prefs = context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE)
        val raw = prefs.getString(PREFS_KEY_ITEMS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<RecentSearchItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentSearchItem(
                        id = obj.optString("id", "${System.currentTimeMillis()}-$i"),
                        query = obj.optString("query", ""),
                        type = obj.optString("type", "query"),
                        targetId = if (obj.has("targetId") && !obj.isNull("targetId")) obj.optLong("targetId") else null,
                        title = if (obj.has("title") && !obj.isNull("title")) obj.optString("title") else null,
                        subtitle = if (obj.has("subtitle") && !obj.isNull("subtitle")) obj.optString("subtitle") else null,
                        avatar = if (obj.has("avatar") && !obj.isNull("avatar")) obj.optString("avatar") else null,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, items: List<RecentSearchItem>) {
        try {
            val prefs = context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("query", item.query)
                    put("type", item.type)
                    if (item.targetId != null) put("targetId", item.targetId)
                    if (item.title != null) put("title", item.title)
                    if (item.subtitle != null) put("subtitle", item.subtitle)
                    if (item.avatar != null) put("avatar", item.avatar)
                    put("timestamp", item.timestamp)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(PREFS_KEY_ITEMS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Silently ignore cache write failures
        }
    }
}

private val SUGGESTED_TOPICS = listOf(
    "🔥 Trending Reels",
    "📸 Photography",
    "🎵 Music & Sounds",
    "🚀 Technology",
    "🎨 Art & Design",
    "⚽ Sports",
    "🍕 Food & Cooking"
)

/**
 * SearchScreen - Native Jetpack Compose implementation of the UNERA Search Page.
 * Matches all features of SearchPage.tsx:
 * - Search by keyword across Users, Reels, Posts, Groups, and Marketplace
 * - Dynamic tab filtering (All, People, Videos & Reels, Posts, Groups, Marketplace)
 * - Cached recent searches with persistence, deduplication, item deletion, and clear-all
 * - Suggested topic pills
 * - User follow/following action
 * - Verified badge indication
 * - Direct navigation into profiles, groups, reels, posts, and marketplace items
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    users: List<User> = emptyList(),
    posts: List<Post> = emptyList(),
    reels: List<Reel> = emptyList(),
    groups: List<Group> = emptyList(),
    marketplaceItems: List<Product> = emptyList(),
    currentUser: User,
    onProfileClick: (Long) -> Unit,
    onOpenGroup: ((Long) -> Unit)? = null,
    onFollow: ((Long) -> Unit)? = null,
    followingIds: Set<Long> = emptySet(),
    onVideoClick: ((Reel) -> Unit)? = null,
    onPostClick: ((Post) -> Unit)? = null,
    onViewProduct: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("all") }
    var recentSearches by remember { mutableStateOf<List<RecentSearchItem>>(emptyList()) }

    // Load recent searches from cache on mount
    LaunchedEffect(Unit) {
        recentSearches = RecentSearchCache.load(context)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Save recent searches helper
    val saveRecentSearches: (List<RecentSearchItem>) -> Unit = { updatedList ->
        recentSearches = updatedList
        RecentSearchCache.save(context, updatedList)
    }

    // Record a search
    val recordSearch: (String, String, Long?, String?, String?, String?) -> Unit =
        { searchQuery, type, targetId, title, subtitle, avatar ->
            val q = searchQuery.trim()
            if (q.isNotEmpty()) {
                val newItem = RecentSearchItem(
                    id = "$type-${targetId ?: q.lowercase()}-${System.currentTimeMillis()}",
                    query = q,
                    type = type,
                    targetId = targetId,
                    title = title,
                    subtitle = subtitle,
                    avatar = avatar,
                    timestamp = System.currentTimeMillis()
                )

                val filtered = recentSearches.filter { item ->
                    !(item.query.equals(q, ignoreCase = true) && item.type == type) &&
                            !(targetId != null && item.targetId == targetId)
                }
                val updated = listOf(newItem) + filtered.take(19)
                saveRecentSearches(updated)
            }
        }

    val removeRecentSearch: (String) -> Unit = { id ->
        val updated = recentSearches.filter { it.id != id }
        saveRecentSearches(updated)
    }

    val clearAllRecent: () -> Unit = {
        saveRecentSearches(emptyList())
    }

    // Filter results
    val trimmedQuery = query.trim().lowercase()

    val filteredUsers = remember(users, trimmedQuery) {
        if (trimmedQuery.isBlank()) emptyList()
        else users.filter { u ->
            u.name.lowercase().contains(trimmedQuery) ||
                    u.username.lowercase().contains(trimmedQuery) ||
                    u.bio.lowercase().contains(trimmedQuery)
        }
    }

    val filteredReels = remember(reels, trimmedQuery) {
        if (trimmedQuery.isBlank()) emptyList()
        else reels.filter { r ->
            r.caption.lowercase().contains(trimmedQuery) ||
                    r.musicTitle.lowercase().contains(trimmedQuery) ||
                    r.author.name.lowercase().contains(trimmedQuery)
        }
    }

    val filteredPosts = remember(posts, trimmedQuery) {
        if (trimmedQuery.isBlank()) emptyList()
        else posts.filter { p ->
            p.content.lowercase().contains(trimmedQuery) ||
                    p.author.name.lowercase().contains(trimmedQuery)
        }
    }

    val filteredGroups = remember(groups, trimmedQuery) {
        if (trimmedQuery.isBlank()) emptyList()
        else groups.filter { g ->
            g.name.lowercase().contains(trimmedQuery) ||
                    g.description.lowercase().contains(trimmedQuery)
        }
    }

    val filteredProducts = remember(marketplaceItems, trimmedQuery) {
        if (trimmedQuery.isBlank()) emptyList()
        else marketplaceItems.filter { p ->
            p.title.lowercase().contains(trimmedQuery) ||
                    p.description.lowercase().contains(trimmedQuery) ||
                    p.category.lowercase().contains(trimmedQuery)
        }
    }

    val totalResults = filteredUsers.size +
            filteredReels.size +
            filteredPosts.size +
            filteredGroups.size +
            filteredProducts.size

    val handleSelectUser: (User) -> Unit = { user ->
        recordSearch(
            user.name.ifBlank { user.username },
            "user",
            user.id,
            user.name.ifBlank { user.username },
            "@${user.username}",
            user.profileImageUrl
        )
        onProfileClick(user.id)
    }

    val handleSelectGroup: (Group) -> Unit = { group ->
        recordSearch(
            group.name,
            "group",
            group.id,
            group.name,
            "${group.membersCount} members",
            group.profileImageUrl.ifBlank { group.coverImageUrl }
        )
        onOpenGroup?.invoke(group.id)
    }

    val handleSelectReel: (Reel) -> Unit = { reel ->
        recordSearch(
            reel.caption.ifBlank { "Video Reel" },
            "reel",
            reel.id,
            reel.caption.ifBlank { "Video Reel" },
            reel.author.name,
            reel.thumbnailUrl
        )
        onVideoClick?.invoke(reel)
    }

    val handleRecentClick: (RecentSearchItem) -> Unit = { item ->
        when (item.type) {
            "user" -> {
                if (item.targetId != null) onProfileClick(item.targetId)
                else query = item.query
            }
            "group" -> {
                if (item.targetId != null) onOpenGroup?.invoke(item.targetId)
                else query = item.query
            }
            "reel" -> {
                val found = reels.find { it.id == item.targetId }
                if (found != null) onVideoClick?.invoke(found)
                else query = item.query
            }
            else -> {
                query = item.query
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B18))
            .testTag("search_screen")
    ) {
        // 1. TOP HEADER & SEARCH INPUT
        Surface(
            color = Color(0xFF0B1120),
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFF8FAFC),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Search Input Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search people, videos, reels, groups, posts…",
                                        color = Color(0xFF64748B),
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .testTag("search_input_field"),
                                    textStyle = TextStyle(
                                        color = Color(0xFFF8FAFC),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color(0xFF1877F2)),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (query.trim().isNotEmpty()) {
                                                recordSearch(query.trim(), "query", null, null, null, null)
                                            }
                                        }
                                    )
                                )
                            }

                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. FILTER TABS (When searching)
                if (query.trim().isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            SearchFilterTab(
                                label = "All ($totalResults)",
                                isSelected = activeTab == "all",
                                onClick = { activeTab = "all" }
                            )
                        }
                        item {
                            SearchFilterTab(
                                label = "People (${filteredUsers.size})",
                                isSelected = activeTab == "people",
                                onClick = { activeTab = "people" }
                            )
                        }
                        item {
                            SearchFilterTab(
                                label = "Videos & Reels (${filteredReels.size})",
                                isSelected = activeTab == "videos",
                                onClick = { activeTab = "videos" }
                            )
                        }
                        item {
                            SearchFilterTab(
                                label = "Posts (${filteredPosts.size})",
                                isSelected = activeTab == "posts",
                                onClick = { activeTab = "posts" }
                            )
                        }
                        item {
                            SearchFilterTab(
                                label = "Groups (${filteredGroups.size})",
                                isSelected = activeTab == "groups",
                                onClick = { activeTab = "groups" }
                            )
                        }
                        if (filteredProducts.isNotEmpty()) {
                            item {
                                SearchFilterTab(
                                    label = "Marketplace (${filteredProducts.size})",
                                    isSelected = activeTab == "marketplace",
                                    onClick = { activeTab = "marketplace" }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. MAIN CONTENT BODY
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ========================================================
            // STATE A: EMPTY QUERY -> SHOW RECENT SEARCHES (CACHED)
            // ========================================================
            if (query.trim().isEmpty()) {
                // Header: Recent Searches + Clear all
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Recent Searches",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        }

                        if (recentSearches.isNotEmpty()) {
                            Text(
                                text = "Clear all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1877F2),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { clearAllRecent() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Recent searches list or Empty State
                if (recentSearches.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                recentSearches.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { handleRecentClick(item) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (!item.avatar.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = item.avatar,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .border(1.dp, Color(0xFF1E293B), CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E293B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val icon = when (item.type) {
                                                        "user" -> Icons.Default.Person
                                                        "group" -> Icons.Default.Groups
                                                        "reel" -> Icons.Default.PlayArrow
                                                        else -> Icons.Default.Schedule
                                                    }
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title ?: item.query,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFF8FAFC),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.subtitle ?: "Recent search",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF64748B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { removeRecentSearch(item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    if (index < recentSearches.lastIndex) {
                                        HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Search for anything",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Find your friends, trending reels, exciting groups, and community posts across UNERA.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Suggested / Popular searches
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "SUGGESTED TOPICS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
                        )

                        OptInFlowRowWrapper(
                            topics = SUGGESTED_TOPICS,
                            onTopicClick = { tag ->
                                val clean = tag.replace(Regex("^[^a-zA-Z0-9]+"), "").trim()
                                query = clean
                                recordSearch(clean, "query", null, null, null, null)
                            }
                        )
                    }
                }
            }

            // ========================================================
            // STATE B: QUERY ENTERED -> DISPLAY SEARCH RESULTS
            // ========================================================
            if (query.trim().isNotEmpty()) {
                if (totalResults == 0) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No results found for \"$query\"",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Check your spelling or try searching for another name, topic, or keyword.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 1. PEOPLE RESULTS
                if ((activeTab == "all" || activeTab == "people") && filteredUsers.isNotEmpty()) {
                    item {
                        SearchSectionCard(
                            title = "People",
                            count = filteredUsers.size
                        ) {
                            val displayUsers = if (activeTab == "people") filteredUsers else filteredUsers.take(5)
                            displayUsers.forEachIndexed { index, user ->
                                val isFollowed = followingIds.contains(user.id) || user.isFollowing
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { handleSelectUser(user) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AsyncImage(
                                            model = user.profileImageUrl,
                                            contentDescription = user.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, Color(0xFF1E293B), CircleShape)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = user.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF8FAFC),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (user.isVerified) {
                                                    VerifiedBadge(size = 15.dp)
                                                }
                                            }
                                            Text(
                                                text = "@${user.username}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (currentUser.id != user.id && onFollow != null) {
                                        Button(
                                            onClick = { onFollow(user.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isFollowed) Color(0xFF1E293B) else Color(0xFF1877F2)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = if (isFollowed) "Following" else "Follow",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isFollowed) Color(0xFF94A3B8) else Color.White
                                            )
                                        }
                                    }
                                }

                                if (index < displayUsers.lastIndex) {
                                    HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                // 2. VIDEOS & REELS RESULTS
                if ((activeTab == "all" || activeTab == "videos") && filteredReels.isNotEmpty()) {
                    item {
                        SearchSectionCard(
                            title = "Videos & Reels",
                            count = filteredReels.size
                        ) {
                            val displayReels = if (activeTab == "videos") filteredReels else filteredReels.take(6)
                            // 2-column or 3-column grid
                            val chunkedReels = displayReels.chunked(2)
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunkedReels.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { reel ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(9f / 16f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.Black)
                                                    .clickable { handleSelectReel(reel) }
                                            ) {
                                                AsyncImage(
                                                    model = reel.thumbnailUrl,
                                                    contentDescription = reel.caption,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Top right play badge
                                                Box(
                                                    modifier = Modifier
                                                        .padding(6.dp)
                                                        .size(24.dp)
                                                        .align(Alignment.TopEnd)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.5f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }

                                                // Gradient scrim at bottom
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(90.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                            )
                                                        )
                                                )

                                                // Bottom caption and stats
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = reel.caption.ifBlank { "Video Reel" },
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.White,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = reel.author.name,
                                                            fontSize = 10.5.sp,
                                                            color = Color(0xFF94A3B8),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Visibility,
                                                                contentDescription = null,
                                                                tint = Color(0xFF94A3B8),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Text(
                                                                text = "${reel.likesCount * 3 + 24}",
                                                                fontSize = 10.sp,
                                                                color = Color(0xFF94A3B8)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Balance uneven row
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. GROUPS RESULTS
                if ((activeTab == "all" || activeTab == "groups") && filteredGroups.isNotEmpty()) {
                    item {
                        SearchSectionCard(
                            title = "Groups",
                            count = filteredGroups.size
                        ) {
                            filteredGroups.forEachIndexed { index, group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { handleSelectGroup(group) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val groupImg = group.profileImageUrl.ifBlank { group.coverImageUrl }
                                        AsyncImage(
                                            model = groupImg,
                                            contentDescription = group.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = group.name,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF8FAFC),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${group.membersCount} members • ${if (group.isPrivate) "Private" else "Public"} Group",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { handleSelectGroup(group) }
                                    ) {
                                        Text(
                                            text = "View",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                if (index < filteredGroups.lastIndex) {
                                    HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                // 4. POSTS RESULTS
                if ((activeTab == "all" || activeTab == "posts") && filteredPosts.isNotEmpty()) {
                    item {
                        SearchSectionCard(
                            title = "Posts",
                            count = filteredPosts.size
                        ) {
                            val displayPosts = if (activeTab == "posts") filteredPosts else filteredPosts.take(5)
                            displayPosts.forEachIndexed { index, post ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            recordSearch(
                                                post.content.take(40).ifBlank { "Post" },
                                                "query",
                                                post.id,
                                                null,
                                                null,
                                                null
                                            )
                                            onPostClick?.invoke(post)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        AsyncImage(
                                            model = post.author.profileImageUrl,
                                            contentDescription = post.author.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                        )
                                        Text(
                                            text = post.author.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF8FAFC)
                                        )
                                        Text(
                                            text = post.timestamp,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Text(
                                        text = post.content,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp
                                    )
                                }

                                if (index < displayPosts.lastIndex) {
                                    HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                // 5. MARKETPLACE RESULTS
                if ((activeTab == "all" || activeTab == "marketplace") && filteredProducts.isNotEmpty()) {
                    item {
                        SearchSectionCard(
                            title = "Marketplace",
                            count = filteredProducts.size
                        ) {
                            val displayProducts = if (activeTab == "marketplace") filteredProducts else filteredProducts.take(4)
                            displayProducts.forEachIndexed { index, product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            recordSearch(
                                                product.title,
                                                "query",
                                                product.id,
                                                product.title,
                                                "${product.currency} ${product.price.toLong()}",
                                                product.imageUrl
                                            )
                                            onViewProduct?.invoke(product.id)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AsyncImage(
                                            model = product.imageUrl,
                                            contentDescription = product.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = product.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF8FAFC),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${product.currency} %,d".format(product.price.toLong()),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF38BDF8)
                                            )
                                            Text(
                                                text = product.location,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFF1877F2).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onViewProduct?.invoke(product.id) }
                                    ) {
                                        Text(
                                            text = "View",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                if (index < displayProducts.lastIndex) {
                                    HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter tab button
 */
@Composable
private fun SearchFilterTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF1877F2) else Color(0xFF0F172A),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.height(30.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color(0xFF94A3B8)
            )
        }
    }
}

/**
 * Container card for search result sections
 */
@Composable
private fun SearchSectionCard(
    title: String,
    count: Int,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1120))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "$count found",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
            HorizontalDivider(color = Color(0xFF1E293B))

            content()
        }
    }
}

/**
 * Suggested topic pills layout
 */
@Composable
private fun OptInFlowRowWrapper(
    topics: List<String>,
    onTopicClick: (String) -> Unit
) {
    // FlowRow from androidx.compose.foundation.layout is standard in Compose BOM
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        topics.forEach { tag ->
            Surface(
                onClick = { onTopicClick(tag) },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.height(34.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}
