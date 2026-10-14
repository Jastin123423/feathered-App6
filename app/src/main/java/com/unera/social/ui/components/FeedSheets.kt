package com.unera.social.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.Comment
import com.unera.social.model.Post
import com.unera.social.model.User
import com.unera.social.model.UserReaction

// Reaction Emojis
val REACTION_EMOJIS = listOf(
    "like" to "👍",
    "love" to "❤️",
    "haha" to "😂",
    "wow" to "😮",
    "sad" to "😢",
    "angry" to "😡",
    "fire" to "🔥",
    "party" to "🎉",
    "clap" to "👏",
    "star" to "⭐"
)

fun getReactionEmoji(type: String): String {
    return REACTION_EMOJIS.find { it.first == type }?.second ?: "👍"
}

/**
 * Fullscreen Gallery Viewer matching web GalleryViewer
 */
@Composable
fun GalleryViewerDialog(
    images: List<String>,
    initialIndex: Int = 0,
    post: Post,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    if (images.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, images.size - 1)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050B18))
                .testTag("gallery_viewer_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar - Solid Opaque (#0B1120)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF0B1120))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentIndex + 1}/${images.size}",
                        color = Color(0xFFF8FAFC),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Middle Image Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF050B18)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = images[currentIndex],
                        contentDescription = "Gallery photo ${currentIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Prev / Next touch areas if multiple images
                    if (currentIndex > 0) {
                        IconButton(
                            onClick = { currentIndex-- },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous",
                                tint = Color.White
                            )
                        }
                    }

                    if (currentIndex < images.size - 1) {
                        IconButton(
                            onClick = { currentIndex++ },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Bottom Engagement Bar (#0B1120)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1120))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${post.likesCount} Reactions",
                            color = Color(0xFFF8FAFC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${post.commentsCount} Discussions • ${post.sharesCount} Shares",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onLike) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (post.isLiked) Color(0xFFF43F5E) else Color(0xFFF8FAFC)
                                )
                            }
                            IconButton(onClick = onComment) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Comment",
                                    tint = Color(0xFFF8FAFC)
                                )
                            }
                            IconButton(onClick = onShare) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Share",
                                    tint = Color(0xFFF8FAFC)
                                )
                            }
                        }

                        IconButton(onClick = onSave) {
                            Icon(
                                imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (post.isSaved) Color(0xFF1877F2) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reactions Sheet Dialog matching web ReactionsSheet
 */
@Composable
fun ReactionsSheetDialog(
    post: Post,
    allUsers: List<User>,
    onDismiss: () -> Unit,
    onUserClick: (User) -> Unit
) {
    var selectedTab by remember { mutableStateOf("all") }

    val reactionTypes = remember(post.reactions) {
        post.reactions.map { it.type }.distinct()
    }

    val filteredReactions = remember(selectedTab, post.reactions) {
        if (selectedTab == "all") post.reactions
        else post.reactions.filter { it.type == selectedTab }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050B18))
                .testTag("reactions_sheet_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF0B1120))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFF8FAFC)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "People who reacted",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                }

                // Filter Tabs Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1120))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTab == "all",
                            onClick = { selectedTab = "all" },
                            label = { Text("All ${post.reactions.size}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1877F2),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }

                    items(reactionTypes) { type ->
                        val count = post.reactions.count { it.type == type }
                        FilterChip(
                            selected = selectedTab == type,
                            onClick = { selectedTab = type },
                            label = { Text("${getReactionEmoji(type)} $count") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1877F2),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // List of reactors
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (filteredReactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No reactions yet.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        items(filteredReactions, key = { it.userId }) { reaction ->
                            val reactorUser = allUsers.find { it.id == reaction.userId }
                                ?: User(reaction.userId, reaction.userName, "", reaction.userAvatar)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onUserClick(reactorUser) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    AsyncImage(
                                        model = reaction.userAvatar.ifBlank { reactorUser.profileImageUrl },
                                        contentDescription = reaction.userName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                    )
                                    // Mini reaction emoji badge
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0B1120))
                                            .border(1.dp, Color(0xFF1E293B), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = getReactionEmoji(reaction.type), fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reaction.userName.ifBlank { reactorUser.name },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFFF8FAFC)
                                    )
                                    if (reactorUser.username.isNotBlank()) {
                                        Text(
                                            text = "@${reactorUser.username}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8)
                                        )
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

/**
 * Share Bottom Sheet Dialog matching web ShareBottomSheet
 */
@Composable
fun ShareBottomSheetDialog(
    post: Post,
    onDismiss: () -> Unit,
    onShareToFeed: () -> Unit,
    onShareToGroup: () -> Unit,
    onSendMessage: () -> Unit,
    onCopyLink: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("share_bottom_sheet_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF334155))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Post Mini Preview Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!post.mediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription = "Post preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.author.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = post.content.ifBlank { "Shared post" },
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option: Share to Feed
                ShareOptionRow(
                    icon = Icons.Default.Newspaper,
                    iconTint = Color(0xFF1877F2),
                    iconBg = Color(0xFF1877F2).copy(alpha = 0.15f),
                    title = "Share to Feed / Profile",
                    subtitle = "Share to your profile & public feed",
                    onClick = onShareToFeed
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option: Share to Groups
                ShareOptionRow(
                    icon = Icons.Default.Groups,
                    iconTint = Color(0xFF45BD62),
                    iconBg = Color(0xFF45BD62).copy(alpha = 0.15f),
                    title = "Share to Groups & Brands",
                    subtitle = "Share with your groups & communities",
                    onClick = onShareToGroup
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option: Send as Message
                ShareOptionRow(
                    icon = Icons.Default.ChatBubble,
                    iconTint = Color(0xFF1877F2),
                    iconBg = Color(0xFF1877F2).copy(alpha = 0.15f),
                    title = "Send as a Message",
                    subtitle = "Share via direct message",
                    onClick = onSendMessage
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option: Copy Post Link
                ShareOptionRow(
                    icon = Icons.Default.Link,
                    iconTint = Color(0xFF1877F2),
                    iconBg = Color(0xFF1877F2).copy(alpha = 0.15f),
                    title = "Copy Post Link",
                    subtitle = "https://featheredsocial.site/post/${post.id}",
                    onClick = onCopyLink
                )

                // Option: Download Original (Slot 0 untouch upload for videos)
                val isVideo = post.postType == "video" || post.mediaType == "video" || !post.videoUrl.isNullOrBlank()
                if (isVideo) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ShareOptionRow(
                        icon = Icons.Default.Download,
                        iconTint = Color(0xFF38BDF8),
                        iconBg = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        title = "Download Original Video",
                        subtitle = "Full untouched original upload",
                        onClick = {
                            val downloadUrl = post.getOriginalDownloadUrl()
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                context.startActivity(intent)
                                Toast.makeText(context, "Opening original download...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to open download", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text(text = "Cancel", color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ShareOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFF8FAFC))
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Discussions & Comments Sheet Dialog matching web CommentsSheet
 */
@Composable
fun DiscussionsSheetDialog(
    post: Post,
    currentUser: User,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onLikeComment: (Long) -> Unit,
    onDeleteComment: ((Long) -> Unit)? = null,
    onProfileClick: ((User) -> Unit)? = null
) {
    var newCommentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<String?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var selectedCommentForActions by remember { mutableStateOf<Comment?>(null) }

    val quickEmojis = listOf("❤️", "🔥", "😂", "👏", "🙌", "😍", "✨", "💯", "🎉")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050B18))
                .testTag("discussions_sheet_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF0B1120))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFF8FAFC)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Discussions",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }

                    Text(
                        text = "${post.commentsCount} discussions",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Scrollable content: Post preview + List of comments
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Original Post Mini Header
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onProfileClick?.invoke(post.author) }
                                ) {
                                    AsyncImage(
                                        model = post.author.profileImageUrl,
                                        contentDescription = post.author.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = post.author.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFF8FAFC)
                                        )
                                        Text(
                                            text = post.timestamp,
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                if (post.content.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = post.content,
                                        fontSize = 13.sp,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Comments list
                    if (post.comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No discussions yet",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "Be the first to start a discussion!",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    } else {
                        items(post.comments, key = { it.id }) { comment ->
                            CommentItemRow(
                                comment = comment,
                                onLike = { onLikeComment(comment.id) },
                                onReply = { replyingTo = comment.author.name },
                                onMoreOptions = { selectedCommentForActions = comment },
                                onUserClick = { onProfileClick?.invoke(comment.author) }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }

                // Quick Emojis Row
                if (showEmojiPicker) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B1120))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        quickEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clickable {
                                        newCommentText += emoji
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                // Replying to banner if active
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF162137))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Replying to $replyingTo",
                            fontSize = 12.sp,
                            color = Color(0xFF1877F2),
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel reply", tint = Color.White)
                        }
                    }
                }

                // Input bar at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1120))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showEmojiPicker = !showEmojiPicker },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SentimentSatisfied,
                            contentDescription = "Emoji",
                            tint = if (showEmojiPicker) Color(0xFF1877F2) else Color(0xFF94A3B8)
                        )
                    }

                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = {
                            Text(
                                text = if (replyingTo != null) "Reply to $replyingTo..." else "Comment as ${currentUser.name.split(" ").firstOrNull() ?: "you"}...",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                val fullText = if (replyingTo != null) "@$replyingTo $newCommentText" else newCommentText
                                onAddComment(fullText)
                                newCommentText = ""
                                replyingTo = null
                                showEmojiPicker = false
                            }
                        },
                        enabled = newCommentText.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (newCommentText.isNotBlank()) Color(0xFF1877F2) else Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (newCommentText.isNotBlank()) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Comment Action Modal
        selectedCommentForActions?.let { c ->
            CommentActionModal(
                isOpen = true,
                onClose = { selectedCommentForActions = null },
                comment = c,
                currentUserId = currentUser.id,
                isAdmin = currentUser.role in listOf("admin", "moderator"),
                onDelete = { commentId ->
                    onDeleteComment?.invoke(commentId)
                },
                onReply = { comment ->
                    replyingTo = comment.author.name
                }
            )
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: Comment,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onMoreOptions: (() -> Unit)? = null,
    onUserClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = comment.author.profileImageUrl,
            contentDescription = comment.author.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onUserClick?.invoke() }
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Bubble (#162137)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF162137))
                    .clickable { onMoreOptions?.invoke() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onUserClick?.invoke() }
                        ) {
                            Text(
                                text = comment.author.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFF8FAFC)
                            )
                            if (comment.author.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge(size = 13.dp)
                            }
                        }

                        if (onMoreOptions != null) {
                            IconButton(
                                onClick = onMoreOptions,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Options",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = comment.text,
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 18.sp
                    )
                }
            }

            // Sub-bar: Time, Like, Reply, Options
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = comment.timestamp,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = if (comment.isLiked) "❤️ Liked" else "🤍 Like",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (comment.isLiked) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                    modifier = Modifier.clickable(onClick = onLike)
                )

                Text(
                    text = "Reply",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.clickable(onClick = onReply)
                )

                if (onMoreOptions != null) {
                    Text(
                        text = "Options",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.clickable(onClick = onMoreOptions)
                    )
                }

                if (comment.likesCount > 0) {
                    Text(
                        text = "❤️ ${comment.likesCount}",
                        fontSize = 11.sp,
                        color = Color(0xFFF43F5E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
