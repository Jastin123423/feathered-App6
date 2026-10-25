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

    // No fake users - users list populated dynamically from database (UserApiService)
    val users = listOf(currentUser)

    val stories = emptyList<Story>()

    val posts = emptyList<Post>()

    val reels = emptyList<Reel>()

    val products = emptyList<Product>()

    // Live events loaded exclusively from EventsApiService
    val events = emptyList<Event>()

    // Real conversations and notifications loaded dynamically
    val conversations = emptyList<Conversation>()

    val notifications = emptyList<Notification>()

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

    val initialSongComments = emptyList<SongComment>()

    val groups = emptyList<Group>()
    val groupPosts = emptyList<GroupPost>()
    val groupInvites = emptyList<GroupInvite>()
}
