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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.Event

// Feathered Design Tokens
private val BgDark = Color(0xFF050B18)
private val CardBgDark = Color(0xFF0B1120)
private val BorderDark = Color(0xFF1E293B)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val BrandBlue = Color(0xFF1877F2)
private val BrandBlueHover = Color(0xFF166FE5)
private val GoingGreen = Color(0xFF45BD62)
private val InterestedGold = Color(0xFFF7B928)
private val LocationRed = Color(0xFFF02849)

@Composable
fun EventsScreen(
    events: List<Event>,
    onToggleInterested: (Long) -> Unit,
    onToggleGoing: (Long) -> Unit,
    onOpenCreateEvent: () -> Unit,
    onProfileClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Date") } // "Date" or "Popular"
    var previewEvent by remember { mutableStateOf<Event?>(null) }

    val filterOptions = listOf("All", "Upcoming", "Going", "Interested", "Tech & Summits", "Music & Beach")

    val filteredAndSortedEvents = remember(events, selectedFilter, selectedSort, searchQuery) {
        var list = events

        // Filter by category / tab
        list = when (selectedFilter) {
            "Going" -> list.filter { it.isGoing }
            "Interested" -> list.filter { it.isInterested }
            "Upcoming" -> list.filter { !it.date.lowercase().contains("past") }
            "Tech & Summits" -> list.filter {
                it.title.contains("Tech", ignoreCase = true) ||
                it.title.contains("Summit", ignoreCase = true) ||
                it.title.contains("Masterclass", ignoreCase = true)
            }
            "Music & Beach" -> list.filter {
                it.title.contains("Acoustic", ignoreCase = true) ||
                it.title.contains("Beach", ignoreCase = true) ||
                it.title.contains("Zanzibar", ignoreCase = true)
            }
            else -> list
        }

        // Filter by search query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.location.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }

        // Sort
        when (selectedSort) {
            "Popular" -> list.sortedByDescending { it.goingCount + it.interestedCount }
            else -> list
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .testTag("events_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Events",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Live Discovery Pulse Indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GoingGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoingGreen
                            )
                        }
                        Text(
                            text = "Discover conferences, sessions & meetups",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    Button(
                        onClick = onOpenCreateEvent,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("create_event_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Event",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // 2. Search Input Field
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search events by title, venue, or city...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBgDark,
                            unfocusedContainerColor = CardBgDark,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = BrandBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("events_search_bar")
                    )
                }
            }

            // 3. Filter Chips & Sort Controls
            item {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterOptions) { filter ->
                            val isSelected = filter == selectedFilter
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) BrandBlue else CardBgDark,
                                border = if (isSelected) null else BorderStroke(1.dp, BorderDark),
                                modifier = Modifier
                                    .clickable { selectedFilter = filter }
                                    .testTag("event_filter_$filter")
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextMuted,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Sort row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredAndSortedEvents.size} events found",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sort:",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            listOf("Date", "Popular").forEach { sortOption ->
                                val isSortActive = selectedSort == sortOption
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSortActive) BrandBlue.copy(alpha = 0.2f) else Color.Transparent,
                                    modifier = Modifier.clickable { selectedSort = sortOption }
                                ) {
                                    Text(
                                        text = sortOption,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSortActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSortActive) BrandBlue else TextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Events Feed List
            if (filteredAndSortedEvents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CardBgDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No events found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try different keywords or filters, or be the first to host an event!",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onOpenCreateEvent,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Event")
                        }
                    }
                }
            } else {
                items(filteredAndSortedEvents, key = { it.id }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)) {
                        EventCard(
                            event = event,
                            onCardClick = { previewEvent = event },
                            onToggleInterested = { onToggleInterested(event.id) },
                            onToggleGoing = { onToggleGoing(event.id) },
                            onProfileClick = onProfileClick
                        )
                    }
                }
            }
        }

        // 5. Full Detail Preview Modal
        if (previewEvent != null) {
            EventDetailModal(
                event = previewEvent!!,
                onDismiss = { previewEvent = null },
                onToggleInterested = {
                    onToggleInterested(previewEvent!!.id)
                    previewEvent = previewEvent!!.copy(
                        isInterested = !previewEvent!!.isInterested,
                        interestedCount = if (!previewEvent!!.isInterested) previewEvent!!.interestedCount + 1 else (previewEvent!!.interestedCount - 1).coerceAtLeast(0)
                    )
                },
                onToggleGoing = {
                    onToggleGoing(previewEvent!!.id)
                    previewEvent = previewEvent!!.copy(
                        isGoing = !previewEvent!!.isGoing,
                        goingCount = if (!previewEvent!!.isGoing) previewEvent!!.goingCount + 1 else (previewEvent!!.goingCount - 1).coerceAtLeast(0)
                    )
                },
                onProfileClick = onProfileClick
            )
        }
    }
}

