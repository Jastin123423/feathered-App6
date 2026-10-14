package com.unera.social.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.Comment
import com.unera.social.model.SongComment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Native Jetpack Compose implementation of CommentActionModal.tsx
 * Supports Reply, Copy Text (with clipboard & animated feedback),
 * Hide/Unhide toggle, and Delete with inline confirmation.
 */
@Composable
fun CommentActionModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    comment: Any? = null,
    authorName: String = "",
    authorAvatar: String = "",
    commentText: String = "",
    isHidden: Boolean = false,
    canHide: Boolean = true,
    canDelete: Boolean = true,
    onToggleHide: ((Any?) -> Unit)? = null,
    onDelete: ((Any?) -> Unit)? = null,
    onReply: ((Any?) -> Unit)? = null
) {
    if (!isOpen || (comment == null && authorName.isBlank() && commentText.isBlank())) {
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var copied by remember(isOpen, comment) { mutableStateOf(false) }
    var isConfirmingDelete by remember(isOpen, comment) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Semi-transparent backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent closing when clicking card */ }
                    )
                    .testTag("comment_action_modal"),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color(0xFF0F172A), // Navy 800
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pull / Drag handle indicator
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF334155))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comment preview card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (authorAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = authorAvatar,
                                    contentDescription = authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1877F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = authorName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = authorName.ifBlank { "User" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFF8FAFC)
                                    )

                                    if (isHidden) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                Color(0xFFF59E0B).copy(alpha = 0.4f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = "Hidden",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFF59E0B)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = commentText,
                                    fontSize = 13.sp,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedContent(
                        targetState = isConfirmingDelete,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                        },
                        label = "DeleteConfirmationAnimation"
                    ) { confirming ->
                        if (confirming) {
                            // Delete Confirmation View
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF43F5E).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Text(
                                    text = "Delete comment?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFFF8FAFC)
                                )

                                Text(
                                    text = "Are you sure you want to permanently delete this comment? This action cannot be undone.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { isConfirmingDelete = false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("comment_cancel_delete_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFCBD5E1)
                                        )
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = {
                                            onClose()
                                            onDelete?.invoke(comment)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("comment_confirm_delete_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF43F5E),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Delete", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Primary Action List
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Reply Action
                                if (onReply != null) {
                                    ActionRow(
                                        icon = Icons.AutoMirrored.Filled.Reply,
                                        iconTint = Color(0xFF38BDF8),
                                        title = "Reply to ${authorName.ifBlank { "comment" }}",
                                        subtitle = "Write a response to this comment",
                                        testTag = "comment_action_reply",
                                        onClick = {
                                            onClose()
                                            onReply.invoke(comment)
                                        }
                                    )
                                }

                                // 2. Copy Text Action
                                ActionRow(
                                    icon = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    iconTint = if (copied) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    title = if (copied) "Copied to clipboard!" else "Copy text",
                                    subtitle = if (copied) "Text is ready to paste" else "Copy the comment text",
                                    testTag = "comment_action_copy",
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = ClipData.newPlainText("Comment", commentText)
                                                clipboard?.setPrimaryClip(clip)
                                                copied = true
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    copied = false
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )

                                // 3. Hide / Unhide Action
                                if (canHide && onToggleHide != null) {
                                    ActionRow(
                                        icon = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        iconTint = Color(0xFFF59E0B),
                                        title = if (isHidden) "Unhide comment" else "Hide comment",
                                        subtitle = if (isHidden) "Make visible to everyone" else "Hide from public discussions",
                                        testTag = "comment_action_hide",
                                        onClick = {
                                            onClose()
                                            onToggleHide.invoke(comment)
                                        }
                                    )
                                }

                                // 4. Delete Action
                                if (canDelete && onDelete != null) {
                                    ActionRow(
                                        icon = Icons.Default.Delete,
                                        iconTint = Color(0xFFF43F5E),
                                        title = "Delete comment",
                                        subtitle = "Permanently remove this comment",
                                        testTag = "comment_action_delete",
                                        titleColor = Color(0xFFF43F5E),
                                        onClick = {
                                            isConfirmingDelete = true
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Cancel button
                                Button(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("comment_action_close"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color(0xFF94A3B8)
                                    )
                                ) {
                                    Text(
                                        text = "Cancel",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
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

/**
 * Convenient overload for SongComment
 */
@Composable
fun CommentActionModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    songComment: SongComment?,
    canHide: Boolean = true,
    canDelete: Boolean = true,
    onToggleHide: ((Long) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
    onReply: ((SongComment) -> Unit)? = null
) {
    if (songComment == null) return

    CommentActionModal(
        isOpen = isOpen,
        onClose = onClose,
        comment = songComment,
        authorName = songComment.userName,
        authorAvatar = songComment.userAvatar,
        commentText = songComment.text,
        isHidden = songComment.isHidden,
        canHide = canHide,
        canDelete = canDelete,
        onToggleHide = { onToggleHide?.invoke(songComment.id) },
        onDelete = { onDelete?.invoke(songComment.id) },
        onReply = { onReply?.invoke(songComment) }
    )
}

/**
 * Convenient overload for Post Comment
 */
@Composable
fun CommentActionModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    comment: Comment?,
    currentUserId: Long,
    isAdmin: Boolean = false,
    onToggleHide: ((Long) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
    onReply: ((Comment) -> Unit)? = null
) {
    if (comment == null) return

    val canDelete = isAdmin || comment.author.id == currentUserId

    CommentActionModal(
        isOpen = isOpen,
        onClose = onClose,
        comment = comment,
        authorName = comment.author.name,
        authorAvatar = comment.author.profileImageUrl,
        commentText = comment.text,
        isHidden = false,
        canHide = onToggleHide != null,
        canDelete = canDelete && onDelete != null,
        onToggleHide = { onToggleHide?.invoke(comment.id) },
        onDelete = { onDelete?.invoke(comment.id) },
        onReply = { onReply?.invoke(comment) }
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    titleColor: Color = Color(0xFFF8FAFC),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF141E33).copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
