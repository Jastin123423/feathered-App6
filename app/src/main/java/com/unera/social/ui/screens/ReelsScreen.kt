package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unera.social.data.VideosApiService
import com.unera.social.model.*
import com.unera.social.ui.components.InstagramVideoCard
import com.unera.social.ui.theme.*
import kotlinx.coroutines.launch

// Web Palette Constants matching VideosPage.tsx
private val WebDarkBg = Color(0xFF0B1120)
private val WebCardBg = Color(0xFF0F172A)
private val WebBorder = Color(0xFF1E293B)
private val WebBlue = Color(0xFF1877F2)
private val WebSkyBlue = Color(0xFF38BDF8)
private val WebTextPrimary = Color(0xFFF8FAFC)
private val WebTextSecondary = Color(0xFF94A3B8)
private val WebTextMuted = Color(0xFF64748B)

private const val VIDEOS_PER_PAGE = 15

data class VideoItem(
    val id: String,
    val author: User,
    val videoUrl: String,
    val thumbnailUrl: String,
    val caption: String,
    val musicTitle: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val isLiked: Boolean,
    val isSaved: Boolean = false,
    val isSponsored: Boolean = false,
    val timestamp: String = "Recent",
    val originalPost: Post? = null,
    val originalReel: Reel? = null
)

/**
 * VideosPage / ReelsScreen - Native 1:1 Implementation of web VideosPage.tsx
 *
 * 1. Clean header (Back button + 'Videos' title).
 * 2. Real data from live backend:
 *    - Ads fetched via GET /api/ads/feeds (if ads prop is empty)
 *    - Videos extracted from live feed/posts (media_type == 'video' || resolvable video URL)
 *    - Zero demo or mock video data
 * 3. Mixed organic and sponsored items with anti-clustering spacing.
 * 4. 15 videos per page with full pagination controls.
 * 5. Full native interaction callbacks wired (React, Discuss, Share, Follow, Profile, Story).
 */
