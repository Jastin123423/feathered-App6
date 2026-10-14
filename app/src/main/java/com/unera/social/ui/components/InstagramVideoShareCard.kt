package com.unera.social.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.Post
import com.unera.social.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun formatCount(count: Long): String {
    if (count <= 0) return "0"
    if (count >= 1_000_000) {
        val formatted = "%.1f".format(count / 1_000_000.0)
        return if (formatted.endsWith(".0")) "${formatted.substringBefore(".0")}M" else "${formatted}M"
    }
    if (count >= 1_000) {
        val formatted = "%.1f".format(count / 1_000.0)
        return if (formatted.endsWith(".0")) "${formatted.substringBefore(".0")}k" else "${formatted}k"
    }
    return count.toString()
}

/**
 * InstagramVideoShareCard - Native Jetpack Compose implementation of InstagramVideoShareCard.tsx.
 *
 * Implements:
 * - Nested Creator Header with Avatar, VerifiedBadge, @username, and quick-open chevron button
 * - Original caption with hashtag (#) & mention (@) rich formatting and expand/collapse
 * - 4:5 video media player container with poster preview
 * - Tap to play/pause with animated central ripple icon
 * - Double-tap to trigger heart burst animation
 * - Top-right watch in full player button
 * - Bottom-left frosted audio track pill ("Original audio - {creator}")
 * - Bottom-right mute/unmute toggle button
 * - Progress scrubber bar at the bottom
 * - Footer stats bar displaying views, likes, comments, and "Watch video →" link
 */
@Composable
fun InstagramVideoShareCard(
    post: Post? = null,
    originalPost: Post? = post?.sharedPost,
    ownerAuthor: User? = originalPost?.author,
    currentUser: User? = null,
    users: List<User> = emptyList(),
    onProfileClick: (Long) -> Unit,
    onVideoClick: ((Post) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val targetPost = originalPost ?: post ?: return

    val author = ownerAuthor ?: targetPost.author
    val authorName = author.name.ifBlank { "Creator" }
    val authorUsername = author.username.ifBlank { "creator" }
    val isVerified = author.isVerified
    val authorId = author.id

    val captionText = targetPost.content
    val posterUrl = targetPost.mediaUrl ?: targetPost.song?.cover ?: ""
    val audioTitle = targetPost.song?.title ?: "Original audio - $authorName"

    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var showPlayRipple by remember { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var isCaptionExpanded by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Double-tap heart animation
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }

    val triggerDoubleTapLike = {
        coroutineScope.launch {
            showHeartBurst = true
            heartScale.snapTo(0.3f)
            heartAlpha.snapTo(1f)
            heartScale.animateTo(1.35f, tween(250, easing = FastOutSlowInEasing))
            delay(150)
            heartAlpha.animateTo(0f, tween(200))
            showHeartBurst = false
        }
    }

    // Simulated playback progress
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(100)
                progress = (progress + 0.015f) % 1f
            }
        }
    }

    val viewsCount = targetPost.likesCount * 14L + 120L
    val likesCount = targetPost.likesCount.toLong()
    val commentsCount = targetPost.commentsCount.toLong()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A101F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.8f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("instagram_video_share_card_${targetPost.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ==========================================
            // 1. ORIGINAL CREATOR HEADER
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1E293B).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Creator Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProfileClick(authorId) }
                ) {
                    AsyncImage(
                        model = author.profileImageUrl,
                        contentDescription = authorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFF1E293B), CircleShape)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = authorName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isVerified) {
                                VerifiedBadge(size = 15.dp)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "@$authorUsername",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "•",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = targetPost.timestamp,
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "•",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Public",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Open in full videos page button
                if (onVideoClick != null) {
                    IconButton(
                        onClick = { onVideoClick(targetPost) },
                        modifier = Modifier
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
                }
            }

            // ==========================================
            // 2. ORIGINAL CAPTION / DESCRIPTION
            // ==========================================
            if (captionText.isNotBlank()) {
                val fullText = captionText
                val displayedText = if (isCaptionExpanded || fullText.length <= 110) fullText else "${fullText.take(110)}..."

                val annotatedString = remember(displayedText, authorName) {
                    buildAnnotatedString {
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
                                    withStyle(SpanStyle(color = Color(0xFFF8FAFC))) {
                                        append(word)
                                    }
                                }
                            }
                            if (idx < words.size - 1) append(" ")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A101F))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    ClickableText(
                        text = annotatedString,
                        style = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "TAG", start = offset, end = offset)
                                .firstOrNull()?.let { onHashtagClick?.invoke(it.item) }
                        }
                    )

                    if (fullText.length > 110) {
                        Text(
                            text = if (isCaptionExpanded) "less" else "more",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier
                                .clickable { isCaptionExpanded = !isCaptionExpanded }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 3. VIDEO MEDIA PLAYER CONTAINER (4:5 Aspect)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { triggerDoubleTapLike() },
                            onTap = {
                                isPlaying = !isPlaying
                                showPlayRipple = true
                                coroutineScope.launch {
                                    delay(650)
                                    showPlayRipple = false
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Video Poster / Media
                if (posterUrl.isNotBlank()) {
                    SeamlessImage(
                        url = posterUrl,
                        contentDescription = "Shared video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A101F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // Idle Play Button when paused
                if (!isPlaying && !showPlayRipple) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier
                                .size(32.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }

                // Central Play/Pause Ripple Indicator on Tap
                if (showPlayRipple) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Double-Tap Bursting Heart Animation
                if (showHeartBurst) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .size(80.dp)
                            .scale(heartScale.value)
                            .alpha(heartAlpha.value)
                    )
                }

                // Top-Right: Open in Videos Page / Fullscreen Viewer
                if (onVideoClick != null) {
                    IconButton(
                        onClick = { onVideoClick(targetPost) },
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
                            contentDescription = "Watch in full player",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Bottom-Left: Audio Track Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = audioTitle,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom-Right: Sound Mute/Unmute Toggle
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
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Scrubber Progress Bar at the bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .background(Color(0xFF1877F2))
                    )
                }
            }

            // ==========================================
            // 4. FOOTER STATS BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A101F))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Views
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${formatCount(viewsCount)} views",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Likes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatCount(likesCount),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Comments
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatCount(commentsCount),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // "Watch video →" link
                if (onVideoClick != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onVideoClick(targetPost) }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Watch video",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
