package com.unera.social.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.unera.social.model.User

// Theme colors matching Feathered web design system
private val HelpBgDark = Color(0xFF050B18)
private val HelpCardDark = Color(0xFF0F172A)
private val HelpCardElevated = Color(0xFF141E33)
private val HelpBorderDark = Color(0xFF1E293B)
private val HelpBlue = Color(0xFF1877F2)
private val HelpBlueHover = Color(0xFF166FE5)
private val HelpBlueDark = Color(0xFF263951)
private val HelpTextPrimary = Color(0xFFF8FAFC)
private val HelpTextSecondary = Color(0xFF94A3B8)
private val HelpTextMuted = Color(0xFF64748B)

data class HelpArticle(
    val id: String,
    val title: String,
    val category: String,
    val preview: String,
    val heroImageUrl: String,
    val steps: List<String>,
    val tip: String? = null,
    val subSections: List<HelpSubSection> = emptyList()
)

data class HelpSubSection(
    val title: String,
    val icon: ImageVector,
    val imageUrl: String,
    val steps: List<String>
)

data class FaqItem(
    val question: String,
    val answer: String
)

val HELP_ARTICLES = listOf(
    HelpArticle(
        id = "create-account",
        title = "How to create an account",
        category = "Account",
        preview = "Joining Feathered is quick and easy. Follow these steps to get started connecting with your community.",
        heroImageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Open the Feathered website or mobile app.",
            "On the login screen, click the green \"Create New Account\" button.",
            "A registration form will appear. Enter your First Name, Surname, and Email Address.",
            "Create a secure Password (must be at least 6 digits/characters).",
            "Select your Date of Birth and Gender.",
            "Click Sign Up to complete the process and join the community."
        ),
        tip = "Use an email address you have access to. This will be needed if you ever need to reset your password or verify your account."
    ),
    HelpArticle(
        id = "upload-profile",
        title = "How to upload profile & cover photos",
        category = "Profile",
        preview = "Personalize your profile to let friends know it's you. Your profile picture and cover photo are the first things people see.",
        heroImageUrl = "https://images.unsplash.com/photo-1511367461989-f85a21fda167?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Navigate to your Profile by clicking your avatar or name in the navigation bar.",
            "To update your avatar, tap the camera icon overlay on your circular profile photo.",
            "To update your cover banner, tap 'Edit Cover Photo' in the header area.",
            "Select an image from your device or choose from curated banner covers.",
            "Save your updates to refresh your public profile instantly."
        ),
        subSections = listOf(
            HelpSubSection(
                title = "Profile Picture",
                icon = Icons.Default.CameraAlt,
                imageUrl = "https://images.unsplash.com/photo-1511367461989-f85a21fda167?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80",
                steps = listOf(
                    "Go to your profile by clicking your name.",
                    "Click the Camera Icon on your circular picture.",
                    "Select Upload Photo."
                )
            ),
            HelpSubSection(
                title = "Cover Photo",
                icon = Icons.Default.Image,
                imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80",
                steps = listOf(
                    "Go to your profile page.",
                    "Click 'Edit Cover Photo' in the banner area.",
                    "Choose an image that represents you."
                )
            )
        ),
        tip = "High-resolution square photos work best for avatars, while panoramic landscape images look great on cover banners."
    ),
    HelpArticle(
        id = "music",
        title = "How to discover and upload music",
        category = "Music",
        preview = "Feathered features a dedicated music system for creators and listeners. Stream tracks, build collections, or publish your own work.",
        heroImageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Navigate to the Music tab from the bottom bar or navigation menu.",
            "Browse Top Charts, Handpicked Gems, or explore albums from local and international creators.",
            "Tap any track to play instantly using the floating bottom audio player with background playback.",
            "Creators can tap '+ Upload Song' to upload MP3 audio, cover artwork, title, and genre.",
            "Track plays and listener metrics update live across the Feathered network."
        ),
        tip = "You can save songs to your library or share them directly to the Feathered feed."
    ),
    HelpArticle(
        id = "marketplace",
        title = "How to buy and sell on Marketplace",
        category = "Marketplace",
        preview = "Buy and sell electronics, clothing, vehicles, and everyday goods locally with zero transaction fees.",
        heroImageUrl = "https://images.unsplash.com/photo-1472851294608-062f824d29cc?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Open the Marketplace tab to browse items available near you.",
            "Filter by categories like Electronics, Vehicles, Property, or Apparel, and set price ranges.",
            "Tap 'Sell Item' to list your product with photos, price, location, condition, and details.",
            "Message buyers or sellers directly inside the app to arrange safe local pickup.",
            "Mark items as sold once your transaction is finished."
        ),
        tip = "Meet in well-lit public places for in-person transactions and inspect items before payment."
    ),
    HelpArticle(
        id = "reels-videos",
        title = "Watching and sharing Reels",
        category = "Reels & Video",
        preview = "Experience seamless short-form vertical video clips and trending audio on Feathered Reels.",
        heroImageUrl = "https://images.unsplash.com/photo-1536240478700-b869070f9279?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Tap the Reels tab to enter full-screen vertical video browsing.",
            "Swipe up to discover the next reel, tap to toggle play/pause, or double-tap to send a bursting heart.",
            "Tap the chat bubble to view discussions or add your own comment.",
            "Tap the share button to forward clips to friends or share to external apps.",
            "Tap the audio pill at the bottom of any reel to discover other clips using the same soundtrack."
        ),
        tip = "You can tap the top-right button on any video in your feed to open it in full video player mode."
    ),
    HelpArticle(
        id = "events",
        title = "Creating and joining events",
        category = "Events",
        preview = "Stay connected with festivals, meetups, tech conferences, and community gatherings.",
        heroImageUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Go to the Events tab to browse upcoming events.",
            "Click 'Going' or 'Interested' to save the event and receive reminders.",
            "Tap 'Create Event' to set up a new public or private event with banner, date, time, and location.",
            "Invite your friends or community members with a single tap.",
            "Filter events by date: Today, This Weekend, or Upcoming."
        ),
        tip = "Organizers can pin links, ticket purchase information, and custom schedules to their event."
    ),
    HelpArticle(
        id = "privacy-security",
        title = "Privacy and security settings",
        category = "Safety & Privacy",
        preview = "Take control of your data, audience visibility, and account security on Feathered.",
        heroImageUrl = "https://images.unsplash.com/photo-1563986768609-322da13575f3?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Go to Menu -> Settings & Privacy.",
            "Under Privacy Settings, choose who can view your posts (Public, Friends, or Only Me).",
            "Manage Blocked Users to prevent specific accounts from contacting or viewing your profile.",
            "Update your password regularly from the Security tab.",
            "Review connected sessions and active devices to ensure account safety."
        ),
        tip = "Never share your password or verification codes with anyone."
    ),
    HelpArticle(
        id = "reporting",
        title = "How to report content or users",
        category = "Safety & Privacy",
        preview = "We are committed to maintaining a safe, welcoming, and harassment-free community for everyone.",
        heroImageUrl = "https://images.unsplash.com/photo-1450133064473-71024230f91b?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Locate the post, comment, message, or profile you want to report.",
            "Tap the three-dots (•••) menu in the top corner.",
            "Select 'Report' and choose the category (Spam, Harassment, Inappropriate content, Misinformation).",
            "Add details if requested and submit. Our moderation team reviews reports promptly.",
            "You can also block the user immediately so their content disappears from your feed."
        ),
        tip = "Reports are always anonymous. The person reported will not know who submitted the report."
    ),
    HelpArticle(
        id = "groups",
        title = "Finding and managing Groups",
        category = "Groups",
        preview = "Join communities of shared interests, collaborate with peers, post discussions, and explore career and hobby groups on Feathered.",
        heroImageUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?ixlib=rb-1.2.1&auto=format&fit=crop&w=1200&q=80",
        steps = listOf(
            "Navigate to Groups from the menu or shortcut bar.",
            "Browse suggested groups by interest or search by keywords.",
            "Tap 'Join Group' to participate in member discussions, share posts, and attend group events.",
            "Create your own group with custom rules, privacy (Public/Private), and cover art.",
            "Group admins can invite friends, manage moderators, and review member requests."
        ),
        tip = "Group posts show up in your main feed alongside posts from friends you follow."
    )
)

