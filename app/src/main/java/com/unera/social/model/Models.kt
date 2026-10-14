package com.unera.social.model

enum class Screen {
    FEED,
    REELS,
    MARKETPLACE,
    EVENTS,
    MESSAGES,
    PROFILE,
    DISCOVER_PEOPLE,
    BIRTHDAYS,
    MEMORIES,
    SAVED_POSTS,
    STORY_FEED,
    MUSIC,
    GROUPS,
    SETTINGS,
    ADS_MANAGER,
    AUTH,
    HELP_SUPPORT,
    SEARCH,
    TERMS
}

enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    EVENT,
    MESSAGE,
    REACTION,
    SHARE,
    GROUP_INVITE,
    BIRTHDAY,
    MENTION,
    MUSIC
}

data class Group(
    val id: Long,
    val adminId: Long = 101L,
    val name: String,
    val description: String = "",
    val coverImageUrl: String = "",
    val profileImageUrl: String = "",
    val membersCount: Int = 0,
    val isMember: Boolean = false,
    val isPrivate: Boolean = false,
    val category: String = "general", // "general", "recruitment", "buy_sell"
    val memberPostingAllowed: Boolean = true,
    val createdAt: String = "2026-01-15T10:00:00Z",
    val members: List<Long> = emptyList()
)

data class GroupPost(
    val id: Long,
    val groupId: Long,
    val author: User,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val videoUrl: String? = null,
    val createdAt: String = "Just now",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val myReaction: String? = null,
    // Buy & Sell fields
    val price: Double? = null,
    val currency: String = "USD",
    val condition: String? = null,
    val location: String? = null,
    val status: String = "available", // "available", "pending", "sold"
    // Careers / Recruitment fields
    val jobTitle: String? = null,
    val company: String? = null,
    val jobType: String? = null, // "Full-time", "Part-time", "Contract", "Internship", "Freelance"
    val salary: String? = null,
    val applicationType: String? = null, // "email", "link"
    val applicationValue: String? = null,
    val expiryDate: String? = null,
    val comments: List<Comment> = emptyList()
)

data class GroupInvite(
    val id: Long,
    val groupId: Long,
    val group: Group,
    val inviter: User,
    val inviteeId: Long,
    val createdAt: String = "Just now"
)

data class User(
    val id: Long,
    val name: String,
    val username: String,
    val profileImageUrl: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=800",
    val coverImageUrl: String = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1200",
    val bio: String = "",
    val location: String = "Dar es Salaam, Tanzania",
    val work: String = "Software Engineer at TechCorp",
    val education: String = "University of Dar es Salaam",
    val website: String = "https://feathered.social",
    val followersCount: Int = 1240,
    val followingCount: Int = 412,
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val birthDate: String? = null,
    val isFollowing: Boolean = false,
    val role: String = "user", // "user", "creator", "moderator", "admin"
    val suspendedUntil: String? = null,
    val suspensionReason: String? = null,
    val isDeleted: Boolean = false,
    val email: String = "jastin@feathered.social",
    val gender: String = "Male"
)

data class UserReaction(
    val userId: Long,
    val userName: String = "",
    val userAvatar: String = "",
    val type: String = "like"
)

data class LinkPreview(
    val url: String,
    val title: String,
    val description: String = "",
    val image: String? = null,
    val domain: String = ""
)

data class Comment(
    val id: Long,
    val author: User,
    val text: String,
    val timestamp: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val imageUrl: String? = null,
    val parentCommentId: Long? = null,
    val replies: List<Comment> = emptyList()
)

data class MediaMetaItem(
    val original: String? = null,
    val resolution1080: String? = null, // "1080" or "full"
    val resolution720: String? = null,  // "720" or "feed"
    val thumbnailUrl: String? = null,   // "thumbnail_url" or "thumb"
    val thumb: String? = null,
    val feed: String? = null,           // 720p primary
    val full: String? = null,           // 1080p HQ
    val type: String? = null,           // "video", "image"
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null
)

