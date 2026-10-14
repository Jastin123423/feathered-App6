package com.unera.social.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Signaling message model matching WebRTC SigMsg in CallOverlay.tsx
 */
sealed class SigMsg {
    data class Offer(val sdp: String, val to: String? = null, val from: String? = null) : SigMsg()
    data class Answer(val sdp: String, val to: String? = null, val from: String? = null) : SigMsg()
    data class Ice(val candidate: String, val to: String? = null, val from: String? = null) : SigMsg()
    data class PeerJoined(val userId: String) : SigMsg()
    data class PeerLeft(val userId: String) : SigMsg()
    data class Hangup(val to: String? = null, val from: String? = null) : SigMsg()
}

/**
 * CallOverlay - Native Android Jetpack Compose counterpart of CallOverlay.tsx.
 *
 * Implements:
 * - Audio & Video call modes with WebRTC signaling states
 * - Initiator and receiver flows with simulated negotiation (Offer/Answer/ICE/Connected)
 * - Live in-call timer once connected
 * - Mute/Unmute microphone
 * - Camera enable/disable and camera flip (front/back)
 * - Speakerphone toggle
 * - Picture-in-picture (PiP) local stream preview with drag & tap-to-swap
 * - Minimizable floating overlay pill when navigating through the app
 * - Audio waveform / equalizer visualization in audio call mode
 * - End-to-end encrypted security badge
 */