val FAQ_ITEMS = listOf(
    FaqItem(
        question = "Is Feathered free to use?",
        answer = "Yes! Feathered is 100% free for users, creators, and sellers. All standard features, messaging, marketplace listings, and ad promotions are free with zero hidden costs."
    ),
    FaqItem(
        question = "How do I change my display name or bio?",
        answer = "Head to your Profile, tap 'Edit Profile', enter your new name, bio, location, or website, and tap Save Changes. Your profile updates instantly across the app."
    ),
    FaqItem(
        question = "Can I download or save posts for later?",
        answer = "Yes! Tap the bookmark icon on any post or reel to save it to your Saved Posts collection. Access your saved items anytime from the side menu."
    ),
    FaqItem(
        question = "How do I contact customer support?",
        answer = "You can send us a message through the contact form below or email us directly at support@feathered.social. Our support team typically responds within 24 hours."
    )
)

val HELP_CATEGORIES = listOf(
    "All Topics",
    "Account",
    "Profile",
    "Music",
    "Marketplace",
    "Reels & Video",
    "Events",
    "Groups",
    "Safety & Privacy"
)

/**
 * HelpSupportScreen - Comprehensive Android counterpart matching web HelpSupport.tsx.
 * Provides live search, category filtering, detailed article views, FAQ accordion,
 * and a direct Contact Support dialog.
 */
