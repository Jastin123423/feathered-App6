package com.unera.social.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import com.unera.social.data.GroupMemberInfo
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unera.social.ui.components.VerifiedBadge
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.*

enum class GroupsTab(val label: String) {
    YOUR_GROUPS("Your groups"),
    DISCOVER("Discover"),
    INVITES("Invites")
}

enum class GroupDetailTab(val label: String) {
    DISCUSSION("Discussion"),
    EVENTS("Events"),
    MEMBERS("Members"),
    ABOUT("About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentUser: User,
    groups: List<Group>,
    groupPosts: List<GroupPost>,
    groupInvites: List<GroupInvite>,
    allEvents: List<Event>,
    allUsers: List<User>,
    initialGroupId: Long? = null,
    groupSuggestions: List<Group> = emptyList(),
    groupMembers: List<GroupMemberInfo> = emptyList(),
    groupEvents: List<Event> = emptyList(),
    isGroupsLoading: Boolean = false,
    isGroupPostsLoading: Boolean = false,
    onBack: () -> Unit,
    onSelectGroup: (Long?) -> Unit = {},
    onCreateGroup: (name: String, desc: String, type: String, category: String, profile: String, cover: String) -> Unit,
    onJoinGroup: (Long) -> Unit,
    onLeaveGroup: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onUpdateGroupSettings: (Long, String, String, Boolean) -> Unit,
    onPostToGroup: (Long, String, List<String>, Double?, String, String?, String?, String, String?, String?, String?, String?, String?, String?) -> Unit,
    onToggleLikePost: (Long, String) -> Unit,
    onAddPostComment: (Long, String) -> Unit,
    onDeletePost: (Long) -> Unit,
    onSharePost: (Long) -> Unit,
    onInviteUser: (Long, Long) -> Unit,
    onAcceptInvite: (Long, Long) -> Unit,
    onDeclineInvite: (Long) -> Unit,
    onToggleMemberPosting: (Long, Long, Boolean) -> Unit,
    onSetMemberRole: (Long, Long, String) -> Unit,
    onRemoveMember: (Long, Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeGroupId by remember { mutableStateOf(initialGroupId) }
    var selectedTopTab by remember { mutableStateOf(GroupsTab.YOUR_GROUPS) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Most visited") }
    var showSortDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeGroupId) {
        onSelectGroup(activeGroupId)
    }

    // Modals
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedCategoryForCreate by remember { mutableStateOf("general") }

    val context = LocalContext.current
    val activeGroup = groups.find { it.id == activeGroupId }

    BackHandler {
        if (activeGroupId != null) {
            activeGroupId = null
        } else {
            onBack()
        }
    }

    if (activeGroup != null) {
        val effectiveEvents = if (groupEvents.isNotEmpty()) groupEvents else allEvents.filter { it.groupId == activeGroup.id }
        GroupDetailScreen(
            group = activeGroup,
            currentUser = currentUser,
            allPosts = groupPosts.filter { it.groupId == activeGroup.id },
            allEvents = effectiveEvents,
            allUsers = allUsers,
            groupMembersInfo = groupMembers,
            isPostsLoading = isGroupPostsLoading,
            onBack = { activeGroupId = null },
            onJoinGroup = { onJoinGroup(activeGroup.id) },
            onLeaveGroup = { onLeaveGroup(activeGroup.id) },
            onDeleteGroup = {
                onDeleteGroup(activeGroup.id)
                activeGroupId = null
            },
            onUpdateSettings = { name, desc, postingAllowed ->
                onUpdateGroupSettings(activeGroup.id, name, desc, postingAllowed)
            },
            onPost = { content, images, price, curr, cond, loc, stat, job, comp, jType, sal, appType, appVal ->
                onPostToGroup(activeGroup.id, content, images, price, curr, cond, loc, stat, job, comp, jType, sal, appType, appVal)
            },
            onToggleLikePost = onToggleLikePost,
            onAddPostComment = onAddPostComment,
            onDeletePost = onDeletePost,
            onSharePost = onSharePost,
            onInviteUser = { userId -> onInviteUser(activeGroup.id, userId) },
            onToggleMemberPosting = { userId, disabled -> onToggleMemberPosting(activeGroup.id, userId, disabled) },
            onSetMemberRole = { userId, role -> onSetMemberRole(activeGroup.id, userId, role) },
            onRemoveMember = { userId -> onRemoveMember(activeGroup.id, userId) },
            onProfileClick = onProfileClick,
            modifier = modifier
        )
    } else {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Color(0xFF0F172A))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(
                                text = "Groups",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showCategoryDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create Group", tint = Color.White)
                            }
                        }
                    }