@Composable
fun ReelsScreen(
    posts: List<Post> = emptyList(),
    ads: List<SponsoredFeedAd> = emptyList(),
    reels: List<Reel> = emptyList(),
    currentUser: User? = null,
    users: List<User> = emptyList(),
    stories: List<Story> = emptyList(),
    isLoading: Boolean = false,
    onReact: ((Post, String) -> Unit)? = null,
    onOpenComments: ((Post) -> Unit)? = null,
    onShare: ((Long, Int) -> Unit)? = null,
    onFollow: ((Long) -> Unit)? = null,
    onProfileClick: ((Long) -> Unit)? = null,
    onStoryClick: ((Long) -> Unit)? = null,
    onLikeReel: ((Long) -> Unit)? = null,
    onSavePost: ((Long) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    // Backward compatibility parameter aliases
    onLikePost: ((Long) -> Unit)? = null,
    onCommentPost: ((Post) -> Unit)? = null,
    onUserClick: ((User) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(1) }
    val coroutineScope = rememberCoroutineScope()

    // 1. Direct fetch for sponsored ads: GET /api/ads/feeds (VideosPage.tsx parity)
    var internalAds by remember { mutableStateOf<List<SponsoredFeedAd>>(ads) }
    LaunchedEffect(ads) {
        if (ads.isNotEmpty()) {
            internalAds = ads
            return@LaunchedEffect
        }
        coroutineScope.launch {
            val fetched = VideosApiService.fetchFeedAds()
            if (fetched.isNotEmpty()) {
                internalAds = fetched
            }
        }
    }

    // 2. Direct fetch for live videos if posts prop is empty:
    // Primary: GET /api/feeds?userId={meId}&limit=50, Fallback: GET /api/posts
    var internalPosts by remember { mutableStateOf<List<Post>>(posts) }
    LaunchedEffect(posts) {
        if (posts.isNotEmpty()) {
            internalPosts = posts
            return@LaunchedEffect
        }
        coroutineScope.launch {
            val fetched = VideosApiService.fetchVideos(userId = currentUser?.id, limit = 50)
            if (fetched.isNotEmpty()) {
                internalPosts = fetched
            }
        }
    }

    // 3. Extract, normalize, and unify ALL videos from feed posts and reels (No demo videos)
    val allVideos = remember(internalPosts, reels) {
        val videoMap = LinkedHashMap<String, Post>()

        // Process feed posts that are videos
        internalPosts.forEach { post ->
            val defaultUrl = post.getDefaultPlayableVideoUrl().takeIf { it.isNotBlank() }
            val vUrl = post.videoUrl?.takeIf { it.isNotBlank() }
                ?: post.mediaUrl?.takeIf { isVideoUrl(it) }
                ?: defaultUrl?.takeIf { isVideoUrl(it) }

            val isVideo = post.mediaType == "video" ||
                    post.postType == "video" ||
                    post.mediaTypes.contains("video") ||
                    post.mediaMeta.any { it.type == "video" || it.feed != null || it.resolution720 != null } ||
                    vUrl != null

            if (isVideo && (vUrl != null || defaultUrl != null || (post.mediaUrl != null && isVideoUrl(post.mediaUrl)))) {
                val resolvedUrl = vUrl ?: defaultUrl ?: post.mediaUrl ?: ""
                val key = if (resolvedUrl.isNotBlank()) resolvedUrl else "post_${post.id}"
                if (!videoMap.containsKey(key)) {
                    val finalMeta = if (post.mediaMeta.isNotEmpty()) {
                        post.mediaMeta
                    } else {
                        listOf(
                            MediaMetaItem(
                                feed = resolvedUrl,
                                full = post.getHdPlayableVideoUrl().ifBlank { resolvedUrl },
                                resolution720 = resolvedUrl,
                                resolution1080 = post.getHdPlayableVideoUrl().ifBlank { resolvedUrl },
                                thumb = post.getPosterUrl(),
                                thumbnailUrl = post.getPosterUrl(),
                                type = "video"
                            )
                        )
                    }
                    val finalMediaUrls = if (post.mediaUrls.isNotEmpty()) {
                        post.mediaUrls
                    } else {
                        listOf(resolvedUrl)
                    }
                    videoMap[key] = post.copy(
                        postType = "video",
                        mediaType = "video",
                        videoUrl = resolvedUrl,
                        mediaUrl = resolvedUrl,
                        mediaMeta = finalMeta,
                        mediaUrls = finalMediaUrls,
                        createdAt = post.createdAt ?: post.timestamp
                    )
                }
            }
        }

        // Process reels (if any real reels exist)
        reels.forEach { reel ->
            val vUrl = reel.videoUrl.takeIf { it.isNotBlank() }
            if (vUrl != null) {
                val key = vUrl
                if (!videoMap.containsKey(key)) {
                    videoMap[key] = Post(
                        id = reel.id,
                        author = reel.author,
                        content = reel.caption,
                        mediaUrl = vUrl,
                        videoUrl = vUrl,
                        mediaType = "video",
                        postType = "video",
                        timestamp = "Recent",
                        likesCount = reel.likesCount,
                        commentsCount = reel.commentsCount,
                        sharesCount = reel.sharesCount,
                        isLiked = reel.isLiked,
                        createdAt = "Recent",
                        userId = reel.author.id
                    )
                }
            }
        }

        videoMap.values.toList()
    }

    // 4. Mix organic videos and sponsored ads with anti-clustering spacing
    val mixedVideos = remember(allVideos, internalAds, internalPosts, users) {
        if (internalAds.isEmpty()) {
            allVideos
        } else {
            val result = mutableListOf<Post>()
            var adIndex = 0
            val minSpacing = 4

            allVideos.forEachIndexed { index, videoPost ->
                result.add(videoPost)
                if ((index + 1) % minSpacing == 0 && adIndex < internalAds.size) {
                    val ad = internalAds[adIndex % internalAds.size]
                    adIndex++
                    result.add(transformAdToPost(ad, internalPosts, users))
                }
            }

            // If allVideos has zero organic items, show live sponsored video/feed ads
            if (allVideos.isEmpty()) {
                internalAds.forEach { ad ->
                    result.add(transformAdToPost(ad, internalPosts, users))
                }
            }

            result
        }
    }

    // 4. Pagination calculation: 15 videos per page (VIDEOS_PER_PAGE = 15)
    val totalVideos = mixedVideos.size
    val totalPages = maxOf(1, (totalVideos + VIDEOS_PER_PAGE - 1) / VIDEOS_PER_PAGE)
    val startIndex = (currentPage - 1) * VIDEOS_PER_PAGE
    val endIndex = minOf(startIndex + VIDEOS_PER_PAGE, totalVideos)
    val currentVideos = if (startIndex < totalVideos) mixedVideos.subList(startIndex, endIndex) else emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WebDarkBg)
            .testTag("videos_page_container")
    ) {
        // 1. CLEAN VIDEO PAGE HEADER (No Videos icon, No videos posted text, No Post Video button, No Share prompt)
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WebDarkBg)
                    .border(width = 1.dp, color = WebBorder)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WebCardBg)
                            .border(1.dp, WebBorder, RoundedCornerShape(8.dp))
                            .testTag("videos_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = WebTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Videos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WebTextPrimary
                    )
                }

                if (onRefresh != null) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WebCardBg)
                            .border(1.dp, WebBorder, RoundedCornerShape(8.dp))
                            .testTag("videos_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Videos",
                            tint = if (isLoading) WebSkyBlue else WebTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. VIDEOS STREAM - AUTOPLAY ENABLED
        if (isLoading && mixedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WebDarkBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = WebBlue,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Loading live videos…",
                        color = WebTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (currentVideos.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("videos_feed_list"),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(currentVideos, key = { it.id }) { videoPost ->
                    InstagramVideoCard(
                        post = videoPost,
                        currentUser = currentUser,
                        users = users,
                        stories = stories,
                        autoplay = false,
                        onProfileClick = { authorId ->
                            if (onProfileClick != null) onProfileClick(authorId)
                            else onUserClick?.invoke(videoPost.author)
                        },
                        onStoryClick = { storyAuthorId ->
                            onStoryClick?.invoke(storyAuthorId)
                        },
                        onReact = { p, type ->
                            if (onReact != null) onReact(p, type)
                            else onLikePost?.invoke(p.id)
                        },
                        onOpenComments = { p ->
                            if (onOpenComments != null) onOpenComments(p)
                            else onCommentPost?.invoke(p)
                        },
                        onShare = { postId, count ->
                            onShare?.invoke(postId, count)
                        },
                        onFollow = { authorId ->
                            onFollow?.invoke(authorId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(1.dp).background(WebBorder))
                }

                // 3. PAGINATION AFTER 15 VIDEOS
                if (totalPages > 1) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            color = WebDarkBg,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WebBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val startIdx = startIndex + 1
                                val endIdx = endIndex
                                Text(
                                    text = "Showing $startIdx–$endIdx of $totalVideos videos",
                                    fontSize = 12.sp,
                                    color = WebTextSecondary
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { if (currentPage > 1) currentPage-- },
                                        enabled = currentPage > 1,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WebCardBg,
                                            contentColor = WebTextPrimary,
                                            disabledContainerColor = WebCardBg.copy(alpha = 0.5f),
                                            disabledContentColor = WebTextMuted
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Prev",
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Prev", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Page numbers buttons
                                    for (pageNum in 1..totalPages) {
                                        if (totalPages > 6 &&
                                            pageNum != 1 &&
                                            pageNum != totalPages &&
                                            kotlin.math.abs(pageNum - currentPage) > 1
                                        ) {
                                            if (pageNum == 2 || pageNum == totalPages - 1) {
                                                Text(
                                                    text = "…",
                                                    fontSize = 11.sp,
                                                    color = WebTextMuted,
                                                    modifier = Modifier.padding(horizontal = 2.dp)
                                                )
                                            }
                                            continue
                                        }

                                        val isActive = pageNum == currentPage
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isActive) WebBlue else WebCardBg)
                                                .border(1.dp, if (isActive) WebBlue else WebBorder, RoundedCornerShape(8.dp))
                                                .clickable { currentPage = pageNum },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pageNum.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) Color.White else WebTextSecondary
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { if (currentPage < totalPages) currentPage++ },
                                        enabled = currentPage < totalPages,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WebCardBg,
                                            contentColor = WebTextPrimary,
                                            disabledContainerColor = WebCardBg.copy(alpha = 0.5f),
                                            disabledContentColor = WebTextMuted
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Next", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Next",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Empty State (1:1 Web Parity)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(WebCardBg)
                        .border(1.dp, WebBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = WebSkyBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Videos Available",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WebTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Videos from the feed and reels will appear here.",
                    fontSize = 13.sp,
                    color = WebTextSecondary
                )
                if (onRefresh != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WebBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetch Live Videos", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun transformAdToPost(ad: SponsoredFeedAd, posts: List<Post>, users: List<User>): Post {
    val originalPost = posts.find { it.id == ad.id }
    val matchedUser = users.find { it.id == (ad.brandId ?: ad.id) }
    val authorName = originalPost?.author?.name ?: matchedUser?.name ?: ad.sponsorName.ifBlank { "Sponsored" }
    val authorAvatar = originalPost?.author?.profileImageUrl ?: matchedUser?.profileImageUrl ?: ad.sponsorAvatar
    val authorId = originalPost?.author?.id ?: matchedUser?.id ?: -(ad.id.coerceAtLeast(1L))

    val authorObj = User(
        id = authorId,
        name = authorName,
        username = originalPost?.author?.username ?: matchedUser?.username ?: "@${authorName.lowercase().replace(" ", "")}",
        profileImageUrl = authorAvatar.ifBlank { "https://ui-avatars.com/api/?name=${authorName}&background=1877F2&color=fff" },
        isVerified = originalPost?.author?.isVerified ?: matchedUser?.isVerified ?: true
    )

    val mediaUrl = ad.videoUrl ?: ad.imageUrl ?: originalPost?.mediaUrl ?: ""
    val isVideo = ad.isVideo || (ad.videoUrl != null) || isVideoUrl(mediaUrl)

    return Post(
        id = -(ad.id.coerceAtLeast(1L)),
        author = authorObj,
        content = originalPost?.content ?: ad.headline.ifBlank { ad.description },
        mediaUrl = mediaUrl,
        mediaUrls = if (mediaUrl.isNotBlank()) listOf(mediaUrl) else emptyList(),
        videoUrl = if (isVideo) mediaUrl else null,
        postType = if (isVideo) "video" else "sponsored",
        mediaType = if (isVideo) "video" else "image",
        timestamp = "Sponsored",
        isSponsored = true,
        sponsoredHeadline = ad.headline.ifBlank { ad.title },
        sponsoredCtaText = ad.ctaText.ifBlank { "Learn More" },
        sponsoredCtaUrl = ad.ctaUrl,
        likesCount = originalPost?.likesCount ?: 0,
        commentsCount = originalPost?.commentsCount ?: 0,
        sharesCount = originalPost?.sharesCount ?: 0,
        createdAt = originalPost?.createdAt ?: "Sponsored",
        userId = authorId
    )
}

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
