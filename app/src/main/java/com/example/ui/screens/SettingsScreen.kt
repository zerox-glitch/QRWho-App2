package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BgDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val URL_PRIVACY_POLICY = "https://qrwho.online/privacy"
private const val URL_TERMS_OF_USE = "https://qrwho.online/terms"
private const val URL_QRWHO_WEBSITE = "https://qrwho.online/"
private const val URL_SUPPORT_QRWHO = "https://ko-fi.com/qrwho"

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onNavigateBack() }

    // Dynamic app version retrieval
    val packageInfo = remember {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "1"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "1"
    }

    // App Preferences in SharedPreferences
    val prefs = remember { context.getSharedPreferences("qrwho_settings", Context.MODE_PRIVATE) }
    var hapticFeedbackEnabled by remember {
        mutableStateOf(prefs.getBoolean("haptic_feedback", true))
    }

    var showAboutDialog by remember { mutableStateOf(false) }

    fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser for: $url", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, CardBorder, CircleShape)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // SECTION 1: ABOUT QRWHO HERO BRANDING
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.qrwho_logo),
                        contentDescription = "QRWho Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "QRWho",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "\"Create beautiful, customizable QR codes that actually scan.\"",
                        color = ElectricCyan,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Installed App Version Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                        color = SurfaceDark
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Installed Version $versionName ($versionCode)",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: ABOUT QRWHO OPTIONS
            Text(
                text = "ABOUT QRWHO",
                color = TextMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column {
                    // 1. Privacy Policy
                    SettingsItemRow(
                        icon = Icons.Default.Security,
                        iconTint = EmeraldGreen,
                        title = "Privacy Policy",
                        subtitle = "100% on-device private processing",
                        isExternalLink = true,
                        testTag = "settings_privacy_policy_button",
                        onClick = { openExternalUrl(URL_PRIVACY_POLICY) }
                    )

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    // 2. Terms of Use
                    SettingsItemRow(
                        icon = Icons.Default.Description,
                        iconTint = ElectricCyan,
                        title = "Terms of Use",
                        subtitle = "License, terms and conditions",
                        isExternalLink = true,
                        testTag = "settings_terms_of_use_button",
                        onClick = { openExternalUrl(URL_TERMS_OF_USE) }
                    )

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    // 3. QRWho Website
                    SettingsItemRow(
                        icon = Icons.Default.Language,
                        iconTint = ElectricCyan,
                        title = "QRWho Website",
                        subtitle = "qrwho.online",
                        isExternalLink = true,
                        testTag = "settings_website_button",
                        onClick = { openExternalUrl(URL_QRWHO_WEBSITE) }
                    )

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    // 4. Support QRWho
                    SettingsItemRow(
                        icon = Icons.Default.LocalCafe,
                        iconTint = Color(0xFFFF5E5B),
                        title = "Support QRWho",
                        subtitle = "Keep QRWho 100% free and watermark-free",
                        isExternalLink = true,
                        testTag = "settings_support_button",
                        onClick = { openExternalUrl(URL_SUPPORT_QRWHO) }
                    )

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    // 5. About
                    SettingsItemRow(
                        icon = Icons.Default.Info,
                        iconTint = ElectricCyan,
                        title = "About",
                        subtitle = "App description, version & copyright",
                        isExternalLink = false,
                        testTag = "settings_about_button",
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: APP PREFERENCES
            Text(
                text = "PREFERENCES",
                color = TextMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column {
                    // Haptic Feedback Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = !hapticFeedbackEnabled
                                hapticFeedbackEnabled = next
                                prefs.edit().putBoolean("haptic_feedback", next).apply()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("settings_haptic_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Haptic Feedback",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Vibrate on scan detection and tap actions",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = hapticFeedbackEnabled,
                            onCheckedChange = { checked ->
                                hapticFeedbackEnabled = checked
                                prefs.edit().putBoolean("haptic_feedback", checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0F172A),
                                checkedTrackColor = ElectricCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }

                    HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                    // Error Correction Standard Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Standard Error Correction: Level H",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "30% Reed-Solomon redundancy for high-res print and camera lock",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer Copyright
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "QRWho Studio",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "© 2026 QRWho · ISO/IEC 18004 Standard · All Rights Reserved",
                    color = TextMuted,
                    fontSize = 10.5.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ABOUT DETAILS DIALOG
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = CardDark,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.qrwho_logo),
                            contentDescription = "QRWho Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "About QRWho",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version $versionName ($versionCode)",
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "QRWho is the premier artistic QR code generator. Built with modern computer vision, Level H error correction, and 100% on-device private processing. Create, style, verify, and export print-ready QR art with zero watermarks.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.5.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                            color = SurfaceDark
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "• 100% On-Device & Private (No cloud upload)",
                                    color = TextPrimary,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Level H (30%) Error Correction by Default",
                                    color = TextPrimary,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• 2048px PNG & Vector SVG Export",
                                    color = TextPrimary,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Built-in Optical Scannability Verification",
                                    color = TextPrimary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "© 2026 QRWho Studio · ISO/IEC 18004 Standard",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isExternalLink: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Icon(
            imageVector = if (isExternalLink) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(17.dp)
        )
    }
}
