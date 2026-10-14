package com.unera.social.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.AdCampaign
import com.unera.social.model.Post
import com.unera.social.model.User

/**
 * SponsoredPostCard - Exact native counterpart of web SponsoredPostCard.tsx
 *
 * Features:
 * - Header with advertiser avatar, name, VerifiedBadge, "Ad · Public (globe icon)" sub-row,
 *   and 3-dot dropdown menu (Hide ad, Report ad, Ad ID)
 * - Headline/Title in #E4E6EB bold 20sp
 * - Description/Content in #B0B3B8 16sp
 * - Media container:
 *   - Video or Image with aspect ratio and max height
 *   - Fallback gradient banner with ad icon
 * - Facebook-style Slim Blue CTA Bar (#0B1120 background, #1E293B border):
 *   - Destination domain (e.g. "VISIT WEBSITE" or clean domain)
 *   - Bold title
 *   - Solid blue action button (#1877F2) with CTA text + chevron icon
 * - Bottom Action Bar (Like, Comment, Share, and Owner "Analytics" button with green tint)
 * - Optional Campaign footer note ("Campaign: ... · Location")
 * - Border-b 8dp #050B18 separator matching web layout
 */
@Composable
fun SponsoredPostCard(
    ad: AdCampaign,
    currentUser: User? = null,
    onProfileClick: ((Long) -> Unit)? = null,
    onAnalyticsClick: ((Long) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onHideAd: ((Long) -> Unit)? = null,
    onReportAd: ((Long) -> Unit)? = null,
    onOpenComments: ((AdCampaign) -> Unit)? = null,
    onReact: ((AdCampaign, String) -> Unit)? = null,
    onShare: ((AdCampaign) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(ad.clicks.coerceAtLeast(1) * 3 + 12) }
    var isHidden by remember { mutableStateOf(false) }

    if (isHidden) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ad hidden. You won't see this ad again.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
        }
        return
    }

    val isOwner = currentUser?.id == 1L || ad.id == 1L

    // Clean destination domain extraction
    val destinationDomain = remember(ad.link) {
        try {
            val target = ad.link
            if (target.isNullOrBlank()) "VISIT WEBSITE"
            else {
                val clean = target.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                clean.split("/").firstOrNull()?.uppercase() ?: "VISIT WEBSITE"
            }
        } catch (_: Exception) {
            "VISIT WEBSITE"
        }
    }

    // Trigger external link or call/email
    val handleCTAClick: () -> Unit = {
        onClick?.invoke()
        try {
            when {
                !ad.link.isNullOrBlank() -> {
                    val fullUrl = if (ad.link.startsWith("http")) ad.link else "https://${ad.link}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                    context.startActivity(intent)
                }
                !ad.phone.isNullOrBlank() -> {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ad.phone}"))
                    context.startActivity(intent)
                }
                !ad.email.isNullOrBlank() -> {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${ad.email}"))
                    context.startActivity(intent)
                }
            }
        } catch (_: Exception) {}
    }

    val isVideo = ad.type == "video"
    val mediaUrl = ad.mediaUrl ?: ad.mediaUrls.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .testTag("sponsored_post_card_${ad.id}")
    ) {
        // 1. HEADER (Advertiser info + globe + menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile / Advertiser Image
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .clickable { onProfileClick?.invoke(ad.id) }
            ) {
                if (!mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = ad.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1877F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ad.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Advertiser Name + Ad / Public line
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onProfileClick?.invoke(ad.id) }
                ) {
                    Text(
                        text = ad.name.ifBlank { "Sponsored Advertiser" },
                        color = Color(0xFFF8FAFC),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    VerifiedBadge(size = 14.dp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Ad",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB0B3B8)
                    )
                    Text(text = "·", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Public",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                    if (ad.location.isNotBlank() && ad.location != "Global") {
                        Text(text = "·", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text(
                            text = ad.location,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3-Dots Menu
            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Ad Options",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Hide ad", color = Color(0xFFF8FAFC), fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            showMenu = false
                            isHidden = true
                            onHideAd?.invoke(ad.id)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Report ad", color = Color(0xFFF8FAFC), fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onReportAd?.invoke(ad.id)
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            text = "Ad ID: ${ad.id}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // 2. TITLE / HEADLINE (Matching web)
        if (ad.name.isNotBlank()) {
            Text(
                text = ad.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE4E6EB),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            )
        }

        // 3. DESCRIPTION (Matching web)
        if (ad.description.isNotBlank()) {
            Text(
                text = ad.description,
                fontSize = 15.sp,
                color = Color(0xFFB0B3B8),
                lineHeight = 21.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. MEDIA (Image or Video thumbnail preview)
        if (!mediaUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 380.dp)
                    .background(Color.Black)
                    .clickable { handleCTAClick() }
            ) {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = ad.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        } else {
            // Default branded fallback banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1877F2), Color(0xFF166FE5))
                        )
                    )
                    .clickable { handleCTAClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // 5. CTA BOTTOM BAR - Facebook Blue Professional Style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1120))
                .border(BorderStroke(1.dp, Color(0xFF1E293B)))
                .clickable { handleCTAClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destinationDomain,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ad.name.ifBlank { "Sponsored" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF8FAFC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = handleCTAClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1877F2),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = ad.cta.ifBlank { "Learn More" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // 6. ACTIONS (Like, Comment, Share, Advertiser Analytics)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like Action
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        isLiked = !isLiked
                        likesCount += if (isLiked) 1 else -1
                        onReact?.invoke(ad, "like")
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFF1877F2) else Color(0xFFB0B3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Like",
                    color = if (isLiked) Color(0xFF1877F2) else Color(0xFFB0B3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Comment Action
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenComments?.invoke(ad) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint = Color(0xFFB0B3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Comment",
                    color = Color(0xFFB0B3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Share Action
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onShare?.invoke(ad) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = Color(0xFFB0B3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Share",
                    color = Color(0xFFB0B3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Owner Analytics Action
            if (isOwner) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onAnalyticsClick?.invoke(ad.id) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Analytics",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Analytics",
                        color = Color(0xFF10B981),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 7. Optional Campaign Info Footer
        if (ad.name.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(0.5.dp, Color(0xFF1E293B)))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Campaign: ${ad.name} • ${ad.location}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 8. 8dp border separator matching web feed styling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFF050B18))
        )
    }
}

/**
 * Convenience adapter to render SponsoredPostCard from Post model directly
 */
@Composable
fun SponsoredPostCard(
    post: Post,
    currentUser: User? = null,
    onProfileClick: ((Long) -> Unit)? = null,
    onAnalyticsClick: ((Long) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onHideAd: ((Long) -> Unit)? = null,
    onReportAd: ((Long) -> Unit)? = null,
    onOpenComments: ((Post) -> Unit)? = null,
    onReact: ((Post, String) -> Unit)? = null,
    onShare: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val adCampaign = remember(post) {
        AdCampaign(
            id = post.id,
            name = post.sponsoredHeadline.ifBlank { post.author.name },
            type = if (post.postType == "video") "video" else "image",
            mediaUrl = post.mediaUrl,
            mediaUrls = post.mediaUrls,
            description = post.content,
            cta = post.sponsoredCtaText.ifBlank { "Learn More" },
            link = post.sponsoredCtaUrl,
            location = post.location ?: "Global",
            impressions = post.likesCount * 12,
            clicks = post.commentsCount * 4
        )
    }

    SponsoredPostCard(
        ad = adCampaign,
        currentUser = currentUser,
        onProfileClick = onProfileClick,
        onAnalyticsClick = onAnalyticsClick,
        onClick = onClick,
        onHideAd = onHideAd,
        onReportAd = onReportAd,
        onOpenComments = { onOpenComments?.invoke(post) },
        onReact = { _, type -> onReact?.invoke(post, type) },
        onShare = { onShare?.invoke(post) },
        modifier = modifier
    )
}
