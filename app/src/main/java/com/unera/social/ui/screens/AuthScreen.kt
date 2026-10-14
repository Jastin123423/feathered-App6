package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.unera.social.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val APP_NAME = "Feathered"
private val APP_LOGO = "https://media.featheredsocial.site/file_00000000fcd8820ebece7f0c251fccf1.png"

private val AuthBgDark = Color(0xFF050B18)
private val AuthCardDark = Color(0xFF1E293B)
private val AuthHoverDark = Color(0xFF334155)
private val AuthTextPrimary = Color(0xFFF8FAFC)
private val AuthTextSecondary = Color(0xFF94A3B8)
private val AuthBluePrimary = Color(0xFF1877F2)
private val AuthBlueHover = Color(0xFF166FE5)
private val AuthGreenSignup = Color(0xFF42B72A)
private val AuthGreenSubmit = Color(0xFF00A400)

data class CountryItem(val code: String, val name: String, val flag: String)

private val COUNTRIES_LIST = listOf(
    CountryItem("TZ", "Tanzania", "🇹🇿"),
    CountryItem("KE", "Kenya", "🇰🇪"),
    CountryItem("UG", "Uganda", "🇺🇬"),
    CountryItem("RW", "Rwanda", "🇷🇼"),
    CountryItem("BI", "Burundi", "🇧🇮"),
    CountryItem("CD", "DR Congo", "🇨🇩"),
    CountryItem("ZA", "South Africa", "🇿🇦"),
    CountryItem("NG", "Nigeria", "🇳🇬"),
    CountryItem("GH", "Ghana", "🇬🇭"),
    CountryItem("US", "United States", "🇺🇸"),
    CountryItem("GB", "United Kingdom", "🇬🇧"),
    CountryItem("CA", "Canada", "🇨🇦"),
    CountryItem("DE", "Germany", "🇩🇪"),
    CountryItem("FR", "France", "🇫🇷"),
    CountryItem("IN", "India", "🇮🇳"),
    CountryItem("AE", "United Arab Emirates", "🇦🇪")
)

/**
 * BrandHeader - Reusable brand logo and gradient title matching web Auth.tsx
 */
@Composable
fun BrandHeader(
    size: Int = 44,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Color(0xFF0B0E14))
                .border(1.dp, Color(0xFF1E293B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_feather_logo),
                contentDescription = APP_NAME,
                tint = Color(0xFF1877F2),
                modifier = Modifier.size((size * 0.65).dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = APP_NAME,
            fontSize = size.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1877F2),
            letterSpacing = (-0.5).sp
        )
    }
}

/**
 * AuthScreen - Top level wrapper coordinating Login, Register, and ForgotPassword views
 */
@Composable
fun AuthScreen(
    initialMode: String = "login",
    onClose: () -> Unit,
    onLogin: (email: String, pass: String) -> Boolean,
    onSignup: (
        username: String,
        email: String,
        pass: String,
        nationality: String,
        location: String,
        birthDate: String,
        gender: String
    ) -> Boolean,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(initialMode) } // "login", "register", "forgot"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuthBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("auth_screen")
    ) {
        when (mode) {
            "login" -> {
                LoginView(
                    onNavigateToRegister = { mode = "register" },
                    onNavigateToForgotPassword = { mode = "forgot" },
                    onClose = onContinueAsGuest,
                    onLoginSubmit = onLogin
                )
            }
            "register" -> {
                RegisterView(
                    onBackToLogin = { mode = "login" },
                    onRegisterSubmit = onSignup
                )
            }
            "forgot" -> {
                ForgotPasswordView(
                    onBackToLogin = { mode = "login" }
                )
            }
        }
    }
}

/**
 * LoginView - Matches web Login in Auth.tsx
 */