data class Post(
    val id: Long,
    val author: User,
    val content: String = "",
    val mediaUrl: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val timestamp: String = "Just now",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val myReaction: String? = null,
    val reactions: List<UserReaction> = emptyList(),
    val feeling: String? = null,
    val location: String? = null,
    val backgroundStyle: String? = null,
    val postType: String = "post", // "post", "video", "reel", "event", "product", "music", "group_post", "story", "shared_post", "sponsored"
    val isSponsored: Boolean = false,
    val sponsoredHeadline: String = "",
    val sponsoredCtaText: String = "Learn More",
    val sponsoredCtaUrl: String = "",
    val groupId: Long? = null,
    val groupName: String? = null,
    val groupImageUrl: String? = null,
    val jobTitle: String? = null,
    val company: String? = null,
    val jobType: String? = null,
    val salary: String? = null,
    val applicationValue: String? = null,
    val product: Product? = null,
    val event: Event? = null,
    val song: Song? = null,
    val story: Story? = null,
    val sharedPost: Post? = null,
    val linkPreview: LinkPreview? = null,
    val comments: List<Comment> = emptyList(),
    val videoUrl: String? = null,
    val mediaType: String? = null,
    val mediaTypes: List<String> = emptyList(),
    val mediaMeta: List<MediaMetaItem> = emptyList(),
    val createdAt: String? = null,
    val userId: Long = author.id,
    val taggedUsers: List<Long> = emptyList()
) {
    /**
     * Priority:
     * 1. media_meta[0].thumb (or .thumbnail_url)
     * 2. If no thumb in meta -> media_meta[0].feed (720p) as poster
     * 3. media_urls[3] (thumbnail slot)
     * 4. media_url / video_url (720p)
     */
    fun getPosterUrl(): String {
        val meta = mediaMeta.firstOrNull()
        meta?.thumb?.takeIf { it.isNotBlank() }?.let { return it }
        meta?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { return it }

        if (mediaUrls.size > 3 && mediaUrls[3].isNotBlank() && (mediaTypes.getOrNull(3) == "image" || isImageUrl(mediaUrls[3]))) {
            return mediaUrls[3]
        }

        val anyImg = mediaUrls.find { isImageUrl(it) }
        if (anyImg != null) return anyImg

        meta?.feed?.takeIf { it.isNotBlank() }?.let { return it }
        meta?.resolution720?.takeIf { it.isNotBlank() }?.let { return it }

        return videoUrl ?: mediaUrl ?: ""
    }

    /**
     * Primary 720p playback URL (mobile default).
     * Priority:
     * 1. media_meta[0].feed (720p)
     * 2. media_meta[0]['720']
     * 3. media_urls[2] (720 slot)
     * 4. media_meta[0].full (1080p if 720 missing)
     * 5. video_url / media_url
     * 6. media_urls[1] (1080) -> media_urls[0] (original)
     */
    fun getDefaultPlayableVideoUrl(): String {
        val meta = mediaMeta.firstOrNull()
        meta?.feed?.takeIf { it.isNotBlank() }?.let { return it }
        meta?.resolution720?.takeIf { it.isNotBlank() }?.let { return it }

        if (mediaUrls.size > 2 && mediaUrls[2].isNotBlank()) {
            return mediaUrls[2]
        }

        meta?.full?.takeIf { it.isNotBlank() }?.let { return it }
        meta?.resolution1080?.takeIf { it.isNotBlank() }?.let { return it }

        videoUrl?.takeIf { it.isNotBlank() && isVideoUrl(it) }?.let { return it }
        mediaUrl?.takeIf { it.isNotBlank() && isVideoUrl(it) }?.let { return it }

        if (mediaUrls.size > 1 && isVideoUrl(mediaUrls[1])) return mediaUrls[1]
        if (mediaUrls.isNotEmpty() && isVideoUrl(mediaUrls[0])) return mediaUrls[0]

        val direct = videoUrl?.takeIf { it.isNotBlank() } ?: mediaUrl?.takeIf { it.isNotBlank() }
        if (direct != null) return direct

        return ""
    }

    /**
     * High quality 1080p playback URL (opt-in HD toggle).
     * Priority:
     * 1. media_meta[0].full (1080p)
     * 2. media_meta[0]['1080']
     * 3. media_urls[1] (1080 slot)
     * 4. media_meta[0].original / media_urls[0]
     * 5. Fallback to 720p feed
     */
    fun getHdPlayableVideoUrl(): String {
        val meta = mediaMeta.firstOrNull()
        meta?.full?.takeIf { it.isNotBlank() }?.let { return it }
        meta?.resolution1080?.takeIf { it.isNotBlank() }?.let { return it }

        if (mediaUrls.size > 1 && mediaUrls[1].isNotBlank()) {
            return mediaUrls[1]
        }

        meta?.original?.takeIf { it.isNotBlank() && isVideoUrl(it) }?.let { return it }
        if (mediaUrls.isNotEmpty() && isVideoUrl(mediaUrls[0])) {
            return mediaUrls[0]
        }

        return getDefaultPlayableVideoUrl()
    }

    /**
     * Original untouched upload file for native DownloadManager.
     * Priority:
     * 1. media_meta[0].original
     * 2. media_urls[0] (original slot)
     * 3. Fallback to HD or default
     */
    fun getOriginalDownloadUrl(): String {
        val meta = mediaMeta.firstOrNull()
        meta?.original?.takeIf { it.isNotBlank() }?.let { return it }
        if (mediaUrls.isNotEmpty() && mediaUrls[0].isNotBlank()) {
            return mediaUrls[0]
        }
        return getHdPlayableVideoUrl()
    }

    fun hasHdQuality(): Boolean {
        val hd = getHdPlayableVideoUrl()
        val def = getDefaultPlayableVideoUrl()
        return hd.isNotBlank() && (hd != def || mediaMeta.firstOrNull()?.full != null || mediaMeta.firstOrNull()?.resolution1080 != null || mediaUrls.size > 1)
    }

    private fun isImageUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val clean = url.substringBefore("?").substringBefore("#").trim().lowercase()
        val lower = url.lowercase()
        return clean.endsWith(".webp") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") ||
                clean.endsWith(".png") || clean.endsWith(".gif") || clean.endsWith(".svg") ||
                lower.contains(".webp") || lower.contains(".jpg") || lower.contains(".jpeg") ||
                lower.contains(".png") || lower.contains("/thumbnail") || lower.contains("/thumb")
    }

    private fun isVideoUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val clean = url.substringBefore("?").substringBefore("#").trim().lowercase()
        val lower = url.lowercase()
        return clean.endsWith(".mp4") || clean.endsWith(".mov") || clean.endsWith(".webm") ||
                clean.endsWith(".m4v") || clean.endsWith(".ogg") || clean.endsWith(".m3u8") ||
                clean.endsWith(".mkv") ||
                lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".webm") ||
                lower.contains(".m4v") || lower.contains(".m3u8") ||
                lower.contains("/videos/") || lower.contains("/video/") ||
                lower.contains("1080p") || lower.contains("720p") || lower.contains("original")
    }
}

