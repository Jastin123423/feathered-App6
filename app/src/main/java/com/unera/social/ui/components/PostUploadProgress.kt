package com.unera.social.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

/**
 * PostUploadState - Matches the TypeScript interface PostUploadState.
 */
data class PostUploadState(
    val isUploading: Boolean = false,
    val progress: Float = 0f, // 0 - 100
    val title: String = "Posting to Feathered",
    val secondaryStatus: String = "Uploading media...",
    val previewUrl: String? = null,
    val isSuccess: Boolean = false,
    val error: String? = null
)

private val BannerBgDark = Color(0xFF0F172A)
private val BannerBorderBottom = Color(0xFF050B18)
private val CardBorderDark = Color(0xFF1E293B)
private val ThumbnailBg = Color(0xFF141E33)
private val BrandBlue = Color(0xFF1877F2)
private val SuccessGreen = Color(0xFF10B981)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val TrackBg = Color(0xFF1E293B)

/**
 * PostUploadProgressBanner - Top feed upload banner matching the Facebook mobile upload interface.
 */
@Composable
fun PostUploadProgressBanner(
    uploadState: PostUploadState?,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    if (uploadState == null) return

    val pct = uploadState.progress.coerceIn(0f, 100f).roundToInt()

    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = uploadState.progress.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "banner_progress"
    )

    // Shimmer effect animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BannerBgDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("post_upload_progress_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visual thumbnail or Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ThumbnailBg)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!uploadState.previewUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = uploadState.previewUrl,
                        contentDescription = "Upload preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BrandBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Overlay when uploading: circular spinner
                if (uploadState.isUploading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                // Overlay when success: green checkmark
                if (uploadState.isSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Text & Progress Bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uploadState.title,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uploadState.isSuccess) "Done" else "$pct%",
                        fontWeight = FontWeight.Bold,
                        color = if (uploadState.isSuccess) SuccessGreen else BrandBlue,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = uploadState.secondaryStatus,
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Animated progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(TrackBg)
                ) {
                    val fillFraction = (animatedProgress / 100f).coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (uploadState.isSuccess) SuccessGreen else BrandBlue)
                    ) {
                        if (uploadState.isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.35f),
                                                Color.Transparent
                                            ),
                                            startX = shimmerTranslate * 300f,
                                            endX = (shimmerTranslate + 1f) * 300f
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Dismiss Button on Success
            if (uploadState.isSuccess && onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .testTag("post_upload_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // 8dp divider block matching feed separation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(BannerBorderBottom)
    )
}

/**
 * PostUploadBottomPill - Floating bottom progress pill shown when scrolling through timeline.
 */
@Composable
fun PostUploadBottomPill(
    uploadState: PostUploadState?,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    if (uploadState == null) return

    val pct = uploadState.progress.coerceIn(0f, 100f).roundToInt()

    val animatedProgress by animateFloatAsState(
        targetValue = uploadState.progress.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "pill_progress"
    )

    Card(
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = uploadState.isSuccess && onDismiss != null) { onDismiss?.invoke() }
            .testTag("post_upload_bottom_pill"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BannerBgDark.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandBlue.copy(alpha = 0.15f))
                        .border(1.dp, BrandBlue.copy(alpha = 0.30f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (uploadState.isSuccess) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = BrandBlue,
                            strokeWidth = 2.dp,
                            trackColor = BrandBlue.copy(alpha = 0.25f)
                        )
                    }
                }

                // Middle Text Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uploadState.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uploadState.isSuccess) "100%" else "$pct%",
                            color = if (uploadState.isSuccess) SuccessGreen else BrandBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = uploadState.secondaryStatus,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (uploadState.isSuccess && onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Mini progress line at bottom of pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(TrackBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((animatedProgress / 100f).coerceIn(0.04f, 1f))
                        .fillMaxHeight()
                        .background(if (uploadState.isSuccess) SuccessGreen else BrandBlue)
                )
            }
        }
    }
}
