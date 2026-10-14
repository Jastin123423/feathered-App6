package com.unera.social.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.*
import com.unera.social.ui.screens.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * InstagramVideoCard - Full Android native counterpart of web InstagramVideoCard.tsx
 *
 * Implements:
 * - 1:1 Instagram header with authentic story ring (gradient #1877F2 -> #0284C7 -> #38BDF8)
 * - Online status indicator badge
 * - Real VerifiedBadge
 * - Sponsored badge and headline, or Original Audio subtitle
 * - Follow/Following action button
 * - 'Watch in Videos' navigation pill
 * - PostMenu (Edit / Delete / Share / Copy Link / Report)
 * - 4:5 / 1:1 Aspect video media container with black background
 * - Play/Pause tap toggle with central icon ripple
 * - Double-tap bursting heart animation (Instagram iconic feature) with automatic 'love' react
 * - Top-Right '>' button to launch full video player / reels
 * - Top-Left 'HD' badge
 * - Bottom-Left audio track frosted pill
 * - Bottom-Right audio sound mute/unmute floating toggle
 * - Bottom #1877F2 scrubber progress bar
 * - Sponsored CTA action bar (destination domain + headline + CTA button)
 * - Reaction summary & discussions count row with overlapping reaction emojis
 * - Action bar: React button (with animated dock), Discuss button, Share button, Bookmark/Save button
 * - Formatted caption with clickable hashtags (#hashtag) and mentions (@mention)
 * - 'more' / 'less' expansion toggle
 * - Outside 'Add a discussion…' input row opening the comments modal
 * - Video Discussion modal with comment actions (like, reply, hide, delete), quick emoji bar, reply banner, and submission form
 * - Full ReactionsSheetDialog & ShareBottomSheetDialog integrations
 */
@Composable
fun InstagramVideoCard(
    post: Post? = null,
    reel: Reel? = null,
    author: User? = null,
    currentUser: User? = null,
    users: List<User> = emptyList(),
    stories: List<Story> = emptyList(),
    hasStory: Boolean? = null,
    autoplay: Boolean = true,
    reactionCount: Int? = null,
    myReaction: String? = null,
    commentCount: Int? = null,
    shareCount: Int? = null,
    emojiList: List<String> = emptyList(),
    reactionText: String? = null,
    onProfileClick: (Long) -> Unit = {},
    onStoryClick: ((Long) -> Unit)? = null,
    onReact: ((Post, String) -> Unit)? = null,
    onShare: ((Long, Int) -> Unit)? = null,
    onShareClick: ((Post) -> Unit)? = null,
    onVideoClick: ((Post) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
    onEdit: ((Long, String) -> Unit)? = null,
    isFollowing: Boolean = false,
    onFollow: ((Long) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onOpenComments: ((Post) -> Unit)? = null,
    onOpenReactions: ((Post) -> Unit)? = null,
    onCommentAdded: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Resolve unified Post entity
    val activePost: Post = remember(post, reel, author) {
        when {
            post != null -> post
            reel != null -> Post(
                id = reel.id,
                author = reel.author,
                content = reel.caption,
                mediaUrl = reel.videoUrl.ifBlank { reel.thumbnailUrl },
                postType = "video",
                likesCount = reel.likesCount,
                commentsCount = reel.commentsCount,
                sharesCount = reel.sharesCount,
                isLiked = reel.isLiked,
                isSaved = reel.isSaved,
                timestamp = "Recent"
            )
            else -> Post(
                id = 0L,
                author = author ?: User(0L, "Creator", "creator", ""),
                content = "",
                postType = "video"
            )
        }
    }

    val activePostId = activePost.id
    val resolvedAuthor: User = remember(author, activePost.author, reel?.author) {
        author ?: activePost.author
    }
    val authorId = resolvedAuthor.id
    val authorName = resolvedAuthor.name.ifBlank { "Creator" }
    val authorAvatar = resolvedAuthor.profileImageUrl.ifBlank {
        "https://ui-avatars.com/api/?name=${authorName}&background=1877F2&color=fff&bold=true"
    }

    // Story status check
    val userHasStory = remember(hasStory, stories, authorId) {
        if (hasStory != null) hasStory
        else stories.any { it.author.id == authorId }
    }

    val isOnline = resolvedAuthor.isOnline || (authorId == 1L || authorId == 2L)
    val isVerified = resolvedAuthor.isVerified

    // Sponsored check
    val isSponsored = activePost.isSponsored || activePost.postType == "sponsored"
    val sponsoredHeadline = activePost.sponsoredHeadline.ifBlank { "Promoted Post" }
    val sponsoredCtaText = activePost.sponsoredCtaText.ifBlank { "Learn More" }
    val sponsoredCtaUrl = activePost.sponsoredCtaUrl

    // Destination domain extraction
    val destinationDomain = remember(sponsoredCtaUrl) {
        if (sponsoredCtaUrl.isBlank()) "SPONSORED"
        else {
            try {
                val clean = sponsoredCtaUrl.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                clean.split("/").firstOrNull()?.uppercase() ?: "SPONSORED"
            } catch (_: Exception) {
                "SPONSORED"
            }
        }
    }

    // Dynamic player states
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showPlayRipple by remember { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var isCaptionExpanded by remember { mutableStateOf(false) }
    var localFollowing by remember { mutableStateOf(isFollowing) }

    // Dynamic reaction states
    var localReaction by remember { mutableStateOf(myReaction ?: activePost.myReaction) }
    var localReactionCount by remember { mutableIntStateOf(reactionCount ?: activePost.likesCount) }
    var showReactionDock by remember { mutableStateOf(false) }

    // Discussion and share states
    var localCommentCount by remember { mutableIntStateOf(commentCount ?: activePost.commentsCount) }
    var localShareCount by remember { mutableIntStateOf(shareCount ?: activePost.sharesCount) }
    var isSaved by remember { mutableStateOf(activePost.isSaved) }

    // Modal dialogs
    var showDiscussModal by remember { mutableStateOf(false) }
    var showReactionsSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    // Comments data for discussion modal
    var commentsList by remember(activePost.comments) {
        mutableStateOf(activePost.comments)
    }

    // 4-Tier Media Optimization:
    // Slot 3: Poster image (media_meta[0].thumb or thumbnail_url or media_urls[3])
    // Slot 2: 720p primary streaming (media_meta[0].feed or 720)
    // Slot 1: 1080p high quality (media_meta[0].full or 1080)
    // Slot 0: Original upload (media_meta[0].original)
    val posterUrl = remember(activePost) {
        val calculated = activePost.getPosterUrl()
        if (calculated.isNotBlank()) calculated
        else activePost.mediaUrl ?: activePost.mediaUrls.firstOrNull() ?: reel?.thumbnailUrl ?: ""
    }
    val mediaThumbnail = posterUrl

    var isHdEnabled by remember { mutableStateOf(false) }
    val defaultVideoUrl = remember(activePost) { activePost.getDefaultPlayableVideoUrl() }
    val hdVideoUrl = remember(activePost) { activePost.getHdPlayableVideoUrl() }
    val hasHdQuality = remember(activePost) { activePost.hasHdQuality() }
    val activeStreamingUrl = if (isHdEnabled && hdVideoUrl.isNotBlank()) hdVideoUrl else defaultVideoUrl

    // Media3 ExoPlayer instance for robust HTTPS / CDN streaming
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = if (isMuted) 0f else 1f
        }
    }
    var isVideoReady by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isVideoReady = true
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e("InstagramVideoCard", "Playback error on $activeStreamingUrl: ${error.message}", error)
                // Fallback to 720p if 1080p fails
                if (isHdEnabled && defaultVideoUrl.isNotBlank() && defaultVideoUrl != activeStreamingUrl) {
                    isHdEnabled = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(activeStreamingUrl, isPlaying) {
        if (isPlaying && activeStreamingUrl.isNotBlank()) {
            val currPos = exoPlayer.currentPosition
            val currentMediaUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
            if (currentMediaUri != activeStreamingUrl) {
                isVideoReady = false
                val mediaItem = MediaItem.fromUri(activeStreamingUrl)
                exoPlayer.setMediaItem(mediaItem)
                if (currPos > 0) {
                    exoPlayer.seekTo(currPos)
                }
                exoPlayer.prepare()
            }
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    LaunchedEffect(isPlaying, isVideoReady) {
        if (isPlaying && isVideoReady) {
            while (isPlaying) {
                val dur = exoPlayer.duration
                val pos = exoPlayer.currentPosition
                if (dur > 0) {
                    playbackProgress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                }
                kotlinx.coroutines.delay(200)
            }
        }
    }

    // Heart burst animation
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }

    fun triggerDoubleTapLike() {
        showHeartBurst = true
        coroutineScope.launch {
            if (localReaction != "love") {
                localReaction = "love"
                localReactionCount++
                onReact?.invoke(activePost, "love")
            }
            heartScale.snapTo(0.3f)
            heartAlpha.snapTo(1f)
            heartScale.animateTo(
                targetValue = 1.4f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            heartAlpha.animateTo(0f, animationSpec = tween(300))
            showHeartBurst = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .border(BorderStroke(0.5.dp, Color(0xFF1E293B)))
            .testTag("instagram_video_card_$activePostId")
    ) {
        // ==========================================
        // 1. INSTAGRAM HEADER
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Avatar with authentic Story Ring
                Box(
                    modifier = Modifier.clickable {
                        if (userHasStory && onStoryClick != null) {
                            onStoryClick(authorId)
                        } else {
                            onProfileClick(authorId)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .then(
                                if (userHasStory) {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(
                                                    Color(0xFF1877F2),
                                                    Color(0xFF0284C7),
                                                    Color(0xFF38BDF8),
                                                    Color(0xFF1877F2)
                                                )
                                            )
                                        )
                                        .padding(2.5.dp)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        AsyncImage(
                            model = authorAvatar,
                            contentDescription = authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        )
                    }

                    // Online green badge
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .border(2.dp, Color(0xFF0F172A), CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                // Author Info
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF8FAFC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onProfileClick(authorId) }
                        )

                        if (isVerified) {
                            VerifiedBadge(size = 15.dp)
                        }

                        Text(text = "•", color = Color(0xFF64748B), fontSize = 12.sp)

                        if (isSponsored) {
                            Surface(
                                color = Color(0xFF1877F2).copy(alpha = 0.20f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.40f))
                            ) {
                                Text(
                                    text = "Sponsored",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = activePost.timestamp,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Audio track subtitle or sponsored headline
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSponsored) {
                            Text(
                                text = sponsoredHeadline,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Original Audio • $authorName",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Top Right Header Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Follow Button (if not me)
                if (currentUser != null && currentUser.id != authorId) {
                    Button(
                        onClick = {
                            localFollowing = !localFollowing
                            onFollow?.invoke(authorId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (localFollowing) Color(0xFF1E293B) else Color(0xFF1877F2),
                            contentColor = if (localFollowing) Color(0xFF94A3B8) else Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (localFollowing) "Following" else "Follow",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // "Watch in Videos" Pill
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .clickable { onVideoClick?.invoke(activePost) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Videos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                // PostMenu (3-dots options)
                PostMenu(
                    item = activePost,
                    currentUser = currentUser,
                    onShare = { onShareClick?.invoke(activePost) ?: run { showShareSheet = true } },
                    onDeleteSuccess = { onDelete?.invoke(it) },
                    onEditSuccess = { onEdit?.invoke(it.id, it.content) }
                )
            }
        }

        // ==========================================
        // 2. VIDEO MEDIA CONTAINER (4:5 Aspect Ratio)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { triggerDoubleTapLike() },
                        onTap = {
                            isPlaying = !isPlaying
                            showPlayRipple = true
                            coroutineScope.launch {
                                delay(700)
                                showPlayRipple = false
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Video Poster Image (ALWAYS visible underneath so screen NEVER goes black while buffering)
            AsyncImage(
                model = posterUrl,
                contentDescription = "Video preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. High-performance Media3 PlayerView (visible when playing and prepared)
            if (activeStreamingUrl.isNotBlank()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            this.player = exoPlayer
                        }
                    },
                    update = { playerView ->
                        playerView.player = exoPlayer
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isPlaying && isVideoReady) 1f else 0f)
                )
            }

            // 3. Central Buffering Spinner while preparing stream
            if (isPlaying && !isVideoReady) {
                CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.size(36.dp)
                )
            }

            // HD Badge in top-left (interactive toggle between 720p default and 1080p full)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isHdEnabled) Color(0xFF1877F2) else Color.Black.copy(alpha = 0.65f))
                    .border(
                        1.dp,
                        if (isHdEnabled) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        isHdEnabled = !isHdEnabled
                        Toast.makeText(
                            context,
                            if (isHdEnabled) "1080p HD Quality Active" else "720p Standard Streaming (Data Saver)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("hd_quality_toggle")
            ) {
                Text(
                    text = if (isHdEnabled) "1080p HD" else "720p",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHdEnabled) Color.White else Color(0xFF38BDF8)
                )
            }

            // Top-Right '>' Button to open in full videos/reels player
            IconButton(
                onClick = { onVideoClick?.invoke(activePost) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open in Videos",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Central Play/Pause Ripple Indicator
            if (showPlayRipple || !isPlaying) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Double-Tap Bursting Heart Animation
            if (showHeartBurst) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier
                        .size(80.dp)
                        .scale(heartScale.value)
                        .alpha(heartAlpha.value)
                )
            }

            // Bottom-Left Audio Frosted Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Original Audio - $authorName",
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom-Right Sound Mute/Unmute Toggle
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Instagram Scrubber Progress Bar along bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.20f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (isPlaying) playbackProgress.coerceAtLeast(0.05f) else 0.15f)
                        .background(Color(0xFF1877F2))
                )
            }
        }

        // ==========================================
        // 3. SPONSORED CTA ACTION BAR (Instagram Ads Style)
        // ==========================================
        if (isSponsored) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1877F2))
                    .clickable {
                        try {
                            if (sponsoredCtaUrl.isNotBlank()) {
                                val url = if (sponsoredCtaUrl.startsWith("http")) sponsoredCtaUrl else "https://$sponsoredCtaUrl"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        } catch (_: Exception) {}
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = destinationDomain,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = sponsoredHeadline,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = sponsoredCtaText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 4. REACTION SUMMARY & DISCUSSION COUNTS ROW
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Overlapping reaction emojis + summary text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable {
                    if (onOpenReactions != null) onOpenReactions(activePost)
                    else showReactionsSheet = true
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    Text("👍", fontSize = 14.sp)
                    Text("❤️", fontSize = 14.sp)
                    Text("🔥", fontSize = 14.sp)
                }
                Text(
                    text = reactionText ?: (if (localReactionCount == 1) "1 Reaction" else "$localReactionCount Reactions"),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC)
                )
            }

            // Right side: Discussions and Shares counts
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "$localCommentCount ${if (localCommentCount == 1) "Discussion" else "Discussions"}",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.clickable {
                        if (onOpenComments != null) onOpenComments(activePost)
                        else showDiscussModal = true
                    }
                )
                if (localShareCount > 0) {
                    Text(
                        text = "$localShareCount ${if (localShareCount == 1) "Share" else "Shares"}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.clickable {
                            if (onShareClick != null) onShareClick(activePost)
                            else showShareSheet = true
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

        // Animated Reaction Dock
        AnimatedVisibility(visible = showReactionDock) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0B1120))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                REACTION_EMOJIS.forEach { (type, emoji) ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val nextReaction = if (localReaction == type) null else type
                                val nextCount = if (localReaction == type) maxOf(0, localReactionCount - 1)
                                else if (localReaction == null) localReactionCount + 1
                                else localReactionCount

                                localReaction = nextReaction
                                localReactionCount = nextCount
                                onReact?.invoke(activePost, type)
                                showReactionDock = false
                            }
                            .padding(4.dp)
                    )
                }
            }
        }

        // ==========================================
        // 5. ACTION BUTTONS ROW (React, Discuss, Share, Save)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. React Button
                TextButton(
                    onClick = {
                        if (localReaction != null) {
                            localReaction = null
                            localReactionCount = maxOf(0, localReactionCount - 1)
                        } else {
                            localReaction = "like"
                            localReactionCount++
                            onReact?.invoke(activePost, "like")
                        }
                    },
                    modifier = Modifier.testTag("like_reel_btn_$activePostId")
                ) {
                    val emoji = localReaction?.let { getReactionEmoji(it) }
                    if (emoji != null) {
                        Text(text = emoji, fontSize = 18.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "React",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = localReaction?.replaceFirstChar { it.uppercase() } ?: "React",
                        color = if (localReaction != null) Color(0xFFF43F5E) else Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Long press / hover trigger button for reaction dock
                IconButton(
                    onClick = { showReactionDock = !showReactionDock },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddReaction,
                        contentDescription = "All reactions",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 2. Discuss Button
                TextButton(
                    onClick = {
                        if (onOpenComments != null) onOpenComments(activePost)
                        else showDiscussModal = true
                    },
                    modifier = Modifier.testTag("discuss_reel_btn_$activePostId")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Discuss",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Discuss",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 3. Share Button
                TextButton(
                    onClick = {
                        if (onShareClick != null) onShareClick(activePost)
                        else showShareSheet = true
                    },
                    modifier = Modifier.testTag("share_reel_btn_$activePostId")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Share",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4. Save / Bookmark Button
            SavePostButton(
                isSaved = isSaved,
                onToggle = {
                    isSaved = !isSaved
                    Toast.makeText(
                        context,
                        if (isSaved) "Video saved to bookmarks" else "Video removed from bookmarks",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                isVideo = true,
                size = 22.dp,
                testTag = "save_reel_btn_$activePostId"
            )
        }

        // ==========================================
        // 6. CAPTION, HASHTAGS & OUTSIDE ADD DISCUSSION
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Formatted Caption with clickable hashtags
            if (activePost.content.isNotBlank()) {
                val fullText = activePost.content
                val displayedText = if (isCaptionExpanded || fullText.length <= 110) fullText else "${fullText.take(110)}..."

                val annotatedString = remember(displayedText, authorName) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))) {
                            append("$authorName ")
                        }
                        val words = displayedText.split(" ")
                        words.forEachIndexed { idx, word ->
                            when {
                                word.startsWith("#") && word.length > 1 -> {
                                    pushStringAnnotation(tag = "TAG", annotation = word)
                                    withStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)) {
                                        append(word)
                                    }
                                    pop()
                                }
                                word.startsWith("@") && word.length > 1 -> {
                                    pushStringAnnotation(tag = "MENTION", annotation = word)
                                    withStyle(SpanStyle(color = Color(0xFF60A5FA), fontWeight = FontWeight.Medium)) {
                                        append(word)
                                    }
                                    pop()
                                }
                                else -> {
                                    withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                                        append(word)
                                    }
                                }
                            }
                            if (idx < words.size - 1) append(" ")
                        }
                    }
                }

                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "TAG", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                onHashtagClick?.invoke(annotation.item)
                            }
                    }
                )

                if (fullText.length > 110) {
                    Text(
                        text = if (isCaptionExpanded) "less" else "more",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.clickable { isCaptionExpanded = !isCaptionExpanded }
                    )
                }
            }

            // "View all X discussions" clickable link
            if (localCommentCount > 0) {
                Text(
                    text = "View all $localCommentCount ${if (localCommentCount == 1) "discussion" else "discussions"}",
                    fontSize = 12.5.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        if (onOpenComments != null) onOpenComments(activePost)
                        else showDiscussModal = true
                    }
                )
            }

            // Outside "Add a discussion…" input field with current user avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B1120))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .clickable {
                        if (onOpenComments != null) onOpenComments(activePost)
                        else showDiscussModal = true
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val myAvatar = currentUser?.profileImageUrl?.ifBlank { null }
                    ?: "https://ui-avatars.com/api/?name=${encodeName(currentUser?.name ?: "You")}&background=1877F2&color=fff&bold=true"

                AsyncImage(
                    model = myAvatar,
                    contentDescription = "My avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )

                Text(
                    text = "Add a discussion…",
                    color = Color(0xFF64748B),
                    fontSize = 13.5.sp,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // ==========================================
    // 7. VIDEO DISCUSSION MODAL
    // ==========================================
    if (showDiscussModal) {
        VideoDiscussionModal(
            post = activePost,
            comments = commentsList,
            currentUser = currentUser,
            onClose = { showDiscussModal = false },
            onAddComment = { text ->
                val newComment = Comment(
                    id = System.currentTimeMillis(),
                    author = currentUser ?: User(1L, "You", "you", ""),
                    text = text,
                    timestamp = "Just now",
                    likesCount = 0
                )
                commentsList = listOf(newComment) + commentsList
                localCommentCount++
                onCommentAdded?.invoke()
            },
            onDeleteComment = { commentId ->
                commentsList = commentsList.filter { it.id != commentId }
                localCommentCount = maxOf(0, localCommentCount - 1)
            }
        )
    }

    // ==========================================
    // 8. REACTIONS SHEET DIALOG
    // ==========================================
    if (showReactionsSheet) {
        ReactionsSheetDialog(
            post = activePost,
            allUsers = users,
            onDismiss = { showReactionsSheet = false },
            onUserClick = { user -> onProfileClick(user.id) }
        )
    }

    // ==========================================
    // 9. SHARE BOTTOM SHEET DIALOG
    // ==========================================
    if (showShareSheet) {
        ShareBottomSheetDialog(
            post = activePost,
            onDismiss = { showShareSheet = false },
            onShareToFeed = {
                localShareCount++
                onShare?.invoke(activePostId, localShareCount)
                Toast.makeText(context, "Shared to Feed", Toast.LENGTH_SHORT).show()
                showShareSheet = false
            },
            onShareToGroup = {
                localShareCount++
                onShare?.invoke(activePostId, localShareCount)
                Toast.makeText(context, "Shared to Group", Toast.LENGTH_SHORT).show()
                showShareSheet = false
            },
            onSendMessage = {
                Toast.makeText(context, "Opening Direct Messages", Toast.LENGTH_SHORT).show()
                showShareSheet = false
            },
            onCopyLink = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Post Link", "https://featheredsocial.site/post/$activePostId")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                showShareSheet = false
            }
        )
    }
}

