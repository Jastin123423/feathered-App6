package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Color tokens matching web Settings.tsx
private val WebBgDark = Color(0xFF050B18)
private val WebCardDark = Color(0xFF0B1120)
private val WebInputDark = Color(0xFF0F172A)
private val WebBorderDark = Color(0xFF1E293B)
private val WebHoverDark = Color(0xFF334155)
private val WebTextPrimary = Color(0xFFF8FAFC)
private val WebTextSecondary = Color(0xFF94A3B8)
private val WebPrimaryBlue = Color(0xFF1877F2)
private val WebPrimaryBlueHover = Color(0xFF166FE5)
private val WebGreen = Color(0xFF45BD62)
private val WebAmber = Color(0xFFF7B928)
private val WebRed = Color(0xFFDC3545)
private val WebRedHover = Color(0xFFDC2626)
private val WebRedAccent = Color(0xFFF3425F)
private val WebPurple = Color(0xFFB250B3)
private val WebBlueAccent = Color(0xFF0055FF)
private val WebNotificationRed = Color(0xFFE41E3F)

data class SettingsSectionItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val color: Color,
    val action: String? = null,
    val isDestructive: Boolean = false
)

private val ACCOUNT_CENTER_SECTIONS = listOf(
    SettingsSectionItem(
        id = "password",
        icon = Icons.Default.Lock,
        title = "Password & Security",
        desc = "Change your password and protect your account",
        color = WebPrimaryBlue
    ),
    SettingsSectionItem(
        id = "email",
        icon = Icons.Default.Email,
        title = "Email Address",
        desc = "Manage your email preferences",
        color = WebGreen
    ),
    SettingsSectionItem(
        id = "two-factor",
        icon = Icons.Default.Security,
        title = "Two-Factor Authentication",
        desc = "Add an extra layer of security to your account",
        color = WebAmber
    )
)

private val PROFILE_DETAILS_SECTIONS = listOf(
    SettingsSectionItem(
        id = "personal-info",
        icon = Icons.Default.Badge,
        title = "Personal Information",
        desc = "Edit your name, bio, and basic details",
        color = WebPrimaryBlue,
        action = "edit-profile"
    ),
    SettingsSectionItem(
        id = "work-education",
        icon = Icons.Default.Work,
        title = "Work & Education",
        desc = "Update your work and education history",
        color = WebGreen,
        action = "edit-profile"
    ),
    SettingsSectionItem(
        id = "contact",
        icon = Icons.Default.ContactPhone,
        title = "Contact Info",
        desc = "Manage your contact information",
        color = WebRedAccent,
        action = "edit-profile"
    ),
    SettingsSectionItem(
        id = "location",
        icon = Icons.Default.LocationOn,
        title = "Location",
        desc = "Set your current city and hometown",
        color = WebAmber,
        action = "edit-profile"
    ),
    SettingsSectionItem(
        id = "website",
        icon = Icons.Default.Language,
        title = "Website & Social Links",
        desc = "Add links to your website and social profiles",
        color = WebBlueAccent,
        action = "edit-profile"
    )
)

private val OTHER_SECTIONS = listOf(
    SettingsSectionItem(
        id = "privacy-settings",
        icon = Icons.Default.AdminPanelSettings,
        title = "Privacy Settings",
        desc = "Control who can see your profile and posts",
        color = WebPurple
    ),
    SettingsSectionItem(
        id = "notifications",
        icon = Icons.Default.Notifications,
        title = "Notification Preferences",
        desc = "Manage how you receive notifications",
        color = WebNotificationRed
    ),
    SettingsSectionItem(
        id = "blocked",
        icon = Icons.Default.Block,
        title = "Blocked Users",
        desc = "View and manage blocked accounts",
        color = WebRedAccent
    ),
    SettingsSectionItem(
        id = "help-support",
        icon = Icons.Default.HelpOutline,
        title = "Help & Support",
        desc = "Guides, FAQs, and contact support",
        color = WebPrimaryBlue
    ),
    SettingsSectionItem(
        id = "terms-of-service",
        icon = Icons.Default.Description,
        title = "Terms of Service",
        desc = "Read our terms, conditions, and legal policies",
        color = WebBlueAccent
    ),
    SettingsSectionItem(
        id = "delete-account",
        icon = Icons.Default.DeleteOutline,
        title = "Delete Account",
        desc = "Permanently delete your account and all data",
        color = WebRed,
        action = "delete-account",
        isDestructive = true
    )
)