@Composable
fun CallOverlay(
    open: Boolean,
    mode: String, // "audio", "video", or "voice"
    callId: String,
    meId: Long,
    peerId: Long,
    peerName: String = "User",
    peerAvatar: String? = null,
    initiator: Boolean = true,
    onClose: () -> Unit
) {
    if (!open) return

    val context = LocalContext.current
    val isVideoMode = mode.equals("video", ignoreCase = true)

    // Call status and media controls state
    var status by remember(callId) {
        mutableStateOf(if (initiator) "Connecting…" else "Incoming call…")
    }
    var isConnected by remember(callId) { mutableStateOf(false) }
    var callSeconds by remember(callId) { mutableStateOf(0) }

    var muted by remember { mutableStateOf(false) }
    var camOff by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(isVideoMode) }
    var isFrontCam by remember { mutableStateOf(true) }
    var isSwapped by remember { mutableStateOf(false) }
    var isMinimized by remember { mutableStateOf(false) }

    // Floating PiP offset for drag gesture
    var pipOffsetX by remember { mutableStateOf(0f) }
    var pipOffsetY by remember { mutableStateOf(0f) }

    // Intercept back button when full screen
    BackHandler(enabled = !isMinimized) {
        onClose()
    }

    // WebRTC Signaling simulation and connection progression
    LaunchedEffect(callId, initiator) {
        if (initiator) {
            status = "Connecting…"
            delay(1200)
            status = "Ringing…"
            delay(2200)
            status = "Connected"
            isConnected = true
        } else {
            status = "Connecting…"
            delay(1000)
            status = "Connected"
            isConnected = true
        }
    }

    // Call duration timer
    LaunchedEffect(isConnected) {
        if (isConnected) {
            callSeconds = 0
            while (true) {
                delay(1000)
                callSeconds++
            }
        }
    }

    // Format timer
    val timerText = remember(callSeconds) {
        val mins = callSeconds / 60
        val secs = callSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    // If minimized, render floating compact pill
    if (isMinimized) {
        MinimizedCallPill(
            peerName = peerName,
            peerAvatar = peerAvatar,
            timerText = if (isConnected) timerText else status,
            isVideo = isVideoMode,
            muted = muted,
            onExpand = { isMinimized = false },
            onToggleMute = {
                muted = !muted
                Toast.makeText(context, if (muted) "Microphone muted" else "Microphone unmuted", Toast.LENGTH_SHORT).show()
            },
            onHangup = onClose
        )
        return
    }

    // Full screen overlay dialog
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B14))
                .testTag("call_overlay_root")
        ) {
            // Background Layer
            if (isVideoMode && !camOff && !isSwapped) {
                // Video mode background (simulated remote video feed with subtle motion)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF0B132B),
                                    Color(0xFF050B14)
                                )
                            )
                        )
                ) {
                    if (!peerAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = peerAvatar,
                            contentDescription = peerName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (isConnected) 0.6f else 0.25f
                                }
                        )
                    }

                    // Video overlay dark gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }
            } else {
                // Audio mode background with elegant deep dark navy mesh gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0B1120),
                                    Color(0xFF070B14),
                                    Color(0xFF020617)
                                )
                            )
                        )
                ) {
                    // Pulsing radial glow in background
                    val infiniteTransition = rememberInfiniteTransition(label = "audio_glow")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(340.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF1877F2).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            // Top App Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize Call Button
                IconButton(
                    onClick = { isMinimized = true },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .testTag("call_overlay_minimize_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize Call",
                        tint = Color.White
                    )
                }

                // Call Security & Status Chip
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isVideoMode) "HD Video • Encrypted" else "Voice Call • Encrypted",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isConnected) timerText else status,
                        color = if (isConnected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Camera Flip Button (Only in Video Mode)
                if (isVideoMode) {
                    IconButton(
                        onClick = {
                            isFrontCam = !isFrontCam
                            Toast.makeText(
                                context,
                                if (isFrontCam) "Front camera" else "Back camera",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .testTag("call_overlay_flip_cam_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }
            }

            // Center Call Content (Avatar & Name & Waveform)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // In Audio Mode or when Cam is Off, display Big Avatar with pulsating wave rings
                if (!isVideoMode || camOff) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(190.dp)
                    ) {
                        // Outer pulsating ring
                        if (isConnected) {
                            val infiniteTransition = rememberInfiniteTransition(label = "waves")
                            val ring1 by infiniteTransition.animateFloat(
                                initialValue = 0.85f,
                                targetValue = 1.35f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "ring1"
                            )
                            val ringAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .scale(ring1)
                                    .border(2.dp, Color(0xFF1877F2).copy(alpha = ringAlpha), CircleShape)
                            )
                        }

                        // Peer Avatar
                        if (!peerAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = peerAvatar,
                                contentDescription = peerName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, Color(0xFF1877F2), CircleShape)
                                    .shadow(20.dp, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1877F2), Color(0xFF0D47A1))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = peerName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = peerName,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isConnected) "Active Call" else status,
                        color = Color(0xFF94A3B8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Audio Waveform Equalizer (reacts when connected and not muted)
                    if (isConnected && !muted) {
                        AudioWaveformVisualizer()
                    }
                }
            }

            // Picture-in-Picture (PiP) Window for Video Mode
            if (isVideoMode) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 70.dp, end = 16.dp)
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clickable { isSwapped = !isSwapped }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                pipOffsetX += dragAmount.x
                                pipOffsetY += dragAmount.y
                            }
                        }
                        .testTag("call_overlay_pip_window")
                ) {
                    if (camOff) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = "Camera Off",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Camera off",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        // Simulated local video camera preview
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF334155))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF1E293B),
                                                Color(0xFF0F172A)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "You",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (isFrontCam) "Front" else "Back",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Tap to swap hint chip
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Tap to swap",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Call Action Controls Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Microphone Mute Toggle Button
                        CallActionButton(
                            icon = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (muted) "Unmute" else "Mute",
                            isActive = muted,
                            activeColor = Color(0xFFEF4444),
                            onClick = {
                                muted = !muted
                                Toast.makeText(
                                    context,
                                    if (muted) "Muted" else "Unmuted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            testTag = "call_btn_mute"
                        )

                        // 2. Camera Toggle Button (if video mode, or toggle mode)
                        CallActionButton(
                            icon = if (camOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            label = if (camOff) "Turn On" else "Camera",
                            isActive = camOff,
                            activeColor = Color(0xFFEF4444),
                            onClick = {
                                camOff = !camOff
                                Toast.makeText(
                                    context,
                                    if (camOff) "Camera disabled" else "Camera enabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            testTag = "call_btn_video"
                        )

                        // 3. Speaker Toggle Button
                        CallActionButton(
                            icon = if (speakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            label = if (speakerOn) "Speaker" else "Earpiece",
                            isActive = speakerOn,
                            activeColor = Color(0xFF1877F2),
                            onClick = {
                                speakerOn = !speakerOn
                                Toast.makeText(
                                    context,
                                    if (speakerOn) "Speakerphone on" else "Earpiece mode",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            testTag = "call_btn_speaker"
                        )

                        // 4. Hangup / End Call Button (Prominent Red)
                        IconButton(
                            onClick = {
                                status = "Call ended"
                                Toast.makeText(context, "Call ended", Toast.LENGTH_SHORT).show()
                                onClose()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(8.dp, CircleShape)
                                .background(Color(0xFFDC2626), CircleShape)
                                .testTag("call_btn_hangup")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimized floating call pill when user minimizes overlay
 */
@Composable
private fun MinimizedCallPill(
    peerName: String,
    peerAvatar: String?,
    timerText: String,
    isVideo: Boolean,
    muted: Boolean,
    onExpand: () -> Unit,
    onToggleMute: () -> Unit,
    onHangup: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .clickable(onClick = onExpand)
                .testTag("call_minimized_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Peer Avatar with green active dot
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (!peerAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = peerAvatar,
                            contentDescription = peerName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF1877F2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = peerName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                            .border(1.5.dp, Color(0xFF0F172A), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = peerName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timerText,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Mute button
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Mute",
                        tint = if (muted) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Hangup button
                IconButton(
                    onClick = onHangup,
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFFDC2626), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hangup",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Animated Audio Equalizer visualizer
 */
@Composable
private fun AudioWaveformVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(36.dp)
    ) {
        val heights = listOf(12.dp, 24.dp, 32.dp, 18.dp, 28.dp, 16.dp, 22.dp)
        heights.forEachIndexed { index, baseHeight ->
            val animFraction by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + (index * 80), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(baseHeight * animFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF38BDF8),
                                Color(0xFF1877F2)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Modern In-Call Action Button with icon, state highlight, and sub-label
 */
@Composable
private fun CallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = Color(0xFF1877F2),
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(50.dp)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f),
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isActive) activeColor else Color.White.copy(alpha = 0.15f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
