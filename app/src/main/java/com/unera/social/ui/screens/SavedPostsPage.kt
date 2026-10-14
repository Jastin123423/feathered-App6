package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import com.unera.social.ui.components.VerifiedBadge
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Styling constants matching web theme
private val PageBg = Color(0xFF0B1120)
private val CardBg = Color(0xFF0F172A)
private val BorderDark = Color(0xFF1E293B)
private val BorderAccent = Color(0xFF38BDF8).copy(alpha = 0.4f)
private val AccentGold = Color(0xFFF59E0B)
private val TextWhite = Color(0xFFF8FAFC)
private val TextGray = Color(0xFF94A3B8)
private val TabActive = Color(0xFF38BDF8)
private val PrimaryBlue = Color(0xFF1877F2)
private val LikeRed = Color(0xFFEF4444)

/**
 * 1:1 Implementation of web's SavedPostsPage component.
 * Features:
 * - Dynamic Feed / Grid view modes
 * - 5-item pagination with "See More" animated button
 * - Video & regular post card rendering with author metadata
 * - Grid cards with video badges, center play buttons, discuss and remove actions
 * - Interactive Lightbox image viewer
 * - Interactive Video preview modal
 * - Interactive Comments modal dialog
 * - Local-first empty state with "Explore Posts" navigation
 */
