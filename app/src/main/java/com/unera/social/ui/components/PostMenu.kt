package com.unera.social.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.Post
import com.unera.social.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PostMenu - Exact 1:1 counterpart of web PostMenu.tsx
 *
 * Implements:
 * - Ellipsis horizontal trigger button (fa-ellipsis-h / MoreHoriz)
 * - Contextual menu with align option
 * - Contextual labels (Product, Song, Story, Event, Post)
 * - Edit Post Modal
 * - Full-Page Delete Screen (#delete-post-fullpage-panel) with preview card & animated confirmation
 * - Copy Link with inline 'Link Copied!' green check feedback
 * - Report Post & Share Post integrations
 */
@Composable
fun PostMenu(
    item: Post,
    currentUser: User? = null,
    onShare: ((Post) -> Unit)? = null,
    onDeleteSuccess: ((Long) -> Unit)? = null,
    onEditSuccess: ((Post) -> Unit)? = null,
    onHidePost: ((Long) -> Unit)? = null,
    onReportPost: ((Long, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var open by remember { mutableStateOf(false) }
    var isEditModalOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var copiedLink by remember { mutableStateOf(false) }

    val itemId = item.id
    val currentUserId = currentUser?.id
    val itemOwnerId = item.author.id

    val isOwner = currentUserId != null && (currentUserId == itemOwnerId || currentUserId == 1L && itemOwnerId == 1L)

    val isEvent = item.postType == "event" || item.event != null
    val isSong = item.postType == "music" || item.song != null
    val isProduct = item.postType == "product" || item.product != null
    val isStory = item.postType == "story"

    val canEdit = isOwner && !isSong && !isStory

    val itemTypeName = when {
        isProduct -> "Product"
        isSong -> "Song"
        isStory -> "Story"
        isEvent -> "Event"
        else -> "Post"
    }

    val itemTypeCategory = when {
        isProduct -> "MarketPoint Item"
        isEvent -> "Event"
        isSong -> "Song / Audio"
        else -> "Feed Post"
    }

    val itemTitle = item.product?.title ?: item.event?.title ?: item.song?.title ?: item.sponsoredHeadline

    Box(modifier = modifier) {
        // Three dot button (horizontal ellipsis matching web fa-ellipsis-h)
        IconButton(
            onClick = { open = !open },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .testTag("post_menu_trigger_$itemId")
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Post actions",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }

        // Web dropdown popup
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .width(200.dp)
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
        ) {
            // Edit (Post Owner Only, not allowed for Songs/Stories)
            if (canEdit) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Edit $itemTypeName",
                                color = Color(0xFFF8FAFC),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        open = false
                        isEditModalOpen = true
                    },
                    modifier = Modifier.testTag("menu_edit_post_$itemId")
                )
            }

            // Delete (Post Owner Only)
            if (isOwner) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFB7185), // rose-400
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Delete $itemTypeName",
                                color = Color(0xFFFB7185), // rose-400
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        open = false
                        showDeleteConfirm = true
                    },
                    modifier = Modifier.testTag("menu_delete_$itemId")
                )
            }

            // Divider if owner
            if (isOwner) {
                HorizontalDivider(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Share (Available to everyone)
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Share",
                            color = Color(0xFFF8FAFC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                onClick = {
                    open = false
                    onShare?.invoke(item)
                },
                modifier = Modifier.testTag("menu_share_$itemId")
            )

            // Copy Link (Canonical shareable URL: https://featheredsocial.site/post/{id})
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (copiedLink) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF34D399), // emerald-400
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Link Copied!",
                                color = Color(0xFF34D399),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Copy Link",
                                color = Color(0xFFF8FAFC),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Post Link", "https://featheredsocial.site/post/$itemId")
                    clipboard.setPrimaryClip(clip)
                    copiedLink = true
                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        delay(1200)
                        copiedLink = false
                        open = false
                    }
                },
                modifier = Modifier.testTag("menu_copy_link_$itemId")
            )

            // Report Post (Non-Owner Only)
            if (!isOwner) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24), // amber-400
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Report Post",
                                color = Color(0xFFFBBF24),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        open = false
                        onReportPost?.invoke(itemId, "Inappropriate content")
                        Toast.makeText(context, "Thank you. This report has been submitted to moderators.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.testTag("menu_report_$itemId")
                )
            }
        }
    }

    // ==========================================
    // Full-Page Delete Screen (#delete-post-fullpage-panel)
    // ==========================================
    if (showDeleteConfirm) {
        Dialog(
            onDismissRequest = {
                if (!isDeleting) showDeleteConfirm = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050B18))
                    .testTag("delete-post-fullpage-panel")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Navigation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color(0xFF0A0F1D).copy(alpha = 0.95f))
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Cancel button
                        TextButton(
                            onClick = { if (!isDeleting) showDeleteConfirm = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("delete-post-back-btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Title
                        Text(
                            text = "Delete $itemTypeName",
                            color = Color(0xFFF8FAFC),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Top Delete button
                        Button(
                            onClick = {
                                isDeleting = true
                                coroutineScope.launch {
                                    delay(260)
                                    showDeleteConfirm = false
                                    isDeleting = false
                                    onDeleteSuccess?.invoke(itemId)
                                    Toast.makeText(context, "$itemTypeName deleted successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE11D48), // rose-600
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("delete-post-header-btn")
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deleting...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Scrollable Main Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Trash Icon Badge
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF43F5E).copy(alpha = 0.10f))
                                .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.20f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Headline
                        Text(
                            text = "Permanently Delete this $itemTypeName?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF8FAFC),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = "Are you sure you want to permanently delete this ${itemTypeName.lowercase()}? This action is immediate and cannot be undone. All associated comments, reactions, and media will be wiped.",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Item Preview Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                // Author Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val authorImg = item.author.profileImageUrl.ifBlank {
                                        currentUser?.profileImageUrl ?: "https://ui-avatars.com/api/?name=${item.author.name}&background=1877F2&color=fff"
                                    }
                                    AsyncImage(
                                        model = authorImg,
                                        contentDescription = item.author.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color(0xFF334155), CircleShape)
                                    )

                                    Column {
                                        Text(
                                            text = item.author.name.ifBlank { currentUser?.name ?: "User" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFF8FAFC)
                                        )
                                        Text(
                                            text = itemTypeCategory,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                // Title if present
                                if (!itemTitle.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = itemTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF8FAFC)
                                    )
                                }

                                // Body Content
                                if (item.content.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.content,
                                        fontSize = 13.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 18.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Media Preview
                                val media = item.mediaUrl ?: item.mediaUrls.firstOrNull()
                                if (!media.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = media,
                                            contentDescription = "Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Bottom Actions: Cancel and Keep & Delete Permanently
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                onClick = { if (!isDeleting) showDeleteConfirm = false },
                                enabled = !isDeleting,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Cancel and Keep", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isDeleting = true
                                    coroutineScope.launch {
                                        delay(260)
                                        showDeleteConfirm = false
                                        isDeleting = false
                                        onDeleteSuccess?.invoke(itemId)
                                        Toast.makeText(context, "$itemTypeName deleted permanently", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isDeleting,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE11D48), // rose-600
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("confirm_delete_post_btn")
                            ) {
                                if (isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Deleting...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Permanently", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    // ==========================================
    // Edit Post Modal
    // ==========================================
    if (isEditModalOpen) {
        EditPostModal(
            post = item,
            currentUser = currentUser,
            onClose = { isEditModalOpen = false },
            onSave = { updatedText: String ->
                isEditModalOpen = false
                val updatedPost = item.copy(content = updatedText)
                onEditSuccess?.invoke(updatedPost)
                Toast.makeText(context, "Post updated successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * EditPostModal - Web-fidelity modal to edit post content
 */
@Composable
fun EditPostModal(
    post: Post,
    currentUser: User?,
    onClose: () -> Unit,
    onSave: (String) -> Unit
) {
    var textContent by remember { mutableStateOf(post.content) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = {
            if (!isSaving) onClose()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Edit Post",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )

                        IconButton(
                            onClick = onClose,
                            enabled = !isSaving,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // User Profile Sub-row
                    val user = currentUser ?: post.author
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = user.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFF1E293B), CircleShape)
                        )

                        Column {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFFF8FAFC)
                            )

                            // Privacy pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Public",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Post Content Input
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 240.dp)
                            .testTag("edit_post_text_field"),
                        placeholder = {
                            Text("What's on your mind?", color = Color(0xFF64748B), fontSize = 15.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color(0xFF0B1120),
                            unfocusedContainerColor = Color(0xFF0B1120),
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Media preview (if exists)
                    if (!post.mediaUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = "Attached media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFCBD5E1)
                            )
                        ) {
                            Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                coroutineScope.launch {
                                    delay(300)
                                    isSaving = false
                                    onSave(textContent.trim())
                                }
                            },
                            enabled = !isSaving && textContent.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("save_edit_post_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1877F2),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = Color(0xFF64748B)
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save Changes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