data class SponsoredFeedAd(
    val id: Long,
    val title: String = "",
    val headline: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val ctaText: String = "Learn More",
    val ctaUrl: String = "",
    val sponsorName: String = "Sponsored",
    val sponsorAvatar: String = "",
    val location: String = "Global",
    val brandId: Long? = null,
    val isVideo: Boolean = false
)

data class Story(
    val id: Long,
    val author: User,
    val mediaUrl: String = "",
    val timestamp: String = "2h ago",
    val isViewed: Boolean = false,
    val type: String = "image", // "image", "video", "text"
    val textContent: String = "",
    val backgroundStyle: String = "linear-gradient(45deg, #1877F2, #0055FF)",
    val musicTitle: String? = null,
    val musicUrl: String? = null,
    val viewsCount: Int = 0,
    val reactionsCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val myReaction: String? = null,
    val isLiked: Boolean = false,
    val viewers: List<User> = emptyList(),
    val duration: Int = 90,
    val musicStart: Int = 0,
    val musicEnd: Int? = null,
    val musicDuration: Int = 0,
    val effectId: String = "none",
    val mediaUrls: List<String> = emptyList()
)

data class StoryViewer(
    val id: Long = 0L,
    val storyId: Long = 0L,
    val userId: Long,
    val viewedAt: String = "Just now",
    val reaction: String? = null,
    val name: String,
    val username: String,
    val profileImageUrl: String,
    val isVerified: Boolean = false,
    val role: String = "user"
)