                    // 3 Tabs: Your groups, Discover, Invites
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GroupsTab.values().forEach { tab ->
                            val selected = selectedTopTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) Color(0xFF1877F2) else Color(0xFF1E293B))
                                    .clickable { selectedTopTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tab.label,
                                        color = if (selected) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (tab == GroupsTab.INVITES && groupInvites.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color(0xFFF3425F))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${groupInvites.size}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search Groups", color = Color(0xFF64748B), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                }
            },
            containerColor = Color(0xFF050B18),
            modifier = modifier
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTopTab) {
                    GroupsTab.YOUR_GROUPS -> {
                        val userGroups = groups.filter { group ->
                            val isOwner = group.adminId == currentUser.id
                            val isMem = group.isMember || group.members.contains(currentUser.id)
                            isOwner || isMem
                        }.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
                        }

                        val createdByMe = userGroups.filter { it.adminId == currentUser.id }
                        val joinedByMe = userGroups.filter { it.adminId != currentUser.id }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Your groups (${userGroups.size})",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Sort",
                                        color = Color(0xFF1877F2),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { showSortDialog = true }
                                    )
                                }
                            }

                            if (createdByMe.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Created by you",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(createdByMe) { group ->
                                    GroupListItem(
                                        group = group,
                                        onClick = { activeGroupId = group.id }
                                    )
                                }
                            }

                            if (joinedByMe.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Groups you joined",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(joinedByMe) { group ->
                                    GroupListItem(
                                        group = group,
                                        onClick = { activeGroupId = group.id }
                                    )
                                }
                            }

                            if (userGroups.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isGroupsLoading) {
                                            CircularProgressIndicator(color = Color(0xFF1877F2))
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(56.dp))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text("No groups yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("Create your first group or discover active ones!", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(
                                                    onClick = { showCategoryDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Create Group")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GroupsTab.DISCOVER -> {
                        val unjoinedFromGroups = groups.filter { group ->
                            val isOwner = group.adminId == currentUser.id
                            val isMem = group.isMember || group.members.contains(currentUser.id)
                            !isOwner && !isMem
                        }
                        val combinedSuggestions = (groupSuggestions + unjoinedFromGroups)
                            .distinctBy { it.id }
                            .filter {
                                searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
                            }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Text(
                                    text = "Discover Groups (${combinedSuggestions.size})",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }

                            if (isGroupsLoading && combinedSuggestions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF1877F2))
                                    }
                                }
                            } else {
                                items(combinedSuggestions) { group ->
                                    GroupListItem(
                                        group = group,
                                        onClick = { activeGroupId = group.id },
                                        showJoinButton = true,
                                        onJoinClick = { onJoinGroup(group.id) }
                                    )
                                }

                                if (combinedSuggestions.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No more groups to discover right now", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GroupsTab.INVITES -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Text(
                                    text = "Group Invites (${groupInvites.size})",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }

                            items(groupInvites) { invite ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = invite.group.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${invite.group.name}" },
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(invite.group.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("Invited by ${invite.inviter.name}", color = Color(0xFF45BD62), fontSize = 12.sp)
                                                Text("${invite.group.membersCount} members", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onAcceptInvite(invite.id, invite.groupId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Accept & Join")
                                            }
                                            OutlinedButton(
                                                onClick = { onDeclineInvite(invite.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Decline")
                                            }
                                        }
                                    }
                                }
                            }

                            if (groupInvites.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.MailOutline, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(52.dp))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("No pending invites", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("When someone invites you to a group, it will show up here.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Category Selection Modal
    if (showCategoryDialog) {
        CategorySelectionDialog(
            onDismiss = { showCategoryDialog = false },
            onSelectCategory = { cat ->
                selectedCategoryForCreate = cat
                showCategoryDialog = false
                showCreateGroupDialog = true
            }
        )
    }

    // Create Group Modal
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            category = selectedCategoryForCreate,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, privacy ->
                onCreateGroup(
                    name,
                    desc,
                    privacy,
                    selectedCategoryForCreate,
                    "https://ui-avatars.com/api/?name=${name}&background=random",
                    "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1500&q=80"
                )
                showCreateGroupDialog = false
            }
        )
    }

    // Sort Dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort groups", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Most visited", "Recently active", "Alphabetical").forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sortMode = mode
                                    showSortDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mode, color = Color.White, fontSize = 16.sp)
                            if (sortMode == mode) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1877F2))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF0F172A)
        )
    }
}

