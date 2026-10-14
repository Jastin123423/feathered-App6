package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.AdCampaign
import java.text.NumberFormat
import java.util.Locale

/**
 * AdDashboardView - Matches web Dashboard.tsx for Ad Analytics exactly.
 * Includes:
 * - Header: "Ad Analytics", "Track your campaign performance and analytics."
 * - Stats Grid (4 cards): Total Impressions, Total Clicks, Total Views, Total Spend ($0 free or calculated)
 *   with trend indicators (up/down %, vs last week)
 * - Second Row Stats: Click-Through Rate (CTR) gauge with progress bar (Industry avg: 1.5-2.5%) & Active Campaigns count card
 * - Weekly Performance Chart: Interactive bar chart for Mon-Sun showing impressions and clicks with time range dropdown
 * - Top Performing Campaigns:
 *   - Mobile view: Dedicated cards with prominent CTR badge (emerald pill), thumbnail, status badge, 3-stat row (Impressions, Views, Clicks)
 *   - Wide/Tablet view: Horizontal row layout with all metrics and status pill
 */
@Composable
fun AdDashboardView(
    campaigns: List<AdCampaign>,
    onBack: (() -> Unit)? = null,
    onNavigateToCampaigns: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTimeRange by remember { mutableStateOf("Last 7 days") }
    var showTimeDropdown by remember { mutableStateOf(false) }

    // Aggregate statistics
    val totalImpressions = remember(campaigns) {
        campaigns.sumOf { it.impressions.toLong() }
    }
    val totalClicks = remember(campaigns) {
        campaigns.sumOf { it.clicks.toLong() }
    }
    val totalViews = remember(campaigns) {
        campaigns.sumOf { it.views.toLong() }
    }
    val totalSpend = remember(campaigns) {
        // Free promo or sum of budgets
        0L
    }

    val activeCampaigns = remember(campaigns) {
        campaigns.count { it.status.equals("active", ignoreCase = true) }
    }

    val overallCtr = remember(totalImpressions, totalClicks) {
        if (totalImpressions > 0L) {
            String.format(Locale.US, "%.2f", (totalClicks.toDouble() / totalImpressions.toDouble()) * 100.0)
        } else {
            "0.00"
        }
    }

    // Weekly Mock Data matching web Dashboard.tsx
    val weeklyData = remember {
        listOf(
            WeeklyDayData("Mon", 1200, 45),
            WeeklyDayData("Tue", 1350, 52),
            WeeklyDayData("Wed", 1100, 38),
            WeeklyDayData("Thu", 1800, 72),
            WeeklyDayData("Fri", 2100, 88),
            WeeklyDayData("Sat", 1900, 76),
            WeeklyDayData("Sun", 1600, 61)
        )
    }

    val maxWeeklyImpression = 2100f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .testTag("ad_dashboard_view"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header with optional Back and Switch to Campaigns
        item(key = "dashboard_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("dashboard_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ad Analytics",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Track your campaign performance and analytics.",
                        fontSize = 13.sp,
                        color = Color(0xFFA1A1AA),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (onNavigateToCampaigns != null) {
                    Button(
                        onClick = onNavigateToCampaigns,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Campaigns",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 2. Primary 4 Stat Cards in 2x2 grid (mobile friendly) matching web
        item(key = "stats_grid") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatCard(
                        title = "Total Impressions",
                        value = formatNumber(totalImpressions),
                        icon = Icons.Default.Visibility,
                        trend = "up",
                        trendValue = "12.5%",
                        iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        iconTint = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "Total Clicks",
                        value = formatNumber(totalClicks),
                        icon = Icons.Default.TouchApp,
                        trend = "up",
                        trendValue = "8.2%",
                        iconBgColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        iconTint = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatCard(
                        title = "Total Views",
                        value = formatNumber(totalViews),
                        icon = Icons.Default.PlayArrow,
                        trend = "down",
                        trendValue = "3.1%",
                        iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        iconTint = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "Total Spend",
                        value = "$${totalSpend}",
                        icon = Icons.Default.AttachMoney,
                        trend = "up",
                        trendValue = "15.3%",
                        iconBgColor = Color(0xFFF43F5E).copy(alpha = 0.15f),
                        iconTint = Color(0xFFFB7185),
                        subtitleNote = "100% Free Promo",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Second Row Stats: Click-Through Rate (CTR) Gauge & Active Campaigns
        item(key = "second_row_stats") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Click-Through Rate Card with custom Progress Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Click-Through Rate (CTR)",
                                    fontSize = 13.sp,
                                    color = Color(0xFFA1A1AA),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$overallCtr%",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "CTR Chart",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Gradient Progress bar matching web
                        val ctrNum = overallCtr.toFloatOrNull() ?: 0f
                        val progressFraction = (ctrNum / 10f).coerceIn(0.05f, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                                        )
                                    )
                            )
                        }

                        Text(
                            text = "Industry average: 1.5 - 2.5%",
                            fontSize = 12.sp,
                            color = Color(0xFF71717A),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }

                // Active Campaigns Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Campaigns",
                                fontSize = 13.sp,
                                color = Color(0xFFA1A1AA),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$activeCampaigns",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = "out of ${campaigns.size} total",
                                fontSize = 12.sp,
                                color = Color(0xFF71717A),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Active Campaigns",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Weekly Performance Chart Card matching web Dashboard.tsx
        item(key = "weekly_performance_chart") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Title and Filter Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Performance",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Box {
                            Surface(
                                onClick = { showTimeDropdown = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTimeRange,
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showTimeDropdown,
                                onDismissRequest = { showTimeDropdown = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                listOf("Last 7 days", "Last 30 days", "Last 90 days").forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            selectedTimeRange = opt
                                            showTimeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bar Chart representation (Impressions & Clicks overlaid bars)
                    var selectedDayTooltip by remember { mutableStateOf<WeeklyDayData?>(null) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyData.forEach { day ->
                            val imprFraction = (day.impressions / maxWeeklyImpression).coerceIn(0.1f, 1f)
                            val clickFraction = ((day.clicks * 20f) / maxWeeklyImpression).coerceIn(0.05f, 0.9f)
                            val isSelected = selectedDayTooltip?.day == day.day

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        selectedDayTooltip = if (isSelected) null else day
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    // Background bar container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                                    )

                                    // Impressions bar (blue)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(fraction = imprFraction)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) Color(0xFF3B82F6)
                                                else Color(0xFF3B82F6).copy(alpha = 0.8f)
                                            )
                                    )

                                    // Clicks bar (emerald) on bottom overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(fraction = clickFraction)
                                            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                                            .background(
                                                if (isSelected) Color(0xFF10B981)
                                                else Color(0xFF10B981).copy(alpha = 0.85f)
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = day.day,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else Color(0xFF71717A),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Tooltip display for clicked day
                    selectedDayTooltip?.let { day ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${day.day} Breakdown",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Impressions: ${day.impressions} • Clicks: ${day.clicks}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    // Chart Legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .border(BorderStroke(0.5.dp, Color(0xFF1E293B)), RoundedCornerShape(0.dp))
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF3B82F6))
                            )
                            Text(text = "Impressions", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF10B981))
                            )
                            Text(text = "Clicks", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                        }
                    }
                }
            }
        }

        // 5. Top Performing Campaigns Section
        item(key = "top_performing_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Top Performing Campaigns",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Campaigns ranked by engagement and impressions",
                            fontSize = 12.sp,
                            color = Color(0xFFA1A1AA),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (campaigns.isNotEmpty()) {
                        Text(
                            text = "${campaigns.take(5).size} shown",
                            fontSize = 11.sp,
                            color = Color(0xFF71717A)
                        )
                    }
                }
            }
        }

        if (campaigns.isEmpty()) {
            item(key = "top_performing_empty") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No campaigns yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA1A1AA)
                        )
                        Text(
                            text = "Create your first ad to see performance data",
                            fontSize = 12.sp,
                            color = Color(0xFF71717A)
                        )
                    }
                }
            }
        } else {
            // Render top campaigns (sorted by engagement/impressions)
            val topCampaigns = campaigns.sortedByDescending { it.impressions }.take(5)

            items(topCampaigns, key = { "top_ad_${it.id}" }) { ad ->
                TopCampaignPerformanceCard(ad = ad)
            }
        }
    }
}

