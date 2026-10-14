package com.unera.social.ui.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.AudioTrack
import com.unera.social.model.SongComment
import com.unera.social.model.User
import java.text.SimpleDateFormat
import java.util.*

private const val DEFAULT_MUSIC_COVER = "https://media.unera.social/task_01kftb3024ed7bm84gy6j485fh_1769336848_img_0.webp"

@Composable
fun MusicCommentsSheet(
    track: AudioTrack?,
    currentUser: User,
    comments: List<SongComment>,
    onClose: () -> Unit,
    onAddComment: (String) -> Unit,
    onToggleHide: (Long) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onProfileClick: (Long) -> Unit
) {
    if (track == null) return

    var inputText by remember { mutableStateOf("") }
    var selectedCommentForActions by remember { mutableStateOf<SongComment?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .clickable(enabled = false) {}
                    .testTag("music_comments_sheet"),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF050B18),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF1E293B))
                        )
                    }

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B1120))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFE4E6EB)
                                )
                            }
                            Text(
                                text = "Discussions",
                                color = Color(0xFFE4E6EB),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = onClose) {
                            Text(
                                text = "Done",
                                color = Color(0xFF1877F2),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Track Preview Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = if (track.cover.isNotBlank()) track.cover else DEFAULT_MUSIC_COVER,
                                contentDescription = track.title,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = Color(0xFFE4E6EB),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Comments List
                    if (comments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No discussions yet",
                                    color = Color(0xFFB0B3B8),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Be the first to start a discussion about this track!",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(comments, key = { it.id }) { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Author avatar
                                    AsyncImage(
                                        model = comment.userAvatar,
                                        contentDescription = comment.userName,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable { onProfileClick(comment.userId) },
                                        contentScale = ContentScale.Crop
                                    )

                                    // Comment bubble
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            color = if (comment.isHidden) Color(0xFF162137).copy(alpha = 0.4f) else Color(0xFF162137).copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (comment.isHidden) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color(0xFF1E293B)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = comment.userName,
                                                            color = Color(0xFFE4E6EB),
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable { onProfileClick(comment.userId) }
                                                        )
                                                        Text(
                                                            text = "• ${formatTimestamp(comment.createdAt)}",
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 11.sp
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (comment.isHidden) {
                                                            Text(
                                                                text = "Hidden",
                                                                color = Color(0xFFFCD34D),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { selectedCommentForActions = comment },
                                                            modifier = Modifier.size(24.dp)
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

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = comment.text,
                                                    color = Color(0xFFF8FAFC),
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        // Actions under comment
                                        Row(
                                            modifier = Modifier.padding(start = 6.dp, top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Like",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable { }
                                            )
                                            Text(
                                                text = "Reply",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable {
                                                    inputText = "@${comment.userName} "
                                                }
                                            )
                                            if (comment.likesCount > 0) {
                                                Text(
                                                    text = "${comment.likesCount} likes",
                                                    color = Color(0xFF64748B),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Comment Input Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0B1120),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Write a comment...", color = Color(0xFF64748B), fontSize = 14.sp) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF070D1D),
                                    unfocusedContainerColor = Color(0xFF070D1D),
                                    focusedBorderColor = Color(0xFF1877F2),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input_field"),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onAddComment(inputText.trim())
                                        inputText = ""
                                    }
                                },
                                enabled = inputText.isNotBlank(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1877F2),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF1E293B),
                                    disabledContentColor = Color(0xFF64748B)
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text("Post", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Comment Options Modal Dialog using CommentActionModal
    selectedCommentForActions?.let { c ->
        val isAuthor = c.userId == currentUser.id
        CommentActionModal(
            isOpen = true,
            onClose = { selectedCommentForActions = null },
            songComment = c,
            canHide = true,
            canDelete = isAuthor || currentUser.role in listOf("admin", "moderator"),
            onToggleHide = { commentId ->
                onToggleHide(commentId)
            },
            onDelete = { commentId ->
                onDeleteComment(commentId)
            },
            onReply = { comment ->
                inputText = "@${comment.userName} "
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val min = diff / (60 * 1000L)
    val hrs = diff / (3600 * 1000L)
    val days = diff / (24 * 3600 * 1000L)

    return when {
        min < 1 -> "Just now"
        min < 60 -> "${min}m"
        hrs < 24 -> "${hrs}h"
        days < 7 -> "${days}d"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
