package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.unera.social.ui.components.VerifiedBadge
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.Story
import com.unera.social.model.User
import com.unera.social.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * StoryViewerDialog
 *
 * Fullscreen story viewer matching the web StoryViewer component in Storypage (824000-845000):
 * - Fixed dark backdrop #050B18
 * - Progress bar segments at the top for each story
 * - Header with author avatar, name, time ago, Follow button, mute toggle, viewers eye button, and close (X)
 * - Media display: text story with gradient background, photo/video with aspect fit/cover
 * - Tap left to go previous, tap right to go next, press & hold to pause
 * - Reaction pill tray (like, love, wow, haha, sad, angry)
 * - Bottom action bar: Heart reaction button with count, Comment/Discussion button with count, Share button with count
 * - Viewers sheet when creator clicks the view counter
 * - Discussions / quick reply input
 */
@Composable
fun StoryViewerDialog(
    story: Story,
    allStories: List<Story>,
    currentUser: User,
    onClose: () -> Unit,
    onNextStory: () -> Unit,
    onPrevStory: () -> Unit,
    onReact: (String) -> Unit,
    onDeleteStory: (() -> Unit)? = null,
    onToggleFollow: ((Long) -> Unit)? = null,
    onProfileClick: ((Long) -> Unit)? = null
) {
    // Current story index among allStories
    val currentIndex = allStories.indexOfFirst { it.id == story.id }.let { if (it >= 0) it else 0 }
    val isOwner = currentUser.id == story.author.id

    var isPaused by remember { mutableStateOf(false) }
    var progress by remember(story.id) { mutableFloatStateOf(0f) }
    var isMuted by remember { mutableStateOf(false) }
    var showReactionsTray by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var showCommentSheet by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var localLiked by remember(story.id, story.myReaction) {
        mutableStateOf(story.myReaction != null || story.isLiked)
    }

    // Auto-advance progress timer (approx 5s for stories, paused if held down)
    LaunchedEffect(story.id, isPaused) {
        if (!isPaused) {
            val stepMs = 50L
            val totalMs = 5000L
            val increment = stepMs.toFloat() / totalMs.toFloat()
            while (progress < 1f) {
                delay(stepMs)
                if (!isPaused) {
                    progress = (progress + increment).coerceAtMost(1f)
                }
            }
            if (progress >= 1f) {
                onNextStory()
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050B18))
                .testTag("story_viewer_dialog")
        ) {
            // Main Story Content Container (max-w 480dp on tablet, full on mobile)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFF0B1120))
                    // Gesture detection: Left tap = prev, Right tap = next, Long press = pause
                    .pointerInput(story.id) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                val width = size.width
                                if (offset.x < width * 0.35f) {
                                    onPrevStory()
                                } else if (offset.x > width * 0.65f) {
                                    onNextStory()
                                } else {
                                    isPaused = !isPaused
                                }
                            }
                        )
                    }
            ) {
                // 1. Story Media Display
                StoryMediaContent(
                    story = story,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Gradient Overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0B1120).copy(alpha = 0.9f),
                                    Color(0xFF0B1120).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom Gradient Overlay for action bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF0B1120).copy(alpha = 0.8f),
                                    Color(0xFF0B1120)
                                )
                            )
                        )
                )

                // 2. Story Progress Bars (one segment per story)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allStories.forEachIndexed { index, s ->
                        val segProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(segProgress)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                // 3. Top Header Bar: Author info, Follow, Mute, Viewers count, Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 26.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Author info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clickable {
                                isPaused = true
                                onProfileClick?.invoke(story.author.id)
                            }
                    ) {
                        AsyncImage(
                            model = story.author.profileImageUrl,
                            contentDescription = story.author.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF1877F2), CircleShape)
                        )

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = story.author.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (story.author.isVerified) {
                                    VerifiedBadge(size = 14.dp)
                                }
                            }
                            Text(
                                text = story.timestamp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }

                        // Follow button if not current user
                        if (!isOwner && onToggleFollow != null) {
                            Button(
                                onClick = { onToggleFollow(story.author.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (story.author.isFollowing) Color(0xFF1E293B) else Color(0xFF1877F2)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (story.author.isFollowing) "Following" else "Follow",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Action buttons (Mute, Viewers, Delete, Close)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Mute button
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Viewers count pill button (opens Viewers sheet)
                        Surface(
                            onClick = {
                                isPaused = true
                                showViewersSheet = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isOwner) Color(0xFF1877F2) else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Viewers",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${story.viewsCount}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Delete button for owner
                        if (isOwner && onDeleteStory != null) {
                            IconButton(
                                onClick = { onDeleteStory() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E).copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Story",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Close button
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("close_story_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 4. Floating Music Pill (if story has music)
                if (!story.musicTitle.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 100.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = story.musicTitle ?: "",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 5. Reactions Tray Popup (Like, Love, Wow, Haha, Sad, Angry)
                AnimatedVisibility(
                    visible = showReactionsTray,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "👍" to "like",
                                "❤️" to "love",
                                "😮" to "wow",
                                "😂" to "haha",
                                "😢" to "sad",
                                "😡" to "angry"
                            ).forEach { (emoji, type) ->
                                Text(
                                    text = emoji,
                                    fontSize = 26.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            onReact(type)
                                            localLiked = true
                                            showReactionsTray = false
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                // 6. Bottom Action Bar: Reactions count, Discussion, Share, Quick comment input
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .navigationBarsPadding()
                ) {
                    // Interaction counts summary row
                    if (story.reactionsCount > 0 || story.commentsCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Reaction emojis & count
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1877F2),
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("❤️", fontSize = 10.sp)
                                    }
                                }
                                Text(
                                    text = "${story.reactionsCount} reactions",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Discussions count
                            if (story.commentsCount > 0) {
                                Text(
                                    text = "${story.commentsCount} discussions",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.clickable {
                                        isPaused = true
                                        showCommentSheet = true
                                    }
                                )
                            }
                        }
                    }

                    // Main Action Row: Quick Reply Field + Reaction + Comment + Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick reply input
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = {
                                Text("Reply to ${story.author.name}…", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            trailingIcon = {
                                if (replyText.isNotBlank()) {
                                    IconButton(onClick = {
                                        replyText = ""
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send Reply",
                                            tint = Color(0xFF1877F2),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )

                        // Heart reaction button (tap = toggle like, long click = open emoji tray)
                        IconButton(
                            onClick = {
                                localLiked = !localLiked
                                onReact("like")
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = if (localLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "React",
                                tint = if (localLiked) Color(0xFFF43F5E) else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Open emoji tray button
                        IconButton(
                            onClick = { showReactionsTray = !showReactionsTray },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Text("😍", fontSize = 18.sp)
                        }

                        // Comment sheet opener
                        IconButton(
                            onClick = {
                                isPaused = true
                                showCommentSheet = true
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Discussions",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 7. Story Viewers Modal Sheet
            if (showViewersSheet) {
                StoryViewersModal(
                    viewers = story.viewers,
                    totalViews = story.viewsCount,
                    onDismiss = {
                        showViewersSheet = false
                        isPaused = false
                    },
                    onProfileClick = onProfileClick
                )
            }

            // 8. Story Comments / Discussion Modal Sheet
            if (showCommentSheet) {
                StoryDiscussionsModal(
                    story = story,
                    currentUser = currentUser,
                    onDismiss = {
                        showCommentSheet = false
                        isPaused = false
                    }
                )
            }
        }
    }
}

/**
 * Story Media View: Handles Text stories (with vibrant background) or Photo/Video stories
 */
@Composable
private fun StoryMediaContent(
    story: Story,
    modifier: Modifier = Modifier
) {
    if (story.type == "text" || story.mediaUrl.isBlank()) {
        // Text story with colored gradient
        val brush = remember(story.backgroundStyle) {
            when {
                story.backgroundStyle.contains("#F3425F") -> Brush.linearGradient(listOf(Color(0xFFF3425F), Color(0xFFE41E3F)))
                story.backgroundStyle.contains("#45BD62") -> Brush.linearGradient(listOf(Color(0xFF45BD62), Color(0xFF31A24C)))
                story.backgroundStyle.contains("#F7B928") -> Brush.linearGradient(listOf(Color(0xFFF7B928), Color(0xFFE3A300)))
                story.backgroundStyle.contains("#A033FF") -> Brush.linearGradient(listOf(Color(0xFFA033FF), Color(0xFF7B1FA2)))
                else -> Brush.linearGradient(listOf(Color(0xFF1877F2), Color(0xFF0055FF)))
            }
        }

        Box(
            modifier = modifier
                .background(brush)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = story.textContent.ifBlank { "Sharing moments on Unera ✨" },
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
        }
    } else {
        // Image or Video story
        AsyncImage(
            model = story.mediaUrl,
            contentDescription = "Story Media",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

/**
 * Story Viewers Modal Sheet
 * Matches web Viewers Dialog in StoryViewer.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewersModal(
    viewers: List<User>,
    totalViews: Int,
    onDismiss: () -> Unit,
    onProfileClick: ((Long) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF334155)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Story Viewers ($totalViews)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (viewers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No viewers yet. Friends will appear here when they view your story.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(viewers, key = { it.id }) { viewer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onDismiss()
                                    onProfileClick?.invoke(viewer.id)
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = viewer.profileImageUrl,
                                contentDescription = viewer.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = viewer.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "@${viewer.username}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "Viewed",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Story Discussions & Comments Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDiscussionsModal(
    story: Story,
    currentUser: User,
    onDismiss: () -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    var commentsList by remember {
        mutableStateOf(
            listOf(
                Triple("Amina Mwangi", "This looks incredible! Where was this taken? 🔥", "10m ago"),
                Triple("Sam Adebayo", "Keep soaring brother, inspired by this! 🚀", "35m ago")
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF334155)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Discussions (${commentsList.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                items(commentsList) { (author, text, time) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1877F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = author.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = author, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = time, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = text, fontSize = 13.sp, color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add comment input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Write a comment…", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFF1877F2),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            commentsList = commentsList + Triple(currentUser.name, newCommentText, "Just now")
                            newCommentText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * CreateStoryDialog
 * Matches web CreateStoryModal (Text story, photo/video media selection, music picker, gradients)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onCreateStory: (mediaUrl: String, type: String, textContent: String, backgroundStyle: String, musicTitle: String?) -> Unit
) {
    var mode by remember { mutableStateOf("text") } // "text" or "media"
    var textContent by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var selectedMusic by remember { mutableStateOf<String?>("Asake - Lonely At The Top") }
    var selectedGradientIndex by remember { mutableIntStateOf(0) }

    val gradients = listOf(
        "linear-gradient(45deg, #1877F2, #0055FF)" to listOf(Color(0xFF1877F2), Color(0xFF0055FF)),
        "linear-gradient(45deg, #F3425F, #E41E3F)" to listOf(Color(0xFFF3425F), Color(0xFFE41E3F)),
        "linear-gradient(45deg, #45BD62, #31A24C)" to listOf(Color(0xFF45BD62), Color(0xFF31A24C)),
        "linear-gradient(45deg, #F7B928, #E3A300)" to listOf(Color(0xFFF7B928), Color(0xFFE3A300)),
        "linear-gradient(45deg, #A033FF, #7B1FA2)" to listOf(Color(0xFFA033FF), Color(0xFF7B1FA2))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0B1120)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }

                    // Mode Switcher Pills (Text vs Media)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("text" to "Text Story", "media" to "Photo/Video").forEach { (m, label) ->
                            val active = mode == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (active) Color(0xFF1877F2) else Color.Transparent)
                                    .clickable { mode = m }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Share button
                    Button(
                        onClick = {
                            if (mode == "text" && textContent.isNotBlank()) {
                                onCreateStory(
                                    "",
                                    "text",
                                    textContent,
                                    gradients[selectedGradientIndex].first,
                                    selectedMusic
                                )
                            } else if (mode == "media" && mediaUrl.isNotBlank()) {
                                onCreateStory(
                                    mediaUrl,
                                    "image",
                                    "",
                                    "",
                                    selectedMusic
                                )
                            }
                        },
                        enabled = (mode == "text" && textContent.isNotBlank()) || (mode == "media" && mediaUrl.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Story Canvas Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mode == "text") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(gradients[selectedGradientIndex].second))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedTextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                placeholder = {
                                    Text(
                                        "Start typing your story…",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // Media mode
                        if (mediaUrl.isNotBlank()) {
                            AsyncImage(
                                model = mediaUrl,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0F172A))
                                    .padding(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Enter an image or video URL",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = mediaUrl,
                                    onValueChange = { mediaUrl = it },
                                    placeholder = { Text("https://example.com/photo.jpg", color = Color(0xFF64748B), fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF1E293B),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF1877F2),
                                        unfocusedBorderColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Controls: Gradient Selectors for Text stories, Music selection pill
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (mode == "text") {
                        // Color gradient selectors
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(gradients.indices.toList()) { idx ->
                                val (raw, colors) = gradients[idx]
                                val selected = selectedGradientIndex == idx
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(colors))
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedGradientIndex = idx }
                                )
                            }
                        }
                    }

                    // Music selector bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = selectedMusic ?: "No Music",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Story Soundtrack",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                selectedMusic = if (selectedMusic == null) "Asake - Lonely At The Top" else null
                            }
                        ) {
                            Text(
                                text = if (selectedMusic != null) "Remove" else "Add Song",
                                color = if (selectedMusic != null) Color(0xFFF43F5E) else Color(0xFF1877F2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
