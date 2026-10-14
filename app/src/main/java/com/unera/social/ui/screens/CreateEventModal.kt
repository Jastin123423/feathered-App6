package com.unera.social.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Color tokens matching the web Feathered palette
private val PageBackground = Color(0xFF050B18)
private val TopBarBackground = Color(0xFF0B1120)
private val SectionCardBg = Color(0xFF0B1120)
private val InputBackground = Color(0xFF1E293B)
private val BorderColor = Color(0xFF1E293B)
private val BrandBlue = Color(0xFF1877F2)
private val BrandBlueHover = Color(0xFF166FE5)
private val LocalTargetRed = Color(0xFFF02849)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val TextDisabled = Color(0xFF64748B)

// Curated stock cover options for fast selection
private val PRESET_COVERS = listOf(
    "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800" to "Tech Summit",
    "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800" to "Beach Bonfire",
    "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800" to "Gala & Party",
    "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800" to "Concert Stage",
    "https://images.unsplash.com/photo-1515187029135-18ee286d815b?w=800" to "Business Meet"
)

// Pre-seeded geographic venue suggestions (Tanzania, East Africa & Global)
private val DEFAULT_LOCATIONS = listOf(
    "Julius Nyerere Int'l Convention Centre, Dar es Salaam",
    "Mlimani City Conference Hall, Dar es Salaam",
    "Kendwa Rocks Beach, Zanzibar",
    "Arusha National Park Gate, Arusha",
    "Sarit Expo Centre, Westlands, Nairobi, Kenya",
    "Kigali Convention Centre, Rwanda",
    "Landmark Centre, Victoria Island, Lagos, Nigeria",
    "Cape Town International Convention Centre, South Africa",
    "Accra International Conference Centre, Ghana",
    "Times Square, New York, USA",
    "ExCeL London, United Kingdom"
)

/**
 * Location search bar with suggestions dropdown and manual fallback.
 */
