package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import coil.compose.AsyncImage
import com.unera.social.model.AdCampaign
import com.unera.social.model.Post
import com.unera.social.model.User

/**
 * AdsManagerScreen - Matches web AdsManager.tsx exactly.
 * Full Ads Manager dashboard with free promotion banner, campaign cards, CTR stats,
 * pause/resume controls, delete confirmation, and creation flow.
 */
@Composable
fun AdsManagerScreen(
    campaigns: List<AdCampaign>,
    currentUser: User,
    userPosts: List<Post>,
    initialPost: Post? = null,
    onPauseCampaign: (Long) -> Unit,
    onResumeCampaign: (Long) -> Unit,
    onDeleteCampaign: (Long) -> Unit,
    onCreateCampaign: (
        postId: Long?,
        name: String,
        type: String,
        mediaUrl: String?,
        mediaUrls: List<String>,
        description: String,
        link: String?,
        phone: String?,
        email: String?,
        cta: String,
        location: String,
        budget: Int,
        days: Int
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreator by remember { mutableStateOf(initialPost != null) }
    var selectedTab by remember { mutableStateOf("dashboard") } // "dashboard", "campaigns"
    var filter by remember { mutableStateOf("all") } // "all", "active", "paused", "completed"
    var campaignToDelete by remember { mutableStateOf<AdCampaign?>(null) }

    val filteredCampaigns = remember(campaigns, filter) {
        if (filter == "all") campaigns
        else campaigns.filter { it.status.equals(filter, ignoreCase = true) }
    }

    if (showCreator) {
        AdCreator(
            currentUser = currentUser,
            userPosts = userPosts,
            initialPost = initialPost,
            onBack = { showCreator = false },
            onSuccess = { showCreator = false },
            onCreateCampaign = { postId, name, type, mediaUrl, mediaUrls, desc, link, phone, email, cta, loc, budget, days ->
                onCreateCampaign(postId, name, type, mediaUrl, mediaUrls, desc, link, phone, email, cta, loc, budget, days)
                showCreator = false
            }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B18))
            .testTag("ads_manager_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Header: My Campaigns & Create Button
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFF8FAFC),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My Campaigns",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Manage your free advertisement campaigns.",
                                fontSize = 13.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }

                        Button(
                            onClick = { showCreator = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Ad", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab selector between Ad Analytics (Dashboard) and Campaigns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Surface(
                            onClick = { selectedTab = "dashboard" },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedTab == "dashboard") Color(0xFF2563EB) else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (selectedTab == "dashboard") Color.White else Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Analytics",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == "dashboard") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == "dashboard") Color.White else Color(0xFFA1A1AA)
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = "campaigns" },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedTab == "campaigns") Color(0xFF2563EB) else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = if (selectedTab == "campaigns") Color.White else Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Campaigns",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == "campaigns") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == "campaigns") Color.White else Color(0xFFA1A1AA)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Filters (shown in Campaigns tab)
                    if (selectedTab == "campaigns") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("all" to "All Status", "active" to "Active", "paused" to "Paused", "completed" to "Completed").forEach { (key, label) ->
                                val isSel = filter == key
                                Surface(
                                    onClick = { filter = key },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFF1E293B) else Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF3B82F6) else Color(0xFF1E293B)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label.split(" ").first(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else Color(0xFFA1A1AA)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Free Promotion Banner matching web
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.10f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "All campaigns are completely free on Feathered • $0 cost",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF34D399)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Main Body: Analytics Dashboard View or Campaigns List View
            if (selectedTab == "dashboard") {
                item(key = "dashboard_body") {
                    AdDashboardView(
                        campaigns = campaigns,
                        onBack = null,
                        onNavigateToCampaigns = { selectedTab = "campaigns" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (filteredCampaigns.isEmpty()) {
                item(key = "empty_campaigns") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF71717A),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No campaigns found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA1A1AA)
                            )
                            Text(
                                text = "Create your first free ad.",
                                fontSize = 13.sp,
                                color = Color(0xFF71717A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showCreator = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("Create Campaign", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredCampaigns, key = { it.id }) { ad ->
                    AdCampaignCard(
                        ad = ad,
                        onTogglePause = {
                            if (ad.status == "active") onPauseCampaign(ad.id)
                            else onResumeCampaign(ad.id)
                        },
                        onDelete = { campaignToDelete = ad }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        // Delete Confirmation Dialog
        campaignToDelete?.let { ad ->
            AlertDialog(
                onDismissRequest = { campaignToDelete = null },
                containerColor = Color(0xFF0B1120),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text("Delete Campaign", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Are you sure you want to delete \"${ad.name}\"? This action cannot be undone.", color = Color(0xFFA1A1AA))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteCampaign(ad.id)
                            campaignToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { campaignToDelete = null }) {
                        Text("Cancel", color = Color(0xFFA1A1AA))
                    }
                }
            )
        }
    }
}

/**
 * AdCampaignCard - Matches web Card view in AdsManager.tsx
 */
@Composable
private fun AdCampaignCard(
    ad: AdCampaign,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit
) {
    val mediaUrl = ad.mediaUrls.firstOrNull() ?: ad.mediaUrl
    val isVideo = ad.type == "video"
    val ctr = if (ad.impressions == 0) "0.0" else "%.1f".format((ad.clicks.toDouble() / ad.impressions) * 100)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Thumbnail + Info + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Thumbnail with FREE badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                ) {
                    if (!mediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = ad.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        // FREE badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .clip(RoundedCornerShape(bottomEnd = 4.dp))
                                .background(Color(0xFF10B981))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("FREE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ad.name.ifBlank { "Campaign #${ad.id}" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (ad.status) {
                                    "active" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    "paused" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> Color(0xFF71717A).copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ad.status.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (ad.status) {
                                "active" -> Color(0xFF34D399)
                                "paused" -> Color(0xFFFBBF24)
                                else -> Color(0xFFA1A1AA)
                            }
                        )
                    }
                }

                // Pause/Resume & Delete Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onTogglePause,
                        enabled = ad.status != "completed",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = if (ad.status == "active") Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (ad.status == "active") "Pause" else "Resume",
                            tint = if (ad.status == "active") Color(0xFFF59E0B) else Color(0xFF34D399),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Stats Grid (5 columns matching web)
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(icon = Icons.Default.Visibility, value = ad.impressions.toString(), label = "Impr.", valueColor = Color.White)
                StatColumn(icon = Icons.Default.PlayArrow, value = ad.views.toString(), label = "Views", valueColor = Color.White)
                StatColumn(icon = Icons.Default.TouchApp, value = ad.clicks.toString(), label = "Clicks", valueColor = Color.White)
                StatColumn(icon = Icons.Default.TrendingUp, value = "$ctr%", label = "CTR", valueColor = Color(0xFF34D399))
                StatColumn(icon = Icons.Default.Star, value = "$0", label = "FREE", valueColor = Color(0xFF34D399))
            }

            // Date Range Footer
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(14.dp))
                    Text("${ad.startDate} - ${ad.endDate}", fontSize = 12.sp, color = Color(0xFF71717A))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                    Text("Free", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    value: String,
    label: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(text = label, fontSize = 10.sp, color = Color(0xFF71717A))
    }
}
