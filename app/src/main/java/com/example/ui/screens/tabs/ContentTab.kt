package com.example.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qr.engine.PayloadKind
import com.example.qr.engine.QrPayload
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ContentTab(
    payload: QrPayload,
    onPayloadChange: (QrPayload) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipScrollState = rememberScrollState()
    var showWifiPassword by remember { mutableStateOf(false) }

    val kinds = listOf(
        Pair(PayloadKind.URL, Icons.Default.Language),
        Pair(PayloadKind.WIFI, Icons.Default.Wifi),
        Pair(PayloadKind.VCARD, Icons.Default.ContactPage),
        Pair(PayloadKind.EVENT, Icons.Default.CalendarMonth),
        Pair(PayloadKind.EMAIL, Icons.Default.AlternateEmail),
        Pair(PayloadKind.PHONE, Icons.Default.Phone),
        Pair(PayloadKind.SMS, Icons.Default.Sms),
        Pair(PayloadKind.WHATSAPP, Icons.Default.Sms),
        Pair(PayloadKind.PAYMENT, Icons.Default.Payment),
        Pair(PayloadKind.GEO, Icons.Default.LocationOn),
        Pair(PayloadKind.TEXT, Icons.Default.TextFields)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Payload type selector chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            kinds.forEach { (kind, icon) ->
                val isSelected = payload.kind == kind
                PayloadChip(
                    label = kind.displayName,
                    icon = icon,
                    isSelected = isSelected,
                    onClick = { onPayloadChange(payload.copy(kind = kind)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Payload Form
        when (payload.kind) {
            PayloadKind.URL -> {
                StudioInputField(
                    label = "Website URL",
                    value = payload.url,
                    onValueChange = { onPayloadChange(payload.copy(url = it)) },
                    placeholder = "https://yourwebsite.com",
                    keyboardType = KeyboardType.Uri
                )
            }
            PayloadKind.WIFI -> {
                StudioInputField(
                    label = "Network SSID",
                    value = payload.wifiSsid,
                    onValueChange = { onPayloadChange(payload.copy(wifiSsid = it)) },
                    placeholder = "My Home Wi-Fi"
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Network Security / Encryption Selector
                Text("Network Security Type", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val secOptions = listOf(
                    "WPA" to "WPA / WPA2 / WPA3",
                    "WEP" to "WEP (Old Network)",
                    "WPS" to "WPS (PIN / Push)",
                    "nopass" to "Open (No Pass)"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    secOptions.forEach { (typeKey, typeLabel) ->
                        val isSel = payload.wifiEncryption.equals(typeKey, ignoreCase = true) ||
                                (typeKey == "WPA" && (payload.wifiEncryption.equals("WPA2", true) || payload.wifiEncryption.equals("WPA3", true))) ||
                                (typeKey == "nopass" && payload.wifiEncryption.equals("Open", true))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) ElectricCyan else CardDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) ElectricCyan else CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onPayloadChange(payload.copy(wifiEncryption = typeKey))
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (typeKey) {
                                        "WPA" -> "WPA/WPA2"
                                        "WEP" -> "WEP (Old)"
                                        "WPS" -> "WPS"
                                        else -> "Open"
                                    },
                                    color = if (isSel) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Password / Key Field (Required for WPA, WEP, and WPS PIN)
                if (payload.wifiEncryption != "nopass" && payload.wifiEncryption != "Open") {
                    OutlinedTextField(
                        value = if (payload.wifiEncryption == "WPS" && payload.wifiWpsMethod == "PIN") payload.wifiWpsPin else payload.wifiPassword,
                        onValueChange = { input ->
                            if (payload.wifiEncryption == "WPS" && payload.wifiWpsMethod == "PIN") {
                                onPayloadChange(payload.copy(wifiWpsPin = input))
                            } else {
                                onPayloadChange(payload.copy(wifiPassword = input))
                            }
                        },
                        label = {
                            Text(
                                text = when (payload.wifiEncryption) {
                                    "WEP" -> "WEP Key (64/128-bit WEP)"
                                    "WPS" -> if (payload.wifiWpsMethod == "PIN") "WPS 8-Digit PIN" else "WPS Password / Key"
                                    else -> "WPA/WPA2 Password"
                                },
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (showWifiPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showWifiPassword = !showWifiPassword }) {
                                Icon(
                                    imageVector = if (showWifiPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = TextMuted
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wifi_password_input"),
                        colors = customTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // WPS Specific Sub-method selector
                if (payload.wifiEncryption == "WPS") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("WPS Method", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("PIN" to "PIN Code", "PBC" to "Push Button").forEach { (methodKey, labelStr) ->
                                val isSelected = payload.wifiWpsMethod == methodKey
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) NeonViolet else SurfaceDark,
                                    modifier = Modifier.clickable { onPayloadChange(payload.copy(wifiWpsMethod = methodKey)) }
                                ) {
                                    Text(
                                        text = labelStr,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Hidden Network", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Network does not broadcast SSID", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = payload.wifiHidden,
                        onCheckedChange = { onPayloadChange(payload.copy(wifiHidden = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                    )
                }
            }
            PayloadKind.VCARD -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioInputField(
                        label = "First Name",
                        value = payload.vcardFirstName,
                        onValueChange = { onPayloadChange(payload.copy(vcardFirstName = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    StudioInputField(
                        label = "Last Name",
                        value = payload.vcardLastName,
                        onValueChange = { onPayloadChange(payload.copy(vcardLastName = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "Phone Number",
                    value = payload.vcardPhone,
                    onValueChange = { onPayloadChange(payload.copy(vcardPhone = it)) },
                    keyboardType = KeyboardType.Phone
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "Email Address",
                    value = payload.vcardEmail,
                    onValueChange = { onPayloadChange(payload.copy(vcardEmail = it)) },
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "Company / Organization",
                    value = payload.vcardOrg,
                    onValueChange = { onPayloadChange(payload.copy(vcardOrg = it)) }
                )
            }
            PayloadKind.EVENT -> {
                StudioInputField(
                    label = "Event Title",
                    value = payload.eventTitle,
                    onValueChange = { onPayloadChange(payload.copy(eventTitle = it)) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "Location",
                    value = payload.eventLocation,
                    onValueChange = { onPayloadChange(payload.copy(eventLocation = it)) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "Description",
                    value = payload.eventDescription,
                    onValueChange = { onPayloadChange(payload.copy(eventDescription = it)) },
                    singleLine = false
                )
            }
            PayloadKind.EMAIL -> {
                StudioInputField(
                    label = "Recipient Email",
                    value = payload.emailTo,
                    onValueChange = { onPayloadChange(payload.copy(emailTo = it)) },
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "Subject",
                    value = payload.emailSubject,
                    onValueChange = { onPayloadChange(payload.copy(emailSubject = it)) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "Message Body",
                    value = payload.emailBody,
                    onValueChange = { onPayloadChange(payload.copy(emailBody = it)) },
                    singleLine = false
                )
            }
            PayloadKind.PHONE -> {
                StudioInputField(
                    label = "Phone Number",
                    value = payload.phoneNumber,
                    onValueChange = { onPayloadChange(payload.copy(phoneNumber = it)) },
                    keyboardType = KeyboardType.Phone
                )
            }
            PayloadKind.SMS -> {
                StudioInputField(
                    label = "Phone Number",
                    value = payload.smsNumber,
                    onValueChange = { onPayloadChange(payload.copy(smsNumber = it)) },
                    keyboardType = KeyboardType.Phone
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "SMS Message",
                    value = payload.smsMessage,
                    onValueChange = { onPayloadChange(payload.copy(smsMessage = it)) },
                    singleLine = false
                )
            }
            PayloadKind.WHATSAPP -> {
                StudioInputField(
                    label = "WhatsApp Number (with country code)",
                    value = payload.waNumber,
                    onValueChange = { onPayloadChange(payload.copy(waNumber = it)) },
                    placeholder = "+1234567890",
                    keyboardType = KeyboardType.Phone
                )
                Spacer(modifier = Modifier.height(10.dp))
                StudioInputField(
                    label = "Pre-filled Message",
                    value = payload.waMessage,
                    onValueChange = { onPayloadChange(payload.copy(waMessage = it)) },
                    singleLine = false
                )
            }
            PayloadKind.PAYMENT -> {
                StudioInputField(
                    label = "Payment ID / Address (UPI, BTC, ETH)",
                    value = payload.paymentAddress,
                    onValueChange = { onPayloadChange(payload.copy(paymentAddress = it)) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioInputField(
                        label = "Amount (Optional)",
                        value = payload.paymentAmount,
                        onValueChange = { onPayloadChange(payload.copy(paymentAmount = it)) },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    StudioInputField(
                        label = "Note",
                        value = payload.paymentNote,
                        onValueChange = { onPayloadChange(payload.copy(paymentNote = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            PayloadKind.GEO -> {
                StudioInputField(
                    label = "Search Query / Address",
                    value = payload.geoQuery,
                    onValueChange = { onPayloadChange(payload.copy(geoQuery = it)) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioInputField(
                        label = "Latitude",
                        value = payload.geoLat.toString(),
                        onValueChange = {
                            val v = it.toDoubleOrNull() ?: payload.geoLat
                            onPayloadChange(payload.copy(geoLat = v))
                        },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    StudioInputField(
                        label = "Longitude",
                        value = payload.geoLng.toString(),
                        onValueChange = {
                            val v = it.toDoubleOrNull() ?: payload.geoLng
                            onPayloadChange(payload.copy(geoLng = v))
                        },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            PayloadKind.TEXT -> {
                StudioInputField(
                    label = "Plain Text",
                    value = payload.text,
                    onValueChange = { onPayloadChange(payload.copy(text = it)) },
                    singleLine = false
                )
            }
        }
    }
}

@Composable
fun PayloadChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) ElectricCyan.copy(alpha = 0.2f) else CardDark
    val border = if (isSelected) ElectricCyan else CardBorder
    val contentColor = if (isSelected) ElectricCyan else TextSecondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = contentColor, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun StudioInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 13.sp) },
        placeholder = placeholder?.let { { Text(it, color = TextMuted, fontSize = 13.sp) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
            .fillMaxWidth()
            .testTag("input_${label.lowercase().replace(" ", "_")}"),
        colors = customTextFieldColors()
    )
}

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ElectricCyan,
    unfocusedBorderColor = CardBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = SurfaceDark,
    unfocusedContainerColor = SurfaceDark,
    cursorColor = ElectricCyan
)