@Composable
fun HelpSupportScreen(
    currentUser: User?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All Topics") }
    var selectedArticle by remember { mutableStateOf<HelpArticle?>(null) }
    var showContactDialog by remember { mutableStateOf(false) }

    // Intercept hardware back button when viewing an article
    BackHandler(enabled = selectedArticle != null) {
        selectedArticle = null
    }

    if (selectedArticle != null) {
        HelpArticleDetailScreen(
            article = selectedArticle!!,
            onBack = { selectedArticle = null }
        )
    } else {
        HelpSupportMainContent(
            currentUser = currentUser,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            onSelectArticle = { selectedArticle = it },
            onOpenContact = { showContactDialog = true },
            onBack = onBack,
            modifier = modifier
        )
    }

    if (showContactDialog) {
        ContactSupportDialog(
            currentUser = currentUser,
            onDismiss = { showContactDialog = false }
        )
    }
}

@Composable
private fun HelpSupportMainContent(
    currentUser: User?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onSelectArticle: (HelpArticle) -> Unit,
    onOpenContact: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredArticles = remember(searchQuery, selectedCategory) {
        HELP_ARTICLES.filter { article ->
            val matchesCategory = selectedCategory == "All Topics" || article.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                article.title.contains(searchQuery, ignoreCase = true) ||
                article.preview.contains(searchQuery, ignoreCase = true) ||
                article.category.contains(searchQuery, ignoreCase = true) ||
                article.steps.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HelpBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("help_support_screen")
    ) {
        // Top App Bar matching Feathered standard header
        Surface(
            color = HelpCardDark,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, HelpBorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("help_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Help & Support",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onOpenContact,
                    modifier = Modifier.testTag("help_contact_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = "Contact Support",
                        tint = HelpBlue
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Hero Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HelpCardDark),
                    border = BorderStroke(1.dp, HelpBorderDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E293B),
                                        HelpCardDark
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(HelpBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = HelpBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Feathered Help Center",
                                    color = HelpBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "How can we help you today?",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Find step-by-step guides, troubleshoot common issues, or reach out to our team.",
                                color = HelpTextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("help_search_input"),
                    placeholder = {
                        Text(
                            text = "Search articles, guides, or keywords...",
                            color = HelpTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = HelpTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = HelpTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HelpCardDark,
                        unfocusedContainerColor = HelpCardDark,
                        focusedBorderColor = HelpBlue,
                        unfocusedBorderColor = HelpBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Category Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HELP_CATEGORIES.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCategory(category) },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = HelpCardDark,
                                labelColor = HelpTextSecondary,
                                selectedContainerColor = HelpBlue,
                                selectedLabelColor = Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) HelpBlue else HelpBorderDark
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Section Header: Help Articles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Articles & Guides (${filteredArticles.size})",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (searchQuery.isNotBlank() || selectedCategory != "All Topics") {
                        Text(
                            text = "Reset",
                            color = HelpBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                onSearchQueryChange("")
                                onSelectCategory("All Topics")
                            }
                        )
                    }
                }
            }

            // Articles List
            if (filteredArticles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HelpCardDark),
                        border = BorderStroke(1.dp, HelpBorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = HelpTextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No matching articles found",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Try adjusting your search terms or select another category.",
                                color = HelpTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredArticles, key = { it.id }) { article ->
                    HelpArticleCard(
                        article = article,
                        onClick = { onSelectArticle(article) }
                    )
                }
            }

            // FAQ Accordion Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Frequently Asked Questions",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(FAQ_ITEMS) { faq ->
                FaqAccordionCard(faq = faq)
            }

            // Contact Us Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HelpCardDark),
                    border = BorderStroke(1.dp, HelpBorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(HelpBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = HelpBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Still need help?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Our support team is available 24/7 to answer questions, resolve account issues, and hear your feedback.",
                            color = HelpTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenContact,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HelpBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("help_contact_support_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Contact Feathered Support",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Email: support@feathered.social",
                            color = HelpTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HelpArticleCard(
    article: HelpArticle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("help_article_${article.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HelpCardDark),
        border = BorderStroke(1.dp, HelpBorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category Icon / Thumbnail
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HelpCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (article.category) {
                        "Account" -> Icons.Default.Person
                        "Profile" -> Icons.Default.AccountCircle
                        "Music" -> Icons.Default.MusicNote
                        "Marketplace" -> Icons.Default.Storefront
                        "Reels & Video" -> Icons.Default.Videocam
                        "Events" -> Icons.Default.Event
                        "Groups" -> Icons.Default.Groups
                        else -> Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = HelpBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = HelpBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = article.category,
                        color = HelpBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = article.preview,
                    color = HelpTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Read Article",
                tint = HelpTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FaqAccordionCard(faq: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HelpCardDark),
        border = BorderStroke(1.dp, HelpBorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = faq.question,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = HelpTextSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = HelpBorderDark, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.answer,
                        color = HelpTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpArticleDetailScreen(
    article: HelpArticle,
    onBack: () -> Unit
) {
    var helpfulFeedback by remember { mutableStateOf<Boolean?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HelpBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("help_article_detail_${article.id}")
    ) {
        // Detail Header
        Surface(
            color = HelpCardDark,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, HelpBorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("help_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = article.category,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Badge
            Surface(
                color = HelpBlue.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = article.category.uppercase(),
                    color = HelpBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Article Title
            Text(
                text = article.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp
            )

            // Article Intro
            Text(
                text = article.preview,
                color = HelpTextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            // Hero Image
            AsyncImage(
                model = article.heroImageUrl,
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, HelpBorderDark, RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )

            // Sub-sections (e.g. Profile Picture & Cover Photo grid)
            if (article.subSections.isNotEmpty()) {
                article.subSections.forEach { sub ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = HelpCardDark),
                        border = BorderStroke(1.dp, HelpBorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            AsyncImage(
                                model = sub.imageUrl,
                                contentDescription = sub.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(HelpCardElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = sub.icon,
                                        contentDescription = null,
                                        tint = HelpBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = sub.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            sub.steps.forEachIndexed { idx, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        color = HelpBlue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = step,
                                        color = HelpTextPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step-by-Step Guide
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HelpCardDark),
                border = BorderStroke(1.dp, HelpBorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Step-by-Step Guide",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    article.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(HelpBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = HelpBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = step,
                                color = HelpTextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Pro Tip Card
            article.tip?.let { tipText ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HelpBlueDark),
                    border = BorderStroke(1.dp, HelpBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tip",
                            tint = HelpBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Pro Tip",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tipText,
                                color = HelpTextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // Helpful Feedback Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HelpCardDark),
                border = BorderStroke(1.dp, HelpBorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Was this article helpful?",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (helpfulFeedback == null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    helpfulFeedback = true
                                    Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, HelpBorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Yes")
                            }

                            OutlinedButton(
                                onClick = {
                                    helpfulFeedback = false
                                    Toast.makeText(context, "Thanks for the feedback. We'll improve this guide.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, HelpBorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("No")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Thank you for your feedback!",
                                color = Color(0xFF34D399),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContactSupportDialog(
    currentUser: User?,
    onDismiss: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Account") }
    var description by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val contactCategories = listOf(
        "Account",
        "Technical Issue",
        "Music & Audio",
        "Marketplace",
        "Report & Safety",
        "General Inquiry"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HelpCardDark),
            border = BorderStroke(1.dp, HelpBorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                if (isSubmitted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Ticket Submitted!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Thank you for reaching out. A Feathered support specialist will review your request and reply via email at ${currentUser?.email ?: "your registered email"}.",
                            color = HelpTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = HelpBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contact Feathered Support",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = HelpTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Category",
                        color = HelpTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        contactCategories.forEach { cat ->
                            val isSel = selectedCategory == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = HelpCardElevated,
                                    labelColor = HelpTextSecondary,
                                    selectedContainerColor = HelpBlue,
                                    selectedLabelColor = Color.White
                                ),
                                border = BorderStroke(1.dp, if (isSel) HelpBlue else HelpBorderDark),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Subject",
                        color = HelpTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Brief summary of your inquiry...", color = HelpTextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = HelpCardElevated,
                            unfocusedContainerColor = HelpCardElevated,
                            focusedBorderColor = HelpBlue,
                            unfocusedBorderColor = HelpBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Description",
                        color = HelpTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("Describe the issue or feedback in detail...", color = HelpTextMuted, fontSize = 13.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = HelpCardElevated,
                            unfocusedContainerColor = HelpCardElevated,
                            focusedBorderColor = HelpBlue,
                            unfocusedBorderColor = HelpBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (subject.isBlank() || description.isBlank()) {
                                Toast.makeText(context, "Please enter both subject and description", Toast.LENGTH_SHORT).show()
                            } else {
                                isSubmitted = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HelpBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("help_submit_ticket_button")
                    ) {
                        Text("Send Support Request", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
