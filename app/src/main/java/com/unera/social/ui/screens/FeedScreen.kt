package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.data.MockData
import com.unera.social.data.PostsApiService
import com.unera.social.model.*
import com.unera.social.ui.components.*


@Composable
fun FeedScreen(
    currentUser: User,
    stories: List<Story>,
    posts: List<Post>,
    onOpenCreatePost: () -> Unit,
    onPhotoClick: () -> Unit = onOpenCreatePost,
    onVideoClick: () -> Unit = onOpenCreatePost,
    onStoryClick: (Story) -> Unit,
    onLikePost: (Long) -> Unit,
    onCommentPost: (Post) -> Unit,
    onAddComment: ((Long, String) -> Unit)? = null,
    onSavePost: (Long) -> Unit,
    onUserClick: (User) -> Unit,
    onAddStoryClick: () -> Unit = onOpenCreatePost,
    allUsers: List<User> = emptyList(),
    peopleYouMayKnow: List<User> = emptyList(),
    onFollowFromPymk: ((Long) -> Unit)? = null,
    onFollowUser: ((Long) -> Unit)? = null,
    onDismissPymk: ((Long) -> Unit)? = null,
    pymkLoading: Boolean = false,
    groups: List<Group> = emptyList(),
    isFeedLoading: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onReactPost: ((Long, String) -> Unit)? = null,
    onSharePost: ((Long) -> Unit)? = null,
    onOpenProduct: ((Product) -> Unit)? = null,
    onOpenEvent: ((Event) -> Unit)? = null,
    onOpenGroup: ((Long) -> Unit)? = null,
    onPlayMusic: ((Song) -> Unit)? = null,
    onOpenVideos: (() -> Unit)? = null,
    onDeletePost: ((Long) -> Unit)? = null,
    onEditPost: ((Long, String) -> Unit)? = null,
    onHidePost: ((Long) -> Unit)? = null,
    onReportPost: ((Long, String) -> Unit)? = null,
    uploadState: PostUploadState? = null,
    onDismissUpload: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var galleryState by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var activePostForGallery by remember { mutableStateOf<Post?>(null) }
    var reactionsSheetPost by remember { mutableStateOf<Post?>(null) }
    var shareSheetPost by remember { mutableStateOf<Post?>(null) }
    var discussionsSheetPost by remember { mutableStateOf<Post?>(null) }
    var followStates by remember { mutableStateOf(mapOf<Long, Boolean>()) }
    var groupJoinedStates by remember { mutableStateOf(mapOf<Long, Boolean>()) }

    val listState = rememberRestoredLazyListState(key = "FEED")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B18))
            .testTag("feed_screen_list")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Stories Section
            item(key = "stories_section") {
                StoriesSection(
                    currentUser = currentUser,
                    stories = stories,
                    onStoryClick = onStoryClick,
                    onAddStoryClick = onAddStoryClick
                )
            }

            // 2. Create Post Bar (Web Style)
            item(key = "create_post_card") {
                CreatePostSection(
                    currentUser = currentUser,
                    onClick = onOpenCreatePost,
                    onPhotoClick = onPhotoClick,
                    onVideoClick = onVideoClick,
                    onEventClick = onOpenCreatePost
                )
                // 8dp gap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFF050B18))
                )
            }

            // 2b. Post Upload Progress Banner (matching Facebook mobile upload interface)
            if (uploadState != null) {
                item(key = "post_upload_progress_banner") {
                    PostUploadProgressBanner(
                        uploadState = uploadState,
                        onDismiss = onDismissUpload
                    )
                }
            }

            // 3. Posts with Interleaved Widgets
            itemsIndexed(posts, key = { _, post -> "post_${post.id}" }) { index, post ->
                val isGroupPost = post.groupId != null || post.postType == "group_post"
                val isMarketplace = post.product != null || post.postType == "product" || post.postType == "marketplace"
                val isSharedPost = post.sharedPost != null || post.postType == "shared_post" ||
                        post.postType == "product_share" || post.postType == "event_share" ||
                        post.postType == "song_share" || post.postType == "story_share"

                // Exact rule matching web Feeds.tsx line 2235:
                // Standalone video card: !isGroupPost && !isMarketplace && !isSharedPost && (media_type === 'video' || type === 'video' || post_type === 'video' || post_type === 'reel')
                val isStandaloneVideo = !isGroupPost && !isMarketplace && !isSharedPost && (
                    post.postType == "video" || post.postType == "reel" ||
                    (post.mediaType == "video" && PostsApiService.isValidVideoUrl(post.videoUrl)) ||
                    (PostsApiService.isValidVideoUrl(post.videoUrl) && post.mediaUrls.isEmpty()) ||
                    (post.mediaMeta.firstOrNull()?.type == "video" && PostsApiService.isValidVideoUrl(post.mediaMeta.firstOrNull()?.resolution720 ?: post.mediaMeta.firstOrNull()?.original))
                )

                if (post.isSponsored || post.postType == "sponsored") {
                    SponsoredPostCard(
                        post = post,
                        currentUser = currentUser,
                        onProfileClick = { onUserClick(post.author) },
                        onAnalyticsClick = { /* Handled in Ads Manager */ },
                        onOpenComments = { discussionsSheetPost = post },
                        onReact = { _, reaction ->
                            onReactPost?.invoke(post.id, reaction) ?: onLikePost(post.id)
                        },
                        onShare = { shareSheetPost = post }
                    )
                } else if (isStandaloneVideo) {
                    InstagramVideoCard(
                        post = post,
                        currentUser = currentUser,
                        stories = stories,
                        isFollowing = followStates[post.author.id] ?: post.author.isFollowing,
                        onFollow = { authorId ->
                            val current = followStates[authorId] ?: post.author.isFollowing
                            followStates = followStates + (authorId to !current)
                        },
                        onProfileClick = { onUserClick(post.author) },
                        onStoryClick = { userId ->
                            val story = stories.find { s -> s.author.id == userId }
                            if (story != null) onStoryClick(story) else onUserClick(post.author)
                        },
                        onReact = { _, reaction ->
                            onReactPost?.invoke(post.id, reaction) ?: onLikePost(post.id)
                        },
                        onOpenComments = { discussionsSheetPost = post },
                        onOpenReactions = { reactionsSheetPost = post },
                        onShareClick = { shareSheetPost = post },
                        onVideoClick = { onOpenVideos?.invoke() },
                        onDelete = { onDeletePost?.invoke(it) },
                        onEdit = { id, text -> onEditPost?.invoke(id, text) }
                    )
                } else {
                    WebPostCard(
                        post = post,
                        currentUser = currentUser,
                        isFollowing = followStates[post.author.id] ?: post.author.isFollowing,
                        onFollowToggle = {
                            val current = followStates[post.author.id] ?: post.author.isFollowing
                            followStates = followStates + (post.author.id to !current)
                            onFollowUser?.invoke(post.author.id)
                        },
                        onLike = { onLikePost(post.id) },
                        onReact = { reaction ->
                            onReactPost?.invoke(post.id, reaction) ?: onLikePost(post.id)
                        },
                        onComment = {
                            discussionsSheetPost = post
                            onCommentPost(post)
                        },
                        onShare = { shareSheetPost = post },
                        onSave = { onSavePost(post.id) },
                        onUserClick = { onUserClick(post.author) },
                        onOpenGallery = { imgs, idx ->
                            activePostForGallery = post
                            galleryState = Pair(imgs, idx)
                        },
                        onOpenReactions = { reactionsSheetPost = post },
                        onOpenProduct = { onOpenProduct?.invoke(it) },
                        onOpenEvent = { onOpenEvent?.invoke(it) },
                        onOpenGroup = { onOpenGroup?.invoke(it) },
                        onPlayMusic = { onPlayMusic?.invoke(it) },
                        onStoryClick = { onStoryClick(it) },
                        onDeletePost = { onDeletePost?.invoke(it) },
                        onEditPost = { id, text -> onEditPost?.invoke(id, text) },
                        onHidePost = { onHidePost?.invoke(it) },
                        onReportPost = { id, reason -> onReportPost?.invoke(id, reason) }
                    )
                }

                // 8dp gap between posts
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFF050B18))
                )

                // Interleaved Widget 1: People You May Know
                val effectivePymk = if (peopleYouMayKnow.isNotEmpty()) peopleYouMayKnow else allUsers.filter { it.id != currentUser.id }
                val pymkInsertIndex1 = when {
                    posts.size < 6 -> if (posts.isNotEmpty()) (posts.size - 1).coerceAtMost(1) else -1
                    posts.size <= 10 -> posts.size - 1
                    else -> 7
                }
                val pymkInsertIndex2 = if (posts.size >= 22) 21 else -1

                if ((index == pymkInsertIndex1 || index == pymkInsertIndex2) && effectivePymk.isNotEmpty()) {
                    PeopleYouMayKnowSection(
                        currentUser = currentUser,
                        allUsers = effectivePymk,
                        followStates = followStates,
                        onFollowToggle = { uid ->
                            val current = followStates[uid] ?: false
                            followStates = followStates + (uid to !current)
                            onFollowFromPymk?.invoke(uid)
                        },
                        onDismiss = { uid ->
                            onDismissPymk?.invoke(uid)
                        },
                        onUserClick = onUserClick
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFF050B18))
                    )
                }

                // Interleaved Widget 2: Groups You May Join after 4th post
                if (index == 3 && groups.isNotEmpty()) {
                    GroupsYouMayJoinSection(
                        groups = groups,
                        joinedStates = groupJoinedStates,
                        onJoinToggle = { gid ->
                            val current = groupJoinedStates[gid] ?: false
                            groupJoinedStates = groupJoinedStates + (gid to !current)
                        },
                        onGroupClick = { gid ->
                            onOpenGroup?.invoke(gid)
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFF050B18))
                    )
                }
            }

            // Empty or loading state for feed
            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFeedLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF1877F2),
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "No posts available",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                onRefresh?.let { doRefresh ->
                                    Button(
                                        onClick = doRefresh,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Refresh Feed", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gallery Viewer Modal
        galleryState?.let { (imgs, idx) ->
            activePostForGallery?.let { currentPost ->
                GalleryViewerDialog(
                    images = imgs,
                    initialIndex = idx,
                    post = currentPost,
                    onDismiss = {
                        galleryState = null
                        activePostForGallery = null
                    },
                    onLike = { onLikePost(currentPost.id) },
                    onComment = {
                        galleryState = null
                        discussionsSheetPost = currentPost
                    },
                    onShare = {
                        galleryState = null
                        shareSheetPost = currentPost
                    },
                    onSave = { onSavePost(currentPost.id) }
                )
            }
        }

        // Reactions Sheet Modal
        reactionsSheetPost?.let { currentPost ->
            ReactionsSheetDialog(
                post = currentPost,
                allUsers = allUsers,
                onDismiss = { reactionsSheetPost = null },
                onUserClick = {
                    reactionsSheetPost = null
                    onUserClick(it)
                }
            )
        }

        // Share Sheet Modal
        shareSheetPost?.let { currentPost ->
            ShareBottomSheetDialog(
                post = currentPost,
                onDismiss = { shareSheetPost = null },
                onShareToFeed = {
                    onSharePost?.invoke(currentPost.id)
                    shareSheetPost = null
                },
                onShareToGroup = {
                    onSharePost?.invoke(currentPost.id)
                    shareSheetPost = null
                },
                onSendMessage = {
                    onSharePost?.invoke(currentPost.id)
                    shareSheetPost = null
                },
                onCopyLink = {
                    onSharePost?.invoke(currentPost.id)
                    shareSheetPost = null
                }
            )
        }

        // Discussions & Comments Sheet Modal
        discussionsSheetPost?.let { curPost ->
            val livePost = posts.find { it.id == curPost.id } ?: curPost
            DiscussionsSheetDialog(
                post = livePost,
                currentUser = currentUser,
                onDismiss = { discussionsSheetPost = null },
                onAddComment = { commentText ->
                    onAddComment?.invoke(livePost.id, commentText)
                },
                onLikeComment = { /* Liked */ },
                onProfileClick = { author ->
                    discussionsSheetPost = null
                    onUserClick(author)
                }
            )
        }

        // Floating bottom progress pill shown when scrolling through timeline
        if (uploadState != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                PostUploadBottomPill(
                    uploadState = uploadState,
                    onDismiss = onDismissUpload
                )
            }
        }
    }
}

