package com.unera.social.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unera.social.model.Screen

private fun formatBadge(count: Int): String {
    if (count <= 0) return ""
    return if (count > 15) "15+" else count.toString()
}

/**
 * FeatheredBottomNav - Matches web BottomNavigation in Layout.tsx exactly.
 *
 * 5 positions:
 * 1. Home (fas fa-th-large / GridView)
 * 2. MarketPoint (fas fa-store / Storefront)
 * 3. CENTRAL ELEVATED CIRCULAR [POST] BUTTON (-top-5.5 elevated circular button with gradient + text "POST")
 * 4. Message (fas fa-comment-dots / ChatBubble)
 * 5. Menu (fas fa-bars / Menu)
 */
@Composable
fun FeatheredBottomNav(
    currentScreen: Screen,
    unreadMessagesCount: Int = 0,
    marketplaceBadgeCount: Int = 0,
    homeBadgeCount: Int = 0,
    onTabSelected: (Screen) -> Unit,
    onPostClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val isHomeActive = currentScreen == Screen.FEED
    val isMarketActive = currentScreen == Screen.MARKETPLACE
    val isMessageActive = currentScreen == Screen.MESSAGES

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("feathered_bottom_navigation"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Nav Bar Surface
        Surface(
            color = Color(0xFF0B1120),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                NavTabItem(
                    label = "Home",
                    icon = Icons.Default.GridView,
                    isActive = isHomeActive,
                    badgeCount = homeBadgeCount,
                    testTag = "nav_tab_home",
                    onClick = { onTabSelected(Screen.FEED) },
                    modifier = Modifier.weight(1f)
                )

                // 2. MarketPoint
                NavTabItem(
                    label = "MarketPoint",
                    icon = Icons.Default.Storefront,
                    isActive = isMarketActive,
                    badgeCount = marketplaceBadgeCount,
                    testTag = "nav_tab_marketplace",
                    onClick = { onTabSelected(Screen.MARKETPLACE) },
                    modifier = Modifier.weight(1f)
                )

                // 3. Central Post Button Area (Label underneath)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onPostClick() }
                        .testTag("nav_tab_post"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "POST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1877F2),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // 4. Message
                NavTabItem(
                    label = "Message",
                    icon = Icons.Default.ChatBubble,
                    isActive = isMessageActive,
                    badgeCount = unreadMessagesCount,
                    testTag = "nav_tab_messages",
                    onClick = { onTabSelected(Screen.MESSAGES) },
                    modifier = Modifier.weight(1f)
                )

                // 5. Menu
                NavTabItem(
                    label = "Menu",
                    icon = Icons.Default.Menu,
                    isActive = false,
                    badgeCount = 0,
                    testTag = "nav_tab_menu",
                    onClick = onMenuClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. CENTRAL ELEVATED CIRCULAR [POST] BUTTON
        // Matching web:
        // absolute -top-5.5 w-14 h-14 rounded-full bg-gradient-to-tr from-[#166FE5] via-[#1877F2] to-[#3B82F6]
        // border-[3.5px] border-[#050B18] shadow-[0_6px_22px_rgba(24,119,242,0.48)]
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFF1877F2),
                        spotColor = Color(0xFF1877F2)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF166FE5), Color(0xFF1877F2), Color(0xFF3B82F6))
                        )
                    )
                    .border(3.5.dp, Color(0xFF050B18), CircleShape)
                    .clickable { onPostClick() }
                    .testTag("nav_post_floating_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Post",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF1877F2) else Color(0xFF94A3B8),
                modifier = Modifier.size(21.dp)
            )

            // Red notification badge matching web: bg-[#E41E3F] text-[10px] font-bold
            if (badgeCount > 0) {
                val badgeText = formatBadge(badgeCount)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-6).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE41E3F))
                        .padding(horizontal = 4.5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color(0xFF1877F2) else Color(0xFF94A3B8)
        )

        // Active indicator pill underneath matching web: w-1.5 h-1 rounded-full bg-[#1877F2] mt-0.5
        if (isActive) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1877F2))
            )
        } else {
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}