@Composable
fun GroupListItem(
    group: Group,
    onClick: () -> Unit,
    showJoinButton: Boolean = false,
    onJoinClick: (() -> Unit)? = null
) {
    val catColor = when (group.category) {
        "recruitment" -> Color(0xFF45BD62)
        "buy_sell" -> Color(0xFFF7B928)
        else -> Color(0xFF1877F2)
    }

    val catIcon = when (group.category) {
        "recruitment" -> Icons.Default.Work
        "buy_sell" -> Icons.Default.Storefront
        else -> Icons.Default.Groups
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = group.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${group.name}" },
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${group.membersCount} members • ${if (group.isPrivate) "Private" else "Public"}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        if (showJoinButton && onJoinClick != null) {
            Button(
                onClick = onJoinClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Join", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    currentUser: User,
    allPosts: List<GroupPost>,
    allEvents: List<Event>,
    allUsers: List<User>,
    groupMembersInfo: List<GroupMemberInfo> = emptyList(),
    isPostsLoading: Boolean = false,
    onBack: () -> Unit,
    onJoinGroup: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onUpdateSettings: (name: String, desc: String, postingAllowed: Boolean) -> Unit,
    onPost: (String, List<String>, Double?, String, String?, String?, String, String?, String?, String?, String?, String?, String?) -> Unit,
    onToggleLikePost: (Long, String) -> Unit,
    onAddPostComment: (Long, String) -> Unit,
    onDeletePost: (Long) -> Unit,
    onSharePost: (Long) -> Unit,
    onInviteUser: (Long) -> Unit,
    onToggleMemberPosting: (Long, Boolean) -> Unit,
    onSetMemberRole: (Long, String) -> Unit,
    onRemoveMember: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(GroupDetailTab.DISCUSSION) }
    var showCreatePostModal by remember { mutableStateOf(false) }
    var showInviteModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isOwner = group.adminId == currentUser.id
    val isMember = isOwner || group.isMember || group.members.contains(currentUser.id)
    val canPost = isOwner || (isMember && group.memberPostingAllowed)

    val catColor = when (group.category) {
        "recruitment" -> Color(0xFF45BD62)
        "buy_sell" -> Color(0xFFF7B928)
        else -> Color(0xFF1877F2)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B18))
    ) {
        // Cover and Back Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = group.coverImageUrl.ifBlank { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1500&q=80" },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Row {
                        IconButton(
                            onClick = { showShareModal = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        if (isOwner) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showSettingsModal = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Group Identity Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = group.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${group.name}" },
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFF1877F2), RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${when (group.category) { "recruitment" -> "Careers & Hiring"; "buy_sell" -> "Buy & Sell"; else -> "General" }} • ${if (group.isPrivate) "Private" else "Public"} • ${group.membersCount} members",
                            color = catColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Join/Joined, Invite, Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isMember) {
                        Button(
                            onClick = onLeaveGroup,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF45BD62), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Joined", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showInviteModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Invite", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onJoinGroup,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Group", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Row: Discussion, Events, Members, About
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    GroupDetailTab.values().forEach { tab ->
                        val selected = selectedTab == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = tab.label,
                                color = if (selected) Color(0xFF1877F2) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF1877F2))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            GroupDetailTab.DISCUSSION -> {
                if (canPost) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable { showCreatePostModal = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = currentUser.profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when (group.category) {
                                        "buy_sell" -> "Sell something in ${group.name}..."
                                        "recruitment" -> "Post a job opening in ${group.name}..."
                                        else -> "Write something to ${group.name}..."
                                    },
                                    color = Color(0xFF94A3B8),
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }
                    }
                }

                items(allPosts) { post ->
                    GroupPostCard(
                        post = post,
                        group = group,
                        currentUser = currentUser,
                        onLike = { onToggleLikePost(post.id, "like") },
                        onComment = { text -> onAddPostComment(post.id, text) },
                        onShare = { onSharePost(post.id) },
                        onDelete = if (isOwner || post.author.id == currentUser.id) { { onDeletePost(post.id) } } else null,
                        onProfileClick = onProfileClick
                    )
                }

                if (allPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPostsLoading) {
                                CircularProgressIndicator(color = Color(0xFF1877F2))
                            } else {
                                Text("No discussions yet. Be the first to start a conversation!", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            GroupDetailTab.EVENTS -> {
                items(allEvents) { event ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(event.date, color = Color(0xFF1877F2), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(event.description, color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }
                }

                if (allEvents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No group events scheduled.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }
                }
            }

            GroupDetailTab.MEMBERS -> {
                if (groupMembersInfo.isNotEmpty()) {
                    items(groupMembersInfo) { member ->
                        val userIsAdmin = member.role.equals("admin", ignoreCase = true) || member.userId == group.adminId
                        val userIsMod = member.role.equals("moderator", ignoreCase = true)
                        var showActionMenu by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .clickable { onProfileClick(member.userId) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = member.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${member.name}" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(member.name.ifBlank { "Member" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (member.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadge(size = 14.dp)
                                    }
                                    if (userIsAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Admin", color = Color(0xFF1877F2), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    } else if (userIsMod) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Moderator", color = Color(0xFF45BD62), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                Text("@${member.username}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                if (member.postingDisabled) {
                                    Text("Posting muted", color = Color(0xFFF3425F), fontSize = 11.sp)
                                }
                            }

                            if (isOwner && member.userId != currentUser.id) {
                                Box {
                                    IconButton(onClick = { showActionMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Manage", tint = Color(0xFF94A3B8))
                                    }
                                    DropdownMenu(
                                        expanded = showActionMenu,
                                        onDismissRequest = { showActionMenu = false },
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (member.postingDisabled) "Enable Posting" else "Mute Posting",
                                                    color = Color.White
                                                )
                                            },
                                            onClick = {
                                                onToggleMemberPosting(member.userId, !member.postingDisabled)
                                                showActionMenu = false
                                            }
                                        )
                                        if (userIsAdmin) {
                                            DropdownMenuItem(
                                                text = { Text("Demote from Admin", color = Color.White) },
                                                onClick = {
                                                    onSetMemberRole(member.userId, "remove-admin")
                                                    showActionMenu = false
                                                }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text("Promote to Admin", color = Color.White) },
                                                onClick = {
                                                    onSetMemberRole(member.userId, "make-admin")
                                                    showActionMenu = false
                                                }
                                            )
                                        }
                                        if (!userIsMod) {
                                            DropdownMenuItem(
                                                text = { Text("Promote to Moderator", color = Color.White) },
                                                onClick = {
                                                    onSetMemberRole(member.userId, "make-moderator")
                                                    showActionMenu = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Remove from Group", color = Color(0xFFF3425F)) },
                                            onClick = {
                                                onRemoveMember(member.userId)
                                                showActionMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val memberIds = (group.members + group.adminId).distinct()
                    val groupUsers = allUsers.filter { memberIds.contains(it.id) }

                    items(groupUsers) { user ->
                        val userIsOwner = user.id == group.adminId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .clickable { onProfileClick(user.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (userIsOwner) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Admin (Owner)", color = Color(0xFF1877F2), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                Text("@${user.username}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }

                            if (isOwner && !userIsOwner) {
                                IconButton(onClick = { onRemoveMember(user.id) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = Color(0xFFF3425F))
                                }
                            }
                        }
                    }
                }
            }

            GroupDetailTab.ABOUT -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("About this community", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(group.description.ifBlank { "No description provided." }, color = Color(0xFFCBD5E1), fontSize = 14.sp, lineHeight = 20.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (group.isPrivate) Icons.Default.Lock else Icons.Default.Public, contentDescription = null, tint = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(if (group.isPrivate) "Private Group" else "Public Group", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(if (group.isPrivate) "Only members can see posts and members" else "Anyone can see who's in the group and what they post", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Created in 2026", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Active East Africa Community", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Post Modal in Group
    if (showCreatePostModal) {
        CreateGroupPostDialog(
            group = group,
            currentUser = currentUser,
            onDismiss = { showCreatePostModal = false },
            onSubmit = { content, images, price, curr, cond, loc, stat, job, comp, jType, sal, appType, appVal ->
                onPost(content, images, price, curr, cond, loc, stat, job, comp, jType, sal, appType, appVal)
                showCreatePostModal = false
            }
        )
    }

    // Invite Modal
    if (showInviteModal) {
        InviteFollowersDialog(
            group = group,
            followers = allUsers.filter { it.id != currentUser.id && !group.members.contains(it.id) },
            onDismiss = { showInviteModal = false },
            onInvite = { userId ->
                onInviteUser(userId)
                Toast.makeText(context, "Invitation sent!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Share Group Dialog
    if (showShareModal) {
        ShareGroupDialog(
            group = group,
            onDismiss = { showShareModal = false },
            onCopyLink = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Group Link", "https://unera.social/groups/${group.id}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Group link copied to clipboard!", Toast.LENGTH_SHORT).show()
                showShareModal = false
            }
        )
    }

    // Settings Modal
    if (showSettingsModal) {
        GroupSettingsDialog(
            group = group,
            onDismiss = { showSettingsModal = false },
            onSave = { name, desc, postingAllowed ->
                onUpdateSettings(name, desc, postingAllowed)
                showSettingsModal = false
            },
            onDelete = {
                showSettingsModal = false
                onDeleteGroup()
            }
        )
    }
}

@Composable
fun GroupPostCard(
    post: GroupPost,
    group: Group,
    currentUser: User,
    onLike: () -> Unit,
    onComment: (String) -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onProfileClick: (Long) -> Unit
) {
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Author Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.author.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(post.author.id) },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.author.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (post.author.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(size = 14.dp)
                        }
                    }
                    Text(post.createdAt, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF94A3B8))
                    }
                }
            }

            // Buy & Sell Badge and Price
            if (post.price != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${post.currency} ${post.price}",
                        color = Color(0xFF45BD62),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    if (!post.condition.isNullOrBlank()) {
                        Text(
                            text = post.condition,
                            color = Color(0xFFF7B928),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (!post.location.isNullOrBlank()) {
                    Text(
                        text = "📍 ${post.location}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            // Job / Careers Box
            if (!post.jobTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(post.jobTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (!post.company.isNullOrBlank()) {
                            Text("🏢 ${post.company}", color = Color(0xFF45BD62), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!post.salary.isNullOrBlank()) {
                            Text("💰 ${post.salary}", color = Color(0xFFF7B928), fontSize = 13.sp)
                        }
                        if (!post.jobType.isNullOrBlank()) {
                            Text("⏳ ${post.jobType}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }

                        if (!post.applicationValue.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Applying via ${post.applicationValue}", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Apply Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Post Content
            Spacer(modifier = Modifier.height(10.dp))
            Text(post.content, color = Color(0xFFF8FAFC), fontSize = 15.sp, lineHeight = 22.sp)

            // Images
            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = post.imageUrls.first(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(6.dp))

            // Action row: Like, Comment, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onLike() }
                        .padding(8.dp)
                ) {
                    Icon(
                        if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color(0xFFF3425F) else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.likesCount}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showCommentInput = !showCommentInput }
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.commentsCount}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onShare() }
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.sharesCount}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }

            // Comment input box
            if (showCommentInput) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a comment...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onComment(commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF1877F2))
                    }
                }

                // Render existing comments
                if (post.comments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        post.comments.takeLast(3).forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(8.dp)
                            ) {
                                Text(c.author.name, color = Color(0xFF1877F2), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(c.text, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialogs

@Composable
fun CategorySelectionDialog(
    onDismiss: () -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val categories = listOf(
        Triple("general", "General", "Standard community for discussions, sharing and connection"),
        Triple("recruitment", "Careers & Recruitment", "Find talent, professional networking and job openings"),
        Triple("buy_sell", "Buy and Sell", "Marketplace for trading goods, electronics and items")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Group Category", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categories.forEach { (catId, title, desc) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCategory(catId) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF0F172A)
    )
}

@Composable
fun CreateGroupDialog(
    category: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, privacy: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Community", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1877F2)
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1877F2)
                    ),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Private Group", color = Color.White, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), description.trim(), if (isPrivate) "private" else "public")
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF0F172A)
    )
}

@Composable
fun CreateGroupPostDialog(
    group: Group,
    currentUser: User,
    onDismiss: () -> Unit,
    onSubmit: (String, List<String>, Double?, String, String?, String?, String, String?, String?, String?, String?, String?, String?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    // Buy & Sell
    var priceStr by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Used - Good") }
    var location by remember { mutableStateOf("") }

    // Recruitment
    var jobTitle by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var applicationValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (group.category) {
                    "buy_sell" -> "List Item for Sale"
                    "recruitment" -> "Post Job Opening"
                    else -> "Post to ${group.name}"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (group.category == "buy_sell") {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (USD)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = condition,
                        onValueChange = { condition = it },
                        label = { Text("Condition (e.g. Pristine, Like New)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Pickup Location", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (group.category == "recruitment") {
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Job Title", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = salary,
                        onValueChange = { salary = it },
                        label = { Text("Salary Range", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = applicationValue,
                        onValueChange = { applicationValue = it },
                        label = { Text("Application Email / Link", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Description / Post Content", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (optional)", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank() || jobTitle.isNotBlank() || priceStr.isNotBlank()) {
                        val images = if (imageUrl.isNotBlank()) listOf(imageUrl.trim()) else emptyList()
                        val parsedPrice = priceStr.toDoubleOrNull()
                        onSubmit(
                            content.trim(),
                            images,
                            parsedPrice,
                            "USD",
                            condition.ifBlank { null },
                            location.ifBlank { null },
                            "available",
                            jobTitle.ifBlank { null },
                            company.ifBlank { null },
                            "Full-time",
                            salary.ifBlank { null },
                            if (applicationValue.isNotBlank()) "email" else null,
                            applicationValue.ifBlank { null }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF0F172A)
    )
}

@Composable
fun InviteFollowersDialog(
    group: Group,
    followers: List<User>,
    onDismiss: () -> Unit,
    onInvite: (Long) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val invitedIds = remember { mutableStateListOf<Long>() }

    val filtered = followers.filter {
        search.isBlank() || it.name.contains(search, ignoreCase = true) || it.username.contains(search, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Followers to ${group.name}", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search users...", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(filtered) { user ->
                        val alreadyInvited = invitedIds.contains(user.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = user.profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(user.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("@${user.username}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    if (!alreadyInvited) {
                                        invitedIds.add(user.id)
                                        onInvite(user.id)
                                    }
                                },
                                enabled = !alreadyInvited,
                                colors = ButtonDefaults.buttonColors(containerColor = if (alreadyInvited) Color(0xFF1E293B) else Color(0xFF1877F2)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (alreadyInvited) "Invited" else "Invite", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = Color(0xFF1877F2)) }
        },
        containerColor = Color(0xFF0F172A)
    )
}

@Composable
fun ShareGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Community", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(group.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("https://unera.social/groups/${group.id}", color = Color(0xFF1877F2), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCopyLink,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Group Link")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF0F172A)
    )
}

@Composable
fun GroupSettingsDialog(
    group: Group,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, postingAllowed: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var desc by remember { mutableStateOf(group.description) }
    var postingAllowed by remember { mutableStateOf(group.memberPostingAllowed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group Settings", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Member Posting", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("When off, only admin can post", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = postingAllowed,
                        onCheckedChange = { postingAllowed = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3425F).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Group", color = Color(0xFFF3425F), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), desc.trim(), postingAllowed) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        },
        containerColor = Color(0xFF0F172A)
    )
}
