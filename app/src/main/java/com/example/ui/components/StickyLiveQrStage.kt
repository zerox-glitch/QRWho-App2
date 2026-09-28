package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.qr.engine.QrStyle
import com.example.qr.engine.ScanCheckResult
import com.example.ui.theme.CardDark
import com.example.ui.theme.ElectricCyan

@Composable
fun StickyLiveQrStage(
    visible: Boolean,
    bitmap: Bitmap?,
    scanResult: ScanCheckResult?,
    style: QrStyle,
    isGenerating: Boolean,
    onScrollToTop: () -> Unit,
    onSaveHistory: () -> Unit,
    onAutoFix: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it / 2 }) + scaleIn(initialScale = 0.85f) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + scaleOut(targetScale = 0.85f) + fadeOut(),
        modifier = modifier
    ) {
        // Pure Live Sticky QR Code Preview (Clean, prominent, no text/labels beside it)
        Surface(
            modifier = Modifier
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp), spotColor = ElectricCyan)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, ElectricCyan, RoundedCornerShape(16.dp))
                .clickable(onClick = onScrollToTop)
                .testTag("sticky_live_qr_preview"),
            color = Color(style.bgColor)
        ) {
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Live Sticky QR Code",
                        modifier = Modifier
                            .size(106.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .size(106.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
