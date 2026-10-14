package com.unera.social.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TermsOfServiceScreen - Native Jetpack Compose implementation of TermsOfService.tsx.
 *
 * Implements complete UNERA Terms of Service page:
 * - Sticky header with branding and "Back to Home"
 * - Important Notice banner
 * - All 16 complete legal terms sections matching web
 * - Highlighting cards for Eligibility, Responsibilities, Prohibited Content, Features, Liability
 * - Contact information and bottom navigation return buttons
 */
@Composable
fun TermsOfServiceScreen(
    onBack: () -> Unit,
    onOpenPrivacy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B18))
            .testTag("terms_of_service_screen")
    ) {
        // Sticky Header
        Surface(
            color = Color(0xFF0F172A),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1877F2), Color(0xFF1D8AF2))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "UNERA Terms",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1877F2)
                    )
                }

                // Back Button
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Back",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }
            }
        }

        // Scrollable Legal Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Title & Effective Date
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "Terms of Service",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Effective Date: February 14, 2025",
                        fontSize = 13.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                }
            }

            // Important Notice Banner
            item {
                Surface(
                    color = Color(0xFF1A2B42),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D88FF).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Important Notice:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE4E6EB)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "By accessing or using the UNERA Platform, you agree to be bound by these Terms. If you disagree with any part of the terms, then you may not access the Service. Please read these Terms carefully before using UNERA.",
                                fontSize = 13.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // 1. Acceptance of Terms
            item {
                TermsSection(
                    title = "1. Acceptance of Terms",
                    content = "By creating an account or using UNERA in any way, you agree to these Terms of Service, our Privacy Policy, and any other policies we may post. These Terms apply to all visitors, users, and others who access or use our Service (\"Users\").\n\nIf you are using UNERA on behalf of a business or entity, you represent that you have the authority to bind that business or entity to these Terms."
                )
            }

            // 2. Eligibility
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "2. Eligibility",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    BulletPointRow(
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF45BD62),
                        text = "You must be at least 13 years old to use UNERA. If you are under 18, you must have a parent or guardian's consent."
                    )
                    BulletPointRow(
                        icon = Icons.Default.Block,
                        iconTint = Color(0xFFE41E3F),
                        text = "You must not be prohibited from receiving our services under the laws of Tanzania or any other applicable jurisdiction."
                    )
                    BulletPointRow(
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF45BD62),
                        text = "You must not have been previously removed or banned from UNERA for violation of our policies."
                    )
                }
            }

            // 3. Accounts
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "3. Accounts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "When you create an account with us, you must provide us information that is accurate, complete, and current at all times. Failure to do so constitutes a breach of the Terms, which may result in immediate termination of your account on our Service.",
                        fontSize = 14.5.sp,
                        color = Color(0xFFD0D2D6),
                        lineHeight = 21.sp
                    )
                    Text(
                        text = "You are responsible for safeguarding the password that you use to access the Service and for any activities or actions under your password. You agree not to disclose your password to any third party.",
                        fontSize = 14.5.sp,
                        color = Color(0xFFD0D2D6),
                        lineHeight = 21.sp
                    )

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Account Responsibilities:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                            BulletItem("You must notify us immediately upon becoming aware of any breach of security.")
                            BulletItem("You may not use another user's account without permission.")
                            BulletItem("You are solely responsible for all activities that occur under your account.")
                            BulletItem("You may not create multiple accounts for deceptive or abusive purposes.")
                        }
                    }
                }
            }

            // 4. User Content
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "4. User Content",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Our Service allows you to post, link, store, share and otherwise make available certain information, text, graphics, videos, music, or other material (\"Content\"). You are responsible for the Content that you post to the Service, including its legality, reliability, and appropriateness.",
                        fontSize = 14.5.sp,
                        color = Color(0xFFD0D2D6),
                        lineHeight = 21.sp
                    )

                    // Prohibited Content Box
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFE41E3F),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Prohibited Content:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC)
                                )
                            }

                            val prohibitedList = listOf(
                                "Hate speech, discrimination, or harassment",
                                "Violence, threats, or incitement to violence",
                                "Nudity, pornography, or sexual content",
                                "Spam, scams, or misleading information",
                                "Illegal activities, goods, or services",
                                "Copyright infringement or stolen content",
                                "Malware, viruses, or harmful code",
                                "Unauthorized commercial communications"
                            )

                            prohibitedList.forEach { rule ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = Color(0xFFE41E3F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = rule,
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Content License: By posting Content on UNERA, you grant us a worldwide, non-exclusive, royalty-free license to use, copy, reproduce, process, adapt, modify, publish, transmit, display, and distribute such Content on our platform.",
                        fontSize = 14.sp,
                        color = Color(0xFFD0D2D6),
                        lineHeight = 20.sp
                    )
                }
            }

            // 5. Intellectual Property Rights
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "5. Intellectual Property Rights",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "The Service and its original content, features, and functionality are and will remain the exclusive property of UNERA and its licensors. The Service is protected by copyright, trademark, and other laws of Tanzania and international jurisdictions.",
                        fontSize = 14.5.sp,
                        color = Color(0xFFD0D2D6),
                        lineHeight = 21.sp
                    )

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Our Intellectual Property Includes:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                            BulletItem("The UNERA name, logo, and brand identity")
                            BulletItem("Our application design, user interface, and graphics")
                            BulletItem("Our proprietary code, algorithms, and databases")
                            BulletItem("Our trademarks and trade dress")
                        }
                    }
                }
            }

            // 6. Platform Features & Services
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "6. Platform Features & Services",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    FeatureCard(
                        icon = Icons.Default.Storefront,
                        iconTint = Color(0xFF1877F2),
                        title = "Marketplace",
                        desc = "UNERA is not a party to transactions between buyers and sellers. We do not guarantee the quality, safety, or legality of items listed. Users are responsible for complying with all applicable laws."
                    )

                    FeatureCard(
                        icon = Icons.Default.MusicNote,
                        iconTint = Color(0xFF0055FF),
                        title = "F-Music",
                        desc = "Users uploading music content must own or have proper licenses for all material. Unauthorized distribution of copyrighted music is prohibited and may result in account termination."
                    )

                    FeatureCard(
                        icon = Icons.Default.Event,
                        iconTint = Color(0xFFF3425F),
                        title = "Events",
                        desc = "Event organizers are solely responsible for their events. UNERA does not endorse or verify events posted on the platform. Users attend events at their own risk."
                    )
                }
            }

            // 7. Third-Party Links & Services
            item {
                TermsSection(
                    title = "7. Third-Party Links & Services",
                    content = "Our Service may contain links to third-party websites or services that are not owned or controlled by UNERA.\n\nUNERA has no control over, and assumes no responsibility for, the content, privacy policies, or practices of any third-party websites or services. We strongly advise you to read the terms and privacy policies of any third-party websites that you visit."
                )
            }

            // 8. Termination
            item {
                TermsSection(
                    title = "8. Termination",
                    content = "We may terminate or suspend your account immediately, without prior notice or liability, for any reason whatsoever, including without limitation if you breach the Terms.\n\nUpon termination, your right to use the Service will immediately cease. If you wish to terminate your account, you may simply discontinue using the Service or delete your account through settings."
                )
            }

            // 9. Disclaimer of Warranties
            item {
                TermsSection(
                    title = "9. Disclaimer of Warranties",
                    content = "THE SERVICE IS PROVIDED ON AN \"AS IS\" AND \"AS AVAILABLE\" BASIS. UNERA MAKES NO REPRESENTATIONS OR WARRANTIES OF ANY KIND, EXPRESS OR IMPLIED, REGARDING THE OPERATION OR AVAILABILITY OF THE SERVICE, OR THE INFORMATION, CONTENT, AND MATERIALS INCLUDED THEREIN."
                )
            }

            // 10. Limitation of Liability
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "10. Limitation of Liability",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        color = Color(0xFF281518),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE41E3F).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "IMPORTANT LIABILITY LIMITATIONS:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE4E6EB)
                            )
                            Text(
                                text = "In no event shall UNERA, nor its directors, employees, or affiliates, be liable for any indirect, incidental, special, or consequential damages resulting from your access to or inability to use the Service, any conduct of third parties, or unauthorized access to your transmissions.",
                                fontSize = 12.5.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // 11-15. Legal standard sections
            item {
                TermsSection(
                    title = "11. Indemnification",
                    content = "You agree to defend, indemnify and hold harmless UNERA and its licensee and licensors from and against any claims, damages, liabilities, and expenses resulting from your use of the Service, violation of these Terms, or violation of third-party rights."
                )
            }

            item {
                TermsSection(
                    title = "12. Governing Law",
                    content = "These Terms shall be governed and construed in accordance with the laws of Tanzania, without regard to its conflict of law provisions."
                )
            }

            item {
                TermsSection(
                    title = "13. Changes to Terms",
                    content = "We reserve the right, at our sole discretion, to modify or replace these Terms at any time. By continuing to access or use our Service after revisions become effective, you agree to be bound by the revised terms."
                )
            }

            item {
                TermsSection(
                    title = "14. Dispute Resolution",
                    content = "Before filing any formal legal claim, you agree to first contact us and attempt to resolve the dispute informally. Any unresolved dispute arising from these Terms shall be resolved through binding arbitration in Tanzania."
                )
            }

            item {
                TermsSection(
                    title = "15. Entire Agreement",
                    content = "These Terms constitute the entire agreement between us regarding our Service, and supersede and replace any prior agreements."
                )
            }

            // 16. Contact Us
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "16. Contact Us",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "If you have any questions about these Terms, please contact us:",
                        fontSize = 14.sp,
                        color = Color(0xFFD0D2D6)
                    )

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ContactRow(
                                icon = Icons.Default.Email,
                                title = "Legal Department",
                                subtitle = "legal@unera.social"
                            )
                            ContactRow(
                                icon = Icons.Default.Language,
                                title = "Website",
                                subtitle = "https://unera.social"
                            )
                            if (onOpenPrivacy != null) {
                                ContactRow(
                                    icon = Icons.Default.Shield,
                                    title = "Related Documents",
                                    subtitle = "Privacy Policy",
                                    isAction = true,
                                    onClick = onOpenPrivacy
                                )
                            }
                        }
                    }
                }
            }

            // Footer Return Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "By using UNERA, you acknowledge that you have read, understood, and agree to be bound by these Terms of Service.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Return to UNERA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (onOpenPrivacy != null) {
                            Button(
                                onClick = onOpenPrivacy,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF8FAFC))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Privacy Policy", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFF8FAFC))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermsSection(
    title: String,
    content: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = content,
            fontSize = 14.5.sp,
            color = Color(0xFFD0D2D6),
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun BulletPointRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFF1877F2))
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC)
                )
            }
            Text(
                text = desc,
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isAction: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF1877F2),
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAction) Color(0xFF1877F2) else Color(0xFFF8FAFC)
            )
        }
    }
}