/**
 * Modern Event Card matching the web Feathered feed layout.
 */
@Composable
fun EventCard(
    event: Event,
    onCardClick: () -> Unit,
    onToggleInterested: () -> Unit,
    onToggleGoing: () -> Unit,
    onProfileClick: ((Long) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("event_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBgDark)
    ) {
        Column {
            // Cover Image with Top Badges & Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = event.imageUrl.ifBlank {
                        "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800"
                    },
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Date badge (top-left)
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = event.date.take(3).uppercase(),
                            color = InterestedGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = event.date.filter { it.isDigit() }.take(2).ifEmpty { "15" },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Status chip (top-right)
                Surface(
                    color = GoingGreen.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Upcoming",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Social Attendance overlay chips at bottom of cover
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = GoingGreen.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = GoingGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${event.goingCount} Going",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        color = InterestedGold.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = InterestedGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${event.interestedCount} Interested",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Card Body
            Column(modifier = Modifier.padding(14.dp)) {
                // Organizer Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = event.organizer.profileImageUrl.ifBlank {
                            "https://ui-avatars.com/api/?name=${event.organizer.name}&background=1877F2&color=fff&bold=true"
                        },
                        contentDescription = "Organizer",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onProfileClick?.invoke(event.organizer.id) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.organizer.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onProfileClick?.invoke(event.organizer.id) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = BorderDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.createdAt,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date & Time Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = BrandBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.date,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Location Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = LocationRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.location,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (event.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = event.description,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderDark, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // RSVP Action Buttons (Going / Interested)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Going Button
                    Button(
                        onClick = onToggleGoing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (event.isGoing) GoingGreen else BrandBlue
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("event_going_btn_${event.id}")
                    ) {
                        if (event.isGoing) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (event.isGoing) "✓ Going" else "Going",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Interested Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (event.isInterested) InterestedGold else Color(0xFF1E293B),
                        border = if (event.isInterested) null else BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggleInterested() }
                            .testTag("event_interested_btn_${event.id}")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (event.isInterested) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = if (event.isInterested) Color.Black else TextWhite,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (event.isInterested) "✓ Interested" else "Interested?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (event.isInterested) Color.Black else TextWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full Event Detail View Modal matching web Rd isPreview.
 */
@Composable
fun EventDetailModal(
    event: Event,
    onDismiss: () -> Unit,
    onToggleInterested: () -> Unit,
    onToggleGoing: () -> Unit,
    onProfileClick: ((Long) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .testTag("event_detail_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Cover Box (40vh)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    AsyncImage(
                        model = event.imageUrl.ifBlank {
                            "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800"
                        },
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Modal Details Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-20).dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBgDark),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Title
                            Text(
                                text = event.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Date & Time info box
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF1E293B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = event.date,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = if (event.time.isNotBlank()) event.time else "Scheduled time",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Location info box
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF1E293B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = LocationRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Location",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = event.location,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Host info box
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clickable { onProfileClick?.invoke(event.organizer.id) }
                            ) {
                                AsyncImage(
                                    model = event.organizer.profileImageUrl.ifBlank {
                                        "https://ui-avatars.com/api/?name=${event.organizer.name}&background=1877F2&color=fff&bold=true"
                                    },
                                    contentDescription = "Organizer",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Hosted by",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = event.organizer.name,
                                        fontSize = 12.sp,
                                        color = BrandBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderDark, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Social Attendees counters
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = GoingGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${event.goingCount}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = GoingGreen
                                        )
                                        Text(
                                            text = "Attending",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Surface(
                                    color = InterestedGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${event.interestedCount}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = InterestedGold
                                        )
                                        Text(
                                            text = "Interested",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action RSVP buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onToggleGoing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (event.isGoing) GoingGreen else BrandBlue
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (event.isGoing) "✓ Going" else "Going",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Button(
                                    onClick = onToggleInterested,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (event.isInterested) InterestedGold else Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (event.isInterested) "✓ Interested" else "Interested",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (event.isInterested) Color.Black else TextWhite
                                    )
                                }
                            }

                            if (event.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "About this event",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = event.description,
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
