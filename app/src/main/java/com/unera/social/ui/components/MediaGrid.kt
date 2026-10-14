package com.unera.social.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * MediaItem - Represents an individual media image matching web MediaItem interface.
 */
data class MediaItem(
    val url: String,
    val alt: String? = null,
    val versions: ImageVersions? = null
)

/**
 * MediaGrid - Complete Android Jetpack Compose counterpart of web MediaGrid.tsx.
 *
 * Implements:
 * - Single image: full-width container with customizable maxHeight, object-contain on black background
 * - 2 images: 2-column square grid with 2dp/4dp gap
 * - 3 images: special layout with left row-span-2 image and 2 stacked square images on the right
 * - 4 or more images: 2x2 square grid; 4th image shows "+X" overlay if media.length > 4
 * - Built-in image preview modal (Lightbox) with:
 *     * Black 90% semi-transparent backdrop
 *     * Top-right close button (✕)
 *     * Top image index counter (e.g., 2 / 5)
 *     * Left (←) and Right (→) circular navigation buttons with circular loop-around
 *     * Touch drag/swipe gesture support for next/previous image navigation
 *     * Hardware BackHandler support
 * - Also supports external delegation via onOpen / onImageClick callbacks.
 */
@Composable
fun MediaGrid(
    media: List<MediaItem>,
    modifier: Modifier = Modifier,
    onOpen: ((String) -> Unit)? = null,
    onImageClick: ((Int) -> Unit)? = null,
    maxHeight: Dp = 420.dp,
    gap: Dp = 2.dp,
    enableInternalPreview: Boolean = true
) {
    if (media.isEmpty()) return

    var activeIndex by remember { mutableStateOf<Int?>(null) }

    val handleClick = { url: String, index: Int ->
        if (onImageClick != null) {
            onImageClick(index)
        } else if (enableInternalPreview) {
            activeIndex = index
        }
        onOpen?.invoke(url)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("media_grid_${media.size}")
    ) {
        when (media.size) {
            1 -> {
                // Single image: full width, object contain/fit on black background with maxHeight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .clip(RoundedCornerShape(0.dp))
                        .clickable { handleClick(media[0].url, 0) }
                        .testTag("media_grid_item_0"),
                    contentAlignment = Alignment.Center
                ) {
                    SeamlessImage(
                        urls = media[0].versions ?: ImageVersions.fromUrl(media[0].url),
                        contentDescription = media[0].alt ?: "Post image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = maxHeight)
                    )
                }
            }

            2 -> {
                // Two images: 2 columns, square aspect ratio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    media.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(Color(0xFF0F172A))
                                .clickable { handleClick(item.url, index) }
                                .testTag("media_grid_item_$index")
                        ) {
                            SeamlessImage(
                                urls = item.versions ?: ImageVersions.fromUrl(item.url),
                                contentDescription = item.alt ?: "Post image ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            3 -> {
                // Three images - special layout: left spans full height, right has 2 stacked squares
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val colWidth = (maxWidth - gap) / 2
                    val totalHeight = colWidth * 2 + gap

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalHeight),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        // Left item (row-span-2)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFF0F172A))
                                .clickable { handleClick(media[0].url, 0) }
                                .testTag("media_grid_item_0")
                        ) {
                            SeamlessImage(
                                urls = media[0].versions ?: ImageVersions.fromUrl(media[0].url),
                                contentDescription = media[0].alt ?: "Post image 1",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Right column (2 stacked squares)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A))
                                    .clickable { handleClick(media[1].url, 1) }
                                    .testTag("media_grid_item_1")
                            ) {
                                SeamlessImage(
                                    urls = media[1].versions ?: ImageVersions.fromUrl(media[1].url),
                                    contentDescription = media[1].alt ?: "Post image 2",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A))
                                    .clickable { handleClick(media[2].url, 2) }
                                    .testTag("media_grid_item_2")
                            ) {
                                SeamlessImage(
                                    urls = media[2].versions ?: ImageVersions.fromUrl(media[2].url),
                                    contentDescription = media[2].alt ?: "Post image 3",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                // Four or more images: 2x2 square grid, 4th image shows "+X" overlay if media.size > 4
                val visibleItems = media.take(4)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    // Row 1: items 0 & 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        for (index in 0..1) {
                            val item = visibleItems[index]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(Color(0xFF0F172A))
                                    .clickable { handleClick(item.url, index) }
                                    .testTag("media_grid_item_$index")
                            ) {
                                SeamlessImage(
                                    urls = item.versions ?: ImageVersions.fromUrl(item.url),
                                    contentDescription = item.alt ?: "Post image ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Row 2: items 2 & 3 (item 3 may have +X overlay)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        for (index in 2..3) {
                            val item = visibleItems[index]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(Color(0xFF0F172A))
                                    .clickable { handleClick(item.url, index) }
                                    .testTag("media_grid_item_$index")
                            ) {
                                SeamlessImage(
                                    urls = item.versions ?: ImageVersions.fromUrl(item.url),
                                    contentDescription = item.alt ?: "Post image ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Show "+X" overlay on the 4th cell if there are more than 4 images
                                if (index == 3 && media.size > 4) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.70f))
                                            .testTag("media_grid_extra_overlay"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${media.size - 4}",
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Image preview modal (Lightbox) matching web React implementation
    if (activeIndex != null) {
        MediaPreviewModal(
            media = media,
            initialIndex = activeIndex!!,
            onDismiss = { activeIndex = null }
        )
    }
}

/**
 * Convenience overload for List<String> URLs.
 */
@JvmName("MediaGridFromStrings")
@Composable
fun MediaGrid(
    images: List<String>,
    modifier: Modifier = Modifier,
    onOpen: ((String) -> Unit)? = null,
    onImageClick: ((Int) -> Unit)? = null,
    maxHeight: Dp = 420.dp,
    gap: Dp = 2.dp,
    enableInternalPreview: Boolean = true
) {
    val mediaItems = remember(images) {
        images.mapIndexed { idx, url -> MediaItem(url = url, alt = "Photo ${idx + 1}") }
    }
    MediaGrid(
        media = mediaItems,
        modifier = modifier,
        onOpen = onOpen,
        onImageClick = onImageClick,
        maxHeight = maxHeight,
        gap = gap,
        enableInternalPreview = enableInternalPreview
    )
}

/**
 * MediaPreviewModal - Lightbox modal matching React `Image preview modal`.
 */
@Composable
fun MediaPreviewModal(
    media: List<MediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, media.size - 1)) }
    var dragAccumulator by remember { mutableStateOf(0f) }

    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() }
                .pointerInput(media.size) {
                    if (media.size > 1) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragAccumulator > 60f) {
                                    // Swipe Right -> previous
                                    currentIndex = if (currentIndex == 0) media.size - 1 else currentIndex - 1
                                } else if (dragAccumulator < -60f) {
                                    // Swipe Left -> next
                                    currentIndex = if (currentIndex == media.size - 1) 0 else currentIndex + 1
                                }
                                dragAccumulator = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragAccumulator += dragAmount
                            }
                        )
                    }
                }
                .testTag("media_preview_modal"),
            contentAlignment = Alignment.Center
        ) {
            // Header Bar: Counter & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Counter badge: e.g. "1 / 4"
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${media.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Close Button ✕
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("media_preview_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Main Display Image (Centered, Fit, with max 90vh)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 72.dp)
                    .clickable(enabled = false) { /* Consume click so tapping image doesn't dismiss */ },
                contentAlignment = Alignment.Center
            ) {
                SeamlessImage(
                    urls = media[currentIndex].versions ?: ImageVersions.fromUrl(media[currentIndex].url),
                    contentDescription = media[currentIndex].alt ?: "Preview image ${currentIndex + 1}",
                    contentScale = ContentScale.Fit,
                    priority = ImagePriority.HIGH,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            // Navigation Arrows (if media.size > 1)
            if (media.size > 1) {
                // Left Arrow ←
                IconButton(
                    onClick = {
                        currentIndex = if (currentIndex == 0) media.size - 1 else currentIndex - 1
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("media_preview_prev")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous image",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Right Arrow →
                IconButton(
                    onClick = {
                        currentIndex = if (currentIndex == media.size - 1) 0 else currentIndex + 1
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("media_preview_next")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next image",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
