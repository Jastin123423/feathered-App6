package com.unera.social.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

data class AdPreviewData(
    val name: String = "",
    val type: String = "image", // "image" or "video"
    val mediaUrl: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val description: String = "",
    val cta: String = "Learn More",
    val link: String? = null,
    val phone: String? = null,
    val email: String? = null
)

/**
 * AdPreview - Matches web AdPreview.tsx exactly.
 * Renders a pixel-perfect preview of a sponsored advertisement on Feathered with lightbox support.
 */
@Composable
fun AdPreview(
    data: AdPreviewData,
    advertiserName: String = "Sponsored",
    advertiserAvatar: String? = null,
    isVerified: Boolean = false,
    isFullView: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedImageIndex by remember { mutableStateOf(0) }
    var showLightbox by remember { mutableStateOf(false) }

    val allMediaUrls = when {
        data.mediaUrls.isNotEmpty() -> data.mediaUrls
        !data.mediaUrl.isNullOrBlank() -> listOf(data.mediaUrl)
        else -> emptyList()
    }

    val isVideo = data.type == "video"

    fun getHostname(url: String?): String {
        if (url.isNullOrBlank()) return "example.com"
        return try {
            val clean = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
            clean.split("/").firstOrNull() ?: "example.com"
        } catch (_: Exception) {
            "example.com"
        }
    }

    val contactDisplay = when {
        !data.link.isNullOrBlank() -> getHostname(data.link)
        !data.phone.isNullOrBlank() -> data.phone
        !data.email.isNullOrBlank() -> data.email
        else -> "example.com"
    }

    val contactIcon: ImageVector = when {
        !data.link.isNullOrBlank() -> Icons.Default.OpenInNew
        !data.phone.isNullOrBlank() -> Icons.Default.Phone
        !data.email.isNullOrBlank() -> Icons.Default.Email
        else -> Icons.Default.Public
    }

    Card(
        modifier = modifier
            .then(
                if (isFullView) Modifier.fillMaxWidth().widthIn(max = 500.dp)
                else Modifier.width(360.dp)
            )
            .testTag("ad_preview_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sponsored Label - Free Promotion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFECFDF5))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FREE PROMOTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }

                Text(
                    text = "Sponsored",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF71717A)
                )
            }

            // Header - Avatar, Name, Verified Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!advertiserAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = advertiserAvatar,
                                contentDescription = advertiserName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = advertiserName.take(1).uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = advertiserName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF18181B)
                        )
                        if (isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { /* Menu */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Media Grid Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF18181B))
            ) {
                when {
                    allMediaUrls.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ad Preview",
                                color = Color(0xFFA1A1AA),
                                fontSize = 14.sp
                            )
                        }
                    }
                    isVideo -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = allMediaUrls.firstOrNull(),
                                contentDescription = "Video Ad",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }
                    }
                    allMediaUrls.size == 1 -> {
                        AsyncImage(
                            model = allMediaUrls[0],
                            contentDescription = "Ad Media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    selectedImageIndex = 0
                                    showLightbox = true
                                }
                        )
                    }
                    allMediaUrls.size == 2 -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = allMediaUrls[0],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        selectedImageIndex = 0
                                        showLightbox = true
                                    }
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            AsyncImage(
                                model = allMediaUrls[1],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        selectedImageIndex = 1
                                        showLightbox = true
                                    }
                            )
                        }
                    }
                    allMediaUrls.size == 3 -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = allMediaUrls[0],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        selectedImageIndex = 0
                                        showLightbox = true
                                    }
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                AsyncImage(
                                    model = allMediaUrls[1],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedImageIndex = 1
                                            showLightbox = true
                                        }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                AsyncImage(
                                    model = allMediaUrls[2],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedImageIndex = 2
                                            showLightbox = true
                                        }
                                )
                            }
                        }
                    }
                    else -> {
                        // 4 or more images
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                AsyncImage(
                                    model = allMediaUrls[0],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedImageIndex = 0
                                            showLightbox = true
                                        }
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                AsyncImage(
                                    model = allMediaUrls[1],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedImageIndex = 1
                                            showLightbox = true
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                AsyncImage(
                                    model = allMediaUrls[2],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedImageIndex = 2
                                            showLightbox = true
                                        }
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            selectedImageIndex = 3
                                            showLightbox = true
                                        }
                                ) {
                                    AsyncImage(
                                        model = allMediaUrls[3],
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (allMediaUrls.size > 4) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.65f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+${allMediaUrls.size - 3}",
                                                color = Color.White,
                                                fontSize = 20.sp,
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

            // Ad Description
            if (data.description.isNotBlank()) {
                Text(
                    text = data.description,
                    fontSize = 14.sp,
                    color = Color(0xFF27272A),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(14.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Free Promotion Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFECFDF5))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✨ Free Promotion",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                    Text(text = "•", color = Color(0xFF34D399))
                    Text(
                        text = "No cost to boost",
                        fontSize = 12.sp,
                        color = Color(0xFF059669)
                    )
                }
            }

            // Link / Contact Info Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFF6FF))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = contactIcon,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = contactDisplay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // CTA Button matching web: w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 rounded-lg text-sm
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Button(
                    onClick = { /* CTA Click */ },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("ad_preview_cta_btn")
                ) {
                    Text(
                        text = data.cta.ifBlank { "Learn More" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Free Promotion Footer
            Text(
                text = "Free advertising on Feathered • Reach more people at no cost",
                fontSize = 10.sp,
                color = Color(0xFFA1A1AA),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // Fullscreen Lightbox Dialog
    if (showLightbox && allMediaUrls.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showLightbox = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showLightbox = false },
                contentAlignment = Alignment.Center
            ) {
                // Close button
                IconButton(
                    onClick = { showLightbox = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous button
                if (allMediaUrls.size > 1) {
                    IconButton(
                        onClick = {
                            selectedImageIndex = (selectedImageIndex - 1 + allMediaUrls.size) % allMediaUrls.size
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            selectedImageIndex = (selectedImageIndex + 1) % allMediaUrls.size
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Full size Image
                AsyncImage(
                    model = allMediaUrls[selectedImageIndex.coerceIn(0, allMediaUrls.size - 1)],
                    contentDescription = "Full Size Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.85f)
                        .clickable(enabled = false) {}
                )

                // Counter pill
                if (allMediaUrls.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.60f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${selectedImageIndex + 1} / ${allMediaUrls.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
