package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun QrGuideModalDialog(
    onDismiss: () -> Unit,
    onLaunchStudio: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Functions Guide", "Scannability Rules", "Pro Tips")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                    .testTag("guide_modal_surface"),
                color = CardDark,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f))
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = "Guide",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "QRWho Guide",
                                    color = TextPrimary,
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic
                                )
                                Text(
                                    text = "Features & Scannability Handbook",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, CircleShape)
                                .testTag("guide_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceDark)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ElectricCyan else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 8.dp)
                                    .testTag("guide_tab_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color(0xFF0F172A) else TextMuted,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp, max = 440.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 2.dp)
                    ) {
                        when (selectedTab) {
                            0 -> FunctionsGuideContent()
                            1 -> ScannabilityRulesContent()
                            2 -> ProTipsContent()
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "Close",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onLaunchStudio()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("guide_launch_studio_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Launch Studio",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FunctionsGuideContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideFeatureCard(
            icon = Icons.Default.Tune,
            iconTint = ElectricCyan,
            title = "1. Studio & 15+ Data Types",
            description = "Create QR codes for URLs, Wi-Fi networks (one-tap password connect), vCard contacts, Email, SMS, Phone, Crypto wallets (BTC, ETH, SOL), UPI payment, Geo coordinates, and plain text."
        )

        GuideFeatureCard(
            icon = Icons.Default.AutoAwesome,
            iconTint = NeonViolet,
            title = "2. Designer Styling & Module Shapes",
            description = "Customize 12+ dot module shapes (Rounded, Circle, Diamond, Star, Heart, Leaf, Liquid), 8 finder eye/pupil shapes, and multi-color Linear, Radial, or Sweep gradients."
        )

        GuideFeatureCard(
            icon = Icons.Default.Image,
            iconTint = Color(0xFFFF9E9C),
            title = "3. Photo-Woven QR Art & Center Logos",
            description = "Blend background images or artwork directly behind the code with Camera-Safe edge kernels, adjustable opacity, and embed your custom brand logo in the center."
        )

        GuideFeatureCard(
            icon = Icons.Default.CheckCircle,
            iconTint = EmeraldGreen,
            title = "4. Live Optical Scannability & 1-Tap Optimize",
            description = "Simulates real mobile camera lenses at multiple distances (260px, 340px, 440px). Tap 'Optimize' to gently calibrate dot scale, quiet zone, and Level H error correction without ruining your colors."
        )

        GuideFeatureCard(
            icon = Icons.Default.Download,
            iconTint = ElectricCyan,
            title = "5. Ultra HD PNG & Vector SVG Export",
            description = "Export print-ready 2048px PNG or infinite-resolution vector SVG graphics suitable for large billboards, posters, menus, or merchandise with zero watermarks."
        )

        GuideFeatureCard(
            icon = Icons.Default.QrCodeScanner,
            iconTint = EmeraldGreen,
            title = "6. Built-in Scanner & Offline Vault",
            description = "Instant camera scanner with flashlight and batch scanning. Automatically saves scanned and created QR codes in an encrypted on-device SQLite vault with custom presets."
        )
    }
}

@Composable
private fun ScannabilityRulesContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideRuleCard(
            number = "1",
            icon = Icons.Default.Tune,
            title = "Contrast is King",
            badge = "Essential",
            badgeColor = EmeraldGreen,
            description = "Smartphone cameras decode brightness differences (luminance), not just colors. Ensure dots are noticeably darker than a light canvas, or significantly brighter than a dark canvas (aim for ≥ 25% contrast)."
        )

        GuideRuleCard(
            number = "2",
            icon = Icons.Default.Tune,
            title = "Protect the Quiet Zone",
            badge = "Focus Lock",
            badgeColor = ElectricCyan,
            description = "Always maintain a clean 2 to 4 cell quiet zone border around your QR code. Without a clean border, camera sensors struggle to detect QR boundaries against textured backgrounds."
        )

        GuideRuleCard(
            number = "3",
            icon = Icons.Default.AutoAwesome,
            title = "Solid Dot Size for Distance",
            badge = "Distance Lock",
            badgeColor = NeonViolet,
            description = "When scanning from a laptop screen or from a distance, higher dot size (88%–95%) and minimal module gaps prevent optical blurring, allowing instant camera lock."
        )

        GuideRuleCard(
            number = "4",
            icon = Icons.Default.Security,
            title = "Level H ECC for Center Logos",
            badge = "30% Recovery",
            badgeColor = Color(0xFFFF9E9C),
            description = "Center logos cover part of the data grid. Level H (30% Reed-Solomon redundancy) ensures your code scans 100% reliably even with a large brand logo embedded in the center."
        )

        GuideRuleCard(
            number = "5",
            icon = Icons.Default.Image,
            title = "Balanced Photo Opacity (45%–65%)",
            badge = "Photo Art",
            badgeColor = ElectricCyan,
            description = "When weaving a background photo, enable the 'Camera-Safe' kernel and keep opacity between 45%–65%. Very high photo contrast can create visual noise that distracts camera focus."
        )

        GuideRuleCard(
            number = "6",
            icon = Icons.Default.CheckCircle,
            title = "Dark-on-Light vs Inverted",
            badge = "Compatibility",
            badgeColor = EmeraldGreen,
            description = "Traditional phone camera apps prioritize dark modules on a light canvas. If you create inverted (light-on-dark) QR codes, ensure your dots are luminous and high-contrast."
        )
    }
}

@Composable
private fun ProTipsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideTipCard(
            icon = Icons.Default.Security,
            iconTint = EmeraldGreen,
            title = "100% On-Device & Private",
            description = "QRWho processes all QR codes, images, and camera scans completely offline on your device. Zero data is ever sent to any remote server or cloud database."
        )

        GuideTipCard(
            icon = Icons.Default.Download,
            iconTint = ElectricCyan,
            title = "Print Vector SVG for Physical Media",
            description = "For business cards, t-shirts, storefronts, and menus, always use SVG export. Vector graphics scale infinitely without any pixelation or blur at any print size."
        )

        GuideTipCard(
            icon = Icons.Default.History,
            iconTint = NeonViolet,
            title = "Save Custom Styles as Presets",
            description = "Created a signature look? In Studio, tap 'Save Preset' to store your unique combination of colors, shapes, frames, and gradients for one-tap reuse anytime."
        )

        GuideTipCard(
            icon = Icons.Default.Lightbulb,
            iconTint = Color(0xFFFFC107),
            title = "Screen Glare & Angle Tip",
            description = "When scanning off a laptop screen, avoid direct light reflections and hold your phone 1 to 2 feet away. The optical evaluator tests this exact distance scenario!"
        )
    }
}

@Composable
private fun GuideFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.5.sp
                )
            }
        }
    }
}

@Composable
private fun GuideRuleCard(
    number: String,
    icon: ImageVector,
    title: String,
    badge: String,
    badgeColor: Color,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            // Header Row: Number + Icon + Title + Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.18f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number,
                            color = ElectricCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.5.sp
            )
        }
    }
}

@Composable
private fun GuideTipCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.5.sp
                )
            }
        }
    }
}