@Composable
fun LoginView(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onClose: () -> Unit,
    onLoginSubmit: (email: String, pass: String) -> Boolean
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("en") }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Close button: top right w-10 h-10 bg-[#1E293B] rounded-full
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(AuthCardDark)
                .testTag("auth_close_guest_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Continue as Guest",
                tint = AuthTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Spacer(modifier = Modifier.height(40.dp))

                // Brand Header
                BrandHeader(size = 44)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Connect with friends and the world around you on Feathered.",
                    fontSize = 16.sp,
                    color = AuthTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 380.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Form Container: max-w-[400px]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (error.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.50f))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = error, color = Color(0xFFFECACA), fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email address", color = AuthTextSecondary, fontSize = 16.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AuthCardDark,
                            unfocusedContainerColor = AuthCardDark,
                            focusedTextColor = AuthTextPrimary,
                            unfocusedTextColor = AuthTextPrimary,
                            focusedBorderColor = AuthBluePrimary,
                            unfocusedBorderColor = AuthCardDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password", color = AuthTextSecondary, fontSize = 16.sp) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = AuthTextSecondary
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AuthCardDark,
                            unfocusedContainerColor = AuthCardDark,
                            focusedTextColor = AuthTextPrimary,
                            unfocusedTextColor = AuthTextPrimary,
                            focusedBorderColor = AuthBluePrimary,
                            unfocusedBorderColor = AuthCardDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    // Login Button: bg-[#1877F2] font-bold text-[18px] py-3 rounded-md
                    Button(
                        onClick = {
                            error = ""
                            if (email.isBlank() || password.isBlank()) {
                                error = "Please enter both email and password."
                                return@Button
                            }
                            loading = true
                            coroutineScope.launch {
                                delay(600)
                                val ok = onLoginSubmit(email, password)
                                loading = false
                                if (!ok) {
                                    error = "Login failed. Please check your credentials."
                                }
                            }
                        },
                        enabled = !loading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthBluePrimary,
                            disabledContainerColor = AuthBluePrimary.copy(alpha = 0.60f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_btn")
                    ) {
                        if (loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logging in...", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text("Log In", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Forgot Password Link
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Forgot password?",
                            fontSize = 14.sp,
                            color = AuthBluePrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { onNavigateToForgotPassword() }
                                .testTag("login_forgot_password_link")
                        )
                    }

                    HorizontalDivider(color = AuthCardDark, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Create New Account Button: bg-[#42B72A] font-bold text-[17px]
                    Button(
                        onClick = onNavigateToRegister,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuthGreenSignup),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_create_account_btn")
                    ) {
                        Text("Create new account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Guest Hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Login to comment, like, and share posts. ",
                        fontSize = 12.sp,
                        color = AuthTextSecondary
                    )
                    Text(
                        text = "Continue as Guest",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuthTextPrimary,
                        modifier = Modifier.clickable { onClose() }
                    )
                    Text(
                        text = " to view.",
                        fontSize = 12.sp,
                        color = AuthTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Language Switcher matching web
                HorizontalDivider(color = AuthCardDark, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("English (US)", "Kiswahili", "Français", "Español").forEachIndexed { idx, lang ->
                        Text(
                            text = lang,
                            fontSize = 12.sp,
                            color = if (idx == 0) Color.White else Color(0xFF64748B),
                            fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * RegisterView - Matches web Register in Auth.tsx
 */
@Composable
fun RegisterView(
    onBackToLogin: () -> Unit,
    onRegisterSubmit: (
        username: String,
        email: String,
        pass: String,
        nationality: String,
        location: String,
        birthDate: String,
        gender: String
    ) -> Boolean
) {
    var firstName by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var selectedCountry by remember { mutableStateOf(COUNTRIES_LIST[0]) } // Tanzania
    var showCountryPicker by remember { mutableStateOf(false) }
    var region by remember { mutableStateOf("Dar es Salaam") }

    var day by remember { mutableIntStateOf(15) }
    var month by remember { mutableIntStateOf(6) }
    var year by remember { mutableIntStateOf(2000) }
    var gender by remember { mutableStateOf("Female") }

    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val birthDate = "%04d-%02d-%02d".format(year, month, day)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            BrandHeader(size = 44)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sign Up", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AuthTextPrimary)
            Text("It's quick and easy.", fontSize = 14.sp, color = AuthTextSecondary)

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (error.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.50f))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = error, color = Color(0xFFFECACA), fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }

                // First Name & Surname row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        placeholder = { Text("First name", color = AuthTextSecondary, fontSize = 15.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AuthCardDark,
                            unfocusedContainerColor = AuthCardDark,
                            focusedTextColor = AuthTextPrimary,
                            unfocusedTextColor = AuthTextPrimary,
                            focusedBorderColor = AuthBluePrimary,
                            unfocusedBorderColor = AuthCardDark
                        ),
                        modifier = Modifier.weight(1f).testTag("register_first_name_input")
                    )

                    OutlinedTextField(
                        value = surname,
                        onValueChange = { surname = it },
                        placeholder = { Text("Surname", color = AuthTextSecondary, fontSize = 15.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AuthCardDark,
                            unfocusedContainerColor = AuthCardDark,
                            focusedTextColor = AuthTextPrimary,
                            unfocusedTextColor = AuthTextPrimary,
                            focusedBorderColor = AuthBluePrimary,
                            unfocusedBorderColor = AuthCardDark
                        ),
                        modifier = Modifier.weight(1f).testTag("register_surname_input")
                    )
                }

                // Nationality selection button
                Surface(
                    onClick = { showCountryPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = AuthCardDark,
                    border = BorderStroke(1.dp, AuthCardDark),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedCountry.flag, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(selectedCountry.name, color = AuthTextPrimary, fontSize = 15.sp)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AuthTextSecondary)
                    }
                }

                // Region Input
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    placeholder = { Text("Region (e.g. Dar es Salaam)", color = AuthTextSecondary, fontSize = 15.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AuthCardDark,
                        unfocusedContainerColor = AuthCardDark,
                        focusedTextColor = AuthTextPrimary,
                        unfocusedTextColor = AuthTextPrimary,
                        focusedBorderColor = AuthBluePrimary,
                        unfocusedBorderColor = AuthCardDark
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("register_region_input")
                )

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email address", color = AuthTextSecondary, fontSize = 15.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AuthCardDark,
                        unfocusedContainerColor = AuthCardDark,
                        focusedTextColor = AuthTextPrimary,
                        unfocusedTextColor = AuthTextPrimary,
                        focusedBorderColor = AuthBluePrimary,
                        unfocusedBorderColor = AuthCardDark
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("register_email_input")
                )

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password (min 6 characters)", color = AuthTextSecondary, fontSize = 15.sp) },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AuthTextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AuthCardDark,
                        unfocusedContainerColor = AuthCardDark,
                        focusedTextColor = AuthTextPrimary,
                        unfocusedTextColor = AuthTextPrimary,
                        focusedBorderColor = AuthBluePrimary,
                        unfocusedBorderColor = AuthCardDark
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("register_password_input")
                )

                // Date of Birth Dropdowns
                Column {
                    Text("Date of birth", fontSize = 12.sp, color = AuthTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Day
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuthCardDark)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), color = AuthTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Month
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuthCardDark)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(months[(month - 1).coerceIn(0, 11)], color = AuthTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Year
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuthCardDark)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(year.toString(), color = AuthTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Gender Selection Radio Cards matching web
                Column {
                    Text("Gender", fontSize = 12.sp, color = AuthTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Female", "Male").forEach { opt ->
                            val isSel = gender == opt
                            Surface(
                                onClick = { gender = opt },
                                shape = RoundedCornerShape(8.dp),
                                color = AuthCardDark,
                                border = BorderStroke(1.dp, if (isSel) AuthBluePrimary else AuthCardDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(opt, color = AuthTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    RadioButton(
                                        selected = isSel,
                                        onClick = { gender = opt },
                                        colors = RadioButtonDefaults.colors(selectedColor = AuthBluePrimary)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "By clicking Sign Up, you agree to our Terms, Privacy Policy and Cookies Policy.",
                    fontSize = 11.sp,
                    color = AuthTextSecondary,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Sign Up Button: bg-[#00A400]
                Button(
                    onClick = {
                        error = ""
                        val fullName = if (surname.isNotBlank()) "${firstName.trim()} ${surname.trim()}" else firstName.trim()
                        if (fullName.isBlank()) {
                            error = "Please enter your name."
                            return@Button
                        }
                        if (email.isBlank()) {
                            error = "Please enter your email."
                            return@Button
                        }
                        if (password.length < 6) {
                            error = "Password must be at least 6 characters."
                            return@Button
                        }
                        loading = true
                        coroutineScope.launch {
                            delay(600)
                            val ok = onRegisterSubmit(
                                fullName,
                                email,
                                password,
                                selectedCountry.name,
                                region,
                                birthDate,
                                gender
                            )
                            loading = false
                            if (!ok) {
                                error = "Signup failed. Please try again."
                            }
                        }
                    },
                    enabled = !loading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AuthGreenSubmit),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("register_submit_btn")
                ) {
                    if (loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Already have an account?",
                        fontSize = 14.sp,
                        color = AuthBluePrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onBackToLogin() }
                    )
                }
            }
        }
    }

    // Country Picker Dialog
    if (showCountryPicker) {
        Dialog(onDismissRequest = { showCountryPicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Nationality", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AuthTextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(COUNTRIES_LIST) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedCountry = c
                                        showCountryPicker = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(c.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(c.name, color = AuthTextPrimary, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ForgotPasswordView - Matches web ForgotPassword in Auth.tsx
 */
@Composable
fun ForgotPasswordView(
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandHeader(size = 40)

            if (isSent) {
                // Reset Link Sent view
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(54.dp)
                )

                Text(
                    text = "Reset Link Sent!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthTextPrimary
                )

                Text(
                    text = "If an account with $email exists, a reset link has been sent. Check your inbox and spam folder.",
                    fontSize = 14.sp,
                    color = AuthTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onBackToLogin,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AuthCardDark),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Back to Login", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Text(
                    text = "Find Your Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthTextPrimary
                )

                Text(
                    text = "Enter your email to search for your account.",
                    fontSize = 15.sp,
                    color = AuthTextSecondary,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email address", color = AuthTextSecondary, fontSize = 16.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AuthCardDark,
                        unfocusedContainerColor = AuthCardDark,
                        focusedTextColor = AuthTextPrimary,
                        unfocusedTextColor = AuthTextPrimary,
                        focusedBorderColor = AuthBluePrimary,
                        unfocusedBorderColor = AuthCardDark
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("forgot_email_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onBackToLogin,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuthCardDark),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (email.isNotBlank()) {
                                isSent = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuthBluePrimary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Send Link", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