/**
 * Top Create Post Section matching web CreatePost
 */
@Composable
fun CreatePostSection(
    currentUser: User,
    onClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onVideoClick: () -> Unit,
    onEventClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("create_post_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = currentUser.profileImageUrl,
                contentDescription = currentUser.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF1E293B), CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "What's on your mind?",
                    fontSize = 15.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Green Photo quick button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPhotoClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Photo",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Photo",
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.6f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Action Bar: Live, Photo, Video, Event
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Video
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Videocam, contentDescription = "Live", tint = Color(0xFF1877F2), modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Live", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Photo
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPhotoClick)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Photo", tint = Color(0xFF10B981), modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Photo", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Video
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onVideoClick)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Video", tint = Color(0xFFF43F5E), modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Video", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Event
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEventClick)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Event, contentDescription = "Event", tint = Color(0xFFF59E0B), modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Event", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Stories Row matching web Stories
 */
@Composable
fun StoriesSection(
    currentUser: User,
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(vertical = 12.dp)
            .testTag("stories_section"),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Create Story Card (112x190dp)
        item(key = "create_story_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .width(112.dp)
                    .height(190.dp)
                    .clickable(onClick = onAddStoryClick)
                    .testTag("add_story_btn")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = currentUser.profileImageUrl,
                        contentDescription = "Create Story",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.72f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.28f)
                            .align(Alignment.BottomCenter)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "Create Story",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    // Floating blue plus icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 36.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF166FE5), Color(0xFF1877F2))
                                )
                            )
                            .border(3.dp, Color(0xFF0F172A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Story",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Story Cards
        items(stories, key = { it.id }) { story ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .width(112.dp)
                    .height(190.dp)
                    .clickable { onStoryClick(story) }
                    .testTag("story_item_${story.id}")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (story.type == "text" || story.mediaUrl.isBlank()) {
                        val gradientColors = remember(story.backgroundStyle) {
                            when {
                                story.backgroundStyle.contains("#F3425F") -> listOf(Color(0xFFF3425F), Color(0xFFE41E3F))
                                story.backgroundStyle.contains("#45BD62") -> listOf(Color(0xFF45BD62), Color(0xFF31A24C))
                                story.backgroundStyle.contains("#F7B928") -> listOf(Color(0xFFF7B928), Color(0xFFE3A300))
                                story.backgroundStyle.contains("#A033FF") -> listOf(Color(0xFFA033FF), Color(0xFF7B1FA2))
                                else -> listOf(Color(0xFF1877F2), Color(0xFF0055FF))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(gradientColors))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = story.textContent.ifBlank { "Story" },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 4,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        AsyncImage(
                            model = story.mediaUrl,
                            contentDescription = story.author.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Dark vignette overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )

                    // Top-left author avatar with blue ring
                    AsyncImage(
                        model = story.author.profileImageUrl,
                        contentDescription = story.author.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF1877F2), CircleShape)
                    )

                    // Bottom-left author name
                    Text(
                        text = story.author.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Main Web Post Card Component matching web Post
 */
@Composable
fun WebPostCard(
    post: Post,
    currentUser: User,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onLike: () -> Unit,
    onReact: (String) -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onUserClick: () -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit,
    onOpenReactions: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onOpenEvent: (Event) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onPlayMusic: (Song) -> Unit,
    onStoryClick: ((Story) -> Unit)? = null,
    onDeletePost: (Long) -> Unit = {},
    onEditPost: (Long, String) -> Unit = { _, _ -> },
    onHidePost: (Long) -> Unit = {},
    onReportPost: ((Long, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReactionDock by remember { mutableStateOf(false) }
    var isExpandedText by remember { mutableStateOf(false) }

    // Media list (single or multiple)
    val mediaList = remember(post.mediaUrl, post.mediaUrls) {
        if (post.mediaUrls.isNotEmpty()) post.mediaUrls
        else if (!post.mediaUrl.isNullOrBlank()) listOf(post.mediaUrl)
        else emptyList()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Author / Group / Ad info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with group sub-icon or regular
                if (post.groupId != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { post.groupId?.let { onOpenGroup(it) } }
                    ) {
                        if (!post.groupImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = post.groupImageUrl,
                                contentDescription = post.groupName ?: "Group",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .align(Alignment.TopStart)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFF1E293B), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .align(Alignment.TopStart)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1877F2).copy(alpha = 0.25f))
                                    .border(1.dp, Color(0xFF1E293B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Group",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AsyncImage(
                            model = post.author.profileImageUrl,
                            contentDescription = post.author.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF0F172A), CircleShape)
                                .clickable { onUserClick() }
                        )
                    }
                } else {
                    AsyncImage(
                        model = post.author.profileImageUrl,
                        contentDescription = post.author.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFF1E293B), CircleShape)
                            .clickable(onClick = onUserClick)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (post.groupId != null) {
                            Text(
                                text = post.groupName ?: "Group",
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier.clickable { post.groupId?.let { onOpenGroup(it) } }
                            )
                        } else {
                            Text(
                                text = post.author.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier.clickable(onClick = onUserClick)
                            )
                        }

                        if (post.author.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            VerifiedBadge(size = 21.dp)
                        }

                        if (post.isSponsored) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Sponsored", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Sub-line: author / timestamp • globe • feeling / group / etc.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val displayTime = PostsApiService.formatRelativeTime(post.timestamp)
                        if (post.groupId != null) {
                            Text(
                                text = post.author.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.clickable(onClick = onUserClick)
                            )
                            Text(
                                text = " • $displayTime • ",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Group",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { post.groupId?.let { onOpenGroup(it) } }
                            )
                        } else {
                            Text(
                                text = displayTime,
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text("•", fontSize = 13.sp, color = Color(0xFF94A3B8))
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Public",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        if (!post.feeling.isNullOrBlank()) {
                            Text("•", fontSize = 13.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "feeling ${post.feeling}",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        if (!post.location.isNullOrBlank()) {
                            Text("•", fontSize = 13.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = post.location,
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Follow button if not current user
                if (post.author.id != currentUser.id && !post.isSponsored) {
                    FilledTonalButton(
                        onClick = onFollowToggle,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isFollowing) Color(0xFF1E293B) else Color(0xFF1877F2)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (isFollowing) "Following" else "+ Follow",
                            color = if (isFollowing) Color(0xFFCBD5E1) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // 3-dots PostMenu component matching web
                PostMenu(
                    item = post,
                    currentUser = currentUser,
                    onShare = { onShare() },
                    onDeleteSuccess = { onDeletePost(post.id) },
                    onEditSuccess = { onEditPost(post.id, it.content) },
                    onHidePost = { onHidePost(post.id) },
                    onReportPost = { id, reason -> onReportPost?.invoke(id, reason) }
                )
            }

            // Sponsored Headline
            if (post.isSponsored && post.sponsoredHeadline.isNotBlank()) {
                Text(
                    text = post.sponsoredHeadline,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
            }

            // Post Text Content
            // Post Text Content (avoid duplicating event/product/song titles that are already inside their respective cards)
            val isDuplicateText = (post.event != null && (post.content.equals(post.event.title, ignoreCase = true) || post.content.equals(post.event.description, ignoreCase = true))) ||
                    (post.product != null && (post.content.equals(post.product.title, ignoreCase = true) || post.content.equals(post.product.description, ignoreCase = true))) ||
                    (post.song != null && post.content.contains(post.song.title, ignoreCase = true))

            if (!isDuplicateText && post.content.isNotBlank() && post.backgroundStyle.isNullOrBlank()) {
                val words = remember(post.content) {
                    post.content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                }
                val hasMoreThan8Words = words.size > 8
                val truncatedContent = remember(post.content, words) {
                    if (words.size > 8) words.take(8).joinToString(" ") + "…" else post.content
                }

                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(
                        text = if (!hasMoreThan8Words || isExpandedText) post.content else truncatedContent,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        color = Color(0xFFE4E6EB)
                    )
                    if (hasMoreThan8Words) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isExpandedText = !isExpandedText }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = if (isExpandedText) "See Less" else "See More",
                                color = Color(0xFF1877F2),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpandedText) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else if (!post.backgroundStyle.isNullOrBlank() && post.content.isNotBlank()) {
                // Colored full bleed text post
                val gradientColors = remember(post.backgroundStyle) {
                    when {
                        post.backgroundStyle.contains("#FF0057") -> listOf(Color(0xFFFF0057), Color(0xFFE64C4C))
                        post.backgroundStyle.contains("#a8ff78") -> listOf(Color(0xFF00B4D8), Color(0xFF0077B6))
                        post.backgroundStyle.contains("#e65c00") -> listOf(Color(0xFFE65C00), Color(0xFFF9D423))
                        else -> listOf(Color(0xFF1877F2), Color(0xFF0055FF))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .background(Brush.linearGradient(gradientColors))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.content,
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )
                }
            }

            // Special Post: Job Posting
            if (post.jobTitle != null) {
                JobPostingCard(
                    jobTitle = post.jobTitle,
                    company = post.company ?: "",
                    location = post.location ?: "Tanzania",
                    salary = post.salary ?: "",
                    jobType = post.jobType ?: "Full-time",
                    onApply = { /* Apply */ }
                )
            }

            // Special Post: Product / MarketPoint (standalone)
            if (post.product != null && post.sharedPost == null) {
                ProductPostCard(
                    product = post.product,
                    onViewProduct = { onOpenProduct(post.product) }
                )
            }

            // Special Post: Event Post (standalone)
            if (post.event != null && post.sharedPost == null) {
                EventPostCard(
                    event = post.event,
                    onEventClick = { onOpenEvent(post.event) }
                )
            }

            // Special Post: Music (standalone)
            if (post.song != null && post.sharedPost == null) {
                MusicPostCard(
                    song = post.song,
                    onPlay = { onPlayMusic(post.song) }
                )
            }

            // Special Post: Story Post
            if (post.story != null || post.postType == "story") {
                val storyItem = post.story ?: Story(
                    id = post.id,
                    author = post.author,
                    mediaUrl = post.mediaUrl ?: (post.mediaUrls.firstOrNull() ?: ""),
                    type = if (post.mediaUrl?.endsWith(".mp4", ignoreCase = true) == true) "video" else if (post.mediaUrl.isNullOrBlank()) "text" else "image",
                    textContent = post.content,
                    backgroundStyle = post.backgroundStyle ?: "",
                    timestamp = post.timestamp
                )
                FeedStoryCard(
                    story = storyItem,
                    post = post,
                    onStoryClick = { onStoryClick?.invoke(storyItem) }
                )
            }

            // Special Post: Shared Post
            if (post.sharedPost != null) {
                val isSharedVideo = post.sharedPost.postType == "video" ||
                        post.sharedPost.postType == "reel" ||
                        post.sharedPost.mediaUrl?.endsWith(".mp4", ignoreCase = true) == true ||
                        post.sharedPost.mediaUrl?.contains("video", ignoreCase = true) == true
                if (isSharedVideo) {
                    InstagramVideoShareCard(
                        post = post,
                        originalPost = post.sharedPost,
                        ownerAuthor = post.sharedPost.author,
                        currentUser = currentUser,
                        onProfileClick = { _ -> onUserClick() },
                        onVideoClick = { _ ->
                            val url = post.sharedPost.mediaUrl
                            if (url != null) onOpenGallery(listOf(url), 0)
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                } else {
                    SharedPostNestedCard(
                        sharedPost = post.sharedPost,
                        onOpenGallery = onOpenGallery,
                        onOpenProduct = onOpenProduct,
                        onOpenEvent = onOpenEvent,
                        onPlayMusic = onPlayMusic,
                        onStoryClick = onStoryClick,
                        onOpenGroup = onOpenGroup
                    )
                }
            }

            // Attached Video or Regular Media Grid (1, 2, 3, 4, 5, 6+ images)
            val isPostVideo = PostsApiService.isValidVideoUrl(post.videoUrl) ||
                    (mediaList.size == 1 && PostsApiService.isValidVideoUrl(mediaList[0])) ||
                    (post.mediaType == "video" && PostsApiService.isValidVideoUrl(post.mediaUrl)) ||
                    (post.mediaMeta.firstOrNull()?.type == "video" && PostsApiService.isValidVideoUrl(post.mediaMeta.firstOrNull()?.resolution720 ?: post.mediaMeta.firstOrNull()?.original))

            if (post.product == null && post.event == null && post.sharedPost == null && (post.story == null && post.postType != "story")) {
                if (isPostVideo) {
                    val videoUrl = post.videoUrl ?: mediaList.firstOrNull() ?: ""
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 460.dp)
                            .background(Color.Black)
                            .clickable { onOpenGallery(listOf(videoUrl), 0) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!post.mediaUrl.isNullOrBlank() && !post.mediaUrl.endsWith(".mp4", true) && !post.mediaUrl.endsWith(".webm", true)) {
                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = "Video Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                } else if (mediaList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    MediaGrid(
                        images = mediaList,
                        onImageClick = { idx -> onOpenGallery(mediaList, idx) }
                    )
                }
            }

            // Sponsored CTA Button
            if (post.isSponsored && post.sponsoredCtaText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = { /* Open Ad URL */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                    ) {
                        Text(
                            text = post.sponsoredCtaText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Engagement Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reactions overlap icons & text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onOpenReactions)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                        Text("👍", fontSize = 14.sp)
                        Text("❤️", fontSize = 14.sp)
                        Text("🔥", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount} Reactions",
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Discussions & Shares
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${post.commentsCount} Discussions",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onComment)
                    )
                    Text("•", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text(
                        text = "${post.sharesCount} Shares",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onShare)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Reaction Dock (on long press / dock active)
            AnimatedVisibility(visible = showReactionDock) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0B1120))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    REACTION_EMOJIS.forEach { (type, emoji) ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onReact(type)
                                    showReactionDock = false
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Action Buttons Row: React, Discuss, Share, Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // React Button
                    TextButton(
                        onClick = {
                            if (showReactionDock) showReactionDock = false
                            else onLike()
                        },
                        modifier = Modifier.testTag("like_post_btn_${post.id}")
                    ) {
                        val reactionEmoji = post.myReaction?.let { getReactionEmoji(it) }
                        if (reactionEmoji != null) {
                            Text(text = reactionEmoji, fontSize = 18.sp)
                        } else {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (post.isLiked) Color(0xFFF43F5E) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.myReaction?.replaceFirstChar { it.uppercase() } ?: if (post.isLiked) "Liked" else "React",
                            color = if (post.isLiked || post.myReaction != null) Color(0xFFF43F5E) else Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Discuss Button
                    TextButton(
                        onClick = onComment,
                        modifier = Modifier.testTag("comment_post_btn_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Discuss",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Discuss",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Share Button
                    TextButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_post_btn_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Share",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bookmark / Save
                SavePostButton(
                    isSaved = post.isSaved,
                    onToggle = onSave,
                    size = 22.dp,
                    testTag = "save_post_btn_${post.id}"
                )
            }

            // Inline Single Comment Preview (Matching Facebook / Web style)
            if (post.comments.isNotEmpty()) {
                val previewComment = post.comments.first()
                HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.5f), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(onClick = onComment),
                    verticalAlignment = Alignment.Top
                ) {
                    AsyncImage(
                        model = previewComment.author.profileImageUrl,
                        contentDescription = previewComment.author.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF162137))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = previewComment.author.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFF8FAFC)
                            )
                            if (previewComment.author.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge(size = 12.dp)
                            }
                        }

                        Text(
                            text = previewComment.text,
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Media Grid Component (1, 2, 3, 4, 5, 6+ images)
 */
@Composable
fun MediaGrid(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    com.unera.social.ui.components.MediaGrid(
        images = images,
        onImageClick = onImageClick
    )
}

/**
 * Special Post: Event Post Card
 */
@Composable
fun EventPostCard(
    event: Event,
    onEventClick: () -> Unit
) {
    var isGoing by remember { mutableStateOf(event.isGoing) }
    var isInterested by remember { mutableStateOf(event.isInterested) }
    val dateParts = remember(event.date) {
        val clean = event.date.trim()
        val tokens = clean.split(" ", ",", "-", "/").filter { it.isNotBlank() }
        val month = tokens.firstOrNull { it.length >= 3 && !it.all { c -> c.isDigit() } }?.take(3)?.uppercase() ?: "OCT"
        val day = tokens.firstOrNull { it.all { c -> c.isDigit() } && it.length <= 2 } ?: "24"
        Pair(month, day)
    }

    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A101F)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEventClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Date Box on top left
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dateParts.first, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF1877F2))
                        Text(dateParts.second, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFF8FAFC))
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFF8FAFC)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${event.date} • ${event.location}",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${event.goingCount} attending • ${event.interestedCount} interested",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isGoing = !isGoing
                            if (isGoing) isInterested = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGoing) Color(0xFF10B981) else Color(0xFF1877F2)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isGoing) "✓ Going" else "Going", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            isInterested = !isInterested
                            if (isInterested) isGoing = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isInterested) Color(0xFFF59E0B) else Color(0xFF1E293B)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isInterested) "✓ Interested" else "Interested",
                            color = if (isInterested) Color.Black else Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Special Post: Product / MarketPoint Card
 */
@Composable
fun ProductPostCard(
    product: Product,
    onViewProduct: (() -> Unit)? = null
) {
    val marketplaceContext = LocalMarketplaceContext.current
    val handleViewProduct = {
        onViewProduct?.invoke() ?: marketplaceContext.onViewProduct(product.id)
    }

    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A101F)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = handleViewProduct)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Product Photo with Price Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.95f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${product.currency} %,d".format(product.price.toLong()),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Title, location, description & View in MarketPoint button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = product.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(product.location, fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }
                }
                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = handleViewProduct,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View in MarketPoint", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * Compatibility PostCard composable for ProfileScreen and other screens
 */
@Composable
fun PostCard(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onUserClick: () -> Unit,
    currentUser: User? = null,
    isFollowing: Boolean = false,
    onFollowToggle: () -> Unit = {},
    onOpenProduct: ((Product) -> Unit)? = null,
    onOpenEvent: ((Event) -> Unit)? = null,
    onOpenGroup: ((Long) -> Unit)? = null,
    onPlayMusic: ((Song) -> Unit)? = null,
    onOpenGallery: ((List<String>, Int) -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDeletePost: ((Long) -> Unit)? = null,
    onEditPost: ((Long, String) -> Unit)? = null,
    onHidePost: ((Long) -> Unit)? = null,
    onReportPost: ((Long, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    WebPostCard(
        post = post,
        currentUser = currentUser ?: post.author,
        isFollowing = isFollowing,
        onFollowToggle = onFollowToggle,
        onLike = onLike,
        onReact = { onLike() },
        onComment = onComment,
        onShare = { onShare?.invoke() },
        onSave = onSave,
        onUserClick = onUserClick,
        onOpenGallery = { list, idx -> onOpenGallery?.invoke(list, idx) },
        onOpenReactions = {},
        onOpenProduct = { onOpenProduct?.invoke(it) },
        onOpenEvent = { onOpenEvent?.invoke(it) },
        onOpenGroup = { onOpenGroup?.invoke(it) },
        onPlayMusic = { onPlayMusic?.invoke(it) },
        onDeletePost = { onDeletePost?.invoke(it) },
        onEditPost = { id, text -> onEditPost?.invoke(id, text) },
        onHidePost = { onHidePost?.invoke(it) },
        onReportPost = onReportPost,
        modifier = modifier
    )
}

@Composable
fun JobPostingCard(
    jobTitle: String,
    company: String,
    location: String,
    salary: String,
    jobType: String,
    onApply: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162137)),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("JOB POSTING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(jobType, fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(jobTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))

            Spacer(modifier = Modifier.height(4.dp))
            Text("$company • $location", fontSize = 13.sp, color = Color(0xFF94A3B8))

            if (salary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(salary, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onApply,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Special Post: Music Post Card
 */
@Composable
fun MusicPostCard(
    song: Song,
    onPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song.cover,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Large play button overlay
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2).copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF8FAFC))
                    Text("${song.artist} • ${song.album}", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }

                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Special Post: Shared Post Nested Card
 */
/**
 * Feed Story Card: Displayed in feed when post is a Story
 */
@Composable
fun FeedStoryCard(
    story: Story,
    post: Post,
    onStoryClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable(onClick = onStoryClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                val isTextStory = story.type == "text" || (story.mediaUrl.isBlank() && (story.textContent.isNotBlank() || post.content.isNotBlank()))
                if (isTextStory) {
                    val brush = remember(story.backgroundStyle) {
                        when (story.backgroundStyle) {
                            "red" -> Brush.linearGradient(listOf(Color(0xFFFF0057), Color(0xFFE64C4C)))
                            "blue" -> Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
                            "green" -> Brush.linearGradient(listOf(Color(0xFFA8FF78), Color(0xFF78FFD6)))
                            "purple" -> Brush.linearGradient(listOf(Color(0xFFE65C00), Color(0xFFF9D423)))
                            "dark" -> Brush.linearGradient(listOf(Color(0xFF434343), Color(0xFF000000)))
                            else -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = story.textContent.ifBlank { post.content },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                } else {
                    AsyncImage(
                        model = story.mediaUrl.ifBlank { post.mediaUrl },
                        contentDescription = "Story Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    val caption = story.textContent.ifBlank { post.content }
                    if (caption.isNotBlank()) {
                        Text(
                            text = caption,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }
                }

                // Story Gradient Pill Badge
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Center Play Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch Story",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = "Views",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Story Update",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onStoryClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Watch Story", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Shared Product Card: When a post shares a marketplace product
 */
@Composable
fun SharedProductCard(
    product: Product,
    originalAuthor: User,
    timestamp: String,
    onViewProduct: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A101F)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewProduct)
    ) {
        Column {
            // Nested Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = originalAuthor.profileImageUrl,
                    contentDescription = originalAuthor.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = originalAuthor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFF8FAFC)
                        )
                        if (originalAuthor.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(size = 12.dp)
                        }
                    }
                    Text(
                        text = "Shared a product from MarketPoint • ${PostsApiService.formatRelativeTime(timestamp)}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1877F2).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("MarketPoint", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1877F2))
                }
            }

            // Product Photo with Price Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.95f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${product.currency} %,d".format(product.price.toLong()),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Title, location, action
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(product.location, fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onViewProduct,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View in MarketPoint", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Shared Event Card: When a post shares an event
 */
@Composable
fun SharedEventCard(
    event: Event,
    originalAuthor: User,
    timestamp: String,
    onEventClick: () -> Unit
) {
    val dateParts = remember(event.date) {
        val clean = event.date.trim()
        val tokens = clean.split(" ", ",", "-", "/").filter { it.isNotBlank() }
        val month = tokens.firstOrNull { it.length >= 3 && !it.all { c -> c.isDigit() } }?.take(3)?.uppercase() ?: "EVENT"
        val day = tokens.firstOrNull { it.all { c -> c.isDigit() } && it.length <= 2 } ?: "•"
        Pair(month, day)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A101F)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEventClick)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = originalAuthor.profileImageUrl,
                    contentDescription = originalAuthor.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = originalAuthor.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "Shared an event • ${PostsApiService.formatRelativeTime(timestamp)}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("EVENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                }
            }

            // Cover Image with Date Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120).copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dateParts.first, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF1877F2))
                        Text(dateParts.second, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFF8FAFC))
                    }
                }
            }

            // Details
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = event.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${event.date} • ${event.location}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${event.goingCount} attending • ${event.interestedCount} interested",
                    fontSize = 11.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onEventClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Event Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Shared Story Card: When a post shares a story
 */
@Composable
fun SharedStoryCard(
    story: Story,
    originalAuthor: User,
    timestamp: String,
    onStoryClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A101F)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStoryClick)
    ) {
        Column {
            // Header with Story Ring
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF8B5CF6)))
                        )
                        .padding(2.dp)
                ) {
                    AsyncImage(
                        model = originalAuthor.profileImageUrl,
                        contentDescription = originalAuthor.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = originalAuthor.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "Shared a Story • ${PostsApiService.formatRelativeTime(timestamp)}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEC4899).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("STORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                }
            }

            // Visual Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (story.type == "text" || (story.mediaUrl.isBlank() && story.textContent.isNotBlank())) {
                    val brush = remember(story.backgroundStyle) {
                        when (story.backgroundStyle) {
                            "red" -> Brush.linearGradient(listOf(Color(0xFFFF0057), Color(0xFFE64C4C)))
                            "blue" -> Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
                            "green" -> Brush.linearGradient(listOf(Color(0xFFA8FF78), Color(0xFF78FFD6)))
                            else -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = story.textContent,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    AsyncImage(
                        model = story.mediaUrl,
                        contentDescription = "Story Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to view story",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Button(
                    onClick = onStoryClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Watch Story", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Special Post: Shared Post Nested Card supporting regular posts, products, events, stories, and songs
 */
@Composable
fun SharedPostNestedCard(
    sharedPost: Post,
    onOpenGallery: (List<String>, Int) -> Unit,
    onOpenProduct: (Product) -> Unit = {},
    onOpenEvent: (Event) -> Unit = {},
    onPlayMusic: (Song) -> Unit = {},
    onStoryClick: ((Story) -> Unit)? = null,
    onOpenGroup: (Long) -> Unit = {}
) {
    if (sharedPost.product != null || sharedPost.postType == "marketplace" || sharedPost.postType == "product") {
        val prod = sharedPost.product ?: Product(
            id = sharedPost.id,
            title = sharedPost.content.ifBlank { "Marketplace Item" },
            price = 50000.0,
            imageUrl = sharedPost.mediaUrl ?: "",
            category = "General",
            location = sharedPost.location ?: "Tanzania",
            description = sharedPost.content,
            seller = sharedPost.author
        )
        SharedProductCard(
            product = prod,
            originalAuthor = sharedPost.author,
            timestamp = sharedPost.timestamp,
            onViewProduct = { onOpenProduct(prod) }
        )
    } else if (sharedPost.event != null || sharedPost.postType == "event") {
        val ev = sharedPost.event ?: Event(
            id = sharedPost.id,
            title = sharedPost.content.ifBlank { "Event" },
            date = "Oct 24, 2026",
            location = sharedPost.location ?: "Tanzania",
            imageUrl = sharedPost.mediaUrl ?: "",
            description = sharedPost.content,
            organizer = sharedPost.author
        )
        SharedEventCard(
            event = ev,
            originalAuthor = sharedPost.author,
            timestamp = sharedPost.timestamp,
            onEventClick = { onOpenEvent(ev) }
        )
    } else if (sharedPost.story != null || sharedPost.postType == "story") {
        val st = sharedPost.story ?: Story(
            id = sharedPost.id,
            author = sharedPost.author,
            mediaUrl = sharedPost.mediaUrl ?: "",
            textContent = sharedPost.content,
            backgroundStyle = sharedPost.backgroundStyle ?: "",
            timestamp = sharedPost.timestamp
        )
        SharedStoryCard(
            story = st,
            originalAuthor = sharedPost.author,
            timestamp = sharedPost.timestamp,
            onStoryClick = { onStoryClick?.invoke(st) }
        )
    } else if (sharedPost.song != null || sharedPost.postType == "music" || sharedPost.postType == "song") {
        val song = sharedPost.song ?: Song(
            id = sharedPost.id.toString(),
            title = sharedPost.content.ifBlank { "Track" },
            artist = sharedPost.author.name,
            audioUrl = sharedPost.mediaUrl ?: "",
            cover = sharedPost.mediaUrl ?: ""
        )
        MusicPostCard(
            song = song,
            onPlay = { onPlayMusic(song) }
        )
    } else {
        // Standard regular post share & shared group post
        var isExpandedSharedText by remember { mutableStateOf(false) }
        val isGroup = sharedPost.groupId != null
        val mediaList = if (sharedPost.mediaUrls.isNotEmpty()) sharedPost.mediaUrls
            else if (!sharedPost.mediaUrl.isNullOrBlank()) listOf(sharedPost.mediaUrl)
            else emptyList()

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A101F)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Nested Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGroup) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { sharedPost.groupId?.let { onOpenGroup(it) } }
                        ) {
                            if (!sharedPost.groupImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = sharedPost.groupImageUrl,
                                    contentDescription = sharedPost.groupName ?: "Group",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.TopStart)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0xFF1E293B), CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.TopStart)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1877F2).copy(alpha = 0.25f))
                                        .border(1.dp, Color(0xFF1E293B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = "Group",
                                        tint = Color(0xFF1877F2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            AsyncImage(
                                model = sharedPost.author.profileImageUrl,
                                contentDescription = sharedPost.author.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF0F172A), CircleShape)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = sharedPost.author.profileImageUrl,
                            contentDescription = sharedPost.author.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isGroup) {
                                Text(
                                    text = sharedPost.groupName ?: "Group",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.clickable { sharedPost.groupId?.let { onOpenGroup(it) } }
                                )
                            } else {
                                Text(
                                    text = sharedPost.author.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFF8FAFC)
                                )
                            }
                            if (sharedPost.author.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge(size = 14.dp)
                            }
                        }
                        Text(
                            text = if (isGroup) {
                                "${sharedPost.author.name} • ${PostsApiService.formatRelativeTime(sharedPost.timestamp)}"
                            } else {
                                PostsApiService.formatRelativeTime(sharedPost.timestamp)
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                if (sharedPost.content.isNotBlank()) {
                    val words = remember(sharedPost.content) {
                        sharedPost.content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    }
                    val hasMoreThan8Words = words.size > 8
                    val truncatedContent = remember(sharedPost.content, words) {
                        if (words.size > 8) words.take(8).joinToString(" ") + "…" else sharedPost.content
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!hasMoreThan8Words || isExpandedSharedText) sharedPost.content else truncatedContent,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    if (hasMoreThan8Words) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isExpandedSharedText = !isExpandedSharedText }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = if (isExpandedSharedText) "See Less" else "See More",
                                color = Color(0xFF1877F2),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpandedSharedText) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (mediaList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (mediaList.size > 1) {
                        MediaGrid(
                            images = mediaList,
                            onImageClick = { idx -> onOpenGallery(mediaList, idx) }
                        )
                    } else {
                        AsyncImage(
                            model = mediaList.first(),
                            contentDescription = "Shared image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenGallery(mediaList, 0) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interleaved Widget: People You May Know Section
 */
@Composable
fun PeopleYouMayKnowSection(
    currentUser: User,
    allUsers: List<User>,
    followStates: Map<Long, Boolean> = emptyMap(),
    onFollowToggle: (Long) -> Unit,
    onDismiss: ((Long) -> Unit)? = null,
    onUserClick: (User) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "People You May Know",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allUsers, key = { it.id }) { user ->
                val isFollowing = followStates[user.id] ?: user.isFollowing
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Square profile photo with optional dismiss button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = user.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onUserClick(user) }
                            )

                            if (onDismiss != null) {
                                IconButton(
                                    onClick = { onDismiss(user.id) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(26.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFF8FAFC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onUserClick(user) }
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    VerifiedBadge(size = 12.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Suggested for you",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onFollowToggle(user.id) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFF334155) else Color(0xFF1877F2)
                                ),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Text(
                                    text = if (isFollowing) "Following" else "+ Follow",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interleaved Widget: Groups You May Join Section
 */
@Composable
fun GroupsYouMayJoinSection(
    groups: List<Group>,
    joinedStates: Map<Long, Boolean>,
    onJoinToggle: (Long) -> Unit,
    onGroupClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Groups You May Join",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                val isJoined = joinedStates[group.id] ?: group.isMember
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .width(220.dp)
                        .clickable { onGroupClick(group.id) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Cover Image with Public/Private tag
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clickable { onGroupClick(group.id) }
                        ) {
                            if (group.coverImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = group.coverImageUrl,
                                    contentDescription = group.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(Color(0xFF1877F2), Color(0xFF166FE5))))
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (group.isPrivate) "🔒 Private" else "🌍 Public",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Info section
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = group.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFF8FAFC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onGroupClick(group.id) }
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${group.membersCount} members • ${group.category}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onJoinToggle(group.id) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isJoined) Color(0xFF334155) else Color(0xFF1877F2)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isJoined) "Joined" else "Join Group",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