@Composable
fun SavedPostsPage(
    currentUser: User?,
    users: List<User> = emptyList(),
    posts: List<Post> = emptyList(),
    reels: List<Reel> = emptyList(),
    onBack: (() -> Unit)? = null,
    onProfileClick: ((Long) -> Unit)? = null,
    onVideoClick: ((Reel) -> Unit)? = null,
    onViewImage: ((String) -> Unit)? = null,
    onOpenComments: ((Post) -> Unit)? = null,
    onReact: ((Long, String) -> Unit)? = null,
    onLikePost: ((Long) -> Unit)? = null,
    onCommentPost: ((Post) -> Unit)? = null,
    onSavePost: ((Long) -> Unit)? = null,
    onSaveReel: ((Long) -> Unit)? = null,
    onShare: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf("feed") } // "feed" or "grid"
    val pageSize = 5
    var visibleCount by remember { mutableIntStateOf(pageSize) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Modals & Dialogs state
    var lightboxImageUrl by remember { mutableStateOf<String?>(null) }
    var activeVideoPreview by remember { mutableStateOf<SavedPostItem?>(null) }
    var activeCommentingItem by remember { mutableStateOf<SavedPostItem?>(null) }
    var newCommentText by remember { mutableStateOf("") }
    var localRemovedIds by remember { mutableStateOf(setOf<Long>()) }

    // Combine saved posts and saved reels
    val allSavedItems = remember(posts, reels, localRemovedIds) {
        val savedPostItems = posts
            .filter { it.isSaved && it.id !in localRemovedIds }
            .map { post ->
                SavedPostItem(
                    id = post.id,
                    type = if (!post.mediaUrl.isNullOrBlank() && post.mediaUrl.contains("video", ignoreCase = true)) "video" else "normal",
                    post = post
                )
            }

        val savedReelItems = reels
            .filter { it.isSaved && it.id !in localRemovedIds }
            .map { reel ->
                SavedPostItem(
                    id = reel.id,
                    type = "video",
                    reel = reel
                )
            }

        (savedPostItems + savedReelItems).sortedByDescending { it.savedAt }
    }

    val visibleItems = remember(allSavedItems, visibleCount) {
        allSavedItems.take(visibleCount)
    }
    val hasMore = visibleCount < allSavedItems.size

    val handleSeeMore = {
        isLoadingMore = true
        coroutineScope.launch {
            delay(250)
            visibleCount += pageSize
            isLoadingMore = false
        }
    }

    val handleRemove = { itemId: Long ->
        localRemovedIds = localRemovedIds + itemId
        onSavePost?.invoke(itemId)
        onSaveReel?.invoke(itemId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .testTag("saved_posts_page")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
        ) {
            // Flat Top Header & Controls (matches web layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                                .testTag("saved_posts_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bookmark icon badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Saved Posts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            lineHeight = 22.sp
                        )
                        Text(
                            text = "${allSavedItems.size} saved item${if (allSavedItems.size == 1) "" else "s"} • Stored locally",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }

                // Grid / List Toggle Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewMode = "feed" },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewMode == "feed") Color(0xFF1E293B) else Color.Transparent)
                            .testTag("saved_posts_feed_view_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Feed view",
                            tint = if (viewMode == "feed") TabActive else TextGray,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewMode = "grid" },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewMode == "grid") Color(0xFF1E293B) else Color.Transparent)
                            .testTag("saved_posts_grid_view_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid view",
                            tint = if (viewMode == "grid") TabActive else TextGray,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Main Content Area
            if (allSavedItems.isEmpty()) {
                // Empty state matching web: rounded-2xl p-8 sm:p-12 text-center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PageBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No saved posts yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Tap the bookmark icon in the action bar of any post or reel to save it here. Your saved items are preserved locally on this device.",
                                fontSize = 13.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (onBack != null) {
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Explore Posts",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (viewMode == "grid") {
                    // Grid View: 2 columns (adaptive on wide displays)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(visibleItems, key = { "grid-${it.id}" }) { item ->
                            SavedGridItemCard(
                                item = item,
                                onCardClick = {
                                    if (item.type == "video") {
                                        if (item.reel != null && onVideoClick != null) {
                                            onVideoClick(item.reel)
                                        } else {
                                            activeVideoPreview = item
                                        }
                                    } else {
                                        val media = item.mediaUrl
                                        if (!media.isNullOrBlank()) {
                                            lightboxImageUrl = media
                                            onViewImage?.invoke(media)
                                        } else {
                                            activeCommentingItem = item
                                            if (item.post != null) onOpenComments?.invoke(item.post)
                                        }
                                    }
                                },
                                onDiscussClick = {
                                    activeCommentingItem = item
                                    if (item.post != null) onOpenComments?.invoke(item.post)
                                },
                                onRemoveClick = { handleRemove(item.id) },
                                onAuthorClick = {
                                    val authorId = item.author.id
                                    onProfileClick?.invoke(authorId)
                                }
                            )
                        }

                        if (hasMore) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                SeeMoreButton(
                                    isLoading = isLoadingMore,
                                    onClick = { handleSeeMore() }
                                )
                            }
                        }
                    }
                } else {
                    // Feed View: Stream of full cards separated by dividers
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 40.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(visibleItems, key = { "feed-${it.id}" }) { item ->
                            SavedFeedItemCard(
                                item = item,
                                currentUser = currentUser,
                                onProfileClick = {
                                    val authorId = item.author.id
                                    onProfileClick?.invoke(authorId)
                                },
                                onLike = {
                                    onLikePost?.invoke(item.id)
                                    onReact?.invoke(item.id, "like")
                                },
                                onComment = {
                                    activeCommentingItem = item
                                    if (item.post != null) onOpenComments?.invoke(item.post)
                                },
                                onShare = {
                                    if (item.post != null) onShare?.invoke(item.post)
                                },
                                onRemove = { handleRemove(item.id) },
                                onImageClick = { url ->
                                    lightboxImageUrl = url
                                    onViewImage?.invoke(url)
                                },
                                onVideoClick = {
                                    if (item.reel != null && onVideoClick != null) {
                                        onVideoClick(item.reel)
                                    } else {
                                        activeVideoPreview = item
                                    }
                                }
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = BorderDark
                            )
                        }

                        if (hasMore) {
                            item {
                                SeeMoreButton(
                                    isLoading = isLoadingMore,
                                    onClick = { handleSeeMore() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 1. LIGHTBOX IMAGE VIEWER
        // ==========================================
        if (lightboxImageUrl != null) {
            Dialog(
                onDismissRequest = { lightboxImageUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { lightboxImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = lightboxImageUrl,
                        contentDescription = "Full view image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    IconButton(
                        onClick = { lightboxImageUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. VIDEO PREVIEW MODAL
        // ==========================================
        if (activeVideoPreview != null) {
            val videoItem = activeVideoPreview!!
            var isVideoPlaying by remember { mutableStateOf(true) }
            var isVideoMuted by remember { mutableStateOf(false) }

            Dialog(
                onDismissRequest = { activeVideoPreview = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Video container preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { isVideoPlaying = !isVideoPlaying },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = videoItem.mediaUrl ?: videoItem.reel?.thumbnailUrl,
                            contentDescription = "Video preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Play / Pause central indicator
                        AnimatedVisibility(
                            visible = !isVideoPlaying,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = videoItem.author.profileImageUrl,
                                contentDescription = videoItem.author.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Column {
                                Text(
                                    text = videoItem.author.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "@${videoItem.author.username}",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { isVideoMuted = !isVideoMuted },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = if (isVideoMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Toggle sound",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { activeVideoPreview = null },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Bottom info overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        if (videoItem.content.isNotBlank()) {
                            Text(
                                text = videoItem.content,
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = if (videoItem.isLiked) LikeRed else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${videoItem.likesCount}",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${videoItem.commentsCount}",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { handleRemove(videoItem.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = AccentGold
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saved", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. COMMENTS MODAL DIALOG
        // ==========================================
        if (activeCommentingItem != null) {
            val item = activeCommentingItem!!
            val post = item.post
            val commentsList = post?.comments ?: emptyList()

            Dialog(
                onDismissRequest = { activeCommentingItem = null }
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Comments (${commentsList.size})",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            IconButton(
                                onClick = { activeCommentingItem = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = BorderDark
                        )

                        // Comments scrollable list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (commentsList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No comments yet. Start the conversation!",
                                            color = TextGray,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(commentsList) { comment ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AsyncImage(
                                            model = comment.author.profileImageUrl,
                                            contentDescription = comment.author.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF162137).copy(alpha = 0.5f))
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = comment.author.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextWhite
                                                )
                                                Text(
                                                    text = comment.timestamp,
                                                    fontSize = 11.sp,
                                                    color = TextGray
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = comment.text,
                                                fontSize = 13.sp,
                                                color = TextWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Input field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = { Text("Write a comment...", color = TextGray, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TabActive,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedContainerColor = Color(0xFF0B1120),
                                    unfocusedContainerColor = Color(0xFF0B1120)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        if (post != null) {
                                            onCommentPost?.invoke(post)
                                        }
                                        newCommentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send comment",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grid Item Card for Saved Posts
 * Matches web aspect-[4/5] rounded-xl overflow-hidden hover:border-[#38BDF8]/40
 */
@Composable
private fun SavedGridItemCard(
    item: SavedPostItem,
    onCardClick: () -> Unit,
    onDiscussClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050B18)),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable(onClick = onCardClick)
            .testTag("saved_grid_item_${item.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val media = item.mediaUrl ?: item.reel?.thumbnailUrl

            if (!media.isNullOrBlank()) {
                AsyncImage(
                    model = media,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (item.type == "video") {
                    // Dark scrim for video
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Top-left "Video" badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = TabActive,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Video",
                            color = TabActive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Text-only post in grid
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"${item.content.ifBlank { "Post" }}\"",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top-right action buttons (Discuss & Remove)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onDiscussClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .testTag("saved_grid_discuss_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Discuss",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .testTag("saved_grid_remove_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Remove from saved",
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom user bar gradient
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clickable(onClick = onAuthorClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = item.author.profileImageUrl,
                        contentDescription = item.author.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(0.8.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )

                    Text(
                        text = item.author.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Feed Item Card for Saved Posts
 * Supports rich video cards and full regular post cards with full engagement bar
 */
@Composable
private fun SavedFeedItemCard(
    item: SavedPostItem,
    currentUser: User?,
    onProfileClick: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLikedState by remember { mutableStateOf(item.isLiked) }
    var likesCountState by remember { mutableIntStateOf(item.likesCount) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("saved_feed_item_${item.id}")
    ) {
        // Author Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onProfileClick)
            ) {
                AsyncImage(
                    model = item.author.profileImageUrl,
                    contentDescription = item.author.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, BorderDark, CircleShape)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.author.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                        if (item.author.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(size = 14.dp)
                        }
                    }
                    Text(
                        text = "@${item.author.username} • ${item.post?.timestamp ?: "Saved"}",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }

            // Remove from saved button in top right
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                    .testTag("saved_feed_remove_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Remove from saved",
                    tint = AccentGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Content Text
        if (item.content.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextWhite,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        // Media (Image or Video)
        val mediaUrl = item.mediaUrl ?: item.reel?.thumbnailUrl
        if (!mediaUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (item.type == "video") onVideoClick()
                        else onImageClick(mediaUrl)
                    }
            ) {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = "Saved media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )

                if (item.type == "video") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Top-left video badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = TabActive,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Video",
                            color = TabActive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Engagement Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        isLikedState = !isLikedState
                        likesCountState = if (isLikedState) likesCountState + 1 else (likesCountState - 1).coerceAtLeast(0)
                        onLike()
                    }
                ) {
                    Icon(
                        imageVector = if (isLikedState) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLikedState) LikeRed else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$likesCountState",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLikedState) LikeRed else TextGray
                    )
                }

                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onComment)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.commentsCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                }

                // Share button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onShare)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.sharesCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                }
            }

            // Saved indicator badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentGold.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Saved",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentGold
                )
            }
        }
    }
}

/**
 * See More Pagination Button
 * Matches web's animated spinner button
 */
@Composable
private fun SeeMoreButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E293B),
                contentColor = TextWhite
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
            modifier = Modifier.testTag("saved_posts_see_more_btn")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Loading...",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextWhite
                )
            } else {
                Text(
                    text = "See More",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextWhite
                )
            }
        }
    }
}
