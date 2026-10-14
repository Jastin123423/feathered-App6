package com.unera.social.data

import com.unera.social.model.*

object MockData {
    val currentUser = User(
        id = 101L,
        name = "Jastin Jovian",
        username = "jastin_jovian",
        profileImageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
        coverImageUrl = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1200",
        bio = "Tech creator & digital explorer 🌍 Building next-gen social networks. Connect, Create, Move! ✨",
        location = "Dar es Salaam, Tanzania",
        followersCount = 3840,
        followingCount = 485,
        isVerified = true,
        isOnline = true
    )

    val users = listOf(
        currentUser,
        User(
            id = 102L,
            name = "Amina Mwangi",
            username = "amina_m",
            profileImageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
            coverImageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1200",
            bio = "Visual storyteller & coffee enthusiast ☕",
            location = "Dar es Salaam, Tanzania",
            followersCount = 5200,
            followingCount = 310,
            isVerified = true,
            isOnline = true,
            birthDate = "1998-10-11",
            isFollowing = false
        ),
        User(
            id = 103L,
            name = "David Kimaro",
            username = "david_k",
            profileImageUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
            coverImageUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1200",
            bio = "Wildlife photographer & conservation advocate 🦁",
            location = "Arusha, Tanzania",
            followersCount = 14200,
            followingCount = 190,
            isVerified = true,
            isOnline = false,
            birthDate = "1995-10-11",
            isFollowing = false
        ),
        User(
            id = 104L,
            name = "Zahara Hassan",
            username = "zahara_style",
            profileImageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400",
            coverImageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200",
            bio = "Fashion designer & lifestyle blogger ✨",
            location = "Dar es Salaam, Tanzania",
            followersCount = 8900,
            followingCount = 420,
            isVerified = true,
            isOnline = true,
            birthDate = "1999-10-15",
            isFollowing = false
        ),
        User(
            id = 105L,
            name = "Samuel Adebayo",
            username = "sam_ade",
            profileImageUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
            coverImageUrl = "https://images.unsplash.com/photo-1516251193007-45ef944ab0c6?w=1200",
            bio = "Afrobeats music producer & sound designer 🎵",
            location = "Lagos, Nigeria",
            followersCount = 21000,
            followingCount = 612,
            isVerified = true,
            isOnline = false,
            birthDate = "1997-10-22",
            isFollowing = false
        ),
        User(
            id = 106L,
            name = "Grace Ndlovu",
            username = "grace_tech",
            profileImageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
            coverImageUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=1200",
            bio = "Frontend Architect & UI/UX explorer 🚀",
            location = "Johannesburg, South Africa",
            followersCount = 9450,
            followingCount = 280,
            isVerified = true,
            isOnline = true,
            birthDate = "1996-11-04",
            isFollowing = false
        ),
        User(
            id = 107L,
            name = "Kwame Mensah",
            username = "kwame_creative",
            profileImageUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400",
            coverImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200",
            bio = "Digital animator & 3D artist 🎨",
            location = "Accra, Ghana",
            followersCount = 11300,
            followingCount = 340,
            isVerified = true,
            isOnline = true,
            birthDate = "1998-10-18",
            isFollowing = false
        )
    )

    val stories = emptyList<Story>()

    val posts = emptyList<Post>()

    val reels = emptyList<Reel>()

    val products = emptyList<Product>()

    val events = listOf(
        Event(
            id = 401L,
            title = "East Africa Tech & Creators Summit 2026",
            date = "Oct 24, 2026 • 9:00 AM",
            location = "Julius Nyerere Int'l Convention Centre, Dar es Salaam",
            imageUrl = "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800",
            description = "Join over 2,000 founders, creators, developers, and investors exploring the future of mobile technology and creative industries across Africa.",
            interestedCount = 450,
            goingCount = 180,
            isInterested = true,
            isGoing = false,
            organizer = currentUser
        ),
        Event(
            id = 402L,
            title = "Zanzibar Sunset Acoustic Sessions",
            date = "Nov 02, 2026 • 5:30 PM",
            location = "Kendwa Rocks Beach, Zanzibar",
            imageUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800",
            description = "An unforgettable evening of live acoustic melodies, bonfire, and seaside vibes featuring top indie artists from around the region.",
            interestedCount = 310,
            goingCount = 95,
            isInterested = false,
            isGoing = true,
            organizer = users[3]
        ),
        Event(
            id = 403L,
            title = "Wildlife Photography Masterclass in Arusha",
            date = "Nov 15, 2026 • 8:00 AM",
            location = "Arusha National Park Gate",
            imageUrl = "https://images.unsplash.com/photo-1534567153574-2b12153a87f0?w=800",
            description = "Hands-on field workshop learning wildlife tracking, framing, fast action autofocus, and golden hour lighting techniques.",
            interestedCount = 120,
            goingCount = 40,
            isInterested = false,
            isGoing = false,
            organizer = users[2]
        )
    )