/**
 * TopCampaignPerformanceCard - Matches web Mobile View & Tablet View cards
 * Features:
 * - Media Thumbnail (or fallback gradient)
 * - Name, location, days
 * - Status Pill badge (active=emerald, paused=amber, completed=zinc)
 * - Prominent CTR Badge (Emerald Pill with large % and CTR label)
 * - 3-Stat Bottom Row (Impressions, Views, Clicks)
 */
@Composable
private fun TopCampaignPerformanceCard(ad: AdCampaign) {
    val ctr = remember(ad.impressions, ad.clicks) {
        if (ad.impressions > 0) {
            String.format(Locale.US, "%.1f", (ad.clicks.toDouble() / ad.impressions.toDouble()) * 100.0)
        } else {
            "0.0"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Thumbnail + Info + Prominent CTR Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Media Thumbnail
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                    ) {
                        if (!ad.mediaUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ad.mediaUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF059669))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Title + location + status badge
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ad.name.ifBlank { "Campaign #${ad.id}" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${ad.location.ifBlank { "Global" }} • ${ad.days}d",
                            fontSize = 12.sp,
                            color = Color(0xFFA1A1AA),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp)
                        )

                        // Status pill
                        val (statusBg, statusBorder, statusColor) = when (ad.status.lowercase()) {
                            "active" -> Triple(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981).copy(alpha = 0.35f), Color(0xFF34D399))
                            "paused" -> Triple(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B).copy(alpha = 0.35f), Color(0xFFFBBF24))
                            else -> Triple(Color(0xFF71717A).copy(alpha = 0.15f), Color(0xFF71717A).copy(alpha = 0.35f), Color(0xFFA1A1AA))
                        }

                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(statusBg)
                                .border(1.dp, statusBorder, RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ad.status.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Prominently Positioned CTR Badge matching web mobile & desktop
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.20f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$ctr%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF34D399),
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "CTR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6EE7B7),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Stats 3-column row matching web mobile
            HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.8f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStatBox(
                    label = "Impressions",
                    value = formatNumber(ad.impressions.toLong()),
                    valueColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
                CompactStatBox(
                    label = "Views",
                    value = formatNumber(ad.views.toLong()),
                    valueColor = Color(0xFF60A5FA),
                    modifier = Modifier.weight(1f)
                )
                CompactStatBox(
                    label = "Clicks",
                    value = formatNumber(ad.clicks.toLong()),
                    valueColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactStatBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D121F))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFFA1A1AA),
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

/**
 * DashboardStatCard - Component for primary 4 stats
 */
@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    trend: String? = null,
    trendValue: String? = null,
    iconBgColor: Color,
    iconTint: Color,
    subtitleNote: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA1A1AA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = value,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (trend != null && trendValue != null) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (trend == "up") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (trend == "up") Color(0xFF34D399) else Color(0xFFF87171),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = trendValue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trend == "up") Color(0xFF34D399) else Color(0xFFF87171)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "vs last week",
                        fontSize = 11.sp,
                        color = Color(0xFF71717A)
                    )
                }
            } else if (subtitleNote != null) {
                Text(
                    text = subtitleNote,
                    fontSize = 11.sp,
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private data class WeeklyDayData(
    val day: String,
    val impressions: Int,
    val clicks: Int
)

private fun formatNumber(num: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(num)
}