/**
 * SettingsScreen - Matches web Settings.tsx exactly.
 */
@Composable
fun SettingsScreen(
    currentUser: User?,
    onClose: () -> Unit,
    onLogout: () -> Unit,
    onUpdateUserDetails: (name: String, bio: String, work: String, education: String, location: String, website: String, gender: String, birthday: String) -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Boolean,
    onDeleteAccount: () -> Unit,
    onOpenHelpSupport: (() -> Unit)? = null,
    onOpenTerms: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showChangePasswordModal by remember { mutableStateOf(false) }
    var showEditProfileModal by remember { mutableStateOf(false) }
    var showDeleteAccountModal by remember { mutableStateOf(false) }
    var showEmailModal by remember { mutableStateOf(false) }
    var showTwoFactorModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showBlockedModal by remember { mutableStateOf(false) }

    if (currentUser == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(WebBgDark)
                .testTag("settings_screen_unauthenticated"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WebAmber,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please log in to access settings",
                    color = WebTextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WebBgDark)
            .testTag("settings_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Hero Banner: matching web bg-gradient-to-r from-[#0B1120] via-[#0F172A] to-[#0B1120] py-12 px-4 border-b border-[#1E293B]
            item(key = "hero_banner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(WebCardDark, WebInputDark, WebCardDark)
                            )
                        )
                        .border(BorderStroke(1.dp, WebBorderDark))
                        .padding(horizontal = 16.dp, vertical = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 800.dp)
                            .align(Alignment.Center)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(WebBorderDark)
                                    .testTag("settings_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = WebTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            AsyncImage(
                                model = currentUser.profileImageUrl,
                                contentDescription = currentUser.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, WebBorderDark, CircleShape)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Settings & Privacy",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WebTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Manage your account settings and profile information",
                                    fontSize = 13.sp,
                                    color = WebTextSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Main Content Area: max-w-[800px] mx-auto px-4 space-y-6
            // Section 1: Account Center
            item(key = "account_center") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 800.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WebPrimaryBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = WebPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Account Center",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebTextPrimary
                            )
                            Text(
                                text = "Manage your password and account security",
                                fontSize = 13.sp,
                                color = WebTextSecondary
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WebCardDark),
                        border = BorderStroke(1.dp, WebBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            ACCOUNT_CENTER_SECTIONS.forEachIndexed { index, section ->
                                SettingsItemRow(
                                    item = section,
                                    onClick = {
                                        when (section.id) {
                                            "password" -> showChangePasswordModal = true
                                            "email" -> showEmailModal = true
                                            "two-factor" -> showTwoFactorModal = true
                                        }
                                    }
                                )
                                if (index < ACCOUNT_CENTER_SECTIONS.size - 1) {
                                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 2: Profile Details
            item(key = "profile_details") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 800.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WebGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = WebGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Profile Details",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebTextPrimary
                            )
                            Text(
                                text = "Edit your name, bio, work, education, and more",
                                fontSize = 13.sp,
                                color = WebTextSecondary
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WebCardDark),
                        border = BorderStroke(1.dp, WebBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Quick Edit All Profile Details Button: matching web gradient banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(WebPrimaryBlue.copy(alpha = 0.12f), WebGreen.copy(alpha = 0.12f))
                                        )
                                    )
                                    .clickable { showEditProfileModal = true }
                                    .padding(16.dp)
                                    .testTag("settings_quick_edit_all_btn"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(WebPrimaryBlue, WebGreen))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Edit All Profile Details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = WebTextPrimary
                                    )
                                    Text(
                                        text = "Update your full profile information in one place",
                                        fontSize = 13.sp,
                                        color = WebTextSecondary
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = WebPrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                            PROFILE_DETAILS_SECTIONS.forEachIndexed { index, section ->
                                SettingsItemRow(
                                    item = section,
                                    onClick = { showEditProfileModal = true }
                                )
                                if (index < PROFILE_DETAILS_SECTIONS.size - 1) {
                                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 3: More Settings
            item(key = "more_settings") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 800.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WebPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = WebPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "More Settings",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebTextPrimary
                            )
                            Text(
                                text = "Privacy, notifications, and account management",
                                fontSize = 13.sp,
                                color = WebTextSecondary
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WebCardDark),
                        border = BorderStroke(1.dp, WebBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            OTHER_SECTIONS.forEachIndexed { index, section ->
                                SettingsItemRow(
                                    item = section,
                                    onClick = {
                                        when (section.id) {
                                            "privacy-settings" -> showPrivacyModal = true
                                            "notifications" -> showNotificationsModal = true
                                            "blocked" -> showBlockedModal = true
                                            "help-support" -> onOpenHelpSupport?.invoke()
                                            "terms-of-service" -> onOpenTerms?.invoke()
                                            "delete-account" -> showDeleteAccountModal = true
                                        }
                                    }
                                )
                                if (index < OTHER_SECTIONS.size - 1) {
                                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Footer: matching web: text-center pt-4 border-t border-[#1E293B]
            item(key = "footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 800.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Feathered © 2025",
                        fontSize = 13.sp,
                        color = WebTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onLogout,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7F1D1D).copy(alpha = 0.30f),
                            contentColor = Color(0xFFF87171)
                        ),
                        modifier = Modifier
                            .testTag("settings_logout_button")
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log Out",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log Out",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ============================================================
        // MODALS
        // ============================================================

        // Change Password Modal
        if (showChangePasswordModal) {
            ChangePasswordModalDialog(
                onClose = { showChangePasswordModal = false },
                onSubmit = onChangePassword
            )
        }

        // Edit Profile Details Modal
        if (showEditProfileModal) {
            EditProfileDetailsModalDialog(
                user = currentUser,
                onClose = { showEditProfileModal = false },
                onSave = onUpdateUserDetails
            )
        }

        // Delete Account Modal
        if (showDeleteAccountModal) {
            DeleteAccountModalDialog(
                onClose = { showDeleteAccountModal = false },
                onConfirm = {
                    onDeleteAccount()
                    onClose()
                },
                userEmail = currentUser.email
            )
        }

        // Email Address Modal
        if (showEmailModal) {
            EmailAddressModalDialog(
                currentEmail = currentUser.email,
                onClose = { showEmailModal = false }
            )
        }

        // Two-Factor Authentication Modal
        if (showTwoFactorModal) {
            TwoFactorModalDialog(
                onClose = { showTwoFactorModal = false }
            )
        }

        // Privacy Settings Modal
        if (showPrivacyModal) {
            PrivacySettingsModalDialog(
                onClose = { showPrivacyModal = false }
            )
        }

        // Notification Preferences Modal
        if (showNotificationsModal) {
            NotificationPreferencesModalDialog(
                onClose = { showNotificationsModal = false }
            )
        }

        // Blocked Users Modal
        if (showBlockedModal) {
            BlockedUsersModalDialog(
                onClose = { showBlockedModal = false }
            )
        }
    }
}

/**
 * Settings Row Component matching web:
 * flex items-center gap-4 p-4 hover:bg-[#1E293B] cursor-pointer transition-colors group
 */
@Composable
private fun SettingsItemRow(
    item: SettingsSectionItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("settings_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (item.isDestructive) Color(0xFFF87171) else WebTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.desc,
                fontSize = 13.sp,
                color = WebTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (item.isDestructive) Color(0xFFEF4444) else WebTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================
// CHANGE PASSWORD MODAL (Exact match to web ChangePasswordModal)
// ============================================================
@Composable
fun ChangePasswordModalDialog(
    onClose: () -> Unit,
    onSubmit: (currentPassword: String, newPassword: String) -> Boolean
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .testTag("change_password_modal"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WebCardDark),
                border = BorderStroke(1.dp, WebBorderDark)
            ) {
                Column {
                    // Header: p-4 border-b border-[#1E293B] flex items-center gap-3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WebBorderDark)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = WebTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Change Password",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = WebTextPrimary
                        )
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    // Body: p-6 space-y-5
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Error banner
                        if (error.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.30f))
                                    .border(1.dp, Color(0xFFB91C1C).copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Success banner
                        if (success.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF065F46).copy(alpha = 0.30f))
                                    .border(1.dp, Color(0xFF047857).copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = success,
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Current Password
                        Column {
                            Text(
                                text = "Current Password",
                                color = WebTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                placeholder = {
                                    Text("Enter current password", color = Color(0xFF64748B), fontSize = 14.sp)
                                },
                                visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showCurrent = !showCurrent }) {
                                        Icon(
                                            imageVector = if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = WebTextSecondary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = WebBorderDark,
                                    unfocusedContainerColor = WebBorderDark,
                                    focusedTextColor = WebTextPrimary,
                                    unfocusedTextColor = WebTextPrimary,
                                    focusedBorderColor = WebPrimaryBlue,
                                    unfocusedBorderColor = WebBorderDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("current_password_input")
                            )
                        }

                        // New Password
                        Column {
                            Text(
                                text = "New Password",
                                color = WebTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                placeholder = {
                                    Text("Enter new password", color = Color(0xFF64748B), fontSize = 14.sp)
                                },
                                visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showNew = !showNew }) {
                                        Icon(
                                            imageVector = if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = WebTextSecondary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = WebBorderDark,
                                    unfocusedContainerColor = WebBorderDark,
                                    focusedTextColor = WebTextPrimary,
                                    unfocusedTextColor = WebTextPrimary,
                                    focusedBorderColor = WebPrimaryBlue,
                                    unfocusedBorderColor = WebBorderDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_password_input")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Minimum 6 characters",
                                color = WebTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // Confirm New Password
                        Column {
                            Text(
                                text = "Confirm New Password",
                                color = WebTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = {
                                    Text("Re-enter new password", color = Color(0xFF64748B), fontSize = 14.sp)
                                },
                                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showConfirm = !showConfirm }) {
                                        Icon(
                                            imageVector = if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = WebTextSecondary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = WebBorderDark,
                                    unfocusedContainerColor = WebBorderDark,
                                    focusedTextColor = WebTextPrimary,
                                    unfocusedTextColor = WebTextPrimary,
                                    focusedBorderColor = WebPrimaryBlue,
                                    unfocusedBorderColor = WebBorderDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_password_input")
                            )
                        }
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    // Footer: p-4 border-t border-[#1E293B]
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                error = ""
                                success = ""
                                if (currentPassword.trim().isEmpty()) {
                                    error = "Current password is required"
                                    return@Button
                                }
                                if (newPassword.trim().isEmpty()) {
                                    error = "New password is required"
                                    return@Button
                                }
                                if (newPassword.length < 6) {
                                    error = "Password must be at least 6 characters"
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    error = "Passwords do not match"
                                    return@Button
                                }
                                if (currentPassword == newPassword) {
                                    error = "New password must be different from current password"
                                    return@Button
                                }

                                loading = true
                                coroutineScope.launch {
                                    delay(400)
                                    val ok = onSubmit(currentPassword, newPassword)
                                    loading = false
                                    if (ok) {
                                        success = "Password changed successfully!"
                                        delay(1500)
                                        onClose()
                                    } else {
                                        error = "Failed to change password"
                                    }
                                }
                            },
                            enabled = !loading,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WebPrimaryBlue,
                                disabledContainerColor = WebPrimaryBlue.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_password_button")
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Changing Password...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Save Changes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
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

// ============================================================
// DELETE ACCOUNT MODAL (Exact match to web DeleteAccountModal)
// ============================================================
@Composable
fun DeleteAccountModalDialog(
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    userEmail: String? = null
) {
    var step by remember { mutableStateOf("confirm") } // "confirm" or "password"
    var confirmText by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .testTag("delete_account_modal"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WebCardDark),
                border = BorderStroke(1.dp, WebBorderDark)
            ) {
                Column {
                    // Header with Red tint: p-4 border-b border-[#1E293B] flex items-center gap-3 bg-red-900/20
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.20f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.50f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Delete Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = WebTextPrimary
                        )
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    // Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (step == "confirm") {
                            // Warning Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.20f))
                                    .border(1.dp, Color(0xFFB91C1C).copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "⚠️ Warning: This action is irreversible!",
                                        color = Color(0xFFF87171),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Deleting your account will permanently remove:",
                                        color = WebTextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val items = listOf(
                                        "All your posts, comments, and reactions",
                                        "Your stories and reels",
                                        "Your messages and conversations",
                                        "Your groups and marketplace listings",
                                        "All your personal data"
                                    )
                                    items.forEach { bullet ->
                                        Text(
                                            text = "• $bullet",
                                            color = WebTextSecondary,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            // Input Confirmation text
                            Column {
                                Row {
                                    Text(
                                        text = "Type ",
                                        color = WebTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "DELETE",
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = " to confirm",
                                        color = WebTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = confirmText,
                                    onValueChange = { confirmText = it },
                                    placeholder = {
                                        Text("Type DELETE here", color = Color(0xFF64748B), fontSize = 14.sp)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = WebBorderDark,
                                        unfocusedContainerColor = WebBorderDark,
                                        focusedTextColor = WebTextPrimary,
                                        unfocusedTextColor = WebTextPrimary,
                                        focusedBorderColor = Color(0xFFEF4444),
                                        unfocusedBorderColor = WebBorderDark
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("delete_confirm_text_input")
                                )
                            }
                        } else {
                            // Password Step
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.20f))
                                    .border(1.dp, Color(0xFFB91C1C).copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "⚠️ Final Step: Confirm with Password",
                                        color = Color(0xFFF87171),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please enter your password to permanently delete your account.",
                                        color = WebTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Your Password",
                                    color = WebTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    placeholder = {
                                        Text("Enter your password", color = Color(0xFF64748B), fontSize = 14.sp)
                                    },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = WebTextSecondary
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = WebBorderDark,
                                        unfocusedContainerColor = WebBorderDark,
                                        focusedTextColor = WebTextPrimary,
                                        unfocusedTextColor = WebTextPrimary,
                                        focusedBorderColor = Color(0xFFEF4444),
                                        unfocusedBorderColor = WebBorderDark
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("delete_password_input")
                                )
                            }
                        }

                        // Error Banner
                        if (error.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.30f))
                                    .border(1.dp, Color(0xFFB91C1C).copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    // Footer Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onClose,
                            enabled = !loading,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WebBorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = WebTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = {
                                error = ""
                                if (step == "confirm") {
                                    if (confirmText.trim() != "DELETE") {
                                        error = "Please type \"DELETE\" to confirm"
                                        return@Button
                                    }
                                    step = "password"
                                } else {
                                    if (password.trim().isEmpty()) {
                                        error = "Please enter your password to confirm deletion"
                                        return@Button
                                    }
                                    loading = true
                                    coroutineScope.launch {
                                        delay(800)
                                        loading = false
                                        onConfirm()
                                    }
                                }
                            },
                            enabled = !loading,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WebRedHover,
                                disabledContainerColor = WebRedHover.copy(alpha = 0.50f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("delete_account_confirm_btn")
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Deleting...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (step == "confirm") "Confirm Deletion" else "Permanently Delete Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
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

// ============================================================
// EDIT PROFILE DETAILS MODAL (Exact match to web EditProfileDetailsModal)
// ============================================================
@Composable
fun EditProfileDetailsModalDialog(
    user: User,
    onClose: () -> Unit,
    onSave: (name: String, bio: String, work: String, education: String, location: String, website: String, gender: String, birthday: String) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var bio by remember { mutableStateOf(user.bio) }
    var work by remember { mutableStateOf(user.work) }
    var education by remember { mutableStateOf(user.education) }
    var location by remember { mutableStateOf(user.location) }
    var website by remember { mutableStateOf(user.website) }
    var gender by remember { mutableStateOf(user.gender.ifBlank { "Male" }) }
    var birthday by remember { mutableStateOf(user.birthDate ?: "1998-10-11") }
    var error by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .heightIn(max = 680.dp)
                    .testTag("edit_profile_details_modal"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WebCardDark),
                border = BorderStroke(1.dp, WebBorderDark)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header: p-4 border-b border-[#1E293B] flex items-center gap-3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WebBorderDark)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = WebTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Edit Profile Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = WebTextPrimary
                        )
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    // Scrollable Body: p-6 overflow-y-auto space-y-5 flex-1
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        if (error.isNotBlank()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.30f))
                                        .border(1.dp, Color(0xFFB91C1C).copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Full Name
                        item {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = WebTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Full Name",
                                        color = WebTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = {
                                        Text("Your full name", color = Color(0xFF64748B), fontSize = 14.sp)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = WebInputDark,
                                        unfocusedContainerColor = WebInputDark,
                                        focusedTextColor = WebTextPrimary,
                                        unfocusedTextColor = WebTextPrimary,
                                        focusedBorderColor = WebPrimaryBlue,
                                        unfocusedBorderColor = WebBorderDark
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_name_input")
                                )
                            }
                        }

                        // Bio
                        item {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = WebTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bio",
                                        color = WebTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { if (it.length <= 500) bio = it },
                                    placeholder = {
                                        Text("Tell people about yourself...", color = Color(0xFF64748B), fontSize = 14.sp)
                                    },
                                    minLines = 3,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = WebInputDark,
                                        unfocusedContainerColor = WebInputDark,
                                        focusedTextColor = WebTextPrimary,
                                        unfocusedTextColor = WebTextPrimary,
                                        focusedBorderColor = WebPrimaryBlue,
                                        unfocusedBorderColor = WebBorderDark
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_bio_input")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${bio.length}/500 characters",
                                    color = WebTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Additional Details Header
                        item {
                            Column {
                                HorizontalDivider(color = WebBorderDark, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = WebPrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Additional Details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = WebTextPrimary
                                    )
                                }
                            }
                        }

                        // Work
                        item {
                            WebEditField(
                                label = "Work",
                                value = work,
                                onValueChange = { work = it },
                                icon = Icons.Default.Work,
                                placeholder = "Where do you work?",
                                tag = "edit_work_input"
                            )
                        }

                        // Education
                        item {
                            WebEditField(
                                label = "Education",
                                value = education,
                                onValueChange = { education = it },
                                icon = Icons.Default.School,
                                placeholder = "Where did you study?",
                                tag = "edit_education_input"
                            )
                        }

                        // Current City
                        item {
                            WebEditField(
                                label = "Current City",
                                value = location,
                                onValueChange = { location = it },
                                icon = Icons.Default.LocationOn,
                                placeholder = "Your current city",
                                tag = "edit_location_input"
                            )
                        }

                        // Website
                        item {
                            WebEditField(
                                label = "Website",
                                value = website,
                                onValueChange = { website = it },
                                icon = Icons.Default.Language,
                                placeholder = "https://your-website.com",
                                tag = "edit_website_input"
                            )
                        }

                        // Gender
                        item {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wc,
                                        contentDescription = null,
                                        tint = WebTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gender",
                                        color = WebTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Male", "Female", "Other").forEach { option ->
                                        val isSelected = gender.equals(option, ignoreCase = true)
                                        Button(
                                            onClick = { gender = option },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) WebPrimaryBlue else WebBorderDark,
                                                contentColor = if (isSelected) Color.White else WebTextSecondary
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("gender_option_${option.lowercase()}")
                                        ) {
                                            Text(
                                                text = option,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Birthday
                        item {
                            WebEditField(
                                label = "Birthday",
                                value = birthday,
                                onValueChange = { birthday = it },
                                icon = Icons.Default.Cake,
                                placeholder = "YYYY-MM-DD",
                                tag = "edit_birthday_input"
                            )
                        }
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    // Footer Save Button: p-4 border-t border-[#1E293B] bg-[#0B1120] rounded-b-xl
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WebCardDark)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (name.trim().isEmpty()) {
                                    error = "Name is required"
                                    return@Button
                                }
                                onSave(
                                    name.trim(),
                                    bio.trim(),
                                    work.trim(),
                                    education.trim(),
                                    location.trim(),
                                    website.trim(),
                                    gender.trim(),
                                    birthday.trim()
                                )
                                onClose()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WebPrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_profile_details_button")
                        ) {
                            Text(
                                text = "Save Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String,
    tag: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WebTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = WebTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = Color(0xFF64748B), fontSize = 14.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = WebInputDark,
                unfocusedContainerColor = WebInputDark,
                focusedTextColor = WebTextPrimary,
                unfocusedTextColor = WebTextPrimary,
                focusedBorderColor = WebPrimaryBlue,
                unfocusedBorderColor = WebBorderDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

// ============================================================
// EMAIL ADDRESS MODAL
// ============================================================
@Composable
fun EmailAddressModalDialog(
    currentEmail: String,
    onClose: () -> Unit
) {
    var email by remember { mutableStateOf(currentEmail) }
    var message by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WebCardDark),
                border = BorderStroke(1.dp, WebBorderDark)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WebBorderDark)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WebTextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Email Address", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WebTextPrimary)
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(WebGreen.copy(alpha = 0.15f))
                                .border(1.dp, WebGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = WebGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Your primary email is verified and protected.",
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Column {
                            Text("Registered Email", color = WebTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = WebInputDark,
                                    unfocusedContainerColor = WebInputDark,
                                    focusedTextColor = WebTextPrimary,
                                    unfocusedTextColor = WebTextPrimary,
                                    focusedBorderColor = WebPrimaryBlue,
                                    unfocusedBorderColor = WebBorderDark
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (message.isNotBlank()) {
                            Text(text = message, color = WebGreen, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                message = "Confirmation link sent to $email"
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WebPrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Send Verification Link", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// TWO-FACTOR AUTHENTICATION MODAL
// ============================================================
@Composable
fun TwoFactorModalDialog(
    onClose: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WebCardDark),
                border = BorderStroke(1.dp, WebBorderDark)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WebBorderDark)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WebTextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Two-Factor Authentication", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WebTextPrimary)
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(WebInputDark)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Two-Factor Authentication",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = WebTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isEnabled) "Active via Authenticator App" else "Disabled (Not recommended)",
                                    fontSize = 13.sp,
                                    color = if (isEnabled) WebGreen else Color(0xFFF87171)
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = WebPrimaryBlue,
                                    uncheckedThumbColor = WebTextSecondary,
                                    uncheckedTrackColor = WebBorderDark
                                )
                            )
                        }

                        Text(
                            text = "Two-factor authentication adds an extra layer of protection when logging in from an unrecognized device or browser.",
                            color = WebTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onClose,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WebPrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// PRIVACY SETTINGS MODAL
// ============================================================
@Composable
fun PrivacySettingsModalDialog(
    onClose: () -> Unit
) {
    var postAudience by remember { mutableStateOf("Public") }
    var allowTagging by remember { mutableStateOf(true) }
    var showActiveStatus by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WebCardDark),
                border = BorderStroke(1.dp, WebBorderDark)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WebBorderDark)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WebTextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Privacy Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WebTextPrimary)
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Default Post Audience", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WebTextPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Public", "Followers", "Only Me").forEach { opt ->
                                val isSel = postAudience == opt
                                Button(
                                    onClick = { postAudience = opt },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) WebPrimaryBlue else WebBorderDark,
                                        contentColor = if (isSel) Color.White else WebTextSecondary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(opt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Allow Photo Tagging", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WebTextPrimary)
                                Text("Let people tag you in photos and posts", fontSize = 12.sp, color = WebTextSecondary)
                            }
                            Switch(
                                checked = allowTagging,
                                onCheckedChange = { allowTagging = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = WebPrimaryBlue
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Active Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WebTextPrimary)
                                Text("Show when you are currently online", fontSize = 12.sp, color = WebTextSecondary)
                            }
                            Switch(
                                checked = showActiveStatus,
                                onCheckedChange = { showActiveStatus = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = WebPrimaryBlue
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onClose,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WebPrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Save Preferences", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// NOTIFICATION PREFERENCES MODAL
// ============================================================
@Composable
fun NotificationPreferencesModalDialog(
    onClose: () -> Unit
) {
    var pushNotifs by remember { mutableStateOf(true) }
    var likesNotifs by remember { mutableStateOf(true) }
    var commentsNotifs by remember { mutableStateOf(true) }
    var messagesNotifs by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WebCardDark),
                border = BorderStroke(1.dp, WebBorderDark)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WebBorderDark)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WebTextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Notification Preferences", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WebTextPrimary)
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Push Notifications", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WebTextPrimary)
                                Text("Receive alerts directly on this device", fontSize = 12.sp, color = WebTextSecondary)
                            }
                            Switch(checked = pushNotifs, onCheckedChange = { pushNotifs = it }, colors = SwitchDefaults.colors(checkedTrackColor = WebPrimaryBlue))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Likes & Reactions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WebTextPrimary)
                                Text("When someone likes your posts or comments", fontSize = 12.sp, color = WebTextSecondary)
                            }
                            Switch(checked = likesNotifs, onCheckedChange = { likesNotifs = it }, colors = SwitchDefaults.colors(checkedTrackColor = WebPrimaryBlue))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Comments", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WebTextPrimary)
                                Text("When someone comments on your posts", fontSize = 12.sp, color = WebTextSecondary)
                            }
                            Switch(checked = commentsNotifs, onCheckedChange = { commentsNotifs = it }, colors = SwitchDefaults.colors(checkedTrackColor = WebPrimaryBlue))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Messages", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WebTextPrimary)
                                Text("When you receive direct chats", fontSize = 12.sp, color = WebTextSecondary)
                            }
                            Switch(checked = messagesNotifs, onCheckedChange = { messagesNotifs = it }, colors = SwitchDefaults.colors(checkedTrackColor = WebPrimaryBlue))
                        }
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onClose,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WebPrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// BLOCKED USERS MODAL
// ============================================================
@Composable
fun BlockedUsersModalDialog(
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WebCardDark),
                border = BorderStroke(1.dp, WebBorderDark)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WebBorderDark)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WebTextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Blocked Users", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WebTextPrimary)
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(WebRedAccent.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = WebRedAccent, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Blocked Accounts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = WebTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You haven't blocked anyone yet. Blocked accounts won't be able to view your profile, stories, or send you messages.",
                            fontSize = 13.sp,
                            color = WebTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }

                    HorizontalDivider(color = WebBorderDark, thickness = 1.dp)

                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onClose,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WebPrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