@Composable
fun LocationSearch(
    value: String,
    onChangeText: (String) -> Unit,
    onSelect: (String) -> Unit,
    userFallbackLocation: String = "",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val filteredSuggestions = remember(value) {
        val query = value.trim().lowercase()
        if (query.isEmpty()) {
            DEFAULT_LOCATIONS.take(5)
        } else {
            DEFAULT_LOCATIONS.filter { it.lowercase().contains(query) }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onChangeText(it)
                    expanded = true
                },
                placeholder = {
                    Text(
                        text = if (userFallbackLocation.isNotEmpty() && value.isEmpty()) {
                            "Type location... ($userFallbackLocation)"
                        } else {
                            "Type location or search city, venue, or address..."
                        },
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location Pin",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = {
                            onChangeText("")
                            expanded = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground,
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = BrandBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("event_location_input")
            )
        }

        // Suggestions Dropdown
        AnimatedVisibility(
            visible = expanded && (filteredSuggestions.isNotEmpty() || value.trim().length >= 3),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SectionCardBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderColor, BorderColor))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (filteredSuggestions.isNotEmpty()) {
                        filteredSuggestions.forEach { suggestion ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(suggestion)
                                        expanded = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = suggestion,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else if (value.trim().length >= 3) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Your typed location will be used.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-page Create Event Modal matching Feathered Web CreateEventModal.
 */
@Composable
fun CreateEventModal(
    currentUser: User,
    onClose: () -> Unit,
    onCreate: (
        title: String,
        description: String,
        date: String,
        time: String,
        location: String,
        visibility: String,
        coverUrl: String,
        groupId: Long?,
        groupName: String?
    ) -> Unit,
    groupId: Long? = null,
    groupName: String? = null
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(if (groupId != null) "group" else "worldwide") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Android Native Visual Media Picker (Zero-permission photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
            errorMessage = null
        }
    }

    val userFallbackLocation = remember(currentUser) {
        // Safe location from user
        "Dar es Salaam, Tanzania"
    }

    val pageTitle = remember(groupId, groupName) {
        if (groupId != null) "Create Event in ${groupName ?: "Group"}" else "Create event"
    }

    fun submitEvent() {
        errorMessage = null

        if (title.trim().isEmpty()) {
            errorMessage = "Event name is required"
            return
        }

        if (date.trim().isEmpty()) {
            errorMessage = "Date is required"
            return
        }

        if (time.trim().isEmpty()) {
            errorMessage = "Time is required"
            return
        }

        val finalLocation = location.trim().ifEmpty { userFallbackLocation }
        if (finalLocation.isEmpty()) {
            errorMessage = "Location is required"
            return
        }

        isUploading = true
        coroutineScope.launch {
            delay(350) // Smooth optimistic transition
            val finalCover = imageUri ?: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1500&q=80"
            onCreate(
                title.trim(),
                desc.trim(),
                date.trim(),
                time.trim(),
                finalLocation,
                if (groupId != null) "group" else visibility,
                finalCover,
                groupId,
                groupName
            )
            isUploading = false
            onClose()
        }
    }

    Dialog(
        onDismissRequest = { if (!isUploading) onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBackground)
                .testTag("create_event_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Top Bar
                Surface(
                    color = TopBarBackground,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onClose,
                            enabled = !isUploading,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("create_event_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = pageTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        TextButton(
                            onClick = { submitEvent() },
                            enabled = !isUploading,
                            modifier = Modifier.testTag("create_event_submit_top_btn")
                        ) {
                            Text(
                                text = if (isUploading) "Creating..." else "Create",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isUploading) TextDisabled else BrandBlue
                            )
                        }
                    }
                }

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Error Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFEF4444).copy(alpha = 0.35f), Color(0xFFEF4444).copy(alpha = 0.35f))
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFFEE2E2),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // 1. Cover Photo Section
                    Column {
                        Text(
                            text = "Cover photo",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = TextWhite,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(208.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .background(TopBarBackground)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("event_cover_picker")
                        ) {
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Event Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Gradient overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.45f)
                                                )
                                            )
                                        )
                                    )

                                // "Change cover" pill button
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Change cover",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                // Clear cover "X" button
                                IconButton(
                                    onClick = { imageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Cover",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(InputBackground, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = TextWhite,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Add cover photo",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap to choose from gallery or use presets below",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        // Presets Quick Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = if (imageUri != null) "1 cover selected" else "Presets:",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(PRESET_COVERS) { (url, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (imageUri == url) BrandBlue.copy(alpha = 0.25f) else InputBackground,
                                        border = if (imageUri == url) ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = Brush.linearGradient(listOf(BrandBlue, BrandBlue))
                                        ) else null,
                                        modifier = Modifier.clickable {
                                            imageUri = url
                                            errorMessage = null
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (imageUri == url) FontWeight.Bold else FontWeight.Normal,
                                            color = if (imageUri == url) BrandBlue else TextWhite,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Event Type / Group Context Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SectionCardBg),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderColor, BorderColor))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(InputBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (groupId != null) Icons.Default.Groups else Icons.Default.Public,
                                    contentDescription = null,
                                    tint = TextWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (groupId != null) "Posting in ${groupName ?: "Group"}" else "Public Event",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = TextWhite
                                )
                                Text(
                                    text = if (groupId != null) {
                                        "Members of this group can discover and engage with this event."
                                    } else {
                                        "Create an event for the Feathered community."
                                    },
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // 3. Event Name Input
                    Column {
                        Text(
                            text = "Event name",
                            fontSize = 14.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("What is your event called?", color = TextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = BrandBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("event_title_input")
                        )
                    }

                    // 4. Start Date & Time
                    Column {
                        Text(
                            text = "Start date & time",
                            fontSize = 14.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                placeholder = { Text("e.g. Nov 14, 2026", color = TextMuted, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputBackground,
                                    unfocusedContainerColor = InputBackground,
                                    focusedBorderColor = BrandBlue,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    cursorColor = BrandBlue
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("event_date_input")
                            )

                            OutlinedTextField(
                                value = time,
                                onValueChange = { time = it },
                                placeholder = { Text("e.g. 6:30 PM", color = TextMuted, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = InputBackground,
                                    unfocusedContainerColor = InputBackground,
                                    focusedBorderColor = BrandBlue,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    cursorColor = BrandBlue
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("event_time_input")
                            )
                        }

                        // Quick date helpers
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            listOf("Today", "Tomorrow", "Next Friday", "Next Saturday").forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = InputBackground,
                                    modifier = Modifier.clickable { date = suggestion }
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 5. Location Search
                    Column {
                        Text(
                            text = "Location",
                            fontSize = 14.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LocationSearch(
                            value = location,
                            onChangeText = { location = it },
                            onSelect = { location = it },
                            userFallbackLocation = userFallbackLocation
                        )

                        Text(
                            text = "You can type manually even if location search does not work.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    // 6. Audience / Visibility (Public events only)
                    if (groupId == null) {
                        Column {
                            Text(
                                text = "Audience",
                                fontSize = 14.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Worldwide card
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (visibility == "worldwide") BrandBlue.copy(alpha = 0.12f) else SectionCardBg
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(
                                            listOf(
                                                if (visibility == "worldwide") BrandBlue else BorderColor,
                                                if (visibility == "worldwide") BrandBlue else BorderColor
                                            )
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { visibility = "worldwide" }
                                        .testTag("audience_worldwide_btn")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = null,
                                                tint = BrandBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Worldwide",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextWhite
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Visible broadly across Feathered.",
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                // Local Only card
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (visibility == "targeted") BrandBlue.copy(alpha = 0.12f) else SectionCardBg
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(
                                            listOf(
                                                if (visibility == "targeted") BrandBlue else BorderColor,
                                                if (visibility == "targeted") BrandBlue else BorderColor
                                            )
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { visibility = "targeted" }
                                        .testTag("audience_local_btn")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = LocalTargetRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Local Only",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextWhite
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Prioritize people near this location.",
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            if (visibility == "targeted") {
                                Text(
                                    text = "The system will try to target users near the location you entered.",
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = TextMuted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    // 7. Description Input
                    Column {
                        Text(
                            text = "Description",
                            fontSize = 14.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            placeholder = { Text("Tell people more about this event...", color = TextMuted, fontSize = 14.sp) },
                            shape = RoundedCornerShape(12.dp),
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = BrandBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .testTag("event_desc_input")
                        )
                        Text(
                            text = "Add important details like dress code, speakers, link, or agenda.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    // 8. Organizer Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SectionCardBg),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderColor, BorderColor))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = currentUser.profileImageUrl.ifBlank {
                                    "https://ui-avatars.com/api/?name=${currentUser.name}&background=1877F2&color=fff&bold=true"
                                },
                                contentDescription = "Organizer",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(InputBackground)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser.name.ifBlank { "Feathered User" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Event organizer",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // 9. Bottom Submit Button
                    Button(
                        onClick = { submitEvent() },
                        enabled = !isUploading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue,
                            disabledContainerColor = InputBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_event_bottom_btn")
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                color = TextMuted,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Creating...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextMuted
                            )
                        } else {
                            Text(
                                text = "Create Event",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
