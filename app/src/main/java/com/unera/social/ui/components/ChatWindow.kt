package com.unera.social.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.ChatAttachment
import com.unera.social.model.ChatMessage
import com.unera.social.model.Conversation
import com.unera.social.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * ChatWindow - Exact Jetpack Compose counterpart of web Chat.tsx
 *
 * Implements:
 * - Edge-to-edge status header with back button (#0084FF), avatar, active green indicator,
 *   voice call, video call, and info icons.
 * - Dynamic message list with day pills, grouping corner radii, delivery/seen double ticks,
 *   reply quote cards, MarketPoint product mention cards, call history records,
 *   voice note waveform players, image/video/document previews, and URL cards.
 * - Composer with (+) attachment menu (Photos, Videos, Audio, Docs), quick photo,
 *   stickers, GIF badge, emoji drawer, Aa text input, microphone voice recorder with live timer,
 *   and dynamic Send (paper-plane) / Like (thumbs-up) button.
 * - Long-press Action Sheet (Reply, Edit, Copy, Delete for me, Delete for everyone).
 * - Full-fidelity CallScreen for voice & video calls.
 * - Full-screen Attachment Viewer modal.
 */
@Composable
fun ChatWindow(
    conversation: Conversation,
    currentUser: User,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendAttachment: ((ChatAttachment, String) -> Unit)? = null,
    onEditMessage: ((String, String) -> Unit)? = null,
    onDeleteMessage: ((String) -> Unit)? = null,
    onViewProduct: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editTarget by remember { mutableStateOf<ChatMessage?>(null) }

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }

    var actionModalMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var viewingAttachment by remember { mutableStateOf<ChatAttachment?>(null) }

    // Voice recording simulation
    var isRecording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableStateOf(0) }

    // Call Screen states
    var callOpen by remember { mutableStateOf(false) }
    var callMode by remember { mutableStateOf("voice") } // "voice", "video"
    var callPhase by remember { mutableStateOf("calling") } // "calling", "active", "ended"
    var callSeconds by remember { mutableStateOf(0) }
    var isMicOn by remember { mutableStateOf(true) }
    var isCamOn by remember { mutableStateOf(true) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    // Voice recorder timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (isRecording) {
                delay(1000)
                recordSeconds++
            }
        }
    }

    // Call timer
    LaunchedEffect(callOpen, callPhase) {
        if (callOpen && callPhase == "calling") {
            delay(2500)
            callPhase = "active"
            callSeconds = 0
            while (callOpen && callPhase == "active") {
                delay(1000)
                callSeconds++
            }
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.size - 1)
        }
    }

    val participant = conversation.participant
    val isRecipientOnline = participant.isOnline

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("chat_window_root")
    ) {
        // ==========================================
        // 1. TOP HEADER
        // ==========================================
        Surface(
            color = Color.Black,
            border = BorderStroke(0.5.dp, Color(0xFF1A1A1A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back button + Avatar + Name & Online Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("chat_header_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0084FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Box(modifier = Modifier.size(40.dp)) {
                        AsyncImage(
                            model = participant.profileImageUrl,
                            contentDescription = participant.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFF242526), CircleShape)
                        )
                        if (isRecipientOnline) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF31A24C))
                                    .border(2.dp, Color.Black, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = participant.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isRecipientOnline) "Active now" else "Offline",
                            fontSize = 12.sp,
                            fontWeight = if (isRecipientOnline) FontWeight.Medium else FontWeight.Normal,
                            color = if (isRecipientOnline) Color(0xFF31A24C) else Color(0xFF8E8E93),
                            maxLines = 1
                        )
                    }
                }

                // Right Action Icons: Phone, Video, Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            callMode = "voice"
                            callPhase = "calling"
                            callOpen = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Voice Call",
                            tint = Color(0xFF0084FF),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            callMode = "video"
                            callPhase = "calling"
                            callOpen = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = Color(0xFF0084FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "${participant.name}'s Profile & Chat Details", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Chat Info",
                            tint = Color(0xFF0084FF),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. REPLY / EDIT BANNER
        // ==========================================
        AnimatedVisibility(visible = replyTo != null || editTarget != null) {
            Surface(
                color = Color(0xFF121212),
                border = BorderStroke(0.5.dp, Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editTarget != null) "EDITING MESSAGE" else "REPLYING TO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0084FF)
                        )
                        Text(
                            text = editTarget?.text ?: replyTo?.text ?: "Attachment",
                            fontSize = 13.sp,
                            color = Color(0xFFF0F2F5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            replyTo = null
                            editTarget = null
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. VOICE RECORDING BANNER
        // ==========================================
        AnimatedVisibility(visible = isRecording) {
            Surface(
                color = Color(0xFF121212),
                border = BorderStroke(0.5.dp, Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF4D4D))
                        )
                        Text(
                            text = "${recordSeconds}s",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Animated simulated waveform bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(20.dp)
                        ) {
                            repeat(12) { i ->
                                val barHeight = (8 + (sin((recordSeconds * 2 + i).toDouble()) * 8).toInt()).coerceIn(4, 20)
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF0084FF))
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { isRecording = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF6B6B))
                    ) {
                        Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ==========================================
        // 4. MESSAGES LIST
        // ==========================================
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Day separator at top
            item(key = "day_divider_top") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFF141414),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF222222))
                    ) {
                        Text(
                            text = conversation.lastTimestamp.ifBlank { "Today" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            itemsIndexed(conversation.messages, key = { _, msg -> msg.id }) { index, msg ->
                val prevMsg = conversation.messages.getOrNull(index - 1)
                val nextMsg = conversation.messages.getOrNull(index + 1)
                val sameAsPrev = prevMsg?.senderId == msg.senderId
                val sameAsNext = nextMsg?.senderId == msg.senderId

                val isMine = msg.isMine
                val bottomSpacing = if (sameAsNext) 3.dp else 10.dp

                // Adaptive corner radiuses
                val bubbleShape = if (isMine) {
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = if (sameAsPrev) 6.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = if (sameAsNext) 6.dp else 18.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = if (sameAsPrev) 6.dp else 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (sameAsNext) 6.dp else 18.dp,
                        bottomEnd = 18.dp
                    )
                }

                // Check for product mention tag e.g. "[product:2]"
                val productMatch = remember(msg.text) {
                    val regex = Regex("\\[product:(\\d+)\\]")
                    regex.find(msg.text)
                }
                val productId = productMatch?.groupValues?.getOrNull(1)?.toLongOrNull()
                val cleanMessageText = remember(msg.text) {
                    msg.text.replace(Regex("\\[product:(\\d+)\\]"), "").trim()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = bottomSpacing),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .pointerInput(msg.id) {
                                detectTapGestures(
                                    onLongPress = {
                                        actionModalMsg = msg
                                    }
                                )
                            }
                    ) {
                        Surface(
                            color = if (isMine) Color(0xFF0084FF) else Color(0xFF242526),
                            shape = bubbleShape,
                            shadowElevation = if (isMine) 2.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                )
                            ) {
                                // 1. Quoted Reply box
                                if (!msg.replyToText.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.25f))
                                            .border(BorderStroke(2.dp, if (isMine) Color.White.copy(alpha = 0.8f) else Color(0xFF0084FF)), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Reply",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMine) Color.White.copy(alpha = 0.9f) else Color(0xFF0084FF)
                                            )
                                            Text(
                                                text = msg.replyToText,
                                                fontSize = 12.sp,
                                                color = if (isMine) Color.White.copy(alpha = 0.85f) else Color(0xFF8E8E93),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // 2. MarketPoint Product Reference Card
                                if (productId != null) {
                                    val marketplaceContext = LocalMarketplaceContext.current
                                    val productData = remember(productId) { marketplaceContext.getProductData(productId) }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isMine) Color.Black.copy(alpha = 0.3f) else Color(0xFF1877F2).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, if (isMine) Color.White.copy(alpha = 0.2f) else Color(0xFF1877F2).copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .clickable { (onViewProduct ?: marketplaceContext.onViewProduct).invoke(productId) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (!productData?.imageUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = productData!!.imageUrl,
                                                    contentDescription = productData.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF0F172A)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Storefront,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1877F2),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = productData?.title ?: "MarketPoint Listing",
                                                    fontSize = 12.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (productData != null) {
                                                        "${productData.currency ?: "TZS"} ${String.format("%,.0f", productData.price)} • ${productData.location}"
                                                    } else "Item #$productId",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (productData != null) Color(0xFF34D399) else Color(0xFF38BDF8)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Call Log Message Box
                                if (msg.isCallMessage) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (msg.callType == "video") Icons.Default.Videocam else Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = if (msg.isMissedCall) Color(0xFFFF6B6B) else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = msg.text.ifBlank {
                                                if (msg.isMissedCall) "Missed ${msg.callType} call" else "${msg.callType.replaceFirstChar { it.uppercase() }} call"
                                            },
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (msg.isMissedCall && !isMine) {
                                            Button(
                                                onClick = {
                                                    callMode = msg.callType
                                                    callPhase = "calling"
                                                    callOpen = true
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF0084FF),
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("Call back", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    // 4. Normal Text Message
                                    if (cleanMessageText.isNotBlank()) {
                                        Text(
                                            text = cleanMessageText,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                            color = if (isMine) Color.White else Color(0xFFF0F2F5)
                                        )
                                    }
                                }

                                // 5. Attachments Preview
                                msg.attachments.forEach { att ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    when (att.fileType) {
                                        "image", "gif" -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 240.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(1.dp, Color(0xFF242526), RoundedCornerShape(12.dp))
                                                    .clickable { viewingAttachment = att }
                                            ) {
                                                AsyncImage(
                                                    model = att.url,
                                                    contentDescription = att.filename,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        "audio" -> {
                                            // WhatsApp / Messenger style voice note waveform
                                            VoiceNotePlayerView(
                                                attachment = att,
                                                isMine = isMine
                                            )
                                        }
                                        else -> {
                                            // Document / other file
                                            Surface(
                                                color = Color(0xFF18191A),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFF242526)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewingAttachment = att }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Description,
                                                        contentDescription = null,
                                                        tint = Color(0xFF0084FF),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = att.filename,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Document",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF8E8E93)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 6. Time & Delivery/Seen ticks sub-row
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = msg.timestamp,
                                        fontSize = 10.sp,
                                        color = if (isMine) Color.White.copy(alpha = 0.75f) else Color(0xFF8E8E93)
                                    )
                                    if (msg.isEdited) {
                                        Text(
                                            text = "(edited)",
                                            fontSize = 9.sp,
                                            color = if (isMine) Color.White.copy(alpha = 0.75f) else Color(0xFF8E8E93)
                                        )
                                    }
                                    // Delivery Ticks
                                    if (isMine) {
                                        DeliveryTicksView(isSeen = msg.isSeen, isDelivered = msg.isDelivered)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. ATTACHMENT DRAWER
        // ==========================================
        AnimatedVisibility(visible = showAttachmentMenu) {
            Surface(
                color = Color(0xFF121212),
                border = BorderStroke(0.5.dp, Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOption(
                        icon = Icons.Default.Image,
                        label = "Photos",
                        color = Color(0xFF0084FF),
                        onClick = {
                            showAttachmentMenu = false
                            val sample = ChatAttachment(
                                url = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
                                fileType = "image",
                                filename = "photo_${System.currentTimeMillis()}.jpg"
                            )
                            onSendAttachment?.invoke(sample, "") ?: onSendMessage("📷 Sent a photo")
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.Videocam,
                        label = "Videos",
                        color = Color(0xFF0084FF),
                        onClick = {
                            showAttachmentMenu = false
                            val sample = ChatAttachment(
                                url = "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=800",
                                fileType = "video",
                                filename = "video_${System.currentTimeMillis()}.mp4"
                            )
                            onSendAttachment?.invoke(sample, "") ?: onSendMessage("🎥 Sent a video")
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.Headphones,
                        label = "Audio",
                        color = Color(0xFF0084FF),
                        onClick = {
                            showAttachmentMenu = false
                            val sample = ChatAttachment(
                                url = "https://actions.google.com/sounds/v1/ambiences/rain_heavy.ogg",
                                fileType = "audio",
                                filename = "audio_clip.ogg",
                                durationSeconds = 12
                            )
                            onSendAttachment?.invoke(sample, "") ?: onSendMessage("🎵 Sent an audio note")
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.Description,
                        label = "Documents",
                        color = Color(0xFF0084FF),
                        onClick = {
                            showAttachmentMenu = false
                            val sample = ChatAttachment(
                                url = "https://example.com/document.pdf",
                                fileType = "document",
                                filename = "Project_Specs.pdf"
                            )
                            onSendAttachment?.invoke(sample, "") ?: onSendMessage("📄 Sent a document")
                        }
                    )
                }
            }
        }

        // ==========================================
        // 6. EMOJI / STICKERS / GIF DRAWER
        // ==========================================
        AnimatedVisibility(visible = showEmojiPicker || showStickerPicker || showGifPicker) {
            Surface(
                color = Color(0xFF121212),
                border = BorderStroke(0.5.dp, Color(0xFF1A1A1A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (showEmojiPicker) {
                    EmojiPickerGrid(onSelect = { emoji -> inputText += emoji })
                } else if (showStickerPicker) {
                    StickerPickerGrid(onSelect = { stickerText ->
                        onSendMessage(stickerText)
                        showStickerPicker = false
                    })
                } else if (showGifPicker) {
                    GifPickerGrid(onSelect = { gifUrl ->
                        val att = ChatAttachment(url = gifUrl, fileType = "gif", filename = "sticker.gif")
                        onSendAttachment?.invoke(att, "") ?: onSendMessage(gifUrl)
                        showGifPicker = false
                    })
                }
            }
        }

        // ==========================================
        // 7. COMPOSER BAR
        // ==========================================
        Surface(
            color = Color.Black,
            border = BorderStroke(0.5.dp, Color(0xFF1A1A1A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Attach (+) icon
                IconButton(
                    onClick = {
                        showAttachmentMenu = !showAttachmentMenu
                        showEmojiPicker = false
                        showStickerPicker = false
                        showGifPicker = false
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Attach",
                        tint = Color(0xFF0084FF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Photos button
                IconButton(
                    onClick = {
                        val sample = ChatAttachment(
                            url = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
                            fileType = "image",
                            filename = "photo.jpg"
                        )
                        onSendAttachment?.invoke(sample, "") ?: onSendMessage("📷 Photo")
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Photos",
                        tint = Color(0xFF0084FF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Stickers button
                IconButton(
                    onClick = {
                        showStickerPicker = !showStickerPicker
                        showEmojiPicker = false
                        showGifPicker = false
                        showAttachmentMenu = false
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = "Stickers",
                        tint = Color(0xFF0084FF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // GIF Badge button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0084FF).copy(alpha = 0.2f),
                    modifier = Modifier
                        .clickable {
                            showGifPicker = !showGifPicker
                            showEmojiPicker = false
                            showStickerPicker = false
                            showAttachmentMenu = false
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GIF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0084FF),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Input Field Pill
                Surface(
                    color = Color(0xFF242526),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp)
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = if (editTarget != null) "Edit message" else "Aa",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 14.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_text_field"),
                            maxLines = 3
                        )

                        // Emoji button inside pill
                        IconButton(
                            onClick = {
                                showEmojiPicker = !showEmojiPicker
                                showStickerPicker = false
                                showGifPicker = false
                                showAttachmentMenu = false
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SentimentSatisfied,
                                contentDescription = "Emoji",
                                tint = Color(0xFF0084FF),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Microphone Voice Note toggle button
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    isRecording = false
                                    // Send simulated voice note
                                    val voiceNote = ChatAttachment(
                                        url = "https://actions.google.com/sounds/v1/ambiences/rain_heavy.ogg",
                                        fileType = "audio",
                                        filename = "voice_note_${System.currentTimeMillis()}.ogg",
                                        durationSeconds = recordSeconds.coerceAtLeast(1)
                                    )
                                    onSendAttachment?.invoke(voiceNote, "") ?: onSendMessage("🎤 Voice Note (${recordSeconds}s)")
                                } else {
                                    isRecording = true
                                }
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Voice Note",
                                tint = if (isRecording) Color(0xFFFF4D4D) else Color(0xFF0084FF),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Send Button (Paper-plane if text entered, Thumbs-up if empty)
                if (inputText.isNotBlank() || editTarget != null) {
                    IconButton(
                        onClick = {
                            val textToSend = inputText.trim()
                            if (editTarget != null) {
                                onEditMessage?.invoke(editTarget!!.id, textToSend)
                                editTarget = null
                            } else {
                                onSendMessage(textToSend)
                                replyTo = null
                            }
                            inputText = ""
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0084FF))
                            .testTag("chat_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            onSendMessage("👍")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Like",
                            tint = Color(0xFF0084FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // 8. LONG-PRESS ACTION SHEET MODAL
    // ==========================================
    if (actionModalMsg != null) {
        val targetMsg = actionModalMsg!!
        Dialog(
            onDismissRequest = { actionModalMsg = null }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF18191A),
                border = BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF3A3B3C))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (targetMsg.isMine) "Your message" else "Message",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message preview box
                    Surface(
                        color = Color(0xFF121212),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF262626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = targetMsg.text.ifBlank { "Attachment" },
                            fontSize = 14.sp,
                            color = Color(0xFFF0F2F5),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Actions list
                    ActionSheetButton(
                        icon = Icons.Default.Reply,
                        label = "Reply",
                        onClick = {
                            replyTo = targetMsg
                            actionModalMsg = null
                        }
                    )

                    ActionSheetButton(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy text",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Chat Message", targetMsg.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            actionModalMsg = null
                        }
                    )

                    if (targetMsg.isMine) {
                        ActionSheetButton(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            onClick = {
                                editTarget = targetMsg
                                inputText = targetMsg.text
                                actionModalMsg = null
                            }
                        )
                    }

                    ActionSheetButton(
                        icon = Icons.Default.Delete,
                        label = "Delete for me",
                        isDanger = true,
                        onClick = {
                            onDeleteMessage?.invoke(targetMsg.id)
                            actionModalMsg = null
                        }
                    )

                    if (targetMsg.isMine) {
                        ActionSheetButton(
                            icon = Icons.Default.DeleteSweep,
                            label = "Delete for everyone",
                            isDanger = true,
                            onClick = {
                                onDeleteMessage?.invoke(targetMsg.id)
                                actionModalMsg = null
                                Toast.makeText(context, "Deleted for everyone", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { actionModalMsg = null },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF3A3B3C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = Color(0xFFF0F2F5), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ==========================================
    // 9. CALL SCREEN (VOICE & VIDEO)
    // ==========================================
    // 9. CALL SCREEN (Full-Fidelity Voice & Video)
    // ==========================================
    if (callOpen) {
        CallOverlay(
            open = callOpen,
            mode = callMode,
            callId = "call_${conversation.id}_${participant.id}",
            meId = currentUser.id,
            peerId = participant.id,
            peerName = participant.name,
            peerAvatar = participant.profileImageUrl,
            initiator = true,
            onClose = {
                callOpen = false
            }
        )
    }

    // ==========================================
    // 10. ATTACHMENT VIEWER MODAL
    // ==========================================
    if (viewingAttachment != null) {
        val att = viewingAttachment!!
        Dialog(
            onDismissRequest = { viewingAttachment = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewingAttachment = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (att.fileType == "image" || att.fileType == "gif") {
                    AsyncImage(
                        model = att.url,
                        contentDescription = att.filename,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        color = Color(0xFF18191A),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF262626)),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF0084FF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = att.filename,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Downloading ${att.filename}...", Toast.LENGTH_SHORT).show()
                                    viewingAttachment = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download File")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * DeliveryTicksView - Renders single, double grey, or double blue seen checkmarks
 */
@Composable
fun DeliveryTicksView(isSeen: Boolean, isDelivered: Boolean) {
    if (isSeen) {
        // Double blue checks
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = "Seen", tint = Color(0xFF0084FF), modifier = Modifier.size(13.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0084FF), modifier = Modifier.size(13.dp))
        }
    } else if (isDelivered) {
        // Double grey checks
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = "Delivered", tint = Color(0xFF8E8E93), modifier = Modifier.size(13.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(13.dp))
        }
    } else {
        // Single grey check
        Icon(Icons.Default.Check, contentDescription = "Sent", tint = Color(0xFF8E8E93), modifier = Modifier.size(13.dp))
    }
}

/**
 * VoiceNotePlayerView - Voice note audio player matching WhatsApp / Messenger waveform
 */
@Composable
fun VoiceNotePlayerView(attachment: ChatAttachment, isMine: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isMine) Color(0xFF0073E6) else Color(0xFF1E293B))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Simulated audio wave bars
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(20) { i ->
                val barHeight = (6 + (sin((i * 1.5).toDouble()) * 8).toInt()).coerceIn(4, 20)
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = if (isPlaying && i < 10) 0.95f else 0.4f))
                )
            }
        }

        Text(
            text = "0:${(attachment.durationSeconds ?: 12).toString().padStart(2, '0')}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmojiPickerGrid(onSelect: (String) -> Unit) {
    val emojis = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
        "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
        "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
        "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
        "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
        "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗",
        "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯",
        "👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✌️", "🤞",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
        "🔥", "✨", "🎉", "💯", "🚀", "⭐", "🌟", "⚡", "🎁", "🎈"
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val rows = emojis.chunked(10)
        items(rows.size) { rowIndex ->
            val row = rows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                row.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier.clickable { onSelect(emoji) }
                    )
                }
            }
        }
    }
}

@Composable
fun StickerPickerGrid(onSelect: (String) -> Unit) {
    val stickers = listOf(
        "❤️ Love", "🔥 Fire", "🎉 Party", "✨ Magic", "💯 100",
        "👏 Bravo", "🙌 Hurray", "😍 In love", "🤣 LOL", "🥳 Celebration",
        "👋 Hello", "🤝 Deal", "⭐ Star", "🚀 Launch", "☕ Coffee",
        "🍕 Pizza", "⚡ Lightning", "🎁 Gift", "🎈 Balloon", "🏆 Champion"
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val rows = stickers.chunked(4)
        items(rows.size) { rowIndex ->
            val row = rows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { sticker ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(sticker) }
                    ) {
                        Text(
                            text = sticker,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GifPickerGrid(onSelect: (String) -> Unit) {
    val quickGifs = listOf(
        Pair("Love", "https://media.tenor.com/5tQq5R6kB6YAAAAC/love-hearts.gif"),
        Pair("Kiss", "https://media.tenor.com/2b8y0xKx2j0AAAAC/kiss-love.gif"),
        Pair("Hug", "https://media.tenor.com/0Qy2Z4nC7yEAAAAC/hug.gif"),
        Pair("Flowers", "https://media.tenor.com/HX1k6yJwTQkAAAAC/flowers-rose.gif"),
        Pair("Rose", "https://media.tenor.com/2q4m8Qj0pGkAAAAC/rose.gif"),
        Pair("Birthday", "https://media.tenor.com/9u1bQ0nPZfEAAAAC/happy-birthday.gif"),
        Pair("Cake", "https://media.tenor.com/Q9y6jB4yG2QAAAAC/birthday-cake.gif"),
        Pair("Clap", "https://media.tenor.com/6Y5bRr7x0ZgAAAAC/clap-applause.gif"),
        Pair("Hype", "https://media.tenor.com/5F3p8Gv6k9AAAAAC/hype-excited.gif"),
        Pair("Fire", "https://media.tenor.com/0fQm8q2x2qkAAAAC/fire-lit.gif"),
        Pair("LOL", "https://media.tenor.com/2roX3uxz_68AAAAC/lol-laugh.gif"),
        Pair("Wow", "https://media.tenor.com/3k9VQqvK9xgAAAAC/wow-amazed.gif")
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val rows = quickGifs.chunked(3)
        items(rows.size) { rowIndex ->
            val row = rows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (title, url) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF18191A))
                            .border(1.dp, Color(0xFF242526), RoundedCornerShape(10.dp))
                            .clickable { onSelect(url) }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionSheetButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDanger) Color(0xFFFF6B6B) else Color(0xFFF0F2F5),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDanger) Color(0xFFFF6B6B) else Color(0xFFF0F2F5)
            )
        }
    }
}
