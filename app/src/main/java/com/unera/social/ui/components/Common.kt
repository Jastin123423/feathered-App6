package com.unera.social.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage

/**
 * Standard spinner matching web Spinner from Common.tsx:
 * <div className="flex justify-center p-8">
 *   <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#1877F2]"></div>
 * </div>
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Color(0xFF1877F2),
    strokeWidth: Dp = 3.dp,
    padding: Dp = 32.dp
) {
    Box(
        modifier = modifier
            .padding(padding)
            .testTag("common_spinner"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = Color(0xFF1E293B).copy(alpha = 0.3f)
        )
    }
}

/**
 * ProfessionalLoader matching web ProfessionalLoader from Common.tsx:
 * Full-screen loading screen with rotating globe, pulsing glow, UNERA gradient logo,
 * oscillating sailing track highlight, and 3 bouncing dots.
 */
@Composable
fun ProfessionalLoader(
    modifier: Modifier = Modifier,
    tagline: String = "Connecting your world",
    brandName: String = "UNERA"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ProfessionalLoaderTransition")

    // 1. Globe continuous rotation (8s linear)
    val globeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlobeRotation"
    )

    // 2. Background depth pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // 3. Sailor glider glide animation (translates from 0% to 100% and back)
    val sailProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SailProgress"
    )

    // 4. Progress fill pulse
    val fillPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FillPulse"
    )

    // 5. Tagline pulse
    val taglineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TaglinePulse"
    )

    // 6. Three bouncing dots with staggered offsets
    val dotBounce1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotBounce1"
    )

    val dotBounce2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotBounce2"
    )

    val dotBounce3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotBounce3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B18))
            .testTag("professional_loader"),
        contentAlignment = Alignment.Center
    ) {
        // Background depth element: 500px glowing radial bloom
        Box(
            modifier = Modifier
                .size(340.dp)
                .graphicsLayer {
                    scaleX = glowPulse
                    scaleY = glowPulse
                    alpha = 0.4f
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1877F2).copy(alpha = 0.22f),
                            Color(0xFF1877F2).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Center Content Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Logo Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Rotating Globe with glowing halo
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing glow halo
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                scaleX = glowPulse
                                scaleY = glowPulse
                            }
                            .clip(CircleShape)
                            .background(Color(0xFF1877F2).copy(alpha = 0.2f))
                    )

                    // Spinning Globe
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "UNERA Globe",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier
                            .size(50.dp)
                            .rotate(globeRotation)
                    )
                }

                // UNERA Gradient Text
                Text(
                    text = brandName,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    style = LocalTextStyle.current.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1877F2), Color(0xFF1D8AF2))
                        )
                    )
                )
            }

            // Sailing Track (w-64 = 256dp, h-1 = 4dp)
            val trackWidth = 256.dp
            val gliderWidth = 96.dp

            Box(
                modifier = Modifier
                    .width(trackWidth)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF0F172A))
            ) {
                // Background progress fill with pulse
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1877F2).copy(alpha = fillPulse))
                )

                // The "Sailor" highlight glider moving back and forth
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val maxWidthPx = maxWidth
                    val offsetDp = (maxWidthPx - gliderWidth) * sailProgress

                    Box(
                        modifier = Modifier
                            .offset(x = offsetDp)
                            .width(gliderWidth)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF1877F2),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle / Tagline
            Text(
                text = tagline.uppercase(),
                color = Color(0xFF94A3B8).copy(alpha = taglineAlpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Bouncing Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = dotBounce1.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2))
                )
                Box(
                    modifier = Modifier
                        .offset(y = dotBounce2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2))
                )
                Box(
                    modifier = Modifier
                        .offset(y = dotBounce3.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2))
                )
            }
        }
    }
}

/**
 * ImageViewer matching web ImageViewer from Common.tsx:
 * Full-screen modal overlay with black 95% backdrop, top-right close button,
 * and pinch-to-zoom / double-tap zoom capabilities.
 */
@Composable
fun ImageViewer(
    imageUrl: String?,
    onClose: () -> Unit
) {
    if (imageUrl.isNullOrBlank()) return

    // Intercept hardware / gesture back button
    BackHandler(onBack = onClose)

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .testTag("full-screen-image-viewer")
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Tap outside / backdrop closes the preview
                            if (scale <= 1.05f) {
                                onClose()
                            } else {
                                // Reset zoom if zoomed in
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Close Button (Fixed Top-Right)
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .testTag("close-full-image-btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close image preview",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Image Container with gesture transform (pinch zoom and pan)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "Full screen preview",
                    contentScale = ContentScale.Fit,
                    loading = {
                        Spinner(size = 40.dp, strokeWidth = 3.5.dp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Prevent clicking on image from triggering backdrop close
                        }
                )
            }
        }
    }
}
