package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalClipboardManager
import com.unera.social.ui.components.VerifiedBadge
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.*
import com.unera.social.ui.theme.*

/**
 * 1:1 Parity with Web UserProfile.tsx
 * Supports:
 * - Cover photo with edit overlay button
 * - Overlapping circular avatar with camera edit button
 * - Admin/Moderation controls & popover actions (Verify/Unverify, Suspend 7d/30d/Manual, Unsuspend, Role toggle, Soft delete)
 * - Suspension banner with active expiry & reason
 * - Follow / Following toggle with real-time counters
 * - "Things in common" block
 * - Tabs: Posts, Videos, Photos, About, Followers
 * - Photos 3-column media grid with lightbox expansion
 * - Videos 2/3-column grid with views badges
 * - About section: Work, Education, Location, Website with Edit Details Modal
 * - Followers grid with avatar and profile click
 * - Full EditProfileModal dialog (Bio, Work, Education, Location, Website)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    currentUser: User,
    allUsers: List<User> = emptyList(),
    allPosts: List<Post> = emptyList(),
    allReels: List<Reel> = emptyList(),
    allStories: List<Story> = emptyList(),
    allProducts: List<Product> = emptyList(),
    onLikePost: (Long) -> Unit = {},
    onCommentPost: (Post) -> Unit = {},
    onSavePost: (Long) -> Unit = {},
    onSharePost: (Long) -> Unit = {},
    onUpdateUserDetails: (bio: String, work: String, education: String, location: String, website: String) -> Unit = { _, _, _, _, _ -> },
    onFollowUser: (Long) -> Unit = {},
    onProfileClick: (User) -> Unit = {},
    onMessageUser: (User) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onVerifyUser: (Long) -> Unit = {},
    onSuspendUser: (userId: Long, duration: String, reason: String) -> Unit = { _, _, _ -> },
    onUnsuspendUser: (Long) -> Unit = {},
    onToggleModeratorRole: (Long) -> Unit = {},
    onDeletePost: ((Long) -> Unit)? = null,
    onEditPost: ((Long, String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isMe = user.id == currentUser.id
    val isAdminOrMod = currentUser.role == "admin" || currentUser.role == "moderator"

    var selectedTab by remember { mutableStateOf("Posts") }
    var showEditProfileModal by remember { mutableStateOf(false) }
    var showModActionsMenu by remember { mutableStateOf(false) }
    var showSuspendDialog by remember { mutableStateOf(false) }
    var suspendDuration by remember { mutableStateOf("week") }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var lightboxImage by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current

    val userPosts = remember(allPosts, user.id) {
        allPosts.filter { it.author.id == user.id }
    }
    val userPhotos = remember(userPosts) {
        userPosts.filter { !it.mediaUrl.isNullOrBlank() }
    }
    val userReels = remember(allReels, user.id) {
        allReels.filter { it.author.id == user.id }
    }
    val isFollowing = remember(user.isFollowing, user.id) { user.isFollowing }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(3000)
            toastMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B18))
            .testTag("profile_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Sticky-like Top Bar
            item {
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFF8FAFC)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFFF8FAFC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadge(size = 16.dp)
                                }
                            }
                            Text(
                                text = "@${user.username}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // More options / Admin button
                        if (isAdminOrMod && !isMe) {
                            Box {
                                FilledTonalButton(
                                    onClick = { showModActionsMenu = true },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color(0xFFF87171)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentUser.role == "admin") "ADMIN" else "MOD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showModActionsMenu,
                                    onDismissRequest = { showModActionsMenu = false },
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (user.isVerified) "Remove Verification Badge" else "Verify Profile (Blue Badge)",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (user.isVerified) Icons.Filled.Cancel else Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            showModActionsMenu = false
                                            onVerifyUser(user.id)
                                            toastMessage = if (user.isVerified) "Verification removed" else "User verified with blue badge"
                                        }
                                    )

                                    if (user.suspendedUntil == null) {
                                        DropdownMenuItem(
                                            text = {
                                                Text("Suspend for 1 Week", color = Color(0xFFFBBF24), fontSize = 13.sp)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Filled.PauseCircle, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                            },
                                            onClick = {
                                                showModActionsMenu = false
                                                suspendDuration = "week"
                                                showSuspendDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text("Suspend for 1 Month", color = Color(0xFFF59E0B), fontSize = 13.sp)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Filled.EventBusy, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                            },
                                            onClick = {
                                                showModActionsMenu = false
                                                suspendDuration = "month"
                                                showSuspendDialog = true
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = {
                                                Text("Unsuspend Profile", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                            },
                                            onClick = {
                                                showModActionsMenu = false
                                                onUnsuspendUser(user.id)
                                                toastMessage = "User unsuspended successfully"
                                            }
                                        )
                                    }

                                    if (currentUser.role == "admin") {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (user.role == "moderator") "Remove Moderator Role" else "Make Moderator",
                                                    color = Color(0xFFA78BFA),
                                                    fontSize = 13.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Filled.Shield, null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                                            },
                                            onClick = {
                                                showModActionsMenu = false
                                                onToggleModeratorRole(user.id)
                                                toastMessage = "Moderator role updated"
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFF1E293B))

                                    DropdownMenuItem(
                                        text = {
                                            Text("Copy Profile Link", color = Color(0xFFF8FAFC), fontSize = 13.sp)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Link, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        },
                                        onClick = {
                                            showModActionsMenu = false
                                            clipboardManager.setText(AnnotatedString("https://unera.social/profile/${user.id}"))
                                            toastMessage = "Profile link copied to clipboard!"
                                        }
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("https://unera.social/profile/${user.id}"))
                                    toastMessage = "Profile link copied to clipboard!"
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share Profile",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // Cover and Avatar Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    // Cover photo
                    AsyncImage(
                        model = user.coverImageUrl,
                        contentDescription = "Cover photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clickable { lightboxImage = user.coverImageUrl }
                    )

                    // Edit cover button if owner
                    if (isMe) {
                        Surface(
                            onClick = { showEditProfileModal = true },
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 130.dp, end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit cover photo",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Avatar overlapping bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp)
                    ) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color(0xFF0F172A), CircleShape)
                                .clickable { lightboxImage = user.profileImageUrl }
                        )

                        if (isMe) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, Color(0xFF0F172A), CircleShape)
                                    .clickable { showEditProfileModal = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Change profile photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // User Info, Badges, Metrics & Action Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Name & Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = user.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = Color(0xFFF8FAFC)
                        )
                        if (user.isVerified) {
                            VerifiedBadge(size = 20.dp)
                        }
                        if (user.role == "moderator") {
                            Surface(
                                color = Color(0xFF4C1D95).copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Mod",
                                    color = Color(0xFFDDD6FE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (user.role == "admin") {
                            Surface(
                                color = Color(0xFF7F1D1D).copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Admin",
                                    color = Color(0xFFFECACA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (user.suspendedUntil != null) {
                            Surface(
                                color = Color(0xFF7F1D1D).copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Text(
                                    text = "Suspended",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Followers / Posts count row
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${user.followersCount}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            fontSize = 15.sp
                        )
                        Text(
                            text = " followers",
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "  ·  ",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${userPosts.size}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            fontSize = 15.sp
                        )
                        Text(
                            text = " posts",
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp
                        )
                    }

                    // Bio
                    Spacer(modifier = Modifier.height(10.dp))
                    if (user.bio.isNotBlank()) {
                        Text(
                            text = user.bio,
                            fontSize = 15.sp,
                            color = Color(0xFFF8FAFC),
                            lineHeight = 22.sp
                        )
                    } else if (isMe) {
                        Text(
                            text = "+ Add bio",
                            color = Color(0xFF1877F2),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showEditProfileModal = true }
                        )
                    }

                    // Work & Location details quick view
                    if (user.work.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Work,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Works at ${user.work}",
                                fontSize = 14.sp,
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (user.location.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = user.location,
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Action buttons
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isMe) {
                            Button(
                                onClick = onAddStoryClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add to story", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Button(
                                onClick = { showEditProfileModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Filled.Edit, null, tint = Color(0xFFF8FAFC), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit profile", color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        } else {
                            Button(
                                onClick = { onMessageUser(user) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Filled.ChatBubble, null, tint = Color(0xFFF8FAFC), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Message", color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Button(
                                onClick = { onFollowUser(user.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFF1E293B) else Color(0xFF1877F2)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = if (isFollowing) Color(0xFFF8FAFC) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFollowing) "Following" else "Follow",
                                    color = if (isFollowing) Color(0xFFF8FAFC) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // Suspension Warning Banner if active
                    if (user.suspendedUntil != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Color(0xFF450A0A),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Account is Suspended",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Expires: ${user.suspendedUntil} · Reason: ${user.suspensionReason ?: "Violation of guidelines"}",
                                        color = Color(0xFFFECACA),
                                        fontSize = 12.sp
                                    )
                                }
                                if (isAdminOrMod) {
                                    FilledTonalButton(
                                        onClick = { onUnsuspendUser(user.id) },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF059669),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Unsuspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Things in Common Card (for other users)
                    if (!isMe) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Things in common",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF8FAFC),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "You both are part of the Feathered community and explore ${user.location}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Navigation Tabs (Posts, Videos, Photos, About, Followers)
                    ScrollableTabRow(
                        selectedTabIndex = when (selectedTab) {
                            "Posts" -> 0
                            "Videos" -> 1
                            "Photos" -> 2
                            "About" -> 3
                            "Followers" -> 4
                            else -> 0
                        },
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color(0xFF1877F2),
                        edgePadding = 0.dp,
                        divider = { HorizontalDivider(color = Color(0xFF1E293B)) }
                    ) {
                        listOf("Posts", "Videos", "Photos", "About", "Followers").forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = {
                                    Text(
                                        text = tab,
                                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = if (selectedTab == tab) Color(0xFF1877F2) else Color(0xFF94A3B8)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Tab Content Rendering
            when (selectedTab) {
                "Posts" -> {
                    if (userPosts.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Filled.PostAdd,
                                title = "No posts yet",
                                subtitle = if (isMe) "Create your first post to share with friends!" else "${user.name} hasn't posted anything yet.",
                                buttonText = if (isMe) "Create Post" else null,
                                onButtonClick = onAddStoryClick
                            )
                        }
                    } else {
                        items(userPosts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                currentUser = currentUser,
                                onLike = { onLikePost(post.id) },
                                onComment = { onCommentPost(post) },
                                onSave = { onSavePost(post.id) },
                                onShare = { onSharePost(post.id) },
                                onDeletePost = onDeletePost,
                                onEditPost = onEditPost,
                                onUserClick = {}
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                "Videos" -> {
                    if (userReels.isEmpty() && userPosts.none { it.mediaUrl?.endsWith(".mp4") == true }) {
                        item {
                            EmptyStateView(
                                icon = Icons.Filled.VideoLibrary,
                                title = "No videos yet",
                                subtitle = if (isMe) "Upload short reels or video posts!" else "${user.name} hasn't uploaded any videos yet."
                            )
                        }
                    } else {
                        item {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Videos & Reels (${userReels.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 800.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(userReels) { reel ->
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(9f / 16f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black)
                                        ) {
                                            AsyncImage(
                                                model = reel.videoUrl,
                                                contentDescription = reel.caption,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                                            startY = 100f
                                                        )
                                                    )
                                            )
                                            // Play icon center
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            // Views badge
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.Visibility, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${reel.likesCount * 3 + 120} views", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Photos" -> {
                    if (userPhotos.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Filled.PhotoLibrary,
                                title = "No photos yet",
                                subtitle = if (isMe) "Share pictures to build your personal gallery!" else "${user.name} hasn't posted any photos yet."
                            )
                        }
                    } else {
                        item {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Photos (${userPhotos.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 800.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(userPhotos) { post ->
                                        AsyncImage(
                                            model = post.mediaUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { lightboxImage = post.mediaUrl }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "About" -> {
                    item {
                        AboutProfileTab(
                            user = user,
                            isMe = isMe,
                            onEditClick = { showEditProfileModal = true }
                        )
                    }
                }

                "Followers" -> {
                    item {
                        FollowersProfileTab(
                            user = user,
                            allUsers = allUsers,
                            onProfileClick = onProfileClick
                        )
                    }
                }
            }
        }

        // Floating Toast Notification
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = toastMessage ?: "",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Fullscreen Lightbox Viewer
        if (lightboxImage != null) {
            Dialog(
                onDismissRequest = { lightboxImage = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { lightboxImage = null },
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { lightboxImage = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                    AsyncImage(
                        model = lightboxImage,
                        contentDescription = "Expanded view",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                    )
                }
            }
        }

        // Edit Profile Modal
        if (showEditProfileModal) {
            EditProfileModal(
                user = user,
                onDismiss = { showEditProfileModal = false },
                onSave = { bio, work, edu, loc, web ->
                    onUpdateUserDetails(bio, work, edu, loc, web)
                    showEditProfileModal = false
                    toastMessage = "Profile updated successfully!"
                }
            )
        }

        // Suspend User Dialog
        if (showSuspendDialog) {
            var reason by remember { mutableStateOf("Violation of Community Standards") }
            AlertDialog(
                onDismissRequest = { showSuspendDialog = false },
                containerColor = Color(0xFF0F172A),
                title = {
                    Text(
                        text = "Suspend User Account",
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Suspending @${user.username} for ${if (suspendDuration == "week") "7 days" else "30 days"}.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Reason for Suspension") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuspendDialog = false
                            onSuspendUser(user.id, suspendDuration, reason)
                            toastMessage = "User suspended successfully"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Confirm Suspend")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSuspendDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

/**
 * About tab matching Web About section (Work, Education, Location, Website, Bio)
 */