data class StoryComment(
    val id: Long,
    val storyId: Long,
    val userId: Long,
    val parentId: Long? = null,
    val content: String,
    val imageUrl: String? = null,
    val createdAt: String = "Just now",
    val likesCount: Int = 0,
    val likedByMe: Boolean = false,
    val repliesCount: Int = 0,
    val name: String,
    val username: String,
    val profileImageUrl: String,
    val isHidden: Boolean = false
)

data class StoryAnalytics(
    val totalViews: Int = 0,
    val uniqueViewers: Int = 0,
    val viewsWithReactions: Int = 0,
    val reactionBreakdown: Map<String, Int> = emptyMap(),
    val completionRate: Double = 0.0,
    val averageViewTime: Double = 0.0
)

data class StoryReactionItem(
    val userId: Long,
    val type: String,
    val name: String,
    val profileImageUrl: String
)

data class StoryReactionsData(
    val reactions: List<StoryReactionItem> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val myReaction: String? = null,
    val commentsCount: Int = 0
)

data class Reel(
    val id: Long,
    val author: User,
    val videoUrl: String,
    val thumbnailUrl: String,
    val caption: String,
    val musicTitle: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int = 12,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false
)

data class SavedPostItem(
    val id: Long,
    val type: String = "normal", // "video" or "normal"
    val savedAt: Long = System.currentTimeMillis(),
    val post: Post? = null,
    val reel: Reel? = null
) {
    val author: User
        get() = post?.author ?: reel?.author ?: User(0L, "User", "user", "")

    val content: String
        get() = post?.content ?: reel?.caption ?: ""

    val mediaUrl: String?
        get() = post?.mediaUrl ?: reel?.thumbnailUrl

    val likesCount: Int
        get() = post?.likesCount ?: reel?.likesCount ?: 0

    val commentsCount: Int
        get() = post?.commentsCount ?: reel?.commentsCount ?: 0

    val sharesCount: Int
        get() = post?.sharesCount ?: reel?.sharesCount ?: 0

    val isLiked: Boolean
        get() = post?.isLiked ?: reel?.isLiked ?: false

    val isSaved: Boolean
        get() = post?.isSaved ?: reel?.isSaved ?: true
}

data class Product(
    val id: Long,
    val title: String,
    val price: Double,
    val discountPrice: Double? = null,
    val currency: String = "TZS",
    val category: String,
    val location: String,
    val country: String = "TZ",
    val imageUrl: String,
    val images: List<String> = emptyList(),
    val description: String,
    val seller: User,
    val phoneNumber: String = "+255 712 345 678",
    val isSaved: Boolean = false,
    val isSponsored: Boolean = false,
    val isFresh: Boolean = true,
    val quantity: Int = 1,
    val createdAt: String = "",
    val views: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val myReaction: String? = null
)

data class LocationSearchResult(
    val displayName: String,
    val countryCode: String,
    val lat: String = "",
    val lon: String = ""
)