/**
 * Backward compatibility overload for VideoItem in ReelsScreen
 */
@Composable
fun InstagramVideoCard(
    videoItem: VideoItem,
    currentUser: User?,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val post = remember(videoItem) {
        Post(
            id = videoItem.id.removePrefix("post_").toLongOrNull() ?: System.currentTimeMillis(),
            author = videoItem.author,
            content = videoItem.caption,
            mediaUrl = videoItem.videoUrl,
            postType = "video",
            likesCount = videoItem.likesCount,
            commentsCount = videoItem.commentsCount,
            sharesCount = videoItem.sharesCount,
            isLiked = videoItem.isLiked,
            isSaved = videoItem.isSaved,
            timestamp = videoItem.timestamp
        )
    }

    InstagramVideoCard(
        post = post,
        currentUser = currentUser,
        onProfileClick = { onUserClick() },
        onOpenComments = { onComment() },
        onReact = { _, _ -> onLike() },
        modifier = modifier
    )
}

/**
 * Full Video Discussion Modal Dialog matching web
 */
@Composable
fun VideoDiscussionModal(
    post: Post,
    comments: List<Comment>,
    currentUser: User?,
    onClose: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingToUser by remember { mutableStateOf<String?>(null) }
    val quickEmojis = listOf("❤️", "🙌", "🔥", "👏", "😍", "😂", "😮", "💯")

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .clickable { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .widthIn(max = 560.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color(0xFF0B1120),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFF0B1120))
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Video Discussion",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${comments.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Comments List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No comments yet",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF8FAFC)
                                        )
                                        Text(
                                            text = "Be the first to start the discussion on this video!",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(comments, key = { it.id }) { comment ->
                                var commentLiked by remember { mutableStateOf(comment.isLiked) }
                                var commentLikesCount by remember { mutableIntStateOf(comment.likesCount) }
                                var isHidden by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (isHidden) Modifier.alpha(0.6f) else Modifier),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = comment.author.profileImageUrl.ifBlank {
                                            "https://ui-avatars.com/api/?name=${comment.author.name}&background=1877F2&color=fff"
                                        },
                                        contentDescription = comment.author.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color(0xFF1E293B), CircleShape)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = comment.author.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = Color(0xFFF8FAFC)
                                            )
                                            Text(
                                                text = comment.timestamp,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            if (isHidden) {
                                                Surface(
                                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Hidden",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFFBBF24),
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = comment.text,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFFCBD5E1),
                                            lineHeight = 18.sp
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Sub-action buttons (Like, Reply, Hide, Delete)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.clickable {
                                                    commentLiked = !commentLiked
                                                    if (commentLiked) commentLikesCount++ else commentLikesCount = maxOf(0, commentLikesCount - 1)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (commentLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                    contentDescription = "Like comment",
                                                    tint = if (commentLiked) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (commentLikesCount > 0) "$commentLikesCount" else "Like",
                                                    fontSize = 11.5.sp,
                                                    color = if (commentLiked) Color(0xFFEF4444) else Color(0xFF94A3B8)
                                                )
                                            }

                                            Text(
                                                text = "Reply",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.clickable {
                                                    replyingToUser = comment.author.name
                                                    commentText = "@${comment.author.name} "
                                                }
                                            )

                                            Text(
                                                text = if (isHidden) "Unhide" else "Hide",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.clickable { isHidden = !isHidden }
                                            )

                                            if (currentUser?.id == comment.author.id || currentUser?.id == 1L) {
                                                Text(
                                                    text = "Delete",
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFFFB7185),
                                                    modifier = Modifier.clickable { onDeleteComment(comment.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Emoji Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B1120))
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { commentText += emoji }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Replying Banner
                    if (replyingToUser != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Replying to @$replyingToUser",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                            IconButton(
                                onClick = {
                                    replyingToUser = null
                                    commentText = ""
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Comment Submission Form
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val myAvatar = currentUser?.profileImageUrl?.ifBlank { null }
                            ?: "https://ui-avatars.com/api/?name=${encodeName(currentUser?.name ?: "You")}&background=1877F2&color=fff&bold=true"

                        AsyncImage(
                            model = myAvatar,
                            contentDescription = "My avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )

                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = {
                                Text("Share your thoughts on this video…", color = Color(0xFF64748B), fontSize = 13.5.sp)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val text = commentText.trim()
                                if (text.isNotBlank()) {
                                    onAddComment(text)
                                    commentText = ""
                                    replyingToUser = null
                                }
                            },
                            enabled = commentText.isNotBlank(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1877F2),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Post", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun encodeName(name: String): String {
    return name.replace(" ", "+")
}