    val conversations = listOf(
        Conversation(
            id = "c1",
            participant = users[1],
            lastMessage = "Hey! Loved your latest post on Feathered!",
            lastTimestamp = "10:32 AM",
            unreadCount = 2,
            messages = listOf(
                ChatMessage("m1", users[1].id, "Hey Jastin, are you attending the summit next month?", "10:20 AM", false),
                ChatMessage("m2", currentUser.id, "Yes absolutely! Speaking on the creator panel.", "10:25 AM", true),
                ChatMessage("m3", users[1].id, "That's fantastic! Let's definitely catch up there.", "10:30 AM", false),
                ChatMessage("m4", users[1].id, "Hey! Loved your latest post on Feathered!", "10:32 AM", false)
            )
        ),
        Conversation(
            id = "c2",
            participant = users[2],
            lastMessage = "Thanks for the feedback on the camera gear!",
            lastTimestamp = "Yesterday",
            unreadCount = 0,
            messages = listOf(
                ChatMessage("m21", currentUser.id, "David, is the Sony a7 IV still available on Marketplace?", "Yesterday", true),
                ChatMessage("m22", users[2].id, "Yes it is! Body only or with the lens?", "Yesterday", false),
                ChatMessage("m23", currentUser.id, "Body only. A friend in town is interested.", "Yesterday", true),
                ChatMessage("m24", users[2].id, "Thanks for the feedback on the camera gear!", "Yesterday", false)
            )
        ),
        Conversation(
            id = "c3",
            participant = users[3],
            lastMessage = "New bomber jackets are ready for shipping! ✨",
            lastTimestamp = "Oct 08",
            unreadCount = 0,
            messages = listOf(
                ChatMessage("m31", users[3].id, "New bomber jackets are ready for shipping! ✨", "Oct 08", false)
            )
        )
    )

    val notifications = listOf(
        Notification(
            id = "n1",
            user = users[1],
            content = "loved your post.",
            timeAgo = "10m ago",
            type = NotificationType.REACTION,
            rawType = "reaction",
            reactionType = "love",
            entityType = "post",
            previewText = "Proud to announce the new mobile experience for Feathered!",
            previewImage = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=300",
            isRead = false,
            timestamp = System.currentTimeMillis() - 10 * 60 * 1000
        ),
        Notification(
            id = "n2",
            user = users[2],
            content = "invited you to join the group \"Kilimanjaro Hikers Club\".",
            timeAgo = "25m ago",
            type = NotificationType.GROUP_INVITE,
            rawType = "group_invite",
            entityType = "group_invite",
            groupId = 501L,
            groupName = "Kilimanjaro Hikers Club",
            inviteId = 1001L,
            isRead = false,
            timestamp = System.currentTimeMillis() - 25 * 60 * 1000
        ),
        Notification(
            id = "n3",
            user = users[3],
            content = "commented on your photo.",
            timeAgo = "1h ago",
            type = NotificationType.COMMENT,
            rawType = "comment",
            entityType = "post",
            previewText = "Stunning view! Where was this shot taken?",
            previewImage = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=300",
            isRead = false,
            timestamp = System.currentTimeMillis() - 60 * 60 * 1000
        ),
        Notification(
            id = "n4",
            user = users[4],
            content = "started following you.",
            timeAgo = "3h ago",
            type = NotificationType.FOLLOW,
            rawType = "follow",
            entityType = "profile",
            isRead = true,
            timestamp = System.currentTimeMillis() - 3 * 3600 * 1000
        ),
        Notification(
            id = "n5",
            user = users[5],
            content = "invited you to the event \"Zanzibar Sunset Acoustic Sessions\".",
            timeAgo = "5h ago",
            type = NotificationType.EVENT,
            rawType = "event",
            entityType = "event",
            entityId = 1L,
            previewText = "Live acoustic performances by the ocean.",
            previewImage = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            isRead = true,
            timestamp = System.currentTimeMillis() - 5 * 3600 * 1000
        ),
        Notification(
            id = "n6",
            user = users[1],
            content = "and 2 others reacted to your reel.",
            timeAgo = "Yesterday",
            type = NotificationType.REACTION,
            rawType = "reaction",
            reactionType = "fire",
            entityType = "reel",
            actorsCount = 3,
            actors = listOf(users[1], users[2], users[3]),
            previewText = "Sunset drive through Ngorongoro 🌅",
            isRead = true,
            timestamp = System.currentTimeMillis() - 26 * 3600 * 1000
        ),
        Notification(
            id = "n7",
            user = users[1],
            content = "has a birthday today! Wish them a happy birthday.",
            timeAgo = "Yesterday",
            type = NotificationType.BIRTHDAY,
            rawType = "birthday",
            entityType = "profile",
            isRead = true,
            timestamp = System.currentTimeMillis() - 30 * 3600 * 1000
        ),
        Notification(
            id = "n8",
            user = users[2],
            content = "shared your story.",
            timeAgo = "2 days ago",
            type = NotificationType.SHARE,
            rawType = "share",
            entityType = "story",
            isRead = true,
            timestamp = System.currentTimeMillis() - 48 * 3600 * 1000
        ),
        Notification(
            id = "n9",
            user = users[3],
            content = "mentioned you in a comment.",
            timeAgo = "3 days ago",
            type = NotificationType.MENTION,
            rawType = "mention",
            entityType = "comment",
            previewText = "@jastin_jovian check out this amazing event!",
            isRead = true,
            timestamp = System.currentTimeMillis() - 72 * 3600 * 1000
        ),
        Notification(
            id = "n10",
            user = users[4],
            content = "played your track \"Afro Beats Mix 2026\".",
            timeAgo = "4 days ago",
            type = NotificationType.MUSIC,
            rawType = "song",
            entityType = "song",
            previewText = "Afro Beats Mix 2026 (Live in Dar)",
            isRead = true,
            timestamp = System.currentTimeMillis() - 96 * 3600 * 1000
        )
    )