data class Event(
    val id: Long,
    val title: String,
    val date: String,
    val location: String,
    val imageUrl: String,
    val description: String,
    val interestedCount: Int = 0,
    val goingCount: Int = 0,
    val isInterested: Boolean = false,
    val isGoing: Boolean = false,
    val organizer: User,
    val time: String = "",
    val visibility: String = "worldwide",
    val groupId: Long? = null,
    val groupName: String? = null,
    val createdAt: String = "Just now"
)

data class ChatAttachment(
    val url: String,
    val fileType: String = "image", // "image", "video", "audio", "document", "gif"
    val mimeType: String = "",
    val filename: String = "Attachment",
    val sizeBytes: Long? = null,
    val durationSeconds: Int? = null
)

data class ChatMessage(
    val id: String,
    val senderId: Long,
    val text: String = "",
    val timestamp: String = "Just now",
    val isMine: Boolean = false,
    val isSeen: Boolean = false,
    val isDelivered: Boolean = true,
    val isEdited: Boolean = false,
    val parentMessageId: String? = null,
    val replyToText: String? = null,
    val attachments: List<ChatAttachment> = emptyList(),
    val isCallMessage: Boolean = false,
    val callType: String = "voice", // "voice", "video"
    val isMissedCall: Boolean = false
)

data class Conversation(
    val id: String,
    val participant: User,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int = 0,
    val messages: List<ChatMessage> = emptyList()
)

data class Notification(
    val id: String,
    val user: User,
    val content: String,
    val timeAgo: String = "Just now",
    val type: NotificationType = NotificationType.LIKE,
    val isRead: Boolean = false,
    val rawType: String = "",
    val reactionType: String = "",
    val entityType: String = "",
    val entityId: Long? = null,
    val groupId: Long? = null,
    val groupName: String? = null,
    val inviteId: Long? = null,
    val previewText: String? = null,
    val previewImage: String? = null,
    val actorsCount: Int = 1,
    val actors: List<User> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class SongStats(
    val plays: Long = 0,
    val likes: Long = 0,
    val shares: Long = 0,
    val downloads: Long = 0,
    val reelsUse: Long = 0
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val cover: String = "https://media.unera.social/task_01kftb3024ed7bm84gy6j485fh_1769336848_img_0.webp",
    val audioUrl: String = "",
    val duration: String = "3:24",
    val durationSeconds: Int = 204,
    val uploaderId: Long = 1,
    val uploadDate: String = "2026-03-15T12:00:00Z",
    val genre: String = "Afrobeats",
    val album: String = "Single",
    val isVerified: Boolean = false,
    val stats: SongStats = SongStats()
)

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Int = 180,
    val url: String = "",
    val uploaderId: Long = 1,
    val cover: String = "https://media.unera.social/task_01kftb3024ed7bm84gy6j485fh_1769336848_img_0.webp",
    val type: String = "music",
    val isVerified: Boolean = false,
    val likesCount: Long = 0
)

data class Album(
    val name: String,
    val songs: List<Song>,
    val cover: String,
    val artist: String,
    val totalTracks: Int
)

data class SongComment(
    val id: Long,
    val songId: String,
    val userId: Long,
    val userName: String,
    val userAvatar: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val likesCount: Long = 0,
    val isHidden: Boolean = false,
    val hiddenBy: Long? = null
)

data class AdCampaign(
    val id: Long,
    val name: String = "",
    val type: String = "image", // "image" or "video"
    val status: String = "active", // "active", "paused", "completed"
    val mediaUrl: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val description: String = "",
    val cta: String = "Learn More",
    val link: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val location: String = "Global",
    val budget: Int = 5,
    val days: Int = 7,
    val startDate: String = "2026-03-01",
    val endDate: String = "2026-03-08",
    val impressions: Int = 0,
    val views: Int = 0,
    val clicks: Int = 0,
    val postId: Long? = null
)
