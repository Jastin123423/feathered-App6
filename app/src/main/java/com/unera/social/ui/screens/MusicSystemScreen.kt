package com.unera.social.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import com.unera.social.ui.components.VerifiedBadge
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.Album
import com.unera.social.model.AudioTrack
import com.unera.social.model.Song
import com.unera.social.model.User
import kotlinx.coroutines.delay

private const val DEFAULT_MUSIC_COVER = "https://media.unera.social/task_01kftb3024ed7bm84gy6j485fh_1769336848_img_0.webp"

enum class MusicView {
    MUSIC,
    ALBUMS,
    ALBUM,
    DASHBOARD,
    UPLOAD,
    ARTIST
}

@Composable
fun MusicSystemScreen(
    currentUser: User,
    users: List<User>,
    songs: List<Song>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    likedTrackKeys: Set<String>,
    myTotalPlays: Long,
    trackPlays: Map<String, Long>,
    uploadProgress: Int?,
    uploadStatus: String?,
    onPlayTrack: (AudioTrack) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleLikeTrack: (String) -> Unit,
    onUploadSong: (title: String, artist: String, genre: String, album: String, coverUrl: String, audioUrl: String) -> Unit,
    onDeleteUploadedSong: (String) -> Unit,
    onProfileClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeView by remember { mutableStateOf(MusicView.MUSIC) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedArtistId by remember { mutableStateOf<Long?>(null) }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedGenreChip by remember { mutableStateOf<String?>(null) }

    // Intercept back navigation
    BackHandler {
        if (activeView != MusicView.MUSIC) {
            if (activeView == MusicView.ALBUM) {
                activeView = MusicView.ALBUMS
            } else {
                activeView = MusicView.MUSIC
            }
        } else {
            onBack()
        }
    }

    // Albums computation
    val albums = remember(songs, users) {
        val grouped = mutableMapOf<String, MutableList<Song>>()
        songs.forEach { song ->
            val alb = song.album.ifBlank { "Single" }
            grouped.getOrPut(alb) { mutableListOf() }.add(song)
        }
        grouped.map { (name, albumSongs) ->
            val first = albumSongs.first()
            val uploader = users.find { it.id == first.uploaderId }
            Album(
                name = name,
                songs = albumSongs,
                cover = first.cover.ifBlank { DEFAULT_MUSIC_COVER },
                artist = uploader?.name ?: first.artist,
                totalTracks = albumSongs.size
            )
        }
    }

    val selectedAlbum = remember(albums, selectedAlbumName) {
        albums.find { it.name == selectedAlbumName }
    }

    // Hero rotating index
    var heroIndex by remember { mutableIntStateOf(0) }
    val featuredSongs = remember(songs) { songs.take(5) }
    val heroSong = featuredSongs.getOrNull(heroIndex) ?: songs.firstOrNull()

    LaunchedEffect(featuredSongs) {
        while (true) {
            delay(5000L)
            if (featuredSongs.isNotEmpty()) {
                heroIndex = (heroIndex + 1) % featuredSongs.size
            }
        }
    }

    // Search & Filtered songs
    val filteredSongs = remember(songs, searchQuery, selectedGenreChip) {
        var list = songs
        if (!selectedGenreChip.isNullOrBlank()) {
            list = list.filter { it.genre.equals(selectedGenreChip, ignoreCase = true) }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.genre.lowercase().contains(q)
            }
        }
        list
    }

    // Sections for the feed
    val trendingSongs = remember(songs) { songs.sortedByDescending { it.stats.plays }.take(10) }
    val handpickedGems = remember(songs) { songs.sortedByDescending { it.stats.likes }.take(10) }
    val bestPicks = remember(songs) { songs.shuffled().take(10) }
    val freshReleases = remember(songs) { songs.takeLast(10).reversed() }

    fun playSongItem(song: Song) {
        val audioTrack = AudioTrack(
            id = song.id,
            title = song.title,
            artist = song.artist,
            duration = song.durationSeconds,
            url = song.audioUrl,
            uploaderId = song.uploaderId,
            cover = song.cover,
            type = "music",
            isVerified = song.isVerified,
            likesCount = song.stats.likes
        )
        onPlayTrack(audioTrack)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF050B18),
        topBar = {
            // Sticky Top Navigation Tabs
            Surface(
                color = Color(0xFF0B1120).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button
                    IconButton(
                        onClick = {
                            if (activeView != MusicView.MUSIC) {
                                if (activeView == MusicView.ALBUM) activeView = MusicView.ALBUMS
                                else activeView = MusicView.MUSIC
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tabs
                    ScrollableTabRow(
                        selectedTabIndex = when (activeView) {
                            MusicView.MUSIC -> 0
                            MusicView.ALBUMS, MusicView.ALBUM -> 1
                            MusicView.DASHBOARD, MusicView.UPLOAD -> 2
                            MusicView.ARTIST -> 3
                        },
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF1877F2),
                        divider = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Tab(
                            selected = activeView == MusicView.MUSIC,
                            onClick = { activeView = MusicView.MUSIC },
                            text = {
                                Text(
                                    text = "MUSIC",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (activeView == MusicView.MUSIC) Color(0xFF1877F2) else Color(0xFF94A3B8)
                                )
                            }
                        )

                        Tab(
                            selected = activeView == MusicView.ALBUMS || activeView == MusicView.ALBUM,
                            onClick = { activeView = MusicView.ALBUMS },
                            text = {
                                Text(
                                    text = "ALBUMS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (activeView == MusicView.ALBUMS || activeView == MusicView.ALBUM) Color(0xFF1877F2) else Color(0xFF94A3B8)
                                )
                            }
                        )

                        Tab(
                            selected = activeView == MusicView.DASHBOARD || activeView == MusicView.UPLOAD,
                            onClick = { activeView = MusicView.DASHBOARD },
                            text = {
                                Text(
                                    text = "STUDIO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (activeView == MusicView.DASHBOARD || activeView == MusicView.UPLOAD) Color(0xFF1877F2) else Color(0xFF94A3B8)
                                )
                            }
                        )

                        if (selectedArtistId != null) {
                            Tab(
                                selected = activeView == MusicView.ARTIST,
                                onClick = { activeView = MusicView.ARTIST },
                                text = {
                                    Text(
                                        text = "ARTIST",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (activeView == MusicView.ARTIST) Color(0xFF1877F2) else Color(0xFF94A3B8)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeView) {
                MusicView.MUSIC -> {
                    MusicFeedContent(
                        heroSong = heroSong,
                        searchQuery = searchQuery,
                        selectedGenre = selectedGenreChip,
                        trendingSongs = trendingSongs,
                        handpickedGems = handpickedGems,
                        bestPicks = bestPicks,
                        freshReleases = freshReleases,
                        filteredSongs = filteredSongs,
                        allSongs = songs,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        likedTrackKeys = likedTrackKeys,
                        trackPlays = trackPlays,
                        onSearchQueryChange = {
                            searchQuery = it
                            if (it.isNotBlank()) selectedGenreChip = null
                        },
                        onSelectGenre = { genre ->
                            if (selectedGenreChip == genre) {
                                selectedGenreChip = null
                            } else {
                                selectedGenreChip = genre
                                searchQuery = ""
                            }
                        },
                        onPlaySong = { playSongItem(it) },
                        onTogglePlay = onTogglePlay,
                        onToggleLikeSong = { onToggleLikeTrack(it.id) },
                        onOpenStudio = { activeView = MusicView.DASHBOARD },
                        onOpenAlbums = { activeView = MusicView.ALBUMS },
                        onArtistClick = { artistId ->
                            selectedArtistId = artistId
                            activeView = MusicView.ARTIST
                        }
                    )
                }

                MusicView.ALBUMS -> {
                    AlbumsListContent(
                        albums = albums,
                        onSelectAlbum = { album ->
                            selectedAlbumName = album.name
                            activeView = MusicView.ALBUM
                        }
                    )
                }

                MusicView.ALBUM -> {
                    if (selectedAlbum != null) {
                        AlbumDetailContent(
                            album = selectedAlbum,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            likedTrackKeys = likedTrackKeys,
                            onPlaySong = { playSongItem(it) },
                            onPlayAll = {
                                val first = selectedAlbum.songs.firstOrNull()
                                if (first != null) playSongItem(first)
                            },
                            onToggleLikeSong = { onToggleLikeTrack(it.id) },
                            onBackToAlbums = { activeView = MusicView.ALBUMS }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Album not found", color = Color.White)
                        }
                    }
                }

                MusicView.DASHBOARD -> {
                    CreatorStudioDashboard(
                        currentUser = currentUser,
                        userSongs = songs.filter { it.uploaderId == currentUser.id },
                        myTotalPlays = myTotalPlays,
                        trackPlays = trackPlays,
                        onOpenUpload = { activeView = MusicView.UPLOAD },
                        onPlaySong = { playSongItem(it) },
                        onDeleteSong = onDeleteUploadedSong
                    )
                }

                MusicView.UPLOAD -> {
                    UploadSongStudio(
                        currentUser = currentUser,
                        uploadProgress = uploadProgress,
                        uploadStatus = uploadStatus,
                        onBack = { activeView = MusicView.DASHBOARD },
                        onSubmitUpload = { title, artist, genre, album, cover, audio ->
                            onUploadSong(title, artist, genre, album, cover, audio)
                            activeView = MusicView.DASHBOARD
                        }
                    )
                }

                MusicView.ARTIST -> {
                    val artistUser = users.find { it.id == selectedArtistId }
                    val artistSongs = songs.filter { it.uploaderId == selectedArtistId }
                    ArtistProfileReleasesContent(
                        artistUser = artistUser,
                        artistSongs = artistSongs,
                        likedTrackKeys = likedTrackKeys,
                        onPlaySong = { playSongItem(it) },
                        onToggleLikeSong = { onToggleLikeTrack(it.id) },
                        onBack = { activeView = MusicView.MUSIC }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. MUSIC FEED VIEW CONTENT
// ==========================================

@Composable
private fun MusicFeedContent(
    heroSong: Song?,
    searchQuery: String,
    selectedGenre: String?,
    trendingSongs: List<Song>,
    handpickedGems: List<Song>,
    bestPicks: List<Song>,
    freshReleases: List<Song>,
    filteredSongs: List<Song>,
    allSongs: List<Song>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    likedTrackKeys: Set<String>,
    trackPlays: Map<String, Long>,
    onSearchQueryChange: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onPlaySong: (Song) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleLikeSong: (Song) -> Unit,
    onOpenStudio: () -> Unit,
    onOpenAlbums: () -> Unit,
    onArtistClick: (Long) -> Unit
) {
    val genres = listOf("Bongo Fleva", "Amapiano", "Afrobeats", "Hip Hop", "RnB", "Gospel")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Entertainment Header Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header title & studio button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "F-Music",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Discover trending sounds, creators and fresh vibes",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = onOpenStudio,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1877F2),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Studio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search input bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search songs, artists...", color = Color(0xFF8D96A8), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF8D96A8))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF8D96A8))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF070D1D),
                            unfocusedContainerColor = Color(0xFF070D1D),
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_search_bar"),
                        singleLine = true
                    )

                    // Hero Banner Card
                    if (heroSong != null && searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HeroBannerCard(
                            song = heroSong,
                            trackPlays = trackPlays,
                            onPlay = { onPlaySong(heroSong) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action Circles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickActionItem(icon = Icons.Default.BarChart, label = "Charts", onClick = {})
                        QuickActionItem(icon = Icons.Default.Album, label = "Albums", onClick = onOpenAlbums)
                        QuickActionItem(icon = Icons.Default.QueueMusic, label = "Playlists", onClick = {})
                        QuickActionItem(icon = Icons.Default.Category, label = "Genres", onClick = {})
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Genre Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(genres) { genre ->
                            val isSelected = selectedGenre == genre
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFF1877F2) else Color(0xFF070D1D),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF1877F2) else Color(0xFF1E293B)),
                                modifier = Modifier.clickable { onSelectGenre(genre) }
                            ) {
                                Text(
                                    text = genre,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mini Now Playing Strip (if playing)
        if (currentTrack != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AsyncImage(
                                model = if (currentTrack.cover.isNotBlank()) currentTrack.cover else DEFAULT_MUSIC_COVER,
                                contentDescription = currentTrack.title,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text("Now Playing", color = Color(0xFF8D96A8), fontSize = 11.sp)
                                Text(
                                    currentTrack.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    currentTrack.artist,
                                    color = Color(0xFFB8BCC7),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) Color(0xFF1877F2) else Color(0xFF2A2F39))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Feed content: If searching or genre selected, display results grid; else display horizontal rows
        if (searchQuery.isNotBlank() || selectedGenre != null) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "Search Results (${filteredSongs.size})" else "$selectedGenre Tracks (${filteredSongs.size})",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            items(filteredSongs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isLiked = "music:${song.id}" in likedTrackKeys,
                    onPlay = { onPlaySong(song) },
                    onToggleLike = { onToggleLikeSong(song) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            // Horizontal Music Row: Trending
            item {
                HorizontalMusicSection(
                    title = "Trending",
                    subtitle = "Popular tracks with strong listener momentum",
                    songs = trendingSongs,
                    badgeText = { index -> if (index == 0) "HOT" else "#${index + 1}" },
                    badgeColor = { index -> if (index == 0) Color(0xFFFF7A00) else Color.Black.copy(alpha = 0.65f) },
                    likedTrackKeys = likedTrackKeys,
                    onPlaySong = onPlaySong,
                    onToggleLikeSong = onToggleLikeSong,
                    onArtistClick = onArtistClick
                )
            }

            // Horizontal Music Row: Handpicked Gems
            item {
                HorizontalMusicSection(
                    title = "Handpicked User Gems",
                    subtitle = "Quality tracks from Feathered creators",
                    songs = handpickedGems,
                    badgeText = { "GEM" },
                    badgeColor = { Color(0xFF1877F2) },
                    likedTrackKeys = likedTrackKeys,
                    onPlaySong = onPlaySong,
                    onToggleLikeSong = onToggleLikeSong,
                    onArtistClick = onArtistClick
                )
            }

            // Horizontal Music Row: Best Picks For You
            item {
                HorizontalMusicSection(
                    title = "Best Picks For You",
                    subtitle = "Balanced by plays, likes, freshness and creator fairness",
                    songs = bestPicks,
                    badgeText = { "TOP" },
                    badgeColor = { Color(0xFF1877F2) },
                    likedTrackKeys = likedTrackKeys,
                    onPlaySong = onPlaySong,
                    onToggleLikeSong = onToggleLikeSong,
                    onArtistClick = onArtistClick
                )
            }

            // Horizontal Music Row: Fresh Releases
            item {
                HorizontalMusicSection(
                    title = "Fresh Releases",
                    subtitle = "New music with early listener signals",
                    songs = freshReleases,
                    badgeText = { "NEW" },
                    badgeColor = { Color(0xFF10B981) },
                    likedTrackKeys = likedTrackKeys,
                    onPlaySong = onPlaySong,
                    onToggleLikeSong = onToggleLikeSong,
                    onArtistClick = onArtistClick
                )
            }

            // All Songs Ranked List (Rank 1..15)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "All Songs",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "A fresh ranked mix from all Feathered creators",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            itemsIndexed(allSongs.take(20), key = { _, s -> s.id }) { index, song ->
                SongRankedListItem(
                    index = index + 1,
                    song = song,
                    isLiked = "music:${song.id}" in likedTrackKeys,
                    isCurrent = currentTrack?.id == song.id,
                    onPlay = { onPlaySong(song) },
                    onToggleLike = { onToggleLikeSong(song) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroBannerCard(
    song: Song,
    trackPlays: Map<String, Long>,
    onPlay: () -> Unit
) {
    val plays = trackPlays["music:${song.id}"] ?: song.stats.plays

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .clickable(onClick = onPlay)
    ) {
        AsyncImage(
            model = if (song.cover.isNotBlank()) song.cover else DEFAULT_MUSIC_COVER,
            contentDescription = song.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FEATURED",
                    color = Color(0xFF1877F2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${formatCompactNumber(plays)} plays",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF1877F2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HorizontalMusicSection(
    title: String,
    subtitle: String,
    songs: List<Song>,
    badgeText: (Int) -> String,
    badgeColor: (Int) -> Color,
    likedTrackKeys: Set<String>,
    onPlaySong: (Song) -> Unit,
    onToggleLikeSong: (Song) -> Unit,
    onArtistClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                MusicFeedCard(
                    song = song,
                    isLiked = "music:${song.id}" in likedTrackKeys,
                    badge = badgeText(index),
                    badgeColor = badgeColor(index),
                    onPlay = { onPlaySong(song) },
                    onLike = { onToggleLikeSong(song) },
                    onArtistClick = { onArtistClick(song.uploaderId) }
                )
            }
        }
    }
}

@Composable
private fun MusicFeedCard(
    song: Song,
    isLiked: Boolean,
    badge: String,
    badgeColor: Color,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onArtistClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onPlay)
    ) {
        // Artwork square with badge & like overlay
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B1120))
        ) {
            AsyncImage(
                model = if (song.cover.isNotBlank()) song.cover else DEFAULT_MUSIC_COVER,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Badge top-left
            if (badge.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Like heart button top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onLike() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFFF4D8D) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom gradient overlay with plays and duration
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = formatCompactNumber(song.stats.plays),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = song.duration,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Artist row with verified badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onArtistClick() }
        ) {
            Text(
                text = song.artist,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (song.isVerified) {
                VerifiedBadge(size = 12.dp)
            }
        }
    }
}

@Composable
private fun SongRankedListItem(
    index: Int,
    song: Song,
    isLiked: Boolean,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onPlay,
        color = if (isCurrent) Color(0xFF1877F2).copy(alpha = 0.15f) else Color.Transparent,
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1877F2).copy(alpha = 0.4f)) else null,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$index",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Center
            )

            AsyncImage(
                model = if (song.cover.isNotBlank()) song.cover else DEFAULT_MUSIC_COVER,
                contentDescription = song.title,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${formatCompactNumber(song.stats.plays)} plays",
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp
            )

            IconButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFFF4D8D) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SongRowItem(
    song: Song,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onPlay,
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = if (song.cover.isNotBlank()) song.cover else DEFAULT_MUSIC_COVER,
                contentDescription = song.title,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.genre}",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFFF4D8D) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. ALBUMS VIEW CONTENT
// ==========================================

@Composable
private fun AlbumsListContent(
    albums: List<Album>,
    onSelectAlbum: (Album) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Albums",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Browse music projects from Feathered creators",
                    color = Color(0xFFA8AFBC),
                    fontSize = 14.sp
                )
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(albums, key = { it.name }) { album ->
                    Column(
                        modifier = Modifier
                            .width(170.dp)
                            .clickable { onSelectAlbum(album) }
                    ) {
                        AsyncImage(
                            model = album.cover,
                            contentDescription = album.name,
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B1120)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = album.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = album.artist,
                            color = Color(0xFFB8BCC7),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${album.totalTracks} songs",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "All Projects",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(albums, key = { "item_" + it.name }) { album ->
            Surface(
                onClick = { onSelectAlbum(album) },
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AsyncImage(
                        model = album.cover,
                        contentDescription = album.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = album.artist,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${album.totalTracks} tracks",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. ALBUM DETAIL VIEW CONTENT
// ==========================================

@Composable
private fun AlbumDetailContent(
    album: Album,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    likedTrackKeys: Set<String>,
    onPlaySong: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onToggleLikeSong: (Song) -> Unit,
    onBackToAlbums: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0B1528), Color(0xFF070D1D))
                            )
                        )
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = onBackToAlbums,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Albums",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = album.cover,
                            contentDescription = album.name,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.name,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = album.artist,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${album.totalTracks} songs",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onPlayAll,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All (${album.totalTracks})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Track items
        itemsIndexed(album.songs, key = { _, s -> s.id }) { index, song ->
            val isCurrent = currentTrack?.id == song.id
            Surface(
                onClick = { onPlaySong(song) },
                color = if (isCurrent) Color(0xFF1877F2).copy(alpha = 0.15f) else Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        color = Color(0xFFB0B3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.width(20.dp),
                        textAlign = TextAlign.Center
                    )

                    AsyncImage(
                        model = if (song.cover.isNotBlank()) song.cover else DEFAULT_MUSIC_COVER,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = Color(0xFF888888),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = song.duration,
                        color = Color(0xFFB0B3B8),
                        fontSize = 13.sp
                    )

                    IconButton(onClick = { onToggleLikeSong(song) }) {
                        Icon(
                            imageVector = if ("music:${song.id}" in likedTrackKeys) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if ("music:${song.id}" in likedTrackKeys) Color(0xFFFF4D8D) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. CREATOR STUDIO DASHBOARD
// ==========================================

@Composable
private fun CreatorStudioDashboard(
    currentUser: User,
    userSongs: List<Song>,
    myTotalPlays: Long,
    trackPlays: Map<String, Long>,
    onOpenUpload: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onDeleteSong: (String) -> Unit
) {
    val totalLikes = remember(userSongs) {
        userSongs.sumOf { it.stats.likes }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Creator Studio",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upload your music and albums. Monitor your listener analytics.",
                        color = Color(0xFF888888),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onOpenUpload,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload New Content", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Your Uploads",
                    value = "${userSongs.size}",
                    icon = Icons.Default.Upload,
                    iconTint = Color(0xFF45BD62),
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "Total Plays",
                    value = formatCompactNumber(myTotalPlays),
                    icon = Icons.Default.PlayCircle,
                    iconTint = Color(0xFF07E8F8),
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "Likes Received",
                    value = formatCompactNumber(totalLikes),
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFFF4D8D),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Catalog Header
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0B1120),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Catalog", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Manage your uploaded music tracks", color = Color(0xFF888888), fontSize = 12.sp)
                }
            }
        }

        if (userSongs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0B1120),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No uploads yet. Tap 'Upload New Content' above to distribute your first song!",
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(userSongs, key = { it.id }) { song ->
                val plays = trackPlays["music:${song.id}"] ?: song.stats.plays
                Surface(
                    onClick = { onPlaySong(song) },
                    color = Color(0xFF0B1120),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = if (song.cover.isNotBlank()) song.cover else DEFAULT_MUSIC_COVER,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${song.artist} • ${song.album}", color = Color(0xFF888888), fontSize = 12.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("${formatCompactNumber(plays)} plays", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${song.stats.likes} likes", color = Color(0xFF888888), fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { onDeleteSong(song.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0B1120),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color(0xFFB0B3B8), fontSize = 11.sp)
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 5. UPLOAD SONG STUDIO VIEW
// ==========================================

@Composable
private fun UploadSongStudio(
    currentUser: User,
    uploadProgress: Int?,
    uploadStatus: String?,
    onBack: () -> Unit,
    onSubmitUpload: (title: String, artist: String, genre: String, album: String, coverUrl: String, audioUrl: String) -> Unit
) {
    var mode by remember { mutableStateOf("single") } // "single" vs "album"
    var artistName by remember { mutableStateOf(currentUser.name) }
    var genre by remember { mutableStateOf("Afrobeats") }
    var coverUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600") }
    var songTitle by remember { mutableStateOf("") }
    var albumTitle by remember { mutableStateOf("") }
    var albumTracks by remember { mutableStateOf(listOf("Track 1 - Intro", "Track 2 - Summer Vibe")) }
    var newTrackInput by remember { mutableStateOf("") }

    val sampleCovers = listOf(
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600",
        "https://images.unsplash.com/photo-1514525253440-b393452e8d26?w=600",
        "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600",
        "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Column {
                    Text("Creator Upload Studio", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Distribute high-fidelity singles or albums to F-Music", color = Color(0xFFA8AFBC), fontSize = 13.sp)
                }
            }
        }

        // Upload progress indicator if uploading
        if (uploadProgress != null) {
            item {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1877F2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(uploadStatus ?: "Uploading...", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$uploadProgress%", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress / 100f },
                            color = Color(0xFF1877F2),
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Mode Switch: Single vs Album
        item {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF070D1D))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { mode = "single" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == "single") Color(0xFF1877F2) else Color.Transparent,
                                contentColor = if (mode == "single") Color.White else Color(0xFF888888)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Single", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { mode = "album" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == "album") Color(0xFF1877F2) else Color.Transparent,
                                contentColor = if (mode == "album") Color.White else Color(0xFF888888)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Album", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("MAIN ARTIST NAME", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = artistName,
                        onValueChange = { artistName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF070D1D),
                            unfocusedContainerColor = Color(0xFF070D1D),
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color(0xFF1E293B)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("GENRE / CATEGORY", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        placeholder = { Text("Pop, Hip Hop, Bongo Fleva, Amapiano...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF070D1D),
                            unfocusedContainerColor = Color(0xFF070D1D),
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color(0xFF1E293B)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("COVER ARTWORK", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleCovers.forEach { sampleUrl ->
                            val isChosen = coverUrl == sampleUrl
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, if (isChosen) Color(0xFF1877F2) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { coverUrl = sampleUrl }
                            ) {
                                AsyncImage(
                                    model = sampleUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (mode == "single") {
                        Text("SONG TITLE", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = songTitle,
                            onValueChange = { songTitle = it },
                            placeholder = { Text("Enter track name...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF070D1D),
                                unfocusedContainerColor = Color(0xFF070D1D),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF1E293B)
                            )
                        )
                    } else {
                        Text("ALBUM NAME", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = albumTitle,
                            onValueChange = { albumTitle = it },
                            placeholder = { Text("Enter album name...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF070D1D),
                                unfocusedContainerColor = Color(0xFF070D1D),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF1E293B)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("TRACKS IN ALBUM (${albumTracks.size})", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        albumTracks.forEachIndexed { idx, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${idx + 1}. $track", color = Color.White, fontSize = 13.sp)
                                IconButton(
                                    onClick = { albumTracks = albumTracks.filterIndexed { i, _ -> i != idx } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTrackInput,
                                onValueChange = { newTrackInput = it },
                                placeholder = { Text("New track title...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (newTrackInput.isNotBlank()) {
                                        albumTracks = albumTracks + newTrackInput.trim()
                                        newTrackInput = ""
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val finalTitle = if (mode == "single") songTitle.ifBlank { "New Single" } else albumTracks.firstOrNull() ?: "New Track"
                            val finalAlbum = if (mode == "single") "Single" else albumTitle.ifBlank { "New Album" }
                            onSubmitUpload(
                                finalTitle,
                                artistName.ifBlank { currentUser.name },
                                genre,
                                finalAlbum,
                                coverUrl,
                                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publish to F-Music", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. ARTIST PROFILE RELEASES VIEW
// ==========================================

@Composable
private fun ArtistProfileReleasesContent(
    artistUser: User?,
    artistSongs: List<Song>,
    likedTrackKeys: Set<String>,
    onPlaySong: (Song) -> Unit,
    onToggleLikeSong: (Song) -> Unit,
    onBack: () -> Unit
) {
    val artistName = artistUser?.name ?: "Artist"
    val artistAvatar = artistUser?.profileImageUrl ?: "https://images.unsplash.com/photo-1514525253440-b393452e8d26?w=400"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1514525253440-b393452e8d26?w=1200",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF050B18))
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AsyncImage(
                        model = artistAvatar,
                        contentDescription = artistName,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF050B18), CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Column {
                        Text(
                            text = artistName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${artistSongs.size} releases on F-Music",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Popular Releases",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        itemsIndexed(artistSongs, key = { _, s -> s.id }) { index, song ->
            SongRankedListItem(
                index = index + 1,
                song = song,
                isLiked = "music:${song.id}" in likedTrackKeys,
                isCurrent = false,
                onPlay = { onPlaySong(song) },
                onToggleLike = { onToggleLikeSong(song) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

private fun formatCompactNumber(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fm", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}