    val songs = listOf(
        Song(
            id = "1",
            title = "Lonely At The Top",
            artist = "Asake",
            cover = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            duration = "3:24",
            durationSeconds = 204,
            uploaderId = 2,
            genre = "Afrobeats",
            album = "Work of Art",
            isVerified = true,
            stats = SongStats(plays = 1450000, likes = 245000, shares = 48000, downloads = 89000, reelsUse = 12000)
        ),
        Song(
            id = "2",
            title = "City Boys",
            artist = "Burna Boy",
            cover = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            duration = "2:48",
            durationSeconds = 168,
            uploaderId = 3,
            genre = "Afrobeats",
            album = "I Told Them...",
            isVerified = true,
            stats = SongStats(plays = 2100000, likes = 320000, shares = 72000, downloads = 135000, reelsUse = 24000)
        ),
        Song(
            id = "3",
            title = "Mnike",
            artist = "Tyler ICU & Tumelo.za",
            cover = "https://images.unsplash.com/photo-1514525253440-b393452e8d26?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            duration = "4:12",
            durationSeconds = 252,
            uploaderId = 4,
            genre = "Amapiano",
            album = "Single",
            isVerified = true,
            stats = SongStats(plays = 2540000, likes = 412000, shares = 95000, downloads = 190000, reelsUse = 38000)
        ),
        Song(
            id = "4",
            title = "Calm Down",
            artist = "Rema",
            cover = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            duration = "3:39",
            durationSeconds = 219,
            uploaderId = 5,
            genre = "Afrobeats",
            album = "Rave & Roses",
            isVerified = true,
            stats = SongStats(plays = 3850000, likes = 580000, shares = 142000, downloads = 280000, reelsUse = 56000)
        ),
        Song(
            id = "5",
            title = "Sawa Sawa",
            artist = "Alikiba",
            cover = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            duration = "3:15",
            durationSeconds = 195,
            uploaderId = 6,
            genre = "Bongo Fleva",
            album = "Only One King",
            isVerified = true,
            stats = SongStats(plays = 890000, likes = 125000, shares = 28000, downloads = 45000, reelsUse = 8900)
        ),
        Song(
            id = "6",
            title = "Water",
            artist = "Tyla",
            cover = "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            duration = "3:20",
            durationSeconds = 200,
            uploaderId = 2,
            genre = "Amapiano",
            album = "Tyla",
            isVerified = true,
            stats = SongStats(plays = 3200000, likes = 605000, shares = 180000, downloads = 295000, reelsUse = 82000)
        ),
        Song(
            id = "7",
            title = "Komasava",
            artist = "Diamond Platnumz",
            cover = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            duration = "3:02",
            durationSeconds = 182,
            uploaderId = 4,
            genre = "Bongo Fleva",
            album = "Single",
            isVerified = true,
            stats = SongStats(plays = 950000, likes = 182000, shares = 39000, downloads = 61000, reelsUse = 14500)
        ),
        Song(
            id = "8",
            title = "Tshwala Bam",
            artist = "TitoM & Yuppe",
            cover = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            duration = "3:44",
            durationSeconds = 224,
            uploaderId = 5,
            genre = "Amapiano",
            album = "Single",
            isVerified = false,
            stats = SongStats(plays = 2710000, likes = 495000, shares = 115000, downloads = 210000, reelsUse = 67000)
        ),
        Song(
            id = "9",
            title = "Nalifurahia",
            artist = "Zabron Singers",
            cover = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            duration = "4:32",
            durationSeconds = 272,
            uploaderId = 6,
            genre = "Gospel",
            album = "Mkono wa Bwana",
            isVerified = true,
            stats = SongStats(plays = 1120000, likes = 195000, shares = 34000, downloads = 54000, reelsUse = 9200)
        ),
        Song(
            id = "10",
            title = "Mapoz",
            artist = "Diamond Platnumz ft. Jay Melody",
            cover = "https://images.unsplash.com/photo-1445985543469-433ecba627a0?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            duration = "3:11",
            durationSeconds = 191,
            uploaderId = 4,
            genre = "Bongo Fleva",
            album = "Single",
            isVerified = true,
            stats = SongStats(plays = 830000, likes = 142000, shares = 29000, downloads = 48000, reelsUse = 11800)
        ),
        Song(
            id = "11",
            title = "Afro Beats Mix 2026",
            artist = "Jastin Jovian",
            cover = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
            duration = "3:58",
            durationSeconds = 238,
            uploaderId = 1,
            genre = "Afrobeats",
            album = "Dar Night Sessions",
            isVerified = true,
            stats = SongStats(plays = 18900, likes = 3420, shares = 850, downloads = 1200, reelsUse = 420)
        ),
        Song(
            id = "12",
            title = "Sunset in Serengeti",
            artist = "Jastin Jovian",
            cover = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
            duration = "3:30",
            durationSeconds = 210,
            uploaderId = 1,
            genre = "RnB",
            album = "Dar Night Sessions",
            isVerified = true,
            stats = SongStats(plays = 5990, likes = 1120, shares = 320, downloads = 540, reelsUse = 190)
        ),
        Song(
            id = "13",
            title = "Bongo Rhythm Vol. 1",
            artist = "Jastin Jovian",
            cover = "https://images.unsplash.com/photo-1520523839898-507128254c12?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3",
            duration = "2:54",
            durationSeconds = 174,
            uploaderId = 1,
            genre = "Bongo Fleva",
            album = "Single",
            isVerified = true,
            stats = SongStats(plays = 8400, likes = 1650, shares = 460, downloads = 780, reelsUse = 260)
        ),
        Song(
            id = "14",
            title = "Breathe Deep",
            artist = "Amina Said",
            cover = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3",
            duration = "3:42",
            durationSeconds = 222,
            uploaderId = 2,
            genre = "RnB",
            album = "Soul Essence",
            isVerified = false,
            stats = SongStats(plays = 42000, likes = 8900, shares = 1800, downloads = 3100, reelsUse = 780)
        ),
        Song(
            id = "15",
            title = "Zanzibar Breeze",
            artist = "Adebayo Bakari",
            cover = "https://images.unsplash.com/photo-1526478806334-5fd488fcaabc?w=600",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3",
            duration = "3:10",
            durationSeconds = 190,
            uploaderId = 3,
            genre = "Hip Hop",
            album = "Coastline",
            isVerified = false,
            stats = SongStats(plays = 78000, likes = 14300, shares = 2900, downloads = 5400, reelsUse = 1100)
        )
    )

    val initialSongComments = listOf(
        SongComment(
            id = 1L,
            songId = "1",
            userId = 2L,
            userName = "Amina Said",
            userAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            text = "This track is on continuous repeat! Production on this is top-notch 🔥",
            createdAt = System.currentTimeMillis() - 3600 * 1000 * 2,
            likesCount = 24
        ),
        SongComment(
            id = 2L,
            songId = "1",
            userId = 3L,
            userName = "Adebayo Bakari",
            userAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            text = "The bassline and vocal dynamics are crazy. Pure genius! 👏",
            createdAt = System.currentTimeMillis() - 3600 * 1000 * 5,
            likesCount = 18
        ),
        SongComment(
            id = 3L,
            songId = "2",
            userId = 4L,
            userName = "Fatima Mwangi",
            userAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
            text = "City Boys anthem all summer long! Loving the vibe 🌟",
            createdAt = System.currentTimeMillis() - 3600 * 1000 * 8,
            likesCount = 42
        )
    )

    val groups = emptyList<Group>()
    val groupPosts = emptyList<GroupPost>()
    val groupInvites = emptyList<GroupInvite>()
}
