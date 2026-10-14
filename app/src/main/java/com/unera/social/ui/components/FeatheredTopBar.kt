package com.unera.social.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.R
import com.unera.social.model.User

/**
 * FeatheredTopBar - Matches web Header in Layout.tsx exactly.
 *
 * Left: Feathered Brand Button (Logo box + Bold Title)
 * Right:
 * 1. MarketPoint circular frosted button (with carrot orange badge)
 * 2. Search circular frosted button
 * 3. Notifications circular frosted button (with carrot orange badge)
 * 4. Profile Avatar button
 */
@Composable
fun FeatheredTopBar(
    currentUser: User,
    unreadNotificationsCount: Int = 0,
    marketplaceBadgeCount: Int = 0,
    isMarketplaceActive: Boolean = false,
    onHomeClick: () -> Unit = {},
    onMarketplaceClick: () -> Unit = {},
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    Surface(
        color = Color(0xFF0B1120),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .testTag("feathered_top_header")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT: Brand Button (Icon + Name) matching web
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onHomeClick() }
                        .testTag("app_brand_header")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0E14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_feather_logo),
                            contentDescription = "Feathered Logo",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Feathered",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.3).sp
                    )
                }

                // RIGHT: MarketPoint, Search, Notifications, Profile Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. MarketPoint Button: w-11 h-11 rounded-full bg-white/[0.08] border border-white/30
                    Box {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMarketplaceActive) Color.White.copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (isMarketplaceActive) Color.White.copy(alpha = 0.60f)
                                    else Color.White.copy(alpha = 0.30f),
                                    CircleShape
                                )
                                .clickable { onMarketplaceClick() }
                                .testTag("top_bar_marketplace_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "MarketPoint",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (marketplaceBadgeCount > 0) {
                            val badgeStr = if (marketplaceBadgeCount > 99) "99+" else marketplaceBadgeCount.toString()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF97316))
                                    .border(1.5.dp, Color(0xFF0F172A), CircleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeStr,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 2. Search Button: w-11 h-11 rounded-full bg-white/[0.08] border border-white/30
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                            .clickable { onSearchClick() }
                            .testTag("top_bar_search_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3. Notification Button: w-11 h-11 rounded-full bg-white/[0.08] border border-white/30 with carrot orange badge
                    Box {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                                .clickable { onNotificationsClick() }
                                .testTag("top_bar_notifications_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (unreadNotificationsCount > 0) {
                            val badgeStr = when {
                                unreadNotificationsCount > 99 -> "99+"
                                unreadNotificationsCount > 15 -> "15+"
                                unreadNotificationsCount > 9 -> "9+"
                                else -> unreadNotificationsCount.toString()
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF97316))
                                    .border(1.5.dp, Color(0xFF0F172A), CircleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeStr,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 4. User Profile Avatar: w-10 h-10 rounded-full border-2 border-[#334155]/80 bg-[#1E293B]
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, Color(0xFF334155).copy(alpha = 0.8f), CircleShape)
                            .clickable { onProfileClick() }
                            .testTag("top_bar_profile_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = currentUser.profileImageUrl,
                            contentDescription = currentUser.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Bottom subtle border border-[#1E293B]
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
        }
    }
}