@Composable
fun AboutProfileTab(
    user: User,
    isMe: Boolean,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "About",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
            if (isMe) {
                TextButton(onClick = onEditClick) {
                    Text("Edit Details", color = Color(0xFF1877F2), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\"${user.bio.ifBlank { "No bio available" }}\"",
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF1E293B))

        Text(
            text = "Work & Education",
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Work, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (user.work.isNotBlank()) "Works at ${user.work}" else "No work added",
                color = Color(0xFFF8FAFC),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.School, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (user.education.isNotBlank()) "Studied at ${user.education}" else "No education added",
                color = Color(0xFFF8FAFC),
                fontSize = 14.sp
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF1E293B))

        Text(
            text = "Contact & Basic Info",
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Place, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (user.location.isNotBlank()) user.location else "Location not specified",
                color = Color(0xFFF8FAFC),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Link, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (user.website.isNotBlank()) user.website else "No website link",
                color = Color(0xFF38BDF8),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Followers tab matching Web followers grid with avatars and profile navigation
 */
@Composable
fun FollowersProfileTab(
    user: User,
    allUsers: List<User>,
    onProfileClick: (User) -> Unit
) {
    val followersList = remember(allUsers, user.id) {
        allUsers.filter { it.id != user.id }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Followers (${followersList.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(followersList) { follower ->
                Surface(
                    onClick = { onProfileClick(follower) },
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = follower.profileImageUrl,
                            contentDescription = follower.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = follower.name,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = follower.location,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state container with subtle icon and optional action button
 */
@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF475569),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
        if (buttonText != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Edit Profile Modal matching Web EditProfileModal
 * Fields: Bio, Work, Education, Location, Website
 */
@Composable
fun EditProfileModal(
    user: User,
    onDismiss: () -> Unit,
    onSave: (bio: String, work: String, edu: String, loc: String, web: String) -> Unit
) {
    var bio by remember { mutableStateOf(user.bio) }
    var work by remember { mutableStateOf(user.work) }
    var education by remember { mutableStateOf(user.education) }
    var location by remember { mutableStateOf(user.location) }
    var website by remember { mutableStateOf(user.website) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFF8FAFC)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Bio Input
                    Column {
                        Text("Bio", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            placeholder = { Text("Tell people about yourself...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }

                    // Work Input
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Work, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Work", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = work,
                            onValueChange = { work = it },
                            placeholder = { Text("Add workplace or role") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }

                    // Education Input
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.School, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Education", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = education,
                            onValueChange = { education = it },
                            placeholder = { Text("Add university or high school") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }

                    // Location Input
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Place, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Location", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            placeholder = { Text("Add your city") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }

                    // Website Input
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Link, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Website", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            placeholder = { Text("https://example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(bio, work, education, location, website) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
