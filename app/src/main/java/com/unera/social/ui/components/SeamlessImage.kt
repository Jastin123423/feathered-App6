package com.unera.social.ui.components

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * ImageVersions - Encapsulates multiple resolution levels of an image, matching web ImageVersions.
 */
data class ImageVersions(
    val original: String = "",
    val high: String = original,
    val medium: String? = null,
    val low: String? = null,
    val placeholder: String? = null
) {
    companion object {
        fun fromUrl(url: String): ImageVersions = ImageVersions(
            original = url,
            high = url,
            medium = url,
            low = url,
            placeholder = null
        )
    }
}

/**
 * Priority for fetching and decoding image assets.
 */
enum class ImagePriority {
    HIGH,
    LOW,
    AUTO
}

/**
 * ImagePreloader - Android Coil equivalent of web imagePreloader utility.
 */
object ImagePreloader {
    fun preload(context: Context, url: String) {
        if (url.isBlank()) return
        val request = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        context.imageLoader.enqueue(request)
    }

    fun preload(context: Context, urls: ImageVersions) {
        urls.placeholder?.takeIf { it.isNotBlank() }?.let { preload(context, it) }
        urls.low?.takeIf { it.isNotBlank() }?.let { preload(context, it) }
        urls.high.takeIf { it.isNotBlank() }?.let { preload(context, it) }
    }
}

/**
 * SeamlessImage - Native Jetpack Compose implementation of SeamlessImage.tsx.
 *
 * Provides:
 * - Progressive loading from placeholder -> low quality -> high quality.
 * - Smooth blur reduction transition (20px -> 5px -> 0px) and alpha crossfade.
 * - Priority-aware image dispatching (HIGH / LOW / AUTO).
 * - Background placeholder color (#1A1A1A) and smooth overlay fade.
 * - Aspect ratio handling.
 * - Memory & disk caching via Coil.
 */
@Composable
fun SeamlessImage(
    urls: ImageVersions,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    aspectRatio: Float? = null,
    priority: ImagePriority = ImagePriority.AUTO,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RectangleShape,
    onLoad: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var isLowLoaded by remember { mutableStateOf(false) }
    var isHighLoaded by remember { mutableStateOf(false) }

    // Preload high or low quality immediately if priority is HIGH
    LaunchedEffect(urls, priority) {
        if (priority == ImagePriority.HIGH) {
            ImagePreloader.preload(context, urls.high)
        } else if (!urls.low.isNullOrBlank()) {
            ImagePreloader.preload(context, urls.low)
        }
    }

    // Determine current blur level
    val targetBlur = when {
        isHighLoaded -> 0.dp
        isLowLoaded -> 4.dp
        else -> 16.dp
    }

    // Smooth alpha transitions
    val highAlpha by animateFloatAsState(
        targetValue = if (isHighLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "highAlpha"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isHighLoaded) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "overlayAlpha"
    )

    val containerModifier = modifier
        .then(if (aspectRatio != null) Modifier.aspectRatio(aspectRatio) else Modifier.heightIn(min = 120.dp))
        .clip(shape)
        .background(Color(0xFF1A1A1A))

    Box(modifier = containerModifier) {
        // 1. Placeholder / Low Quality Layer (if available)
        val previewUrl = urls.low ?: urls.placeholder
        if (!previewUrl.isNullOrBlank() && !isHighLoaded) {
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(targetBlur)
            } else {
                Modifier
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(previewUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = contentScale,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        isLowLoaded = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
            )
        }

        // 2. High Quality Primary Image Layer
        val highUrl = urls.high.ifBlank { urls.original }
        if (highUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(highUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        if (!isHighLoaded) {
                            isHighLoaded = true
                            onLoad?.invoke()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isHighLoaded) highAlpha else 0f)
            )
        }

        // 3. Subtle smoothing overlay fading out once loaded
        if (overlayAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(overlayAlpha)
                    .background(Color.Black.copy(alpha = 0.05f))
            )
        }
    }
}

/**
 * Convenience overload for single URL string.
 */
@Composable
fun SeamlessImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    aspectRatio: Float? = null,
    priority: ImagePriority = ImagePriority.AUTO,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RectangleShape,
    onLoad: (() -> Unit)? = null
) {
    SeamlessImage(
        urls = ImageVersions.fromUrl(url),
        contentDescription = contentDescription,
        modifier = modifier,
        aspectRatio = aspectRatio,
        priority = priority,
        contentScale = contentScale,
        shape = shape,
        onLoad = onLoad
    )
}
