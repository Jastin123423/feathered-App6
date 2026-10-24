package com.unera.social.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unera.social.data.GroupsApiService
import com.unera.social.data.GroupMemberInfo
import com.unera.social.data.MarketplaceApiService
import com.unera.social.data.MockData
import com.unera.social.data.PostsApiService
import com.unera.social.data.StoriesApiService
import com.unera.social.data.VideosApiService
import android.content.Context
import android.net.Uri
import com.unera.social.data.MediaUploadHelper
import com.unera.social.model.*
import com.unera.social.ui.components.PostUploadState
import org.json.JSONObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeatheredUiState(
    val currentScreen: Screen = Screen.FEED,
    val currentUser: User = MockData.currentUser,
    val users: List<User> = MockData.users,
    val posts: List<Post> = emptyList(),
    val isFeedLoading: Boolean = false,
    val videoPosts: List<Post> = emptyList(),
    val isVideosLoading: Boolean = false,
    val sponsoredFeedAds: List<SponsoredFeedAd> = emptyList(),
    val isFeedAdsLoading: Boolean = false,
    val userProfilePosts: Map<Long, List<Post>> = emptyMap(),
    val isProfilePostsLoading: Boolean = false,
    val stories: List<Story> = emptyList(),
    val isStoriesLoading: Boolean = false,
    val storyViewers: Map<Long, List<StoryViewer>> = emptyMap(),
    val isStoryViewersLoading: Boolean = false,
    val storyComments: Map<Long, List<StoryComment>> = emptyMap(),
    val isStoryCommentsLoading: Boolean = false,
    val storyReactions: Map<Long, StoryReactionsData> = emptyMap(),
    val storyAnalytics: Map<Long, StoryAnalytics> = emptyMap(),
    val reels: List<Reel> = emptyList(),
    val products: List<Product> = emptyList(),
    val isProductsLoading: Boolean = false,
    val selectedCategory: String = "all",
    val selectedCountry: String = "all",
    val marketMode: String = "for_you", // "for_you", "local", "worldwide"
    val events: List<Event> = MockData.events,
    val conversations: List<Conversation> = MockData.conversations,
    val activeConversationId: String? = null,
    val notifications: List<Notification> = MockData.notifications,
    val searchQuery: String = "",
    val showCreatePostDialog: Boolean = false,
    val createPostInitialPicker: String? = null,
    val selectedPostForComments: Post? = null,
    val showSearchDialog: Boolean = false,
    val showNotificationsOverlay: Boolean = false,
    val showCreateEventDialog: Boolean = false,
    val showCreateProductDialog: Boolean = false,
    val selectedProductForDetail: Product? = null,
    val selectedStory: Story? = null,
    val showCreateStoryDialog: Boolean = false,
    val showMenuOverlay: Boolean = false,
    val selectedUserProfile: User? = null,
    // F-Music state
    val songs: List<Song> = MockData.songs,
    val currentAudioTrack: AudioTrack? = null,
    val isPlayingAudio: Boolean = false,
    val audioPositionSeconds: Int = 0,
    val audioDurationSeconds: Int = 180,
    val isPlayerExpanded: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val audioVolume: Float = 1.0f,
    val likedTrackKeys: Set<String> = setOf("music:1", "music:4", "music:6"),
    val myTotalPlays: Long = 33290L,
    val trackPlays: Map<String, Long> = emptyMap(),
    val songComments: Map<String, List<SongComment>> = mapOf(
        "1" to MockData.initialSongComments.filter { it.songId == "1" },
        "2" to MockData.initialSongComments.filter { it.songId == "2" }
    ),
    val downloadedTrackIds: Set<String> = emptySet(),
    val downloadingTrackId: String? = null,
    val downloadProgress: Int = 0,
    val downloadToast: String? = null,
    val activeDiscussionTrack: AudioTrack? = null,
    val activeShareTrack: AudioTrack? = null,
    val songUploadProgress: Int? = null,
    val songUploadStatus: String? = null,
    // Groups state
    val groups: List<Group> = emptyList(),
    val groupPosts: List<GroupPost> = emptyList(),
    val groupInvites: List<GroupInvite> = emptyList(),
    val groupSuggestions: List<Group> = emptyList(),
    val groupMembers: Map<Long, List<GroupMemberInfo>> = emptyMap(),
    val groupEvents: Map<Long, List<Event>> = emptyMap(),
    val isGroupsLoading: Boolean = false,
    val isGroupPostsLoading: Boolean = false,
    val activeGroupId: Long? = null,
    val groupMembersOverrides: Map<Long, Map<Long, String>> = emptyMap(), // groupId -> (userId -> role)
    val groupPostingDisabledOverrides: Map<Long, Set<Long>> = emptyMap(), // groupId -> Set<userId>
    // Ads Manager state
    val adCampaigns: List<AdCampaign> = listOf(
        AdCampaign(
            id = 1L,
            name = "Summer Safari Adventure 2026",
            type = "image",
            status = "active",
            mediaUrl = "https://images.unsplash.com/photo-1516426122078-c23e76319801?w=800",
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1516426122078-c23e76319801?w=800",
                "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e?w=800"
            ),
            description = "Experience the majestic Serengeti migration and Ngorongoro Crater! Book early and enjoy 20% off all luxury tented lodge packages. 🦁🌿",
            cta = "Book Now",
            link = "https://tanzaniasafari.example.com",
            phone = "+255 754 112 233",
            email = "safari@example.com",
            location = "Tanzania & East Africa",
            budget = 15,
            days = 14,
            startDate = "2026-03-01",
            endDate = "2026-03-15",
            impressions = 14200,
            views = 9850,
            clicks = 680,
            postId = 1L
        ),
        AdCampaign(
            id = 2L,
            name = "Authentic Kitenge Bomber Jackets",
            type = "image",
            status = "active",
            mediaUrl = "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800",
            mediaUrls = listOf("https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800"),
            description = "Handmade artisan Kitenge bomber jackets now available with free delivery across Dar es Salaam. Unisex sizes S to XXL! 🎨🔥",
            cta = "Shop Now",
            link = "https://kitengefashion.example.com",
            phone = "+255 682 998 877",
            email = "orders@kitengefashion.com",
            location = "Dar es Salaam, Tanzania",
            budget = 10,
            days = 7,
            startDate = "2026-03-05",
            endDate = "2026-03-12",
            impressions = 8940,
            views = 6200,
            clicks = 412,
            postId = 2L
        ),
        AdCampaign(
            id = 3L,
            name = "Kilimanjaro Summit Expedition",
            type = "video",
            status = "paused",
            mediaUrl = "https://images.unsplash.com/photo-1589556264800-08ae9e129a8c?w=800",
            mediaUrls = listOf("https://images.unsplash.com/photo-1589556264800-08ae9e129a8c?w=800"),
            description = "Conquer the roof of Africa with certified guides and full safety equipment. Machame and Lemosho route expeditions departing every week.",
            cta = "Learn More",
            link = "https://kilimanjaroclimb.example.com",
            phone = "+255 712 345 678",
            location = "Global",
            budget = 25,
            days = 21,
            startDate = "2026-02-10",
            endDate = "2026-03-03",
            impressions = 24600,
            views = 18400,
            clicks = 980,
            postId = 3L
        )
    ),
    val activeAdCampaignToBoostPost: Post? = null,
    // Authentication State
    val isLoggedIn: Boolean = true,
    val isGuest: Boolean = false,
    val showAuthModal: Boolean = false,
    val authMode: String = "login", // "login", "register", "forgot"
    val postUploadState: PostUploadState? = null,
    val fullScreenImageViewerUrl: String? = null,
    val isAppLoading: Boolean = false
)

class FeatheredViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeatheredUiState())
    val uiState: StateFlow<FeatheredUiState> = _uiState.asStateFlow()

    init {
        refreshLiveProducts()
        refreshFeed()
        refreshGroups()
        refreshStories()
        refreshVideos()
        refreshFeedAds()
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isVideosLoading = true) }
            val uid = if (_uiState.value.isLoggedIn && !_uiState.value.isGuest) _uiState.value.currentUser.id else null
            val liveVideos = VideosApiService.fetchVideos(userId = uid, limit = 50)
            _uiState.update { state ->
                state.copy(
                    videoPosts = liveVideos,
                    isVideosLoading = false
                )
            }
        }
    }

    fun refreshFeedAds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFeedAdsLoading = true) }
            val liveAds = VideosApiService.fetchFeedAds()
            _uiState.update { state ->
                state.copy(
                    sponsoredFeedAds = liveAds,
                    isFeedAdsLoading = false
                )
            }
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFeedLoading = true) }
            val isUserLoggedIn = _uiState.value.isLoggedIn && !_uiState.value.isGuest
            val livePosts = if (isUserLoggedIn) {
                // Logged in feed fetches live data from api/feeds
                PostsApiService.fetchFeedPosts(userId = _uiState.value.currentUser.id)
            } else {
                // Non-logged in feed fetches from api/posts
                PostsApiService.fetchPublicPosts()
            }
            _uiState.update { state ->
                state.copy(
                    posts = livePosts,
                    isFeedLoading = false
                )
            }
        }
    }

    fun fetchUserProfilePosts(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProfilePostsLoading = true) }
            val userPosts = PostsApiService.fetchUserPosts(userId)
            _uiState.update { state ->
                val updatedMap = state.userProfilePosts + (userId to userPosts)
                state.copy(
                    userProfilePosts = updatedMap,
                    isProfilePostsLoading = false
                )
            }
        }
    }

    fun refreshLiveProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProductsLoading = true) }
            val liveProducts = MarketplaceApiService.fetchLiveProducts()
            _uiState.update { state ->
                state.copy(
                    products = liveProducts,
                    isProductsLoading = false
                )
            }
        }
    }

    fun setScreen(screen: Screen) {
        _uiState.update {
            it.copy(
                currentScreen = screen,
                activeConversationId = if (screen != Screen.MESSAGES) null else it.activeConversationId
            )
        }
        if (screen == Screen.MARKETPLACE) {
            refreshLiveProducts()
        } else if (screen == Screen.FEED) {
            refreshFeed()
            refreshStories()
        } else if (screen == Screen.REELS) {
            refreshVideos()
            refreshFeedAds()
        } else if (screen == Screen.PROFILE) {
            val uid = _uiState.value.selectedUserProfile?.id ?: _uiState.value.currentUser.id
            fetchUserProfilePosts(uid)
        } else if (screen == Screen.GROUPS) {
            refreshGroups()
        }
    }

    fun setMarketMode(mode: String) {
        _uiState.update { it.copy(marketMode = mode) }
    }

    fun setSelectedCountry(country: String) {
        _uiState.update { it.copy(selectedCountry = country) }
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun toggleLikePost(postId: Long) {
        val post = _uiState.value.posts.find { it.id == postId } ?: _uiState.value.videoPosts.find { it.id == postId }
        var isNewLiked = false
        _uiState.update { state ->
            val updatedPosts = state.posts.map { p ->
                if (p.id == postId) {
                    val newLiked = !p.isLiked
                    isNewLiked = newLiked
                    val newCount = if (newLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                    p.copy(isLiked = newLiked, likesCount = newCount, myReaction = if (newLiked) "like" else null)
                } else p
            }
            val updatedVideoPosts = state.videoPosts.map { p ->
                if (p.id == postId) {
                    val newLiked = !p.isLiked
                    isNewLiked = newLiked
                    val newCount = if (newLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                    p.copy(isLiked = newLiked, likesCount = newCount, myReaction = if (newLiked) "like" else null)
                } else p
            }
            val isProd = post?.product != null || post?.postType == "product"
            val pid = post?.product?.id ?: postId
            val updatedProducts = if (isProd) {
                state.products.map { prod ->
                    if (prod.id == pid) {
                        val newLiked = !prod.isLiked
                        prod.copy(
                            isLiked = newLiked,
                            myReaction = if (newLiked) "like" else null,
                            likesCount = if (newLiked) prod.likesCount + 1 else (prod.likesCount - 1).coerceAtLeast(0)
                        )
                    } else prod
                }
            } else state.products
            state.copy(
                posts = updatedPosts,
                videoPosts = updatedVideoPosts,
                products = updatedProducts,
                selectedPostForComments = if (state.selectedPostForComments?.id == postId) {
                    updatedPosts.find { it.id == postId } ?: updatedVideoPosts.find { it.id == postId }
                } else state.selectedPostForComments
            )
        }

        viewModelScope.launch {
            val reactType = if (isNewLiked) "like" else "unlike"
            VideosApiService.reactToPost(postId, _uiState.value.currentUser.id, reactType)
        }

        if (post != null && (post.product != null || post.postType == "product")) {
            val pid = post.product?.id ?: postId
            val action = if (post.isLiked) "unlike" else "like"
            viewModelScope.launch {
                MarketplaceApiService.reactToProduct(pid, _uiState.value.currentUser.id, action)
            }
        }
    }

    fun reactToPost(postId: Long, reaction: String) {
        val post = _uiState.value.posts.find { it.id == postId } ?: _uiState.value.videoPosts.find { it.id == postId }
        var finalReactionToSend = reaction
        _uiState.update { state ->
            val updatedPosts = state.posts.map { p ->
                if (p.id == postId) {
                    val isSame = p.myReaction == reaction
                    val newReaction = if (isSame) null else reaction
                    finalReactionToSend = newReaction ?: "unlike"
                    val newLiked = newReaction != null
                    val prevHadReaction = p.myReaction != null
                    val newCount = when {
                        !prevHadReaction && newReaction != null -> p.likesCount + 1
                        prevHadReaction && newReaction == null -> (p.likesCount - 1).coerceAtLeast(0)
                        else -> p.likesCount
                    }
                    val existingWithoutMe = p.reactions.filter { it.userId != state.currentUser.id }
                    val updatedReactions = if (newReaction != null) {
                        existingWithoutMe + UserReaction(state.currentUser.id, state.currentUser.name, state.currentUser.profileImageUrl, newReaction)
                    } else existingWithoutMe
                    p.copy(
                        isLiked = newLiked,
                        myReaction = newReaction,
                        likesCount = newCount,
                        reactions = updatedReactions
                    )
                } else p
            }
            val updatedVideoPosts = state.videoPosts.map { p ->
                if (p.id == postId) {
                    val isSame = p.myReaction == reaction
                    val newReaction = if (isSame) null else reaction
                    finalReactionToSend = newReaction ?: "unlike"
                    val newLiked = newReaction != null
                    val prevHadReaction = p.myReaction != null
                    val newCount = when {
                        !prevHadReaction && newReaction != null -> p.likesCount + 1
                        prevHadReaction && newReaction == null -> (p.likesCount - 1).coerceAtLeast(0)
                        else -> p.likesCount
                    }
                    val existingWithoutMe = p.reactions.filter { it.userId != state.currentUser.id }
                    val updatedReactions = if (newReaction != null) {
                        existingWithoutMe + UserReaction(state.currentUser.id, state.currentUser.name, state.currentUser.profileImageUrl, newReaction)
                    } else existingWithoutMe
                    p.copy(
                        isLiked = newLiked,
                        myReaction = newReaction,
                        likesCount = newCount,
                        reactions = updatedReactions
                    )
                } else p
            }
            val isProd = post?.product != null || post?.postType == "product"
            val pid = post?.product?.id ?: postId
            val updatedProducts = if (isProd) {
                state.products.map { prod ->
                    if (prod.id == pid) {
                        val isSame = prod.myReaction == reaction
                        val newReaction = if (isSame) null else reaction
                        val prevHad = prod.myReaction != null
                        val newCount = when {
                            !prevHad && newReaction != null -> prod.likesCount + 1
                            prevHad && newReaction == null -> (prod.likesCount - 1).coerceAtLeast(0)
                            else -> prod.likesCount
                        }
                        prod.copy(isLiked = newReaction != null, myReaction = newReaction, likesCount = newCount)
                    } else prod
                }
            } else state.products
            state.copy(
                posts = updatedPosts,
                videoPosts = updatedVideoPosts,
                products = updatedProducts,
                selectedPostForComments = if (state.selectedPostForComments?.id == postId) {
                    updatedPosts.find { it.id == postId } ?: updatedVideoPosts.find { it.id == postId }
                } else state.selectedPostForComments
            )
        }

        viewModelScope.launch {
            VideosApiService.reactToPost(postId, _uiState.value.currentUser.id, finalReactionToSend)
        }

        if (post != null && (post.product != null || post.postType == "product")) {
            val pid = post.product?.id ?: postId
            val isSame = post.myReaction == reaction
            val action = if (isSame) "unlike" else reaction
            viewModelScope.launch {
                MarketplaceApiService.reactToProduct(pid, _uiState.value.currentUser.id, action)
            }
        }
    }

    fun toggleSavePost(postId: Long) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map {
                    if (it.id == postId) it.copy(isSaved = !it.isSaved) else it
                }
            )
        }
    }

    fun toggleSaveReel(reelId: Long) {
        _uiState.update { state ->
            state.copy(
                reels = state.reels.map {
                    if (it.id == reelId) it.copy(isSaved = !it.isSaved) else it
                }
            )
        }
    }

    fun removeSavedItem(itemId: Long) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map {
                    if (it.id == itemId) it.copy(isSaved = false) else it
                },
                reels = state.reels.map {
                    if (it.id == itemId) it.copy(isSaved = false) else it
                }
            )
        }
    }

    fun toggleFollowUser(userId: Long) {
        _uiState.update { state ->
            val updatedUsers = state.users.map { user ->
                if (user.id == userId) {
                    val newFollowing = !user.isFollowing
                    val newCount = if (newFollowing) user.followersCount + 1 else (user.followersCount - 1).coerceAtLeast(0)
                    user.copy(isFollowing = newFollowing, followersCount = newCount)
                } else user
            }
            state.copy(users = updatedUsers)
        }
        viewModelScope.launch {
            VideosApiService.followUser(_uiState.value.currentUser.id, userId)
        }
    }

    fun addComment(postId: Long, commentText: String) {
        if (commentText.isBlank()) return
        val post = _uiState.value.posts.find { it.id == postId }
            ?: _uiState.value.videoPosts.find { it.id == postId }
            ?: _uiState.value.selectedPostForComments
        val newComment = Comment(
            id = System.currentTimeMillis(),
            author = _uiState.value.currentUser,
            text = commentText.trim(),
            timestamp = "Just now"
        )
        _uiState.update { state ->
            val updatedPosts = state.posts.map { p ->
                if (p.id == postId) {
                    p.copy(
                        comments = listOf(newComment) + p.comments,
                        commentsCount = p.commentsCount + 1
                    )
                } else p
            }
            val updatedVideoPosts = state.videoPosts.map { p ->
                if (p.id == postId) {
                    p.copy(
                        comments = listOf(newComment) + p.comments,
                        commentsCount = p.commentsCount + 1
                    )
                } else p
            }
            val isProd = post?.product != null || post?.postType == "product"
            val pid = post?.product?.id ?: postId
            val updatedProducts = if (isProd) {
                state.products.map { prod ->
                    if (prod.id == pid) prod.copy(commentsCount = prod.commentsCount + 1) else prod
                }
            } else state.products
            state.copy(
                posts = updatedPosts,
                videoPosts = updatedVideoPosts,
                products = updatedProducts,
                selectedPostForComments = updatedPosts.find { it.id == postId } ?: updatedVideoPosts.find { it.id == postId }
            )
        }

        if (post != null && (post.product != null || post.postType == "product")) {
            val pid = post.product?.id ?: postId
            viewModelScope.launch {
                MarketplaceApiService.addReview(
                    productId = pid,
                    userId = _uiState.value.currentUser.id,
                    text = commentText.trim()
                )
            }
        } else {
            viewModelScope.launch {
                val created = VideosApiService.postComment(
                    postId = postId,
                    userId = _uiState.value.currentUser.id,
                    text = commentText.trim()
                )
                if (created != null) {
                    _uiState.update { state ->
                        val updatedPosts = state.posts.map { p ->
                            if (p.id == postId) {
                                p.copy(
                                    comments = listOf(created) + p.comments.filter { it.id != newComment.id }
                                )
                            } else p
                        }
                        val updatedVideoPosts = state.videoPosts.map { p ->
                            if (p.id == postId) {
                                p.copy(
                                    comments = listOf(created) + p.comments.filter { it.id != newComment.id }
                                )
                            } else p
                        }
                        val updatedSel = if (state.selectedPostForComments?.id == postId) {
                            state.selectedPostForComments?.copy(
                                comments = listOf(created) + (state.selectedPostForComments?.comments?.filter { it.id != newComment.id } ?: emptyList())
                            )
                        } else state.selectedPostForComments
                        state.copy(
                            posts = updatedPosts,
                            videoPosts = updatedVideoPosts,
                            selectedPostForComments = updatedSel
                        )
                    }
                }
            }
        }
    }

    fun deleteComment(postId: Long, commentId: Long) {
        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    val remaining = post.comments.filter { it.id != commentId }
                    post.copy(
                        comments = remaining,
                        commentsCount = (post.commentsCount - 1).coerceAtLeast(0)
                    )
                } else post
            }
            val updatedVideoPosts = state.videoPosts.map { post ->
                if (post.id == postId) {
                    val remaining = post.comments.filter { it.id != commentId }
                    post.copy(
                        comments = remaining,
                        commentsCount = (post.commentsCount - 1).coerceAtLeast(0)
                    )
                } else post
            }
            state.copy(
                posts = updatedPosts,
                videoPosts = updatedVideoPosts,
                selectedPostForComments = updatedPosts.find { it.id == postId } ?: updatedVideoPosts.find { it.id == postId }
            )
        }
        viewModelScope.launch {
            VideosApiService.deleteComment(commentId, _uiState.value.currentUser.id)
        }
    }

    fun likeComment(commentId: Long) {
        viewModelScope.launch {
            VideosApiService.likeComment(commentId, _uiState.value.currentUser.id)
        }
    }

    fun editComment(commentId: Long, newText: String) {
        viewModelScope.launch {
            VideosApiService.editComment(commentId, _uiState.value.currentUser.id, newText)
        }
    }

    fun createPost(
        context: Context? = null,
        content: String,
        mediaUrl: String? = null,
        feeling: String? = null,
        mediaUrls: List<String> = emptyList(),
        location: String? = null,
        backgroundStyle: String? = null,
        taggedUsers: List<Long> = emptyList()
    ) {
        val allMedia = if (mediaUrls.isNotEmpty()) mediaUrls else if (!mediaUrl.isNullOrBlank()) listOf(mediaUrl) else emptyList()
        if (content.isBlank() && allMedia.isEmpty() && backgroundStyle.isNullOrBlank()) return
        val newPost = Post(
            id = System.currentTimeMillis(),
            author = _uiState.value.currentUser,
            content = content.trim(),
            mediaUrl = allMedia.firstOrNull(),
            mediaUrls = allMedia,
            timestamp = "Just now",
            feeling = feeling,
            location = location,
            backgroundStyle = backgroundStyle,
            taggedUsers = taggedUsers,
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0
        )
        // Dismiss dialog and begin upload progress banner immediately
        _uiState.update { state ->
            state.copy(
                showCreatePostDialog = false,
                postUploadState = PostUploadState(
                    isUploading = true,
                    progress = 10f,
                    title = "Posting to Feathered",
                    secondaryStatus = if (allMedia.isNotEmpty()) "Preparing media..." else "Publishing post...",
                    previewUrl = allMedia.firstOrNull(),
                    isSuccess = false
                )
            )
        }

        viewModelScope.launch {
            val uploadedMetaList = mutableListOf<MediaMetaItem>()
            val uploadedUrls = mutableListOf<String>()
            var primaryVideoUrl: String? = null
            var primaryThumbUrl: String? = null
            var isAnyVideo = false

            for ((index, item) in allMedia.withIndex()) {
                val isLocalUri = item.startsWith("content://") || item.startsWith("file://")
                if (isLocalUri && context != null) {
                    val uri = Uri.parse(item)
                    val mimeType = context.contentResolver.getType(uri) ?: ""
                    val isVideoItem = mimeType.startsWith("video") || item.contains(".mp4") || item.contains("video")
                    if (isVideoItem) isAnyVideo = true

                    val result = MediaUploadHelper.processAndUploadMedia(
                        context = context,
                        uri = uri,
                        isVideoHint = isVideoItem
                    ) { statusMsg, pct ->
                        _uiState.update { state ->
                            state.copy(
                                postUploadState = state.postUploadState?.copy(
                                    progress = 10f + (pct * 0.7f),
                                    secondaryStatus = statusMsg
                                )
                            )
                        }
                    }

                    if (result != null) {
                        uploadedUrls.add(result.originalUrl)
                        uploadedMetaList.add(result.toMediaMetaItem())
                        if (result.isVideo && primaryVideoUrl == null) {
                            primaryVideoUrl = result.originalUrl
                            primaryThumbUrl = result.thumbUrl
                        }
                    } else {
                        // Fallback to local URI if upload failed
                        uploadedUrls.add(item)
                    }
                } else {
                    // Already an HTTP URL
                    uploadedUrls.add(item)
                    val isVid = PostsApiService.isValidVideoUrl(item)
                    if (isVid) {
                        isAnyVideo = true
                        if (primaryVideoUrl == null) primaryVideoUrl = item
                    }
                    uploadedMetaList.add(
                        MediaMetaItem(
                            original = item,
                            resolution1080 = item,
                            resolution720 = item,
                            feed = item,
                            full = item,
                            type = if (isVid) "video" else "image"
                        )
                    )
                }
            }

            _uiState.update { state ->
                state.copy(
                    postUploadState = state.postUploadState?.copy(
                        progress = 85f,
                        secondaryStatus = "Finalizing post..."
                    )
                )
            }

            val finalMediaUrls = if (uploadedUrls.isNotEmpty()) uploadedUrls else allMedia
            val finalMediaType = if (isAnyVideo) "video" else if (finalMediaUrls.isNotEmpty()) "image" else null

            val livePost = PostsApiService.createPost(
                userId = _uiState.value.currentUser.id,
                content = content,
                mediaUrl = finalMediaUrls.firstOrNull(),
                mediaUrls = finalMediaUrls,
                mediaType = finalMediaType,
                videoUrl = primaryVideoUrl,
                thumbnailUrl = primaryThumbUrl,
                mediaMeta = uploadedMetaList,
                feeling = feeling,
                location = location,
                background = backgroundStyle,
                taggedUsers = taggedUsers
            )

            // Add post to feed and mark complete
            _uiState.update { state ->
                val finalPost = livePost ?: newPost.copy(
                    mediaUrl = finalMediaUrls.firstOrNull(),
                    mediaUrls = finalMediaUrls,
                    mediaType = finalMediaType,
                    videoUrl = primaryVideoUrl,
                    mediaMeta = uploadedMetaList
                )
                state.copy(
                    posts = listOf(finalPost) + state.posts.filter { it.id != newPost.id },
                    postUploadState = state.postUploadState?.copy(
                        progress = 100f,
                        isUploading = false,
                        isSuccess = true,
                        title = "Posted to Feathered",
                        secondaryStatus = "Just now"
                    )
                )
            }
            delay(3500)
            // Auto dismiss success banner after delay
            _uiState.update { state ->
                if (state.postUploadState?.isSuccess == true) {
                    state.copy(postUploadState = null)
                } else state
            }
        }
    }

    fun dismissPostUpload() {
        _uiState.update { it.copy(postUploadState = null) }
    }

    fun sharePost(postId: Long, destination: String = "feed") {
        val post = _uiState.value.posts.find { it.id == postId } ?: _uiState.value.videoPosts.find { it.id == postId }
        _uiState.update { state ->
            val updatedPosts = state.posts.map { p ->
                if (p.id == postId) {
                    p.copy(sharesCount = p.sharesCount + 1)
                } else p
            }
            val updatedVideoPosts = state.videoPosts.map { p ->
                if (p.id == postId) {
                    p.copy(sharesCount = p.sharesCount + 1)
                } else p
            }
            val isProd = post?.product != null || post?.postType == "product"
            val pid = post?.product?.id ?: postId
            val updatedProducts = if (isProd) {
                state.products.map { prod ->
                    if (prod.id == pid) prod.copy(sharesCount = prod.sharesCount + 1) else prod
                }
            } else state.products
            state.copy(posts = updatedPosts, videoPosts = updatedVideoPosts, products = updatedProducts)
        }

        if (post != null && (post.product != null || post.postType == "product")) {
            val pid = post.product?.id ?: postId
            viewModelScope.launch {
                MarketplaceApiService.shareProduct(
                    productId = pid,
                    userId = _uiState.value.currentUser.id,
                    destination = destination
                )
            }
        } else {
            viewModelScope.launch {
                VideosApiService.sharePost(
                    postId = postId,
                    userId = _uiState.value.currentUser.id,
                    destination = destination
                )
            }
        }
    }

    fun deletePost(postId: Long) {
        val post = _uiState.value.posts.find { it.id == postId }
        val pid = post?.product?.id ?: postId
        _uiState.update { state ->
            state.copy(
                posts = state.posts.filterNot { it.id == postId },
                products = if (post?.product != null || post?.postType == "product") state.products.filterNot { it.id == pid } else state.products
            )
        }
        if (post != null && (post.product != null || post.postType == "product")) {
            viewModelScope.launch {
                MarketplaceApiService.deleteProduct(pid, _uiState.value.currentUser.id)
            }
        }
    }

    fun editPost(postId: Long, newContent: String) {
        val post = _uiState.value.posts.find { it.id == postId }
        val pid = post?.product?.id ?: postId
        _uiState.update { state ->
            val updated = state.posts.map { p ->
                if (p.id == postId) p.copy(content = newContent) else p
            }
            val updatedProducts = state.products.map { prod ->
                if (prod.id == pid) prod.copy(description = newContent) else prod
            }
            state.copy(posts = updated, products = updatedProducts)
        }
        if (post != null && (post.product != null || post.postType == "product")) {
            viewModelScope.launch {
                MarketplaceApiService.updateProduct(pid, mapOf("description" to newContent))
            }
        }
    }

    fun hidePost(postId: Long) {
        _uiState.update { state ->
            state.copy(posts = state.posts.filterNot { it.id == postId })
        }
    }

    fun toggleLikeReel(reelId: Long) {
        _uiState.update { state ->
            state.copy(
                reels = state.reels.map { reel ->
                    if (reel.id == reelId) {
                        val newLiked = !reel.isLiked
                        val newLikes = if (newLiked) reel.likesCount + 1 else (reel.likesCount - 1).coerceAtLeast(0)
                        reel.copy(isLiked = newLiked, likesCount = newLikes)
                    } else reel
                }
            )
        }
    }

    fun toggleSaveProduct(productId: Long) {
        _uiState.update { state ->
            state.copy(
                products = state.products.map {
                    if (it.id == productId) it.copy(isSaved = !it.isSaved) else it
                }
            )
        }
    }

    fun createProduct(
        title: String,
        price: Double,
        discountPrice: Double? = null,
        category: String,
        location: String,
        country: String = "TZ",
        description: String,
        imageUrl: String,
        images: List<String> = emptyList(),
        phone: String = ""
    ) {
        val allImages = if (images.isNotEmpty()) images else if (imageUrl.isNotBlank()) listOf(imageUrl) else listOf("https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800")
        val newProduct = Product(
            id = System.currentTimeMillis(),
            title = title,
            price = price,
            discountPrice = discountPrice,
            category = category,
            location = location,
            country = country,
            currency = if (country.equals("TZ", ignoreCase = true)) "TZS" else "USD",
            imageUrl = allImages.first(),
            images = allImages,
            description = description,
            seller = _uiState.value.currentUser,
            phoneNumber = phone.ifBlank { "+255 712 345 678" },
            isFresh = true
        )

        // Optimistic Feed Bridge Post (Section 6)
        val feedPost = Post(
            id = newProduct.id,
            author = _uiState.value.currentUser,
            content = description,
            mediaUrl = newProduct.imageUrl,
            mediaUrls = newProduct.images,
            timestamp = "Just now",
            postType = "product",
            product = newProduct
        )

        _uiState.update { state ->
            val userProfilePosts = state.userProfilePosts.toMutableMap()
            val myPosts = userProfilePosts[state.currentUser.id] ?: emptyList()
            userProfilePosts[state.currentUser.id] = listOf(feedPost) + myPosts
            state.copy(
                products = listOf(newProduct) + state.products,
                posts = listOf(feedPost) + state.posts,
                userProfilePosts = userProfilePosts,
                showCreateProductDialog = false
            )
        }

        // Asynchronously post to live API and schedule silent refresh
        viewModelScope.launch {
            val published = MarketplaceApiService.publishProduct(newProduct)
            if (published != null && published.id != newProduct.id) {
                _uiState.update { state ->
                    state.copy(
                        products = state.products.map { if (it.id == newProduct.id) published else it },
                        posts = state.posts.map { if (it.id == newProduct.id) it.copy(id = published.id, product = published) else it }
                    )
                }
            }
            delay(3000)
            refreshFeed()
        }
    }

    fun toggleInterestedEvent(eventId: Long) {
        _uiState.update { state ->
            state.copy(
                events = state.events.map { event ->
                    if (event.id == eventId) {
                        val isInt = !event.isInterested
                        val newInterestedCount = if (isInt) event.interestedCount + 1 else (event.interestedCount - 1).coerceAtLeast(0)
                        event.copy(isInterested = isInt, interestedCount = newInterestedCount)
                    } else event
                }
            )
        }
    }

    fun toggleGoingEvent(eventId: Long) {
        _uiState.update { state ->
            state.copy(
                events = state.events.map { event ->
                    if (event.id == eventId) {
                        val isGoing = !event.isGoing
                        val newGoingCount = if (isGoing) event.goingCount + 1 else (event.goingCount - 1).coerceAtLeast(0)
                        event.copy(isGoing = isGoing, goingCount = newGoingCount)
                    } else event
                }
            )
        }
    }

    fun createEvent(
        title: String,
        date: String,
        location: String,
        description: String,
        imageUrl: String,
        time: String = "",
        visibility: String = "worldwide",
        groupId: Long? = null,
        groupName: String? = null
    ) {
        val newEvent = Event(
            id = System.currentTimeMillis(),
            title = title,
            date = date,
            location = location,
            description = description,
            imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800" },
            organizer = _uiState.value.currentUser,
            time = time,
            visibility = visibility,
            groupId = groupId,
            groupName = groupName,
            createdAt = "Just now"
        )
        _uiState.update { state ->
            state.copy(
                events = listOf(newEvent) + state.events,
                showCreateEventDialog = false
            )
        }
    }

    fun openConversation(convId: String) {
        _uiState.update { state ->
            val updated = state.conversations.map {
                if (it.id == convId) it.copy(unreadCount = 0) else it
            }
            state.copy(
                conversations = updated,
                activeConversationId = convId
            )
        }
    }

    fun closeConversation() {
        _uiState.update { it.copy(activeConversationId = null) }
    }

    fun sendMessage(convId: String, text: String) {
        if (text.isBlank()) return
        val newMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderId = _uiState.value.currentUser.id,
            text = text.trim(),
            timestamp = "Just now",
            isMine = true
        )
        _uiState.update { state ->
            val updated = state.conversations.map { conv ->
                if (conv.id == convId) {
                    conv.copy(
                        lastMessage = text.trim(),
                        lastTimestamp = "Just now",
                        messages = conv.messages + newMsg
                    )
                } else conv
            }
            state.copy(conversations = updated)
        }
    }

    fun sendAttachmentMessage(convId: String, attachment: com.unera.social.model.ChatAttachment, text: String = "") {
        val newMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderId = _uiState.value.currentUser.id,
            text = text.trim(),
            timestamp = "Just now",
            isMine = true,
            attachments = listOf(attachment)
        )
        _uiState.update { state ->
            val updated = state.conversations.map { conv ->
                if (conv.id == convId) {
                    conv.copy(
                        lastMessage = if (text.isNotBlank()) text.trim() else "📎 ${attachment.filename}",
                        lastTimestamp = "Just now",
                        messages = conv.messages + newMsg
                    )
                } else conv
            }
            state.copy(conversations = updated)
        }
    }

    fun editChatMessage(convId: String, messageId: String, newText: String) {
        _uiState.update { state ->
            val updated = state.conversations.map { conv ->
                if (conv.id == convId) {
                    val updatedMsgs = conv.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(text = newText, isEdited = true) else msg
                    }
                    conv.copy(messages = updatedMsgs)
                } else conv
            }
            state.copy(conversations = updated)
        }
    }

    fun deleteChatMessage(convId: String, messageId: String) {
        _uiState.update { state ->
            val updated = state.conversations.map { conv ->
                if (conv.id == convId) {
                    conv.copy(messages = conv.messages.filterNot { it.id == messageId })
                } else conv
            }
            state.copy(conversations = updated)
        }
    }

    fun startConversationWithUser(user: User, product: Product? = null) {
        val existing = _uiState.value.conversations.find { it.participant.id == user.id }
        val introMessage = if (product != null) {
            "${product.title}: Hello, I am interested in this product. Is it still available? [product:${product.id}]"
        } else null
        
        if (existing != null) {
            if (introMessage != null) {
                sendMessage(existing.id, introMessage)
            }
            openConversation(existing.id)
            setScreen(Screen.MESSAGES)
        } else {
            val initialMsgs = if (introMessage != null) {
                listOf(
                    ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        senderId = _uiState.value.currentUser.id,
                        text = introMessage,
                        timestamp = "Just now",
                        isMine = true
                    )
                )
            } else emptyList()

            val newConv = Conversation(
                id = "c_${System.currentTimeMillis()}",
                participant = user,
                lastMessage = introMessage ?: "Started a conversation",
                lastTimestamp = "Just now",
                unreadCount = 0,
                messages = initialMsgs
            )
            _uiState.update { state ->
                state.copy(
                    conversations = listOf(newConv) + state.conversations,
                    activeConversationId = newConv.id,
                    currentScreen = Screen.MESSAGES,
                    selectedProductForDetail = null
                )
            }
        }

        if (product != null) {
            viewModelScope.launch {
                MarketplaceApiService.sendProductInquiry(
                    recipientId = user.id,
                    productId = product.id,
                    productTitle = product.title,
                    userId = _uiState.value.currentUser.id
                )
            }
        }
    }

    fun markNotificationsAsRead() {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map { it.copy(isRead = true) }
            )
        }
    }

    fun markNotificationRead(id: String, isRead: Boolean = true) {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map {
                    if (it.id == id) it.copy(isRead = isRead) else it
                }
            )
        }
    }

    fun deleteNotification(id: String) {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.filter { it.id != id }
            )
        }
    }

    fun acceptGroupInvite(notificationId: String, groupId: Long?) {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
            )
        }
    }

    fun declineGroupInvite(notificationId: String, groupId: Long?) {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
            )
        }
    }

    fun updateBio(newBio: String) {
        _uiState.update { state ->
            val updatedUser = state.currentUser.copy(bio = newBio.trim())
            state.copy(
                currentUser = updatedUser,
                selectedUserProfile = if (state.selectedUserProfile?.id == updatedUser.id) updatedUser else state.selectedUserProfile
            )
        }
    }

    fun updateUserDetails(
        bio: String,
        work: String,
        education: String,
        location: String,
        website: String,
        coverImageUrl: String? = null,
        profileImageUrl: String? = null
    ) {
        _uiState.update { state ->
            val updatedUser = state.currentUser.copy(
                bio = bio.trim(),
                work = work.trim(),
                education = education.trim(),
                location = location.trim(),
                website = website.trim(),
                coverImageUrl = coverImageUrl ?: state.currentUser.coverImageUrl,
                profileImageUrl = profileImageUrl ?: state.currentUser.profileImageUrl
            )
            state.copy(
                currentUser = updatedUser,
                selectedUserProfile = if (state.selectedUserProfile?.id == updatedUser.id) updatedUser else state.selectedUserProfile
            )
        }
    }

    fun updateFullProfileDetails(
        name: String,
        bio: String,
        work: String,
        education: String,
        location: String,
        website: String,
        gender: String,
        birthDate: String
    ) {
        _uiState.update { state ->
            val updatedUser = state.currentUser.copy(
                name = name.ifBlank { state.currentUser.name }.trim(),
                bio = bio.trim(),
                work = work.trim(),
                education = education.trim(),
                location = location.trim(),
                website = website.trim(),
                gender = gender.trim(),
                birthDate = birthDate.trim().ifBlank { state.currentUser.birthDate }
            )
            val updatedUsers = state.users.map { if (it.id == updatedUser.id) updatedUser else it }
            state.copy(
                currentUser = updatedUser,
                users = updatedUsers,
                selectedUserProfile = if (state.selectedUserProfile?.id == updatedUser.id) updatedUser else state.selectedUserProfile
            )
        }
    }

    fun changePassword(current: String, new: String): Boolean {
        return true
    }

    fun deleteAccount() {
        _uiState.update { state ->
            val deletedUser = state.currentUser.copy(isDeleted = true, name = "[Deleted User]")
            state.copy(
                currentUser = deletedUser,
                currentScreen = Screen.FEED
            )
        }
    }

    fun openUserProfile(user: User) {
        _uiState.update { state ->
            state.copy(
                selectedUserProfile = user,
                currentScreen = Screen.PROFILE
            )
        }
        fetchUserProfilePosts(user.id)
    }

    fun openUserProfileById(userId: Long) {
        val cached = _uiState.value.users.find { it.id == userId }
        if (cached != null) {
            openUserProfile(cached)
        } else {
            viewModelScope.launch {
                val liveUser = VideosApiService.fetchUser(userId)
                if (liveUser != null) {
                    _uiState.update { state ->
                        state.copy(users = state.users + liveUser)
                    }
                    openUserProfile(liveUser)
                } else {
                    val fallback = if (userId == _uiState.value.currentUser.id) _uiState.value.currentUser else User(id = userId, name = "User $userId", username = "@user$userId")
                    openUserProfile(fallback)
                }
            }
        }
    }

    fun openCurrentUserProfile() {
        val current = _uiState.value.currentUser
        _uiState.update { state ->
            state.copy(
                selectedUserProfile = current,
                currentScreen = Screen.PROFILE
            )
        }
        fetchUserProfilePosts(current.id)
    }

    fun toggleVerifyUser(userId: Long) {
        _uiState.update { state ->
            val updatedUsers = state.users.map { u ->
                if (u.id == userId) u.copy(isVerified = !u.isVerified) else u
            }
            val updatedCurrent = if (state.currentUser.id == userId) state.currentUser.copy(isVerified = !state.currentUser.isVerified) else state.currentUser
            val updatedSelected = if (state.selectedUserProfile?.id == userId) state.selectedUserProfile.copy(isVerified = !state.selectedUserProfile.isVerified) else state.selectedUserProfile
            state.copy(users = updatedUsers, currentUser = updatedCurrent, selectedUserProfile = updatedSelected)
        }
    }

    fun suspendUser(userId: Long, duration: String, reason: String) {
        val until = when (duration) {
            "week" -> "In 7 days"
            "month" -> "In 30 days"
            else -> "Indefinitely"
        }
        _uiState.update { state ->
            val updatedUsers = state.users.map { u ->
                if (u.id == userId) u.copy(suspendedUntil = until, suspensionReason = reason) else u
            }
            val updatedSelected = if (state.selectedUserProfile?.id == userId) state.selectedUserProfile.copy(suspendedUntil = until, suspensionReason = reason) else state.selectedUserProfile
            state.copy(users = updatedUsers, selectedUserProfile = updatedSelected)
        }
    }

    fun unsuspendUser(userId: Long) {
        _uiState.update { state ->
            val updatedUsers = state.users.map { u ->
                if (u.id == userId) u.copy(suspendedUntil = null, suspensionReason = null) else u
            }
            val updatedSelected = if (state.selectedUserProfile?.id == userId) state.selectedUserProfile.copy(suspendedUntil = null, suspensionReason = null) else state.selectedUserProfile
            state.copy(users = updatedUsers, selectedUserProfile = updatedSelected)
        }
    }

    fun toggleModeratorRole(userId: Long) {
        _uiState.update { state ->
            val updatedUsers = state.users.map { u ->
                if (u.id == userId) {
                    val nextRole = if (u.role == "moderator") "user" else "moderator"
                    u.copy(role = nextRole)
                } else u
            }
            val updatedSelected = if (state.selectedUserProfile?.id == userId) {
                val nextRole = if (state.selectedUserProfile.role == "moderator") "user" else "moderator"
                state.selectedUserProfile.copy(role = nextRole)
            } else state.selectedUserProfile
            state.copy(users = updatedUsers, selectedUserProfile = updatedSelected)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShowCreatePostDialog(show: Boolean, initialPicker: String? = null) {
        _uiState.update { it.copy(showCreatePostDialog = show, createPostInitialPicker = if (show) initialPicker else null) }
    }

    fun setSelectedPostForComments(post: Post?) {
        _uiState.update { it.copy(selectedPostForComments = post) }
        if (post != null) {
            val pid = post.product?.id ?: post.id
            if (post.product != null || post.postType == "product") {
                viewModelScope.launch {
                    val liveReviews = MarketplaceApiService.fetchProductReviews(pid, _uiState.value.currentUser.id)
                    if (liveReviews.isNotEmpty()) {
                        _uiState.update { state ->
                            val updatedPosts = state.posts.map { p ->
                                if (p.id == post.id) p.copy(comments = liveReviews, commentsCount = liveReviews.size) else p
                            }
                            val updatedSel = if (state.selectedPostForComments?.id == post.id) {
                                state.selectedPostForComments?.copy(comments = liveReviews, commentsCount = liveReviews.size)
                            } else state.selectedPostForComments
                            state.copy(
                                posts = updatedPosts,
                                selectedPostForComments = updatedSel
                            )
                        }
                    }
                }
            } else {
                viewModelScope.launch {
                    val liveComments = VideosApiService.fetchComments(post.id, _uiState.value.currentUser.id)
                    if (liveComments.isNotEmpty()) {
                        _uiState.update { state ->
                            val updatedPosts = state.posts.map { p ->
                                if (p.id == post.id) p.copy(comments = liveComments, commentsCount = liveComments.size) else p
                            }
                            val updatedVideoPosts = state.videoPosts.map { p ->
                                if (p.id == post.id) p.copy(comments = liveComments, commentsCount = liveComments.size) else p
                            }
                            val updatedSel = if (state.selectedPostForComments?.id == post.id) {
                                state.selectedPostForComments?.copy(comments = liveComments, commentsCount = liveComments.size)
                            } else state.selectedPostForComments
                            state.copy(
                                posts = updatedPosts,
                                videoPosts = updatedVideoPosts,
                                selectedPostForComments = updatedSel
                            )
                        }
                    }
                }
            }
        }
    }

    fun setShowSearchDialog(show: Boolean) {
        _uiState.update { it.copy(showSearchDialog = show) }
    }

    fun setShowNotificationsOverlay(show: Boolean) {
        _uiState.update { it.copy(showNotificationsOverlay = show) }
        if (show) markNotificationsAsRead()
    }

    fun setShowCreateEventDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateEventDialog = show) }
    }

    fun setShowCreateProductDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateProductDialog = show) }
    }

    fun setSelectedProductForDetail(product: Product?) {
        _uiState.update { it.copy(selectedProductForDetail = product) }
    }

    fun openProductById(pid: Long) {
        val existing = _uiState.value.products.find { it.id == pid }
        if (existing != null) {
            setSelectedProductForDetail(existing)
        } else {
            viewModelScope.launch {
                val prod = MarketplaceApiService.fetchProductById(pid)
                if (prod != null) {
                    _uiState.update { it.copy(products = listOf(prod) + it.products) }
                    setSelectedProductForDetail(prod)
                }
            }
        }
    }

    fun openImageViewer(url: String?) {
        _uiState.update { it.copy(fullScreenImageViewerUrl = url) }
    }

    fun closeImageViewer() {
        _uiState.update { it.copy(fullScreenImageViewerUrl = null) }
    }

    fun setAppLoading(loading: Boolean) {
        _uiState.update { it.copy(isAppLoading = loading) }
    }

    fun refreshStories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isStoriesLoading = true) }
            val viewerId = _uiState.value.currentUser.id
            val liveStories = StoriesApiService.fetchStories(viewerId)
            _uiState.update { it.copy(stories = liveStories, isStoriesLoading = false) }
        }
    }

    fun setSelectedStory(story: Story?) {
        _uiState.update { it.copy(selectedStory = story) }
        if (story != null) {
            val viewerId = _uiState.value.currentUser.id
            viewModelScope.launch {
                // 1. Record view
                StoriesApiService.recordView(story.id, viewerId)
                // 2. Fetch reactions
                val reactionsData = StoriesApiService.fetchReactions(story.id, viewerId)
                if (reactionsData != null) {
                    _uiState.update { state ->
                        state.copy(storyReactions = state.storyReactions + (story.id to reactionsData))
                    }
                }
                // 3. Fetch comments
                val comments = StoriesApiService.fetchComments(story.id, viewerId)
                _uiState.update { state ->
                    state.copy(storyComments = state.storyComments + (story.id to comments))
                }
                // 4. If owner, fetch viewers and analytics
                if (story.author.id == viewerId) {
                    val viewers = StoriesApiService.fetchViewers(story.id)
                    val analytics = StoriesApiService.fetchAnalytics(story.id)
                    _uiState.update { state ->
                        val updatedViewers = state.storyViewers + (story.id to viewers)
                        val updatedAnalytics = if (analytics != null) state.storyAnalytics + (story.id to analytics) else state.storyAnalytics
                        state.copy(storyViewers = updatedViewers, storyAnalytics = updatedAnalytics)
                    }
                }
            }
        }
    }

    fun setShowCreateStoryDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateStoryDialog = show) }
    }

    fun createStory(
        mediaUrl: String,
        type: String = "image",
        textContent: String = "",
        backgroundStyle: String = "linear-gradient(45deg, #1877F2, #0055FF)",
        musicTitle: String? = null,
        musicUrl: String? = null,
        duration: Int = 90
    ) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            _uiState.update { it.copy(isStoriesLoading = true, showCreateStoryDialog = false) }
            val mediaUrls = if (mediaUrl.isNotBlank()) listOf(mediaUrl) else emptyList()
            val created = StoriesApiService.createStory(
                userId = currentUserId,
                type = type,
                textContent = textContent.takeIf { it.isNotBlank() },
                mediaUrl = mediaUrl.takeIf { it.isNotBlank() },
                mediaUrls = mediaUrls,
                backgroundStyle = backgroundStyle.takeIf { it.isNotBlank() },
                musicUrl = musicUrl,
                musicTitle = musicTitle,
                duration = duration
            )
            if (created != null) {
                _uiState.update { state ->
                    state.copy(
                        stories = listOf(created) + state.stories.filter { it.id != created.id },
                        isStoriesLoading = false
                    )
                }
            } else {
                refreshStories()
            }
        }
    }

    fun deleteStory(storyId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            _uiState.update { state ->
                val updated = state.stories.filter { it.id != storyId }
                state.copy(
                    stories = updated,
                    selectedStory = if (state.selectedStory?.id == storyId) null else state.selectedStory
                )
            }
            StoriesApiService.deleteStory(storyId, currentUserId)
        }
    }

    fun reactToStory(storyId: Long, reaction: String) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                val updated = state.stories.map { s ->
                    if (s.id == storyId) {
                        val isSame = s.myReaction == reaction
                        s.copy(
                            myReaction = if (isSame) null else reaction,
                            reactionsCount = if (isSame) maxOf(0, s.reactionsCount - 1) else if (s.myReaction == null) s.reactionsCount + 1 else s.reactionsCount,
                            isLiked = if (isSame) false else true
                        )
                    } else s
                }
                state.copy(
                    stories = updated,
                    selectedStory = state.selectedStory?.let { sel ->
                        if (sel.id == storyId) {
                            val isSame = sel.myReaction == reaction
                            sel.copy(
                                myReaction = if (isSame) null else reaction,
                                reactionsCount = if (isSame) maxOf(0, sel.reactionsCount - 1) else if (sel.myReaction == null) sel.reactionsCount + 1 else sel.reactionsCount,
                                isLiked = if (isSame) false else true
                            )
                        } else sel
                    }
                )
            }
            StoriesApiService.reactToStory(storyId, currentUserId, reaction)
            val updatedReactions = StoriesApiService.fetchReactions(storyId, currentUserId)
            if (updatedReactions != null) {
                _uiState.update { state ->
                    state.copy(storyReactions = state.storyReactions + (storyId to updatedReactions))
                }
            }
        }
    }

    fun fetchStoryViewers(storyId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStoryViewersLoading = true) }
            val viewers = StoriesApiService.fetchViewers(storyId)
            _uiState.update { state ->
                state.copy(
                    storyViewers = state.storyViewers + (storyId to viewers),
                    isStoryViewersLoading = false
                )
            }
        }
    }

    fun fetchStoryComments(storyId: Long) {
        val viewerId = _uiState.value.currentUser.id
        viewModelScope.launch {
            _uiState.update { it.copy(isStoryCommentsLoading = true) }
            val comments = StoriesApiService.fetchComments(storyId, viewerId)
            _uiState.update { state ->
                state.copy(
                    storyComments = state.storyComments + (storyId to comments),
                    isStoryCommentsLoading = false
                )
            }
        }
    }

    fun addStoryComment(storyId: Long, content: String, imageUrl: String? = null, parentId: Long? = null) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            val created = StoriesApiService.addComment(storyId, currentUserId, content, imageUrl, parentId)
            if (created != null) {
                _uiState.update { state ->
                    val existing = state.storyComments[storyId] ?: emptyList()
                    val updated = existing + created
                    state.copy(storyComments = state.storyComments + (storyId to updated))
                }
            } else {
                fetchStoryComments(storyId)
            }
        }
    }

    fun toggleHideStoryComment(storyId: Long, commentId: Long, hide: Boolean) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            _uiState.update { state ->
                val existing = state.storyComments[storyId] ?: emptyList()
                val updated = existing.map { if (it.id == commentId) it.copy(isHidden = hide) else it }
                state.copy(storyComments = state.storyComments + (storyId to updated))
            }
            StoriesApiService.toggleHideComment(storyId, currentUserId, commentId, if (hide) "hide" else "unhide")
        }
    }

    fun deleteStoryComment(storyId: Long, commentId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            _uiState.update { state ->
                val existing = state.storyComments[storyId] ?: emptyList()
                val updated = existing.filter { it.id != commentId && it.parentId != commentId }
                state.copy(storyComments = state.storyComments + (storyId to updated))
            }
            StoriesApiService.deleteComment(storyId, commentId, currentUserId)
        }
    }

    fun likeStoryComment(commentId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            StoriesApiService.likeComment(commentId, currentUserId)
        }
    }

    fun replyToStory(storyId: Long, text: String) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            StoriesApiService.replyToStory(storyId, currentUserId, text)
        }
    }

    fun shareStory(storyId: Long, destination: String = "feed", message: String = "") {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            StoriesApiService.shareStory(storyId, currentUserId, destination, message)
        }
    }

    fun setShowMenuOverlay(show: Boolean) {
        _uiState.update { it.copy(showMenuOverlay = show) }
    }

    // ==========================================
    // F-MUSIC AUDIO PLAYER & CATALOG METHODS
    // ==========================================

    private var playbackJob: Job? = null
    private var downloadJob: Job? = null

    fun playTrack(track: AudioTrack) {
        _uiState.update { state ->
            val trackKey = "music:${track.id}"
            val currentPlays = state.trackPlays[trackKey] ?: (state.songs.find { it.id == track.id }?.stats?.plays ?: 0L)
            val isMyTrack = state.currentUser.id == track.uploaderId
            state.copy(
                currentAudioTrack = track,
                isPlayingAudio = true,
                audioPositionSeconds = 0,
                audioDurationSeconds = if (track.duration > 0) track.duration else 180,
                trackPlays = state.trackPlays + (trackKey to (currentPlays + 1L)),
                myTotalPlays = if (isMyTrack) state.myTotalPlays + 1L else state.myTotalPlays
            )
        }
        startPlaybackTicker()
    }

    private fun startPlaybackTicker() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                if (!state.isPlayingAudio || state.currentAudioTrack == null) break

                if (state.audioPositionSeconds >= state.audioDurationSeconds) {
                    if (state.isRepeatEnabled) {
                        _uiState.update { it.copy(audioPositionSeconds = 0) }
                    } else {
                        nextTrack()
                    }
                } else {
                    _uiState.update { it.copy(audioPositionSeconds = it.audioPositionSeconds + 1) }
                }
            }
        }
    }

    fun togglePlayAudio() {
        val currentlyPlaying = _uiState.value.isPlayingAudio
        if (currentlyPlaying) {
            playbackJob?.cancel()
            _uiState.update { it.copy(isPlayingAudio = false) }
        } else {
            if (_uiState.value.currentAudioTrack != null) {
                _uiState.update { it.copy(isPlayingAudio = true) }
                startPlaybackTicker()
            }
        }
    }

    fun seekAudio(seconds: Int) {
        _uiState.update { state ->
            state.copy(audioPositionSeconds = seconds.coerceIn(0, state.audioDurationSeconds))
        }
    }

    fun nextTrack() {
        val state = _uiState.value
        val songs = state.songs
        if (songs.isEmpty()) return

        val currentId = state.currentAudioTrack?.id
        val currentIndex = songs.indexOfFirst { it.id == currentId }

        val nextIndex = if (state.isShuffleEnabled && songs.size > 1) {
            var rand = (songs.indices).random()
            while (rand == currentIndex) {
                rand = (songs.indices).random()
            }
            rand
        } else {
            if (currentIndex >= 0) (currentIndex + 1) % songs.size else 0
        }

        val nextSong = songs[nextIndex]
        val audioTrack = AudioTrack(
            id = nextSong.id,
            title = nextSong.title,
            artist = nextSong.artist,
            duration = nextSong.durationSeconds,
            url = nextSong.audioUrl,
            uploaderId = nextSong.uploaderId,
            cover = nextSong.cover,
            type = "music",
            isVerified = nextSong.isVerified,
            likesCount = nextSong.stats.likes
        )
        playTrack(audioTrack)
    }

    fun prevTrack() {
        val state = _uiState.value
        if (state.audioPositionSeconds > 3) {
            _uiState.update { it.copy(audioPositionSeconds = 0) }
            return
        }

        val songs = state.songs
        if (songs.isEmpty()) return

        val currentId = state.currentAudioTrack?.id
        val currentIndex = songs.indexOfFirst { it.id == currentId }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else songs.size - 1

        val prevSong = songs[prevIndex]
        val audioTrack = AudioTrack(
            id = prevSong.id,
            title = prevSong.title,
            artist = prevSong.artist,
            duration = prevSong.durationSeconds,
            url = prevSong.audioUrl,
            uploaderId = prevSong.uploaderId,
            cover = prevSong.cover,
            type = "music",
            isVerified = prevSong.isVerified,
            likesCount = prevSong.stats.likes
        )
        playTrack(audioTrack)
    }

    fun stopAudio() {
        playbackJob?.cancel()
        _uiState.update { it.copy(isPlayingAudio = false, audioPositionSeconds = 0) }
    }

    fun closeAudioPlayer() {
        playbackJob?.cancel()
        _uiState.update {
            it.copy(
                currentAudioTrack = null,
                isPlayingAudio = false,
                audioPositionSeconds = 0,
                isPlayerExpanded = false
            )
        }
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isPlayerExpanded = expanded) }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun toggleRepeat() {
        _uiState.update { it.copy(isRepeatEnabled = !it.isRepeatEnabled) }
    }

    fun setAudioVolume(vol: Float) {
        _uiState.update { it.copy(audioVolume = vol.coerceIn(0f, 1f)) }
    }

    fun toggleLikeTrack(trackId: String, type: String = "music") {
        _uiState.update { state ->
            val key = "$type:$trackId"
            val isLiked = key in state.likedTrackKeys
            val updatedKeys = if (isLiked) state.likedTrackKeys - key else state.likedTrackKeys + key

            val updatedSongs = state.songs.map { s ->
                if (s.id == trackId) {
                    val newLikes = if (isLiked) maxOf(0L, s.stats.likes - 1) else s.stats.likes + 1
                    s.copy(stats = s.stats.copy(likes = newLikes))
                } else s
            }

            val updatedTrack = state.currentAudioTrack?.let { curr ->
                if (curr.id == trackId) {
                    curr.copy(likesCount = if (isLiked) maxOf(0L, curr.likesCount - 1) else curr.likesCount + 1)
                } else curr
            }

            state.copy(
                likedTrackKeys = updatedKeys,
                songs = updatedSongs,
                currentAudioTrack = updatedTrack
            )
        }
    }

    fun downloadTrack(trackId: String) {
        val state = _uiState.value
        val trackTitle = state.songs.find { it.id == trackId }?.title
            ?: state.currentAudioTrack?.takeIf { it.id == trackId }?.title
            ?: "Track"

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update { it.copy(downloadingTrackId = trackId, downloadProgress = 15, downloadToast = "Starting download...") }
            delay(400)
            _uiState.update { it.copy(downloadProgress = 45) }
            delay(400)
            _uiState.update { it.copy(downloadProgress = 85) }
            delay(350)
            _uiState.update { it.copy(downloadProgress = 100) }
            delay(300)
            _uiState.update {
                it.copy(
                    downloadingTrackId = null,
                    downloadProgress = 0,
                    downloadedTrackIds = it.downloadedTrackIds + trackId,
                    downloadToast = "Downloaded \"$trackTitle\""
                )
            }
            delay(3000)
            _uiState.update { it.copy(downloadToast = null) }
        }
    }

    fun dismissDownloadToast() {
        _uiState.update { it.copy(downloadToast = null) }
    }

    fun openSongDiscussions(track: AudioTrack) {
        _uiState.update { it.copy(activeDiscussionTrack = track) }
    }

    fun closeSongDiscussions() {
        _uiState.update { it.copy(activeDiscussionTrack = null) }
    }

    fun addSongComment(songId: String, text: String) {
        if (text.isBlank()) return
        val currentUser = _uiState.value.currentUser
        val newComment = SongComment(
            id = System.currentTimeMillis(),
            songId = songId,
            userId = currentUser.id,
            userName = currentUser.name,
            userAvatar = currentUser.profileImageUrl,
            text = text.trim(),
            createdAt = System.currentTimeMillis()
        )
        _uiState.update { state ->
            val existing = state.songComments[songId] ?: emptyList()
            state.copy(
                songComments = state.songComments + (songId to (listOf(newComment) + existing))
            )
        }
    }

    fun toggleHideSongComment(songId: String, commentId: Long) {
        _uiState.update { state ->
            val list = state.songComments[songId] ?: emptyList()
            val updated = list.map { c ->
                if (c.id == commentId) c.copy(isHidden = !c.isHidden) else c
            }
            state.copy(songComments = state.songComments + (songId to updated))
        }
    }

    fun deleteSongComment(songId: String, commentId: Long) {
        _uiState.update { state ->
            val list = state.songComments[songId] ?: emptyList()
            state.copy(songComments = state.songComments + (songId to list.filter { it.id != commentId }))
        }
    }

    fun openShareTrack(track: AudioTrack) {
        _uiState.update { it.copy(activeShareTrack = track) }
    }

    fun closeShareTrack() {
        _uiState.update { it.copy(activeShareTrack = null) }
    }

    fun uploadSong(
        title: String,
        artist: String,
        genre: String,
        album: String,
        coverUrl: String,
        audioUrl: String,
        duration: String = "3:15"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(songUploadProgress = 20, songUploadStatus = "Uploading audio...") }
            delay(400)
            _uiState.update { it.copy(songUploadProgress = 60, songUploadStatus = "Compressing artwork...") }
            delay(400)
            _uiState.update { it.copy(songUploadProgress = 90, songUploadStatus = "Publishing to catalog...") }
            delay(400)

            val newSong = Song(
                id = "song_${System.currentTimeMillis()}",
                title = title.ifBlank { "Untitled Track" },
                artist = artist.ifBlank { _uiState.value.currentUser.name },
                cover = coverUrl.ifBlank { "https://media.unera.social/task_01kftb3024ed7bm84gy6j485fh_1769336848_img_0.webp" },
                audioUrl = audioUrl.ifBlank { "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" },
                duration = duration,
                durationSeconds = 195,
                uploaderId = _uiState.value.currentUser.id,
                uploadDate = "Just now",
                genre = genre.ifBlank { "Afrobeats" },
                album = album.ifBlank { "Single" },
                isVerified = _uiState.value.currentUser.isVerified,
                stats = SongStats(plays = 1L, likes = 0L)
            )

            _uiState.update { state ->
                state.copy(
                    songs = listOf(newSong) + state.songs,
                    songUploadProgress = 100,
                    songUploadStatus = "Track published!"
                )
            }
            delay(1200)
            _uiState.update { it.copy(songUploadProgress = null, songUploadStatus = null) }
        }
    }

    fun deleteUploadedSong(songId: String) {
        _uiState.update { state ->
            val updated = state.songs.filter { it.id != songId }
            state.copy(
                songs = updated,
                currentAudioTrack = if (state.currentAudioTrack?.id == songId) null else state.currentAudioTrack
            )
        }
    }

    // ==========================================
    // GROUPS & COMMUNITY METHODS
    // ==========================================

    // ==========================================
    // GROUPS & COMMUNITY METHODS (LIVE API)
    // ==========================================

    fun refreshGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGroupsLoading = true) }
            val viewerId = _uiState.value.currentUser.id
            val liveGroups = GroupsApiService.fetchGroups(viewerId)
            val suggestions = GroupsApiService.fetchGroupSuggestions(viewerId)
            val invites = GroupsApiService.fetchGroupInvites(viewerId)
            _uiState.update { state ->
                state.copy(
                    groups = liveGroups,
                    groupSuggestions = suggestions,
                    groupInvites = invites,
                    isGroupsLoading = false
                )
            }
            val currentActiveId = _uiState.value.activeGroupId
            if (currentActiveId != null) {
                loadGroupData(currentActiveId)
            }
        }
    }

    fun loadGroupData(groupId: Long) {
        val viewerId = _uiState.value.currentUser.id
        viewModelScope.launch {
            // 1. Group Details & member IDs
            val (updatedGroup, memberIds) = GroupsApiService.fetchGroupDetails(groupId, viewerId)
            if (updatedGroup != null) {
                val fullGroup = if (memberIds.isNotEmpty()) updatedGroup.copy(members = memberIds) else updatedGroup
                _uiState.update { state ->
                    val existing = state.groups.map { if (it.id == groupId) fullGroup else it }
                    val finalGroups = if (existing.none { it.id == groupId }) listOf(fullGroup) + existing else existing
                    state.copy(groups = finalGroups)
                }
            }
            // 2. Posts
            _uiState.update { it.copy(isGroupPostsLoading = true) }
            val posts = GroupsApiService.fetchGroupPosts(groupId, viewerId)
            _uiState.update { state ->
                val otherPosts = state.groupPosts.filter { it.groupId != groupId }
                state.copy(groupPosts = posts + otherPosts, isGroupPostsLoading = false)
            }
            // 3. Members
            val members = GroupsApiService.fetchGroupMembers(groupId)
            _uiState.update { state ->
                state.copy(groupMembers = state.groupMembers + (groupId to members))
            }
            // 4. Events
            val events = GroupsApiService.fetchGroupEvents(groupId, viewerId)
            _uiState.update { state ->
                state.copy(groupEvents = state.groupEvents + (groupId to events))
            }
        }
    }

    fun setActiveGroupId(groupId: Long?) {
        _uiState.update { it.copy(activeGroupId = groupId) }
        if (groupId != null) {
            loadGroupData(groupId)
        }
    }

    fun createGroup(
        name: String,
        description: String,
        type: String, // "public" or "private"
        category: String, // "general", "recruitment", "buy_sell"
        profileImage: String,
        coverImage: String
    ) {
        viewModelScope.launch {
            val created = GroupsApiService.createGroup(
                name = name.ifBlank { "New Community" },
                description = description,
                type = type,
                category = category,
                profileImage = profileImage,
                coverImage = coverImage,
                adminId = _uiState.value.currentUser.id
            )
            if (created != null) {
                _uiState.update { state ->
                    state.copy(
                        groups = listOf(created) + state.groups,
                        activeGroupId = created.id
                    )
                }
                loadGroupData(created.id)
            } else {
                val newId = System.currentTimeMillis()
                val fallbackGroup = Group(
                    id = newId,
                    adminId = _uiState.value.currentUser.id,
                    name = name.ifBlank { "New Community" },
                    description = description,
                    isPrivate = type == "private",
                    category = category,
                    coverImageUrl = coverImage.ifBlank { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1500&q=80" },
                    profileImageUrl = profileImage.ifBlank { "https://ui-avatars.com/api/?name=${name}&background=random" },
                    membersCount = 1,
                    isMember = true,
                    memberPostingAllowed = true,
                    members = listOf(_uiState.value.currentUser.id)
                )
                _uiState.update { state ->
                    state.copy(
                        groups = listOf(fallbackGroup) + state.groups,
                        activeGroupId = newId
                    )
                }
            }
        }
    }

    fun joinGroup(groupId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        _uiState.update { state ->
            val updatedGroups = state.groups.map { group ->
                if (group.id == groupId) {
                    val newMembers = if (group.members.contains(currentUserId)) group.members else group.members + currentUserId
                    group.copy(
                        isMember = true,
                        members = newMembers,
                        membersCount = newMembers.size.coerceAtLeast(group.membersCount + 1)
                    )
                } else group
            }
            state.copy(groups = updatedGroups)
        }
        viewModelScope.launch {
            GroupsApiService.joinGroup(groupId, currentUserId)
            loadGroupData(groupId)
        }
    }

    fun leaveGroup(groupId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        _uiState.update { state ->
            val updatedGroups = state.groups.map { group ->
                if (group.id == groupId) {
                    val newMembers = group.members.filter { it != currentUserId }
                    group.copy(
                        isMember = false,
                        members = newMembers,
                        membersCount = (group.membersCount - 1).coerceAtLeast(0)
                    )
                } else group
            }
            state.copy(groups = updatedGroups)
        }
        viewModelScope.launch {
            GroupsApiService.leaveOrRemoveMember(groupId, currentUserId, currentUserId)
            loadGroupData(groupId)
        }
    }

    fun deleteGroup(groupId: Long) {
        _uiState.update { state ->
            state.copy(
                groups = state.groups.filter { it.id != groupId },
                groupPosts = state.groupPosts.filter { it.groupId != groupId },
                activeGroupId = if (state.activeGroupId == groupId) null else state.activeGroupId
            )
        }
        viewModelScope.launch {
            GroupsApiService.deleteGroup(groupId)
        }
    }

    fun updateGroupSettings(groupId: Long, name: String, description: String, memberPostingAllowed: Boolean) {
        _uiState.update { state ->
            val updated = state.groups.map { g ->
                if (g.id == groupId) {
                    g.copy(
                        name = name.ifBlank { g.name },
                        description = description,
                        memberPostingAllowed = memberPostingAllowed
                    )
                } else g
            }
            state.copy(groups = updated)
        }
        viewModelScope.launch {
            GroupsApiService.updateGroup(
                groupId = groupId,
                name = name,
                description = description,
                memberPostingAllowed = memberPostingAllowed
            )
        }
    }

    fun updateGroupImage(groupId: Long, isCover: Boolean, imageUrl: String) {
        _uiState.update { state ->
            val updated = state.groups.map { g ->
                if (g.id == groupId) {
                    if (isCover) g.copy(coverImageUrl = imageUrl) else g.copy(profileImageUrl = imageUrl)
                } else g
            }
            state.copy(groups = updated)
        }
        viewModelScope.launch {
            GroupsApiService.updateGroup(
                groupId = groupId,
                coverImage = if (isCover) imageUrl else null,
                profileImage = if (!isCover) imageUrl else null
            )
        }
    }

    fun postToGroup(
        groupId: Long,
        content: String,
        imageUrls: List<String> = emptyList(),
        price: Double? = null,
        currency: String = "USD",
        condition: String? = null,
        location: String? = null,
        status: String = "available",
        jobTitle: String? = null,
        company: String? = null,
        jobType: String? = null,
        salary: String? = null,
        applicationType: String? = null,
        applicationValue: String? = null
    ) {
        viewModelScope.launch {
            val createdPost = GroupsApiService.createGroupPost(
                groupId = groupId,
                userId = _uiState.value.currentUser.id,
                content = content,
                mediaUrls = imageUrls,
                price = price,
                currency = currency,
                condition = condition,
                location = location,
                status = status,
                jobTitle = jobTitle,
                company = company,
                jobType = jobType,
                salary = salary,
                applicationType = applicationType,
                applicationValue = applicationValue
            )
            if (createdPost != null) {
                _uiState.update { state ->
                    state.copy(groupPosts = listOf(createdPost) + state.groupPosts)
                }
            } else {
                val newPost = GroupPost(
                    id = System.currentTimeMillis(),
                    groupId = groupId,
                    author = _uiState.value.currentUser,
                    content = content,
                    imageUrls = imageUrls,
                    createdAt = "Just now",
                    likesCount = 0,
                    commentsCount = 0,
                    sharesCount = 0,
                    isLiked = false,
                    price = price,
                    currency = currency,
                    condition = condition,
                    location = location,
                    status = status,
                    jobTitle = jobTitle,
                    company = company,
                    jobType = jobType,
                    salary = salary,
                    applicationType = applicationType,
                    applicationValue = applicationValue
                )
                _uiState.update { state ->
                    state.copy(groupPosts = listOf(newPost) + state.groupPosts)
                }
            }
        }
    }

    fun toggleLikeGroupPost(postId: Long, reaction: String = "like") {
        val currentUserId = _uiState.value.currentUser.id
        _uiState.update { state ->
            val updated = state.groupPosts.map { post ->
                if (post.id == postId) {
                    val wasLiked = post.isLiked
                    val newCount = if (wasLiked) (post.likesCount - 1).coerceAtLeast(0) else post.likesCount + 1
                    post.copy(
                        isLiked = !wasLiked,
                        likesCount = newCount,
                        myReaction = if (wasLiked) null else reaction
                    )
                } else post
            }
            state.copy(groupPosts = updated)
        }
        viewModelScope.launch {
            GroupsApiService.likeGroupPost(postId, currentUserId, reaction)
        }
    }

    fun addGroupPostComment(postId: Long, text: String) {
        if (text.isBlank()) return
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            val createdComment = GroupsApiService.addGroupPostComment(postId, currentUserId, text)
            val newComment = createdComment ?: Comment(
                id = System.currentTimeMillis(),
                author = _uiState.value.currentUser,
                text = text,
                timestamp = "Just now",
                likesCount = 0,
                isLiked = false
            )
            _uiState.update { state ->
                val updated = state.groupPosts.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            comments = post.comments + newComment,
                            commentsCount = post.commentsCount + 1
                        )
                    } else post
                }
                state.copy(groupPosts = updated)
            }
        }
    }

    fun deleteGroupPost(postId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        _uiState.update { state ->
            state.copy(groupPosts = state.groupPosts.filter { it.id != postId })
        }
        viewModelScope.launch {
            GroupsApiService.deleteGroupPost(postId, currentUserId)
        }
    }

    fun shareGroupPost(postId: Long) {
        _uiState.update { state ->
            val updated = state.groupPosts.map { post ->
                if (post.id == postId) post.copy(sharesCount = post.sharesCount + 1) else post
            }
            state.copy(groupPosts = updated)
        }
    }

    fun inviteUserToGroup(groupId: Long, userId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        viewModelScope.launch {
            GroupsApiService.sendGroupInvite(groupId, currentUserId, listOf(userId))
        }
    }

    fun acceptGroupInvite(inviteId: Long, groupId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        joinGroup(groupId)
        _uiState.update { state ->
            state.copy(groupInvites = state.groupInvites.filter { it.id != inviteId })
        }
        viewModelScope.launch {
            GroupsApiService.respondGroupInvite(inviteId, groupId, currentUserId, "accepted")
        }
    }

    fun declineGroupInvite(inviteId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        _uiState.update { state ->
            state.copy(groupInvites = state.groupInvites.filter { it.id != inviteId })
        }
        viewModelScope.launch {
            GroupsApiService.respondGroupInvite(inviteId, 0L, currentUserId, "declined")
        }
    }

    fun toggleMemberPosting(groupId: Long, userId: Long, disabled: Boolean) {
        val currentUserId = _uiState.value.currentUser.id
        _uiState.update { state ->
            val currentGroupDisabled = state.groupPostingDisabledOverrides[groupId] ?: emptySet()
            val newGroupDisabled = if (disabled) currentGroupDisabled + userId else currentGroupDisabled - userId
            state.copy(
                groupPostingDisabledOverrides = state.groupPostingDisabledOverrides + (groupId to newGroupDisabled)
            )
        }
        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("group_id", groupId)
                put("user_id", userId)
                put("disabled", disabled)
                put("actor_id", currentUserId)
            }
            GroupsApiService.patchMemberAction("toggle-posting", payload)
            val members = GroupsApiService.fetchGroupMembers(groupId)
            _uiState.update { it.copy(groupMembers = it.groupMembers + (groupId to members)) }
        }
    }

    fun setMemberRole(groupId: Long, userId: Long, role: String) {
        val currentUserId = _uiState.value.currentUser.id
        _uiState.update { state ->
            val currentGroupRoles = state.groupMembersOverrides[groupId] ?: emptyMap()
            state.copy(
                groupMembersOverrides = state.groupMembersOverrides + (groupId to (currentGroupRoles + (userId to role)))
            )
        }
        viewModelScope.launch {
            val action = when (role) {
                "admin" -> "make-admin"
                "moderator" -> "make-moderator"
                else -> "remove-admin"
            }
            val payload = JSONObject().apply {
                put("group_id", groupId)
                put("user_id", userId)
                put("actor_id", currentUserId)
            }
            GroupsApiService.patchMemberAction(action, payload)
            val members = GroupsApiService.fetchGroupMembers(groupId)
            _uiState.update { it.copy(groupMembers = it.groupMembers + (groupId to members)) }
        }
    }

    fun removeGroupMember(groupId: Long, userId: Long) {
        val currentUserId = _uiState.value.currentUser.id
        _uiState.update { state ->
            val updatedGroups = state.groups.map { group ->
                if (group.id == groupId) {
                    val newMembers = group.members.filter { it != userId }
                    group.copy(
                        members = newMembers,
                        membersCount = (group.membersCount - 1).coerceAtLeast(0)
                    )
                } else group
            }
            state.copy(groups = updatedGroups)
        }
        viewModelScope.launch {
            GroupsApiService.leaveOrRemoveMember(groupId, userId, currentUserId)
            val members = GroupsApiService.fetchGroupMembers(groupId)
            _uiState.update { it.copy(groupMembers = it.groupMembers + (groupId to members)) }
        }
    }

    // ==========================================
    // ADS MANAGER ACTIONS
    // ==========================================

    fun pauseAdCampaign(id: Long) {
        _uiState.update { state ->
            val updated = state.adCampaigns.map {
                if (it.id == id) it.copy(status = "paused") else it
            }
            state.copy(adCampaigns = updated)
        }
    }

    fun resumeAdCampaign(id: Long) {
        _uiState.update { state ->
            val updated = state.adCampaigns.map {
                if (it.id == id) it.copy(status = "active") else it
            }
            state.copy(adCampaigns = updated)
        }
    }

    fun deleteAdCampaign(id: Long) {
        _uiState.update { state ->
            val updated = state.adCampaigns.filter { it.id != id }
            state.copy(adCampaigns = updated)
        }
    }

    fun createAdCampaign(
        postId: Long?,
        name: String,
        type: String,
        mediaUrl: String?,
        mediaUrls: List<String>,
        description: String,
        link: String?,
        phone: String?,
        email: String?,
        cta: String,
        location: String,
        budget: Int,
        days: Int
    ) {
        val newCampaign = AdCampaign(
            id = System.currentTimeMillis(),
            name = name.ifBlank { "Campaign #${System.currentTimeMillis() % 1000}" },
            type = type,
            status = "active",
            mediaUrl = mediaUrl,
            mediaUrls = mediaUrls,
            description = description,
            cta = cta,
            link = link,
            phone = phone,
            email = email,
            location = location.ifBlank { "Global" },
            budget = budget,
            days = days,
            startDate = "2026-03-11",
            endDate = "2026-03-${11 + days}",
            impressions = 0,
            views = 0,
            clicks = 0,
            postId = postId
        )
        _uiState.update { state ->
            state.copy(
                adCampaigns = listOf(newCampaign) + state.adCampaigns,
                activeAdCampaignToBoostPost = null
            )
        }
    }

    fun setAdCampaignToBoostPost(post: Post?) {
        _uiState.update { it.copy(activeAdCampaignToBoostPost = post) }
    }

    // ==========================================
    // AUTHENTICATION ACTIONS
    // ==========================================

    fun openAuth(mode: String = "login") {
        _uiState.update { it.copy(showAuthModal = true, authMode = mode) }
    }

    fun closeAuth() {
        _uiState.update { it.copy(showAuthModal = false) }
    }

    fun setAuthMode(mode: String) {
        _uiState.update { it.copy(authMode = mode) }
    }

    fun continueAsGuest() {
        _uiState.update {
            it.copy(
                isGuest = true,
                showAuthModal = false,
                currentScreen = Screen.FEED
            )
        }
        refreshFeed()
    }

    fun loginUser(email: String, pass: String): Boolean {
        val matchedUser = _uiState.value.users.find { it.email.equals(email.trim(), ignoreCase = true) }
            ?: _uiState.value.currentUser.copy(email = email.trim())

        _uiState.update { state ->
            state.copy(
                currentUser = matchedUser,
                isLoggedIn = true,
                isGuest = false,
                showAuthModal = false,
                currentScreen = Screen.FEED
            )
        }
        refreshFeed()
        return true
    }

    fun registerUser(
        username: String,
        email: String,
        pass: String,
        nationality: String,
        location: String,
        birthDate: String,
        gender: String
    ): Boolean {
        val newUser = User(
            id = System.currentTimeMillis(),
            name = username.trim(),
            username = username.lowercase().replace(" ", "_"),
            email = email.trim(),
            profileImageUrl = "https://ui-avatars.com/api/?name=${username.replace(" ", "+")}&background=1877F2&color=fff&bold=true",
            location = location.ifBlank { "Dar es Salaam, Tanzania" },
            gender = gender,
            birthDate = birthDate,
            isOnline = true
        )

        _uiState.update { state ->
            state.copy(
                currentUser = newUser,
                users = listOf(newUser) + state.users,
                isLoggedIn = true,
                isGuest = false,
                showAuthModal = false,
                currentScreen = Screen.FEED
            )
        }
        refreshFeed()
        return true
    }

    fun logoutUser() {
        _uiState.update { state ->
            state.copy(
                isLoggedIn = false,
                showAuthModal = true,
                authMode = "login"
            )
        }
        refreshFeed()
    }
}
