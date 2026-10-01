package com.example

import com.example.qr.engine.PayloadKind
import com.example.qr.engine.QrEffect
import com.example.qr.engine.QrGenerator
import com.example.qr.engine.QrPayload
import com.example.qr.engine.QrPresets
import com.example.qr.engine.QrScannabilityEvaluator
import com.example.qr.engine.QrStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrEngineTest {

    @Test
    fun testPayloadEncoding_url() {
        val payload = QrPayload(kind = PayloadKind.URL, url = "https://qrwho.vercel.app")
        assertEquals("https://qrwho.vercel.app", payload.toEncodedText())
    }

    @Test
    fun testPayloadEncoding_wifi() {
        val payload = QrPayload(
            kind = PayloadKind.WIFI,
            wifiSsid = "Studio_WiFi",
            wifiPassword = "secret;pass",
            wifiEncryption = "WPA"
        )
        val text = payload.toEncodedText()
        assertTrue(text.startsWith("WIFI:S:Studio_WiFi;"))
        assertTrue(text.contains("T:WPA;"))
        assertTrue(text.contains("P:secret\\;pass;"))
    }

    @Test
    fun testPayloadEncoding_vcard() {
        val payload = QrPayload(
            kind = PayloadKind.VCARD,
            vcardFirstName = "QRWho",
            vcardLastName = "Studio",
            vcardPhone = "+123456789"
        )
        val text = payload.toEncodedText()
        assertTrue(text.startsWith("BEGIN:VCARD\r\n"))
        assertTrue(text.contains("FN:QRWho Studio"))
        assertTrue(text.contains("TEL;TYPE=CELL:+123456789"))
        assertTrue(text.endsWith("END:VCARD\r\n"))
    }

    @Test
    fun testQrGeneration_and_Evaluation() {
        val payload = "https://qrwho.vercel.app"
        val style = QrStyle(
            moduleShape = com.example.qr.engine.ModuleShape.Square,
            eyeShape = com.example.qr.engine.EyeShape.Square,
            ballShape = com.example.qr.engine.EyeShape.Square,
            gradientType = com.example.qr.engine.GradientType.None,
            moduleGap = 0f,
            dotScale = 1.0f
        )
        val bmp = QrGenerator.generateQrBitmap(payload, style, sizePx = 512)
        assertNotNull(bmp)
        assertEquals(512, bmp.width)
        assertEquals(512, bmp.height)

        val eval = QrScannabilityEvaluator.evaluate(bmp)
        assertTrue(eval.isScannable)
        assertEquals(payload, eval.decodedText)
        assertEquals("Verified", eval.status)
        assertTrue(eval.score >= 90)
    }

    @Test
    fun testVectorSvgGeneration() {
        val payload = "https://qrwho.vercel.app"
        val style = QrStyle()
        val svg = QrGenerator.generateQrSvg(payload, style, sizePx = 1024)
        assertNotNull(svg)
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("viewBox=\"0 0 1024 1024\""))
        assertTrue(svg.contains("</svg>"))
    }

    @Test
    fun testShowcaseStyles_fallbackList() {
        assertTrue(QrPresets.fallbackList.size >= 9)
        val ukiyo = QrPresets.fallbackList.find { it.id == "art-ukiyo" }
        assertNotNull(ukiyo)
        assertEquals("Ukiyo Wave", ukiyo?.name)
    }

    @Test
    fun testScannability_distanceStyling_isScannable() {
        val payload = "https://qrwho.vercel.app"
        val style = QrStyle(
            moduleShape = com.example.qr.engine.ModuleShape.Rounded,
            eyeShape = com.example.qr.engine.EyeShape.Rounded,
            ballShape = com.example.qr.engine.EyeShape.Circle,
            dotScale = 0.92f,
            moduleGap = 0.02f,
            fgColor = 0xFF0F172A.toInt(),
            bgColor = 0xFFFFFFFF.toInt()
        )
        val bmp = QrGenerator.generateQrBitmap(payload, style, sizePx = 512)
        val eval = QrScannabilityEvaluator.evaluate(bmp, style)
        assertTrue("Styled code with high contrast should be scannable", eval.isScannable)
        assertTrue("Score should be >= 90% for high contrast styled code", eval.score >= 90)
        assertEquals(payload, eval.decodedText)
    }

    @Test
    fun testScannability_lowContrast_isCapped() {
        val payload = "https://qrwho.vercel.app"
        val style = QrStyle(
            fgColor = 0xFF888888.toInt(),
            bgColor = 0xFFAAAAAA.toInt(), // very low contrast delta (~13%)
            dotScale = 0.90f
        )
        val bmp = QrGenerator.generateQrBitmap(payload, style, sizePx = 512)
        val eval = QrScannabilityEvaluator.evaluate(bmp, style)
        assertTrue("Low contrast QR code must not be scored >= 80%", eval.score < 80)
        org.junit.Assert.assertFalse("Low contrast QR code must not be marked isScannable", eval.isScannable)
    }

    @Test
    fun testScannability_invertedCode_isCapped() {
        val payload = "https://qrwho.vercel.app"
        val style = QrStyle(
            fgColor = 0xFFFFFFFF.toInt(), // white dots on black background (inverted)
            bgColor = 0xFF000000.toInt(),
            eyeColor = 0xFFFFFFFF.toInt(),
            ballColor = 0xFFFFFFFF.toInt()
        )
        val bmp = QrGenerator.generateQrBitmap(payload, style, sizePx = 512)
        val eval = QrScannabilityEvaluator.evaluate(bmp, style)
        assertTrue("Inverted QR code score should be reasonable", eval.score in 65..85)
    }

    @Test
    fun testOptimizeScan_preservesUserColorsAndEnhancesPhysics() {
        val payload = "https://qrwho.vercel.app"
        val cyanColor = 0xFF00E5FF.toInt()
        val style = QrStyle(
            fgColor = cyanColor,
            bgColor = 0xFFFFFFFF.toInt(),
            dotScale = 0.70f,
            moduleGap = 0.08f,
            quietZone = 1,
            ecc = "M"
        )
        val result = QrScannabilityEvaluator.optimizeScan(
            current = style,
            payloadText = payload,
            photoBitmap = null,
            customLogo = null
        )
        assertTrue(result.ok)
        assertEquals("H", result.style.ecc)
        assertTrue("Dot scale should be boosted >= 0.90f", result.style.dotScale >= 0.90f)
        assertTrue("Quiet zone should be at least 2", result.style.quietZone >= 2)
        assertTrue("Module gap should be reduced <= 0.02f", result.style.moduleGap <= 0.02f)
        // Ensure color is NOT replaced by pure black!
        org.junit.Assert.assertNotEquals(0xFF000000.toInt(), result.style.fgColor)
        org.junit.Assert.assertNotEquals(0xFF0F172A.toInt(), result.style.fgColor)
    }

    @Test
    fun testNeonGlowScannability() {
        val payload = "https://qrwho.vercel.app"
        val style = QrStyle(
            fgColor = 0xFF00F0FF.toInt(),
            bgColor = 0xFF000000.toInt(),
            eyeColor = 0xFF00F0FF.toInt(),
            ballColor = 0xFF00FF87.toInt(),
            effect = QrEffect.Glow,
            effectIntensity = 1.0f,
            dotScale = 0.90f
        )
        val bmp = QrGenerator.generateQrBitmap(payload, style, sizePx = 512)
        val eval = QrScannabilityEvaluator.evaluate(bmp, style)
        assertTrue("Neon glow QR code must be scannable (eval.isScannable=${eval.isScannable}, score=${eval.score}, decoded=${eval.decodedText})", eval.isScannable)
    }

    @Test
    fun testNeonGlowOnLightBgScannability() {
        val payload = "https://qrwho.vercel.app"
        val style = QrStyle(
            fgColor = 0xFF0F172A.toInt(),
            bgColor = 0xFFFFFFFF.toInt(),
            eyeColor = 0xFF0F172A.toInt(),
            ballColor = 0xFF0F172A.toInt(),
            effect = QrEffect.Glow,
            effectIntensity = 1.0f,
            dotScale = 0.90f
        )
        val bmp = QrGenerator.generateQrBitmap(payload, style, sizePx = 512)
        val eval = QrScannabilityEvaluator.evaluate(bmp, style)
        assertTrue("Neon glow on light bg must be scannable (eval.isScannable=${eval.isScannable}, score=${eval.score}, decoded=${eval.decodedText})", eval.isScannable)
    }

    @Test
    fun testOptimizeScan_withNeonGlow() {
        val payload = "https://qrwho.vercel.app"
        val style = QrStyle(
            fgColor = 0xFF00F0FF.toInt(),
            bgColor = 0xFF000000.toInt(),
            eyeColor = 0xFF00F0FF.toInt(),
            ballColor = 0xFF00FF87.toInt(),
            effect = QrEffect.Glow,
            effectIntensity = 1.5f,
            dotScale = 0.85f
        )
        val result = QrScannabilityEvaluator.optimizeScan(
            current = style,
            payloadText = payload,
            photoBitmap = null,
            customLogo = null
        )
        assertTrue("Optimize result must be ok", result.ok)
        val testBmp = QrGenerator.generateQrBitmap(payload, result.style, sizePx = 512)
        val eval = QrScannabilityEvaluator.evaluate(testBmp, result.style)
        assertTrue("Optimized neon QR must be scannable (isScannable=${eval.isScannable}, score=${eval.score})", eval.isScannable)
    }
}
