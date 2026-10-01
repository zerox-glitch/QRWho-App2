package com.example.qr.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object QrGenerator {

    private val thumbnailCache = object : android.util.LruCache<Int, Bitmap>(400) {}
    private val cachedSampleMatrices = mutableMapOf<Int, com.google.zxing.qrcode.encoder.ByteMatrix>()

    private fun getSampleMatrix(version: Int = 3): com.google.zxing.qrcode.encoder.ByteMatrix {
        val v = version.coerceIn(1, 40)
        val existing = cachedSampleMatrices[v]
        if (existing != null) return existing
        val hints = mutableMapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0
        )
        if (v > 1) {
            hints[EncodeHintType.QR_VERSION] = v
        }
        val qr = try {
            Encoder.encode("https://qrwho.vercel.app", ErrorCorrectionLevel.M, hints)
        } catch (_: Exception) {
            Encoder.encode("https://qrwho.vercel.app", ErrorCorrectionLevel.M, mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 0
            ))
        }
        val m = qr.matrix ?: com.google.zxing.qrcode.encoder.ByteMatrix(25, 25)
        cachedSampleMatrices[v] = m
        return m
    }

    /**
     * Quickly renders an authentic miniature QR code for presets, showcase cards, and galleries.
     */
    fun generateThumbnail(qrStyle: QrStyle, sizePx: Int = 180, context: Context? = null): Bitmap = getOrGenerateThumbnail(qrStyle, sizePx, context)

    fun getOrGenerateThumbnail(qrStyle: QrStyle, sizePx: Int = 120, context: Context? = null): Bitmap {
        val key = qrStyle.hashCode() * 31 + sizePx
        val cached = thumbnailCache.get(key)
        if (cached != null) return cached

        val matrix = getSampleMatrix(qrStyle.minVersion)
        val matrixSize = matrix.width
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val hasFrame = qrStyle.frameStyle != FrameStyle.None
        val insets = calculateFrameInsets(qrStyle.frameStyle, qrStyle.artDirection, sizePx)

        val qz = qrStyle.quietZone.coerceIn(0, 8)
        val totalModules = matrixSize + qz * 2
        val usableWidth = sizePx - insets.left - insets.right
        val usableHeight = sizePx - insets.top - insets.bottom
        val cellSize = min(usableWidth, usableHeight) / totalModules.toFloat()
        val originX = insets.left + (usableWidth - matrixSize * cellSize) / 2f
        val originY = insets.top + (usableHeight - matrixSize * cellSize) / 2f

        // Draw background
        val effectiveBgColor = resolveEffectiveBgColor(qrStyle)
        val artId = qrStyle.artDirection
        if (artId != null && ArtFrameRenderer.isArtFrame(artId)) {
            val artBmp = SamplePhotos.getThumbnail(context, artId, sizePx)
            renderPhotoUnderlay(canvas, artBmp, qrStyle, originX, originY, matrixSize * cellSize, sizePx, cellSize, matrixSize)
        } else if (!qrStyle.transparentBg) {
            val bgPaint = Paint().apply { color = effectiveBgColor }
            canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)
        } else {
            bitmap.eraseColor(0x00000000)
        }

        if (hasFrame) {
            drawFrameBackground(canvas, qrStyle.frameStyle, sizePx, qrStyle)
        }

        // Module paint
        val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = qrStyle.fgColor
            style = Paint.Style.FILL
        }
        if (qrStyle.gradientType != GradientType.None) {
            modulePaint.shader = LinearGradient(
                originX, originY,
                originX + matrixSize * cellSize, originY + matrixSize * cellSize,
                qrStyle.fgColor, qrStyle.gradientTo, Shader.TileMode.CLAMP
            )
        }

        val dotScale = qrStyle.dotScale.coerceIn(0.60f, 1.0f)
        val gap = cellSize * qrStyle.moduleGap.coerceIn(0f, 0.15f)

        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (isFinderCell(x, y, matrixSize)) continue
                val isDark = matrix.get(x, y).toInt() == 1
                if (!isDark) continue

                val cellLeft = originX + x * cellSize
                val cellTop = originY + y * cellSize
                val cx = cellLeft + cellSize / 2f
                val cy = cellTop + cellSize / 2f
                val activeSize = (cellSize - gap * 2) * dotScale
                val nDark = y > 0 && matrix.get(x, y - 1).toInt() == 1
                val sDark = y < matrixSize - 1 && matrix.get(x, y + 1).toInt() == 1
                val wDark = x > 0 && matrix.get(x - 1, y).toInt() == 1
                val eDark = x < matrixSize - 1 && matrix.get(x + 1, y).toInt() == 1

                drawModuleShape(
                    canvas = canvas,
                    shape = qrStyle.moduleShape,
                    cx = cx,
                    cy = cy,
                    size = activeSize,
                    paint = modulePaint,
                    gx = x,
                    gy = y,
                    nDark = nDark,
                    sDark = sDark,
                    wDark = wDark,
                    eDark = eDark,
                    matrixSize = matrixSize
                )
            }
        }

        // Draw the 3 Finder Eyes
        drawEye(canvas, originX, originY, cellSize, qrStyle, 0, originX, originY, matrixSize)
        drawEye(canvas, originX + (matrixSize - 7) * cellSize, originY, cellSize, qrStyle, 0, originX, originY, matrixSize)
        drawEye(canvas, originX, originY + (matrixSize - 7) * cellSize, cellSize, qrStyle, 0, originX, originY, matrixSize)

        // Draw border artistic decorations if configured
        drawArtisticBorderDecorations(canvas, qrStyle.artDirection, sizePx, originX, originY, matrixSize * cellSize, qrStyle)

        if (hasFrame) {
            drawFrameForeground(canvas, qrStyle.frameStyle, qrStyle.frameCaption, sizePx, originX, originY, matrixSize * cellSize, qrStyle)
        }

        thumbnailCache.put(key, bitmap)
        return bitmap
    }

    fun generateQrBitmap(
        payload: String,
        qrStyle: QrStyle,
        photoBitmap: Bitmap? = null,
        customLogo: Bitmap? = null,
        sizePx: Int = 1024,
        context: Context? = null
    ): Bitmap {
        val ecc = when (qrStyle.ecc.uppercase()) {
            "L" -> ErrorCorrectionLevel.L
            "M" -> ErrorCorrectionLevel.M
            "Q" -> ErrorCorrectionLevel.Q
            else -> ErrorCorrectionLevel.H // Level H default for maximum scannability and art embedding
        }

        val hints = mutableMapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ecc,
            EncodeHintType.MARGIN to 0
        )
        if (qrStyle.minVersion in 1..40) {
            hints[EncodeHintType.QR_VERSION] = qrStyle.minVersion
        }

        val qrCode: QRCode = try {
            Encoder.encode(payload, ecc, hints)
        } catch (_: Exception) {
            try {
                val fallbackHints = mapOf<EncodeHintType, Any>(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ecc,
                    EncodeHintType.MARGIN to 0
                )
                Encoder.encode(payload, ecc, fallbackHints)
            } catch (_: Exception) {
                Encoder.encode("https://qrwho.vercel.app", ecc, mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ecc,
                    EncodeHintType.MARGIN to 0
                ))
            }
        }

        val matrix = qrCode.matrix ?: return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val matrixSize = matrix.width

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Calculate Frame geometry
        val hasFrame = qrStyle.frameStyle != FrameStyle.None
        val insets = calculateFrameInsets(qrStyle.frameStyle, qrStyle.artDirection, sizePx)

        val qz = qrStyle.quietZone.coerceIn(0, 8)

        val totalCells = matrixSize + qz * 2
        val usableWidth = sizePx - insets.left - insets.right
        val usableHeight = sizePx - insets.top - insets.bottom
        val cellSize = min(usableWidth, usableHeight) / totalCells.toFloat()

        val originX = insets.left + (usableWidth - matrixSize * cellSize) / 2f
        val originY = insets.top + (usableHeight - matrixSize * cellSize) / 2f

        val effectiveBgColor = resolveEffectiveBgColor(qrStyle)

        // 1. Draw Background
        if (!qrStyle.transparentBg) {
            bitmap.eraseColor(effectiveBgColor)
            val bgPaint = Paint().apply {
                color = effectiveBgColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)
        } else {
            bitmap.eraseColor(0x00000000)
        }

        if (hasFrame) {
            drawFrameBackground(canvas, qrStyle.frameStyle, sizePx, qrStyle)
        }

        // 2. Draw Photo Underlay
        val effectivePhoto = photoBitmap ?: if (qrStyle.artDirection != null && ArtFrameRenderer.isArtFrame(qrStyle.artDirection)) {
            SamplePhotos.getThumbnail(context, qrStyle.artDirection, sizePx)
        } else null

        if (effectivePhoto != null) {
            renderPhotoUnderlay(canvas, effectivePhoto, qrStyle, originX, originY, matrixSize * cellSize, sizePx, cellSize, matrixSize)
        }

        val hasPhoto = (photoBitmap != null || effectivePhoto != null) && qrStyle.imageMode != ImageMode.None && qrStyle.imageMode != ImageMode.Logo

        // 3. Setup Module Shader / Paint
        val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = qrStyle.fgColor
        }

        val bodyW = matrixSize * cellSize
        when (qrStyle.gradientType) {
            GradientType.Linear -> {
                modulePaint.shader = LinearGradient(
                    originX, originY, originX + bodyW, originY,
                    qrStyle.fgColor, qrStyle.gradientTo, Shader.TileMode.CLAMP
                )
            }
            GradientType.Vertical -> {
                modulePaint.shader = LinearGradient(
                    originX, originY, originX, originY + bodyW,
                    qrStyle.fgColor, qrStyle.gradientTo, Shader.TileMode.CLAMP
                )
            }
            GradientType.Diagonal -> {
                modulePaint.shader = LinearGradient(
                    originX, originY, originX + bodyW, originY + bodyW,
                    qrStyle.fgColor, qrStyle.gradientTo, Shader.TileMode.CLAMP
                )
            }
            GradientType.Radial -> {
                modulePaint.shader = RadialGradient(
                    originX + bodyW / 2f, originY + bodyW / 2f, bodyW * 0.7f,
                    qrStyle.fgColor, qrStyle.gradientTo, Shader.TileMode.CLAMP
                )
            }
            GradientType.None -> {
                modulePaint.shader = null
            }
        }

        // 4. Reserve Center Safe Zone for Logo if present
        val centerZone = if (customLogo != null) {
            val logoRadiusModules = (matrixSize * (qrStyle.logoScale.coerceIn(0.18f, 0.28f)) / 2f).toInt()
            val mid = matrixSize / 2
            Rect(mid - logoRadiusModules, mid - logoRadiusModules, mid + logoRadiusModules, mid + logoRadiusModules)
        } else {
            null
        }

        // 5. Draw QR Modules (Skip Finders)
        val hasEffect = qrStyle.effect != QrEffect.None
        val rawGap = cellSize * qrStyle.moduleGap.coerceIn(0f, 0.15f)
        val gap = rawGap.coerceIn(0f, cellSize * 0.15f)
        val rawScale = qrStyle.dotScale.coerceIn(0.55f, 1.0f)
        val dotScale = rawScale
        val effCol = resolveVibrantEffectColor(qrStyle)
        val isDarkBg = isDarkColor(effectiveBgColor)

        // PASS 1 (If Effect Active): Continuous Depth Underlay (Cast Shadows, Cavity Carving, Ambient Glow Halos)
        if (hasEffect) {
            // 1.1 Finder Eyes Underlay Shadows
            drawEyeUnderlay(canvas, originX, originY, cellSize, qrStyle, effCol)
            drawEyeUnderlay(canvas, originX + (matrixSize - 7) * cellSize, originY, cellSize, qrStyle, effCol)
            drawEyeUnderlay(canvas, originX, originY + (matrixSize - 7) * cellSize, cellSize, qrStyle, effCol)

            // 1.2 Module Grid Underlay
            for (y in 0 until matrixSize) {
                for (x in 0 until matrixSize) {
                    if (isFinderCell(x, y, matrixSize)) continue
                    if (centerZone != null && centerZone.contains(x, y)) continue
                    if (matrix.get(x, y).toInt() != 1) continue

                    val cellLeft = originX + x * cellSize
                    val cellTop = originY + y * cellSize
                    val cx = cellLeft + cellSize / 2f
                    val cy = cellTop + cellSize / 2f
                    val activeSize = (cellSize - gap * 2) * dotScale

                    val nDark = y > 0 && matrix.get(x, y - 1).toInt() == 1
                    val sDark = y < matrixSize - 1 && matrix.get(x, y + 1).toInt() == 1
                    val wDark = x > 0 && matrix.get(x - 1, y).toInt() == 1
                    val eDark = x < matrixSize - 1 && matrix.get(x + 1, y).toInt() == 1

                    drawModuleUnderlay(
                        canvas = canvas,
                        shape = qrStyle.moduleShape,
                        cx = cx,
                        cy = cy,
                        size = activeSize,
                        gx = x,
                        gy = y,
                        nDark = nDark,
                        sDark = sDark,
                        wDark = wDark,
                        eDark = eDark,
                        matrixSize = matrixSize,
                        effect = qrStyle.effect,
                        effectColor = effCol,
                        intensity = qrStyle.effectIntensity,
                        isDarkBg = isDarkBg
                    )
                }
            }
        }

        // PASS 2: Module Surfaces, 3D Pedestals, Tactile Bevels, Illuminated Rims & High-Contrast Cores
        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (isFinderCell(x, y, matrixSize)) continue
                if (centerZone != null && centerZone.contains(x, y)) continue

                val isDark = matrix.get(x, y).toInt() == 1
                if (!isDark) continue

                val cellLeft = originX + x * cellSize
                val cellTop = originY + y * cellSize
                val cx = cellLeft + cellSize / 2f
                val cy = cellTop + cellSize / 2f
                var activeSize = (cellSize - gap * 2) * dotScale

                // Photo-driven module styling
                if (hasPhoto && photoBitmap != null) {
                    val normX = (x.toFloat() / matrixSize).coerceIn(0f, 1f)
                    val normY = (y.toFloat() / matrixSize).coerceIn(0f, 1f)
                    val pxX = (normX * (photoBitmap.width - 1)).toInt().coerceIn(0, photoBitmap.width - 1)
                    val pxY = (normY * (photoBitmap.height - 1)).toInt().coerceIn(0, photoBitmap.height - 1)
                    val sample = photoBitmap.getPixel(pxX, pxY)
                    val luma = (0.299f * android.graphics.Color.red(sample) + 0.587f * android.graphics.Color.green(sample) + 0.114f * android.graphics.Color.blue(sample)) / 255f

                    when (qrStyle.imageMode) {
                        ImageMode.Mosaic -> {
                            val contrastBoost = qrStyle.contrast.coerceIn(0.5f, 2.0f)
                            val r = (android.graphics.Color.red(sample) * (1f - (1f - luma) * 0.4f * contrastBoost)).toInt().coerceIn(0, 255)
                            val g = (android.graphics.Color.green(sample) * (1f - (1f - luma) * 0.4f * contrastBoost)).toInt().coerceIn(0, 255)
                            val b = (android.graphics.Color.blue(sample) * (1f - (1f - luma) * 0.4f * contrastBoost)).toInt().coerceIn(0, 255)
                            modulePaint.shader = null
                            modulePaint.color = android.graphics.Color.rgb(r, g, b)
                        }
                        ImageMode.Halftone -> {
                            activeSize *= (1.15f - luma * 0.40f).coerceIn(0.68f, 1.0f)
                        }
                        ImageMode.Duotone -> {
                            val t = luma.coerceIn(0f, 1f)
                            val c1 = qrStyle.fgColor
                            val c2 = qrStyle.gradientTo
                            val dr = (android.graphics.Color.red(c1) * (1 - t) + android.graphics.Color.red(c2) * t).toInt()
                            val dg = (android.graphics.Color.green(c1) * (1 - t) + android.graphics.Color.green(c2) * t).toInt()
                            val db = (android.graphics.Color.blue(c1) * (1 - t) + android.graphics.Color.blue(c2) * t).toInt()
                            modulePaint.shader = null
                            modulePaint.color = android.graphics.Color.rgb(dr, dg, db)
                        }
                        ImageMode.Mono -> {
                            val v = (luma * 160).toInt().coerceIn(10, 180)
                            modulePaint.shader = null
                            modulePaint.color = android.graphics.Color.rgb(v, v, v)
                        }
                        ImageMode.Paint -> {
                            val coreSize = activeSize * (1f - qrStyle.artisticStrength * 0.45f).coerceIn(0.65f, 0.95f)
                            drawModuleSurface(
                                canvas = canvas,
                                shape = qrStyle.moduleShape,
                                cx = cx,
                                cy = cy,
                                size = coreSize,
                                paint = modulePaint,
                                gx = x,
                                gy = y,
                                effect = qrStyle.effect,
                                effectColor = effCol,
                                intensity = qrStyle.effectIntensity
                            )
                            continue
                        }
                        else -> {}
                    }
                }

                val nDark = y > 0 && matrix.get(x, y - 1).toInt() == 1
                val sDark = y < matrixSize - 1 && matrix.get(x, y + 1).toInt() == 1
                val wDark = x > 0 && matrix.get(x - 1, y).toInt() == 1
                val eDark = x < matrixSize - 1 && matrix.get(x + 1, y).toInt() == 1

                drawModuleSurface(
                    canvas = canvas,
                    shape = qrStyle.moduleShape,
                    cx = cx,
                    cy = cy,
                    size = activeSize,
                    paint = modulePaint,
                    gx = x,
                    gy = y,
                    nDark = nDark,
                    sDark = sDark,
                    wDark = wDark,
                    eDark = eDark,
                    matrixSize = matrixSize,
                    effect = qrStyle.effect,
                    effectColor = effCol,
                    intensity = qrStyle.effectIntensity
                )
            }
        }

        // 6. Draw The 3 Finder Eyes (Top-Left, Top-Right, Bottom-Left)
        drawEye(canvas, originX, originY, cellSize, qrStyle, effCol, originX, originY, matrixSize)
        drawEye(canvas, originX + (matrixSize - 7) * cellSize, originY, cellSize, qrStyle, effCol, originX, originY, matrixSize)
        drawEye(canvas, originX, originY + (matrixSize - 7) * cellSize, cellSize, qrStyle, effCol, originX, originY, matrixSize)

        // 7. Render Border Artistic Decorations if configured
        drawArtisticBorderDecorations(canvas, qrStyle.artDirection, sizePx, originX, originY, bodyW, qrStyle)

        // 8. Render Center Logo (Custom PNG Logo)
        if (customLogo != null) {
            renderCenterLogo(canvas, customLogo, sizePx, originX, originY, bodyW, qrStyle)
        }

        // 9. Render Decorative Frame if requested
        if (hasFrame) {
            drawFrameForeground(canvas, qrStyle.frameStyle, qrStyle.frameCaption, sizePx, originX, originY, bodyW, qrStyle)
        }

        return bitmap
    }

    /**
     * Generates a 100% genuine vector SVG representation of the QR code with
     * custom module shapes, gradients, finder patterns, and frames.
     */
    fun generateQrSvg(
        payload: String,
        qrStyle: QrStyle,
        sizePx: Int = 1024
    ): String {
        val ecc = when (qrStyle.ecc.uppercase()) {
            "L" -> ErrorCorrectionLevel.L
            "M" -> ErrorCorrectionLevel.M
            "Q" -> ErrorCorrectionLevel.Q
            else -> ErrorCorrectionLevel.H
        }

        val hints = mutableMapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ecc,
            EncodeHintType.MARGIN to 0
        )
        if (qrStyle.minVersion in 1..40) {
            hints[EncodeHintType.QR_VERSION] = qrStyle.minVersion
        }

        val qrCode: QRCode = try {
            Encoder.encode(payload, ecc, hints)
        } catch (_: Exception) {
            try {
                val fallbackHints = mapOf<EncodeHintType, Any>(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ecc,
                    EncodeHintType.MARGIN to 0
                )
                Encoder.encode(payload, ecc, fallbackHints)
            } catch (_: Exception) {
                Encoder.encode("https://qrwho.vercel.app", ecc, mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ecc,
                    EncodeHintType.MARGIN to 0
                ))
            }
        }

        val matrix = qrCode.matrix ?: return "<svg viewBox=\"0 0 $sizePx $sizePx\"></svg>"
        val matrixSize = matrix.width

        val hasFrame = qrStyle.frameStyle != FrameStyle.None
        val insets = calculateFrameInsets(qrStyle.frameStyle, sizePx)

        val qz = qrStyle.quietZone.coerceIn(0, 8)
        val totalCells = matrixSize + qz * 2
        val usableWidth = sizePx - insets.left - insets.right
        val usableHeight = sizePx - insets.top - insets.bottom
        val cellSize = min(usableWidth, usableHeight) / totalCells.toFloat()

        val originX = insets.left + (usableWidth - matrixSize * cellSize) / 2f
        val originY = insets.top + (usableHeight - matrixSize * cellSize) / 2f
        val gap = cellSize * qrStyle.moduleGap.coerceIn(0f, 0.15f)
        val dotScale = qrStyle.dotScale.coerceIn(0.60f, 1.0f)

        val effBg = resolveEffectiveBgColor(qrStyle)
        val fgHex = String.format("#%06X", 0xFFFFFF and qrStyle.fgColor)
        val bgHex = String.format("#%06X", 0xFFFFFF and effBg)
        val gradHex = String.format("#%06X", 0xFFFFFF and qrStyle.gradientTo)
        val (effEyeColor, effBallColor) = resolveEffectiveEyeColors(qrStyle, effBg)
        val eyeHex = String.format("#%06X", 0xFFFFFF and effEyeColor)
        val ballHex = String.format("#%06X", 0xFFFFFF and effBallColor)

        val fillAttr = if (qrStyle.gradientType != GradientType.None) "url(#qrGrad)" else fgHex

        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $sizePx $sizePx\" width=\"100%\" height=\"100%\">\n")

            // Gradients and Definitions
            append("  <defs>\n")
            if (qrStyle.gradientType == GradientType.Diagonal) {
                append("    <linearGradient id=\"qrGrad\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">\n")
                append("      <stop offset=\"0%\" stop-color=\"$fgHex\" />\n")
                append("      <stop offset=\"100%\" stop-color=\"$gradHex\" />\n")
                append("    </linearGradient>\n")
            } else if (qrStyle.gradientType == GradientType.Linear) {
                append("    <linearGradient id=\"qrGrad\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"0%\">\n")
                append("      <stop offset=\"0%\" stop-color=\"$fgHex\" />\n")
                append("      <stop offset=\"100%\" stop-color=\"$gradHex\" />\n")
                append("    </linearGradient>\n")
            } else if (qrStyle.gradientType == GradientType.Vertical) {
                append("    <linearGradient id=\"qrGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">\n")
                append("      <stop offset=\"0%\" stop-color=\"$fgHex\" />\n")
                append("      <stop offset=\"100%\" stop-color=\"$gradHex\" />\n")
                append("    </linearGradient>\n")
            } else if (qrStyle.gradientType == GradientType.Radial) {
                append("    <radialGradient id=\"qrGrad\" cx=\"50%\" cy=\"50%\" r=\"60%\">\n")
                append("      <stop offset=\"0%\" stop-color=\"$fgHex\" />\n")
                append("      <stop offset=\"100%\" stop-color=\"$gradHex\" />\n")
                append("    </radialGradient>\n")
            }

            // SVG Effects Filters
            when (qrStyle.effect) {
                QrEffect.Raised3D, QrEffect.Shadow -> {
                    append("    <filter id=\"qrEffectFilter\" x=\"-20%\" y=\"-20%\" width=\"140%\" height=\"140%\">\n")
                    append("      <feDropShadow dx=\"2.5\" dy=\"2.5\" stdDeviation=\"1.8\" flood-color=\"#000000\" flood-opacity=\"0.45\" />\n")
                    append("    </filter>\n")
                }
                QrEffect.Engraved -> {
                    append("    <filter id=\"qrEffectFilter\" x=\"-20%\" y=\"-20%\" width=\"140%\" height=\"140%\">\n")
                    append("      <feDropShadow dx=\"-1.5\" dy=\"-1.5\" stdDeviation=\"1\" flood-color=\"#000000\" flood-opacity=\"0.55\" />\n")
                    append("    </filter>\n")
                }
                QrEffect.Glow -> {
                    append("    <filter id=\"qrEffectFilter\" x=\"-30%\" y=\"-30%\" width=\"160%\" height=\"160%\">\n")
                    append("      <feGaussianBlur stdDeviation=\"3.5\" result=\"coloredBlur\"/>\n")
                    append("      <feMerge>\n")
                    append("        <feMergeNode in=\"coloredBlur\"/>\n")
                    append("        <feMergeNode in=\"SourceGraphic\"/>\n")
                    append("      </feMerge>\n")
                    append("    </filter>\n")
                }
                else -> {}
            }
            append("  </defs>\n")

            // Background
            if (!qrStyle.transparentBg) {
                append("  <rect width=\"$sizePx\" height=\"$sizePx\" fill=\"$bgHex\" />\n")
            }

            // QR Modules
            val filterAttr = if (qrStyle.effect != QrEffect.None && qrStyle.effect != QrEffect.Outline) " filter=\"url(#qrEffectFilter)\"" else ""
            val strokeAttr = if (qrStyle.effect == QrEffect.Outline) " stroke=\"#FFFFFF\" stroke-width=\"1.5\"" else ""
            append("  <g fill=\"$fillAttr\"$filterAttr$strokeAttr>\n")
            for (y in 0 until matrixSize) {
                for (x in 0 until matrixSize) {
                    if (isFinderCell(x, y, matrixSize)) continue
                    if (matrix.get(x, y).toInt() != 1) continue

                    val cx = originX + x * cellSize + cellSize / 2f
                    val cy = originY + y * cellSize + cellSize / 2f
                    val activeSize = (cellSize - gap * 2) * dotScale
                    val r = activeSize / 2f
                    val left = cx - r
                    val top = cy - r

                    when (qrStyle.moduleShape) {
                        ModuleShape.Square -> {
                            append("    <rect x=\"$left\" y=\"$top\" width=\"$activeSize\" height=\"$activeSize\" />\n")
                        }
                        ModuleShape.Rounded -> {
                            val rx = activeSize * 0.25f
                            append("    <rect x=\"$left\" y=\"$top\" width=\"$activeSize\" height=\"$activeSize\" rx=\"$rx\" ry=\"$rx\" />\n")
                        }
                        ModuleShape.Squircle, ModuleShape.Fluid -> {
                            val rx = activeSize * 0.38f
                            append("    <rect x=\"$left\" y=\"$top\" width=\"$activeSize\" height=\"$activeSize\" rx=\"$rx\" ry=\"$rx\" />\n")
                        }
                        ModuleShape.Dots, ModuleShape.Bubbles -> {
                            append("    <circle cx=\"$cx\" cy=\"$cy\" r=\"$r\" />\n")
                        }
                        ModuleShape.Diamond -> {
                            append("    <polygon points=\"$cx,$top ${cx + r},$cy $cx,${cy + r} ${cx - r},$cy\" />\n")
                        }
                        ModuleShape.Hex -> {
                            val pts = buildString {
                                for (i in 0 until 6) {
                                    val angle = (Math.PI / 3 * i - Math.PI / 6)
                                    val px = cx + cos(angle).toFloat() * r
                                    val py = cy + sin(angle).toFloat() * r
                                    append("$px,$py ")
                                }
                            }.trim()
                            append("    <polygon points=\"$pts\" />\n")
                        }
                        else -> {
                            val rx = activeSize * 0.20f
                            append("    <rect x=\"$left\" y=\"$top\" width=\"$activeSize\" height=\"$activeSize\" rx=\"$rx\" ry=\"$rx\" />\n")
                        }
                    }
                }
            }
            append("  </g>\n")

            // Vector Finder Eyes
            append("  <!-- Finder Eyes -->\n")
            append(renderEyeSvg(originX, originY, cellSize, qrStyle.eyeShape, qrStyle.ballShape, eyeHex, ballHex, bgHex, originX, originY, matrixSize))
            append(renderEyeSvg(originX + (matrixSize - 7) * cellSize, originY, cellSize, qrStyle.eyeShape, qrStyle.ballShape, eyeHex, ballHex, bgHex, originX, originY, matrixSize))
            append(renderEyeSvg(originX, originY + (matrixSize - 7) * cellSize, cellSize, qrStyle.eyeShape, qrStyle.ballShape, eyeHex, ballHex, bgHex, originX, originY, matrixSize))

            // Frame caption if present
            if (hasFrame && qrStyle.frameCaption.isNotEmpty()) {
                val s = sizePx.toFloat()
                append("  <text x=\"${s / 2f}\" y=\"${s * 0.93f}\" font-family=\"sans-serif\" font-size=\"${s * 0.045f}\" font-weight=\"bold\" text-anchor=\"middle\" fill=\"$fgHex\">${qrStyle.frameCaption}</text>\n")
            }

            append("</svg>\n")
        }
    }

    private fun renderEyeSvg(
        ox: Float, oy: Float, cell: Float,
        eyeShape: EyeShape, ballShape: EyeShape,
        eyeHex: String, ballHex: String, bgHex: String,
        originX: Float = ox, originY: Float = oy, matrixSize: Int = 33
    ): String = buildString {
        val s = cell * 7
        val r = s / 2f
        val cx = ox + r
        val cy = oy + r

        val isTopLeft = Math.abs(ox - originX) < cell * 0.5f && Math.abs(oy - originY) < cell * 0.5f
        val isTopRight = Math.abs(ox - (originX + (matrixSize - 7) * cell)) < cell * 0.5f && Math.abs(oy - originY) < cell * 0.5f
        val isBottomLeft = Math.abs(ox - originX) < cell * 0.5f && Math.abs(oy - (originY + (matrixSize - 7) * cell)) < cell * 0.5f

        val (px, py) = when {
            isTopLeft -> Pair(ox, oy)
            isTopRight -> Pair(ox - cell, oy)
            isBottomLeft -> Pair(ox, oy - cell)
            else -> Pair(ox - cell * 0.5f, oy - cell * 0.5f)
        }
        val pW = cell * 8
        val pH = cell * 8
        // Clean 8x8 separator plate behind finder pattern to guarantee no dots overlap
        append("  <rect x=\"$px\" y=\"$py\" width=\"$pW\" height=\"$pH\" fill=\"$bgHex\" rx=\"${cell * 0.35f}\" />\n")

        // Outer Ring
        append("  <rect x=\"$ox\" y=\"$oy\" width=\"$s\" height=\"$s\" fill=\"$eyeHex\" rx=\"${if (eyeShape == EyeShape.Circle) r else if (eyeShape == EyeShape.Rounded) s * 0.22f else 0f}\" />\n")
        // Inner Gap
        append("  <rect x=\"${ox + cell}\" y=\"${oy + cell}\" width=\"${cell * 5}\" height=\"${cell * 5}\" fill=\"$bgHex\" rx=\"${if (eyeShape == EyeShape.Circle) cell * 2.5f else if (eyeShape == EyeShape.Rounded) cell else 0f}\" />\n")
        // Pupil Ball
        val bSize = cell * 3
        val bRadius = bSize / 2f
        append("  <rect x=\"${ox + cell * 2}\" y=\"${oy + cell * 2}\" width=\"$bSize\" height=\"$bSize\" fill=\"$ballHex\" rx=\"${if (ballShape == EyeShape.Circle) bRadius else if (ballShape == EyeShape.Rounded) bSize * 0.25f else 0f}\" />\n")
    }

    private fun isFinderCell(x: Int, y: Int, matrixSize: Int): Boolean {
        // Top-left
        if (x < 8 && y < 8) return true
        // Top-right
        if (x >= matrixSize - 8 && y < 8) return true
        // Bottom-left
        if (x < 8 && y >= matrixSize - 8) return true
        return false
    }

    private fun renderPhotoUnderlay(
        canvas: Canvas,
        photo: Bitmap,
        style: QrStyle,
        ox: Float,
        oy: Float,
        bodyPx: Float,
        sizePx: Int,
        cellSize: Float,
        matrixSize: Int
    ) {
        val zoom = style.photoZoom.coerceIn(0.5f, 2.5f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (style.imageOpacity.coerceIn(0.15f, 1.0f) * 255).toInt()
        }

        canvas.save()
        canvas.clipRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat())

        // Calculate source rect with zoom centered
        val sw = photo.width.toFloat()
        val sh = photo.height.toFloat()
        val cropW = sw / zoom
        val cropH = sh / zoom
        val left = ((sw - cropW) / 2f).coerceAtLeast(0f)
        val top = ((sh - cropH) / 2f).coerceAtLeast(0f)
        val right = (left + cropW).coerceAtMost(sw)
        val bottom = (top + cropH).coerceAtMost(sh)
        val srcRect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

        // Always draw the photo covering the full canvas to avoid awkward white borders around the QR
        val destRect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawBitmap(photo, srcRect, destRect, paint)

        // Gentle scrim to keep modules readable
        val scrimColor = if (isDarkColor(style.bgColor)) 0x28000000 else 0x1EFFFFFF
        val scrimPaint = Paint().apply { color = scrimColor; this.style = Paint.Style.FILL }
        canvas.drawRect(destRect, scrimPaint)

        // Finder plates: Opaque rounded paper islands behind the 3 finder eyes in all photo modes
        // to isolate finder patterns from photo noise, guaranteeing verified camera decode and eliminating white border glitches.
        val effectivePlateBg = resolveEffectiveBgColor(style)
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = effectivePlateBg
            this.style = Paint.Style.FILL
        }
        val plateR = cellSize * 0.35f
        val corners = listOf(
            Pair(0, 0),
            Pair(matrixSize - 7, 0),
            Pair(0, matrixSize - 7)
        )
        for ((ex, ey) in corners) {
            val cx = ox + ex * cellSize
            val cy = oy + ey * cellSize
            val sepX = if (ex == 0) cx else cx - cellSize
            val sepY = if (ey == 0) cy else cy - cellSize
            canvas.drawRoundRect(
                RectF(sepX, sepY, sepX + cellSize * 8, sepY + cellSize * 8),
                plateR, plateR, platePaint
            )
        }

        canvas.restore()
    }

    fun isDarkColor(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val luma = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return luma < 0.5
    }

    private fun renderCenterLogo(
        canvas: Canvas,
        logo: Bitmap,
        sizePx: Int,
        ox: Float,
        oy: Float,
        bodyPx: Float,
        style: QrStyle
    ) {
        val cx = ox + bodyPx / 2f
        val cy = oy + bodyPx / 2f
        val logoW = bodyPx * style.logoScale.coerceIn(0.15f, 0.28f)

        // Draw PNG logo directly as transparent photo with no borders, no plates, and no backgrounds behind it
        val destRect = RectF(cx - logoW / 2f, cy - logoW / 2f, cx + logoW / 2f, cy + logoW / 2f)
        canvas.drawBitmap(logo, Rect(0, 0, logo.width, logo.height), destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    fun getSlightlyLighterColor(color: Int): Int {
        val alpha = (color shr 24) and 0xFF
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, g, b, hsv)

        if (hsv[2] < 0.35f) {
            hsv[2] = 0.85f // Luminous tint matching the selected hue
        } else {
            hsv[1] = (hsv[1] * 0.70f).coerceIn(0f, 1f) // Slightly reduce saturation
            hsv[2] = (hsv[2] * 1.30f).coerceIn(0f, 1f) // Boost brightness
        }

        val a = if (alpha == 0) 0xFF else alpha
        return android.graphics.Color.HSVToColor(a, hsv)
    }

    fun resolveVibrantEffectColor(qrStyle: QrStyle): Int {
        val baseCol = if (qrStyle.gradientType != GradientType.None) qrStyle.gradientTo else qrStyle.fgColor
        return getSlightlyLighterColor(baseCol)
    }

    private fun drawModuleUnderlay(
        canvas: Canvas,
        shape: ModuleShape,
        cx: Float,
        cy: Float,
        size: Float,
        gx: Int,
        gy: Int,
        nDark: Boolean = false,
        sDark: Boolean = false,
        wDark: Boolean = false,
        eDark: Boolean = false,
        matrixSize: Int = 33,
        effect: QrEffect = QrEffect.None,
        effectColor: Int = 0,
        intensity: Float = 1.0f,
        isDarkBg: Boolean = true
    ) {
        val safeIntensity = intensity.coerceIn(0.4f, 2.5f)
        when (effect) {
            QrEffect.None -> {}
            QrEffect.Raised3D -> {
                val sDist = (size * 0.22f * safeIntensity).coerceIn(3.0f, 18f)

                // 1. Soft wide ambient cast shadow
                val ambPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x38000000
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx + sDist * 1.4f, cy + sDist * 1.4f, size * 1.12f, ambPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 2. Crisp 3D contact drop shadow
                val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x75000000
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx + sDist, cy + sDist, size * 1.0f, contactPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Engraved -> {
                val inDist = (size * 0.18f * safeIntensity).coerceIn(2.5f, 15f)

                // 1. Deep carved cavity shadow top-left
                val pitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xAA000000.toInt()
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx - inDist * 1.1f, cy - inDist * 1.1f, size * 1.08f, pitPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 2. Chiseled lip reflection bottom-right
                val chiselPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xAAFFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx + inDist * 1.1f, cy + inDist * 1.1f, size * 1.08f, chiselPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Glow -> {
                if (isDarkBg) {
                    val glowCol = if (effectColor != 0) effectColor else getSlightlyLighterColor(0xFF00F0FF.toInt())
                    val bloom = (size * 0.08f * safeIntensity).coerceIn(1.0f, 3.2f)

                    // 1. Soft atmospheric neon aura (strictly bounded to prevent inter-module bleed)
                    val outerHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = (glowCol and 0x00FFFFFF) or 0x18000000
                        style = Paint.Style.FILL
                    }
                    drawRawModuleShape(canvas, shape, cx, cy, size + bloom * 1.15f, outerHalo, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                    // 2. Concentrated neon corona
                    val midHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = (glowCol and 0x00FFFFFF) or 0x30000000
                        style = Paint.Style.FILL
                    }
                    drawRawModuleShape(canvas, shape, cx, cy, size + bloom * 0.6f, midHalo, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
                }
            }
            QrEffect.Shadow -> {
                val sDist = (size * 0.22f * safeIntensity).coerceIn(3.5f, 20f)
                val ambPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x32000000
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx + sDist * 1.6f, cy + sDist * 1.6f, size * 1.14f, ambPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x70000000
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx + sDist, cy + sDist, size * 1.0f, contactPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Emboss -> {
                val eDist = (size * 0.16f * safeIntensity).coerceIn(2.5f, 15f)
                val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xAAFFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx - eDist * 1.2f, cy - eDist * 1.2f, size * 1.04f, hlPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x88000000.toInt()
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx + eDist * 1.2f, cy + eDist * 1.2f, size * 1.04f, shadowPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Outline -> {
                val strokeW = (size * 0.18f * safeIntensity).coerceIn(3f, 9f)
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xEEFFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                drawRawModuleShape(canvas, shape, cx, cy, size * 1.02f, outlinePaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Glassmorphism -> {
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x45000000
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx + size * 0.12f, cy + size * 0.12f, size * 1.05f, shadowPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
        }
    }

    private fun drawModuleSurface(
        canvas: Canvas,
        shape: ModuleShape,
        cx: Float,
        cy: Float,
        size: Float,
        paint: Paint,
        gx: Int,
        gy: Int,
        nDark: Boolean = false,
        sDark: Boolean = false,
        wDark: Boolean = false,
        eDark: Boolean = false,
        matrixSize: Int = 33,
        effect: QrEffect = QrEffect.None,
        effectColor: Int = 0,
        intensity: Float = 1.0f
    ) {
        val safeIntensity = intensity.coerceIn(0.4f, 2.5f)
        when (effect) {
            QrEffect.None -> {
                drawRawModuleShape(canvas, shape, cx, cy, size, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Raised3D -> {
                val sDist = (size * 0.22f * safeIntensity).coerceIn(3.0f, 18f)

                // 1. Extruded 3D Sidewall Pedestal (physical block thickness between shadow and face)
                val sidewallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x65000000
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx + sDist * 0.45f, cy + sDist * 0.45f, size, sidewallPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 2. Elevated Top Face
                val faceX = cx - sDist * 0.22f
                val faceY = cy - sDist * 0.22f
                drawRawModuleShape(canvas, shape, faceX, faceY, size, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 3. Specular Top-Left Illuminated Bevel Crest (Crisp directional light reflection)
                val bevelStrokeW = (size * 0.14f).coerceIn(1.8f, 4.0f)
                val hlStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xCCFFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = bevelStrokeW
                }
                drawRawModuleShape(canvas, shape, faceX - sDist * 0.35f, faceY - sDist * 0.35f, size * 0.94f, hlStrokePaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 4. Bottom-Right Deep Shaded Bevel Contour (Defines 3D rounded button geometry)
                val shadeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x75000000
                    style = Paint.Style.STROKE
                    strokeWidth = bevelStrokeW
                }
                drawRawModuleShape(canvas, shape, faceX + sDist * 0.25f, faceY + sDist * 0.25f, size * 0.94f, shadeStrokePaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 5. Guaranteed Optical Center Core for instantaneous mobile scanner decode
                drawRawModuleShape(canvas, shape, cx, cy, size * 0.70f, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Engraved -> {
                val inDist = (size * 0.18f * safeIntensity).coerceIn(2.5f, 15f)

                // 1. Recessed Sunken Floor (Pushed down-right into cavity)
                val floorX = cx + inDist * 0.25f
                val floorY = cy + inDist * 0.25f
                drawRawModuleShape(canvas, shape, floorX, floorY, size * 0.86f, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 2. Inner cavity top-left shadow rim
                val strokeW = (size * 0.14f).coerceIn(1.8f, 3.8f)
                val innerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x75000000
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                drawRawModuleShape(canvas, shape, floorX - inDist * 0.25f, floorY - inDist * 0.25f, size * 0.86f, innerRimPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 3. High-contrast sunken core
                drawRawModuleShape(canvas, shape, floorX, floorY, size * 0.65f, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Glow -> {
                val glowCol = if (effectColor != 0) effectColor else getSlightlyLighterColor(paint.color)

                // 1. High-contrast solid module body
                drawRawModuleShape(canvas, shape, cx, cy, size, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 2. Electric neon inner rim (kept strictly inside module boundary)
                val strokeW = (size * 0.10f).coerceIn(1.0f, 2.2f)
                val neonEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = (glowCol and 0x00FFFFFF) or 0xDD000000.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                drawRawModuleShape(canvas, shape, cx, cy, size - strokeW, neonEdgePaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // 3. Hot-white laser core in center of module
                if (size >= 8f) {
                    val corePip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0x88FFFFFF.toInt()
                        style = Paint.Style.FILL
                    }
                    drawRawModuleShape(canvas, shape, cx, cy, size * 0.28f, corePip, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
                }
            }
            QrEffect.Shadow -> {
                val sDist = (size * 0.22f * safeIntensity).coerceIn(3.5f, 20f)

                // Floating module body
                drawRawModuleShape(canvas, shape, cx, cy, size, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // Subtle bottom-right edge separator
                val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x50000000
                    style = Paint.Style.STROKE
                    strokeWidth = (size * 0.10f).coerceIn(1.5f, 3.0f)
                }
                drawRawModuleShape(canvas, shape, cx + sDist * 0.2f, cy + sDist * 0.2f, size * 0.96f, edgePaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Emboss -> {
                val eDist = (size * 0.16f * safeIntensity).coerceIn(2.5f, 15f)

                // Stamped core
                drawRawModuleShape(canvas, shape, cx, cy, size * 0.88f, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // Stamped crest & groove strokes
                val strokeW = (size * 0.12f).coerceIn(1.5f, 3.5f)
                val crestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xAAFFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                drawRawModuleShape(canvas, shape, cx - eDist * 0.5f, cy - eDist * 0.5f, size * 0.88f, crestPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x80000000.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                drawRawModuleShape(canvas, shape, cx + eDist * 0.5f, cy + eDist * 0.5f, size * 0.88f, groovePaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Outline -> {
                // Inner dark boundary
                val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x99000000.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 2.0f
                }
                drawRawModuleShape(canvas, shape, cx, cy, size * 0.96f, innerPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // Solid core
                drawRawModuleShape(canvas, shape, cx, cy, size * 0.84f, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
            QrEffect.Glassmorphism -> {
                // Glass body
                drawRawModuleShape(canvas, shape, cx, cy, size, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // Upper specular gloss sheen
                val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x90FFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                drawRawModuleShape(canvas, shape, cx - size * 0.08f, cy - size * 0.18f, size * 0.65f, glossPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // Glass refractive rim
                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x70FFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 1.8f
                }
                drawRawModuleShape(canvas, shape, cx, cy, size, rimPaint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)

                // Optical core pip
                drawRawModuleShape(canvas, shape, cx, cy, size * 0.40f, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize)
            }
        }
    }

    private fun drawModuleShape(
        canvas: Canvas,
        shape: ModuleShape,
        cx: Float,
        cy: Float,
        size: Float,
        paint: Paint,
        gx: Int,
        gy: Int,
        nDark: Boolean = false,
        sDark: Boolean = false,
        wDark: Boolean = false,
        eDark: Boolean = false,
        matrixSize: Int = 33,
        effect: QrEffect = QrEffect.None,
        effectColor: Int = 0,
        intensity: Float = 1.0f
    ) {
        if (effect != QrEffect.None) {
            drawModuleUnderlay(canvas, shape, cx, cy, size, gx, gy, nDark, sDark, wDark, eDark, matrixSize, effect, effectColor, intensity)
        }
        drawModuleSurface(canvas, shape, cx, cy, size, paint, gx, gy, nDark, sDark, wDark, eDark, matrixSize, effect, effectColor, intensity)
    }

    private fun drawRawModuleShape(
        canvas: Canvas,
        shape: ModuleShape,
        cx: Float,
        cy: Float,
        size: Float,
        paint: Paint,
        gx: Int,
        gy: Int,
        nDark: Boolean = false,
        sDark: Boolean = false,
        wDark: Boolean = false,
        eDark: Boolean = false,
        matrixSize: Int = 33
    ) {
        val r = size / 2f
        val x = cx - r
        val y = cy - r

        when (shape) {
            ModuleShape.Square -> {
                canvas.drawRect(x, y, x + size, y + size, paint)
            }
            ModuleShape.Rounded -> {
                canvas.drawRoundRect(RectF(x, y, x + size, y + size), size * 0.28f, size * 0.28f, paint)
            }
            ModuleShape.Squircle -> {
                canvas.drawRoundRect(RectF(x, y, x + size, y + size), size * 0.42f, size * 0.42f, paint)
            }
            ModuleShape.Dots -> {
                canvas.drawCircle(cx, cy, r * 0.96f, paint)
            }
            ModuleShape.Diamond -> {
                val path = Path().apply {
                    moveTo(cx, y)
                    lineTo(x + size, cy)
                    lineTo(cx, y + size)
                    lineTo(x, cy)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            ModuleShape.Star -> {
                val path = Path()
                val innerR = r * 0.42f
                for (i in 0 until 10) {
                    val angle = (Math.PI / 5 * i - Math.PI / 2).toFloat()
                    val curR = if (i % 2 == 0) r else innerR
                    val px = cx + cos(angle.toDouble()).toFloat() * curR
                    val py = cy + sin(angle.toDouble()).toFloat() * curR
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            ModuleShape.Plus -> {
                val t = size * 0.34f
                val o = (size - t) / 2f
                canvas.drawRect(x + o, y, x + o + t, y + size, paint)
                canvas.drawRect(x, y + o, x + size, y + o + t, paint)
            }
            ModuleShape.Classy -> {
                val cr = size * 0.55f
                val radii = floatArrayOf(0f, 0f, cr, cr, 0f, 0f, cr, cr)
                val path = Path().apply {
                    addRoundRect(RectF(x, y, x + size, y + size), radii, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
            }
            ModuleShape.Leaf -> {
                val cr = size * 0.62f
                val radii = floatArrayOf(cr, cr, 0f, 0f, cr, cr, 0f, 0f)
                val path = Path().apply {
                    addRoundRect(RectF(x, y, x + size, y + size), radii, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
            }
            ModuleShape.Fluid -> {
                val cr = size * 0.52f
                val tl = if (nDark || wDark) 0f else cr
                val tr = if (nDark || eDark) 0f else cr
                val br = if (sDark || eDark) 0f else cr
                val bl = if (sDark || wDark) 0f else cr
                val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
                val path = Path().apply {
                    addRoundRect(RectF(x, y, x + size, y + size), radii, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
            }
            ModuleShape.Hex -> {
                val path = Path()
                for (i in 0 until 6) {
                    val angle = (Math.PI / 3 * i - Math.PI / 6).toFloat()
                    val px = cx + cos(angle.toDouble()).toFloat() * r * 0.98f
                    val py = cy + sin(angle.toDouble()).toFloat() * r * 0.98f
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            ModuleShape.Heart -> {
                val hr = size * 0.24f
                val path = Path().apply {
                    addCircle(cx - size * 0.2f, cy - size * 0.12f, hr, Path.Direction.CW)
                    addCircle(cx + size * 0.2f, cy - size * 0.12f, hr, Path.Direction.CW)
                    moveTo(cx - size * 0.42f, cy - size * 0.05f)
                    lineTo(cx + size * 0.42f, cy - size * 0.05f)
                    lineTo(cx, cy + size * 0.45f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            ModuleShape.Dash -> {
                val hash = (gx * 374761393 + gy * 668265263) and 0x7FFFFFFF
                val horizontal = hash % 2 == 0
                if (horizontal) {
                    val barH = size * 0.44f
                    val barW = size * 0.95f
                    canvas.drawRoundRect(RectF(cx - barW / 2f, cy - barH / 2f, cx + barW / 2f, cy + barH / 2f), barH / 2f, barH / 2f, paint)
                } else {
                    val barW = size * 0.44f
                    val barH = size * 0.95f
                    canvas.drawRoundRect(RectF(cx - barW / 2f, cy - barH / 2f, cx + barW / 2f, cy + barH / 2f), barW / 2f, barW / 2f, paint)
                }
            }
            ModuleShape.HBar -> {
                val barH = size * 0.48f
                val barW = size * 0.96f
                canvas.drawRoundRect(RectF(cx - barW / 2f, cy - barH / 2f, cx + barW / 2f, cy + barH / 2f), barH / 2f, barH / 2f, paint)
            }
            ModuleShape.VBar -> {
                val barW = size * 0.48f
                val barH = size * 0.96f
                canvas.drawRoundRect(RectF(cx - barW / 2f, cy - barH / 2f, cx + barW / 2f, cy + barH / 2f), barW / 2f, barW / 2f, paint)
            }
            ModuleShape.Cross -> {
                canvas.save()
                canvas.rotate(45f, cx, cy)
                val barL = size * 0.95f
                val barT = size * 0.38f
                canvas.drawRoundRect(RectF(cx - barL / 2f, cy - barT / 2f, cx + barL / 2f, cy + barT / 2f), barT / 2f, barT / 2f, paint)
                canvas.drawRoundRect(RectF(cx - barT / 2f, cy - barL / 2f, cx + barT / 2f, cy + barL / 2f), barT / 2f, barT / 2f, paint)
                canvas.restore()
            }
            ModuleShape.Diag -> {
                canvas.save()
                canvas.rotate(45f, cx, cy)
                val len = size * 1.02f
                val thick = size * 0.36f
                canvas.drawRoundRect(RectF(cx - len / 2f, cy - thick / 2f, cx + len / 2f, cy + thick / 2f), thick / 2f, thick / 2f, paint)
                canvas.restore()
            }
            ModuleShape.Radial -> {
                val c = (matrixSize - 1) / 2f
                val ang = Math.atan2((gy - c).toDouble(), (gx - c).toDouble()).toFloat() * (180f / Math.PI.toFloat())
                canvas.save()
                canvas.rotate(ang, cx, cy)
                val hash = (gx * 374761393 + gy * 668265263) and 0x7FFFFFFF
                val wob = 0.8f + ((hash ushr 5) % 20) / 100f
                val path = Path().apply {
                    moveTo(cx + size * 0.55f * wob, cy)
                    lineTo(cx - size * 0.45f * wob, cy - size * 0.28f)
                    lineTo(cx - size * 0.30f * wob, cy)
                    lineTo(cx - size * 0.45f * wob, cy + size * 0.28f)
                    close()
                }
                canvas.drawPath(path, paint)
                canvas.restore()
            }
            ModuleShape.Bubbles -> {
                val hash = (gx * 374761393 + gy * 668265263) and 0x7FFFFFFF
                val br = size * (0.26f + ((hash % 64) / 64f) * 0.22f)
                canvas.drawCircle(cx, cy, br, paint)
            }
            ModuleShape.Confetti -> {
                val hash = (gx * 374761393 + gy * 668265263) and 0x7FFFFFFF
                val angle = (hash % 360).toFloat()
                val len = size * (0.66f + ((hash ushr 9) % 28) / 100f)
                val thick = size * 0.38f
                canvas.save()
                canvas.rotate(angle, cx, cy)
                canvas.drawRoundRect(RectF(cx - len / 2f, cy - thick / 2f, cx + len / 2f, cy + thick / 2f), thick / 2f, thick / 2f, paint)
                canvas.restore()
            }
        }
    }

    fun resolveEffectiveBgColor(qrStyle: QrStyle): Int {
        return if (qrStyle.bgColor != 0) qrStyle.bgColor else 0xFFFFFFFF.toInt()
    }

    fun resolveEffectiveEyeColors(qrStyle: QrStyle, effBg: Int = resolveEffectiveBgColor(qrStyle)): Pair<Int, Int> {
        val bgIsDark = isDarkColor(effBg)
        var eyeColor = qrStyle.eyeColor
        var ballColor = qrStyle.ballColor

        if (bgIsDark) {
            // Background is dark -> Eyes & pupils MUST be light colors with strong contrast
            if (isDarkColor(eyeColor)) {
                eyeColor = if (!isDarkColor(qrStyle.fgColor) && getContrastRatio(qrStyle.fgColor, effBg) >= 2.5f) {
                    qrStyle.fgColor
                } else if (qrStyle.gradientType != GradientType.None && !isDarkColor(qrStyle.gradientTo)) {
                    qrStyle.gradientTo
                } else {
                    0xFFFFFFFF.toInt()
                }
            }
            if (isDarkColor(ballColor)) {
                ballColor = if (!isDarkColor(qrStyle.fgColor) && getContrastRatio(qrStyle.fgColor, effBg) >= 2.5f) {
                    qrStyle.fgColor
                } else if (!isDarkColor(eyeColor)) {
                    eyeColor
                } else {
                    0xFFFFFFFF.toInt()
                }
            }
        } else {
            // Background is light -> Eyes & pupils MUST be dark colors with strong contrast
            if (!isDarkColor(eyeColor)) {
                eyeColor = if (isDarkColor(qrStyle.fgColor) && getContrastRatio(qrStyle.fgColor, effBg) >= 2.5f) {
                    qrStyle.fgColor
                } else if (qrStyle.gradientType != GradientType.None && isDarkColor(qrStyle.gradientTo)) {
                    qrStyle.gradientTo
                } else {
                    0xFF0F172A.toInt()
                }
            }
            if (!isDarkColor(ballColor)) {
                ballColor = if (isDarkColor(qrStyle.fgColor) && getContrastRatio(qrStyle.fgColor, effBg) >= 2.5f) {
                    qrStyle.fgColor
                } else if (isDarkColor(eyeColor)) {
                    eyeColor
                } else {
                    0xFF0F172A.toInt()
                }
            }
        }

        // Guarantee high contrast ratio >= 3.0:1
        if (getContrastRatio(eyeColor, effBg) < 3.0f) {
            eyeColor = if (bgIsDark) 0xFFFFFFFF.toInt() else 0xFF0F172A.toInt()
        }
        if (getContrastRatio(ballColor, effBg) < 3.0f) {
            ballColor = eyeColor
        }

        return Pair(eyeColor, ballColor)
    }

    fun getContrastRatio(c1: Int, c2: Int): Float {
        val r1 = (c1 shr 16 and 0xFF) / 255f
        val g1 = (c1 shr 8 and 0xFF) / 255f
        val b1 = (c1 and 0xFF) / 255f
        val l1 = 0.2126f * (if (r1 <= 0.03928f) r1 / 12.92f else Math.pow(((r1 + 0.055) / 1.055), 2.4).toFloat()) +
                 0.7152f * (if (g1 <= 0.03928f) g1 / 12.92f else Math.pow(((g1 + 0.055) / 1.055), 2.4).toFloat()) +
                 0.0722f * (if (b1 <= 0.03928f) b1 / 12.92f else Math.pow(((b1 + 0.055) / 1.055), 2.4).toFloat())

        val r2 = (c2 shr 16 and 0xFF) / 255f
        val g2 = (c2 shr 8 and 0xFF) / 255f
        val b2 = (c2 and 0xFF) / 255f
        val l2 = 0.2126f * (if (r2 <= 0.03928f) r2 / 12.92f else Math.pow(((r2 + 0.055) / 1.055), 2.4).toFloat()) +
                 0.7152f * (if (g2 <= 0.03928f) g2 / 12.92f else Math.pow(((g2 + 0.055) / 1.055), 2.4).toFloat()) +
                 0.0722f * (if (b2 <= 0.03928f) b2 / 12.92f else Math.pow(((b2 + 0.055) / 1.055), 2.4).toFloat())

        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun drawEyeUnderlay(canvas: Canvas, ox: Float, oy: Float, cell: Float, qrStyle: QrStyle, effCol: Int = 0) {
        val s = cell * 7
        val bx = ox + cell * 2
        val by = oy + cell * 2
        val bSize = cell * 3
        val eyeIntensity = qrStyle.effectIntensity.coerceIn(0.4f, 2.5f)

        when (qrStyle.effect) {
            QrEffect.None -> {}
            QrEffect.Raised3D -> {
                val sDist = (cell * 0.42f * eyeIntensity).coerceIn(3.5f, 22f)
                val ambPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x38000000; style = Paint.Style.FILL }
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x75000000; style = Paint.Style.FILL }

                drawEyeLayer(canvas, ox + sDist * 1.4f, oy + sDist * 1.4f, s * 1.05f, qrStyle.eyeShape, ambPaint)
                drawEyeLayer(canvas, ox + sDist, oy + sDist, s, qrStyle.eyeShape, shadowPaint)

                drawEyeLayer(canvas, bx + sDist * 1.25f, by + sDist * 1.25f, bSize * 1.06f, qrStyle.ballShape, ambPaint)
                drawEyeLayer(canvas, bx + sDist * 0.9f, by + sDist * 0.9f, bSize, qrStyle.ballShape, shadowPaint)
            }
            QrEffect.Engraved -> {
                val inDist = (cell * 0.36f * eyeIntensity).coerceIn(3.0f, 18f)
                val pitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAA000000.toInt(); style = Paint.Style.FILL }
                val chiselPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xBBFFFFFF.toInt(); style = Paint.Style.FILL }

                drawEyeLayer(canvas, ox - inDist * 1.1f, oy - inDist * 1.1f, s * 1.05f, qrStyle.eyeShape, pitPaint)
                drawEyeLayer(canvas, ox + inDist * 1.1f, oy + inDist * 1.1f, s * 1.05f, qrStyle.eyeShape, chiselPaint)

                drawEyeLayer(canvas, bx - inDist, by - inDist, bSize * 1.06f, qrStyle.ballShape, pitPaint)
                drawEyeLayer(canvas, bx + inDist, by + inDist, bSize * 1.06f, qrStyle.ballShape, chiselPaint)
            }
            QrEffect.Glow -> {
                val bgIsDark = isDarkColor(resolveEffectiveBgColor(qrStyle))
                if (bgIsDark) {
                    val eyeGlowCol = getSlightlyLighterColor(qrStyle.eyeColor)
                    val bloom = (cell * 0.10f * eyeIntensity).coerceIn(1.2f, 3.5f)
                    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = (eyeGlowCol and 0x00FFFFFF) or 0x22000000
                        style = Paint.Style.FILL
                    }
                    drawEyeLayer(canvas, ox - bloom * 0.5f, oy - bloom * 0.5f, s + bloom, qrStyle.eyeShape, halo)
                }
            }
            QrEffect.Shadow -> {
                val sDist = (cell * 0.40f * eyeIntensity).coerceIn(3.5f, 20f)
                val ambPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30000000; style = Paint.Style.FILL }
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x70000000; style = Paint.Style.FILL }

                drawEyeLayer(canvas, ox + sDist * 1.5f, oy + sDist * 1.5f, s * 1.06f, qrStyle.eyeShape, ambPaint)
                drawEyeLayer(canvas, ox + sDist, oy + sDist, s, qrStyle.eyeShape, shadowPaint)

                drawEyeLayer(canvas, bx + sDist * 1.3f, by + sDist * 1.3f, bSize * 1.06f, qrStyle.ballShape, ambPaint)
                drawEyeLayer(canvas, bx + sDist * 0.9f, by + sDist * 0.9f, bSize, qrStyle.ballShape, shadowPaint)
            }
            QrEffect.Emboss -> {
                val eDist = (cell * 0.32f * eyeIntensity).coerceIn(3.0f, 16f)
                val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAFFFFFF.toInt(); style = Paint.Style.FILL }
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88000000.toInt(); style = Paint.Style.FILL }

                drawEyeLayer(canvas, ox - eDist * 1.1f, oy - eDist * 1.1f, s * 1.02f, qrStyle.eyeShape, hlPaint)
                drawEyeLayer(canvas, ox + eDist * 1.1f, oy + eDist * 1.1f, s * 1.02f, qrStyle.eyeShape, shadowPaint)

                drawEyeLayer(canvas, bx - eDist * 0.8f, by - eDist * 0.8f, bSize * 1.02f, qrStyle.ballShape, hlPaint)
                drawEyeLayer(canvas, bx + eDist * 0.8f, by + eDist * 0.8f, bSize * 1.02f, qrStyle.ballShape, shadowPaint)
            }
            QrEffect.Outline -> {
                val strokeW = (cell * 0.38f * eyeIntensity).coerceIn(3f, 10f)
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xEEFFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, outlinePaint)
                drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, outlinePaint)
            }
            QrEffect.Glassmorphism -> {
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x45000000; style = Paint.Style.FILL }
                drawEyeLayer(canvas, ox + cell * 0.25f, oy + cell * 0.25f, s * 1.03f, qrStyle.eyeShape, shadowPaint)
                drawEyeLayer(canvas, bx + cell * 0.2f, by + cell * 0.2f, bSize * 1.03f, qrStyle.ballShape, shadowPaint)
            }
        }
    }

    private fun drawEye(
        canvas: Canvas,
        ox: Float,
        oy: Float,
        cell: Float,
        qrStyle: QrStyle,
        effCol: Int = 0,
        originX: Float = ox,
        originY: Float = oy,
        matrixSize: Int = 33
    ) {
        val s = cell * 7
        val effectiveBg = resolveEffectiveBgColor(qrStyle)
        val (eyeColor, ballColor) = resolveEffectiveEyeColors(qrStyle, effectiveBg)
        val bgColor = effectiveBg

        // 0. Pristine 8x8 Module Separator Quiet Island
        // Eliminates any dot overlapping or shadow bleeding on the finder eyes, guaranteeing verified scannability
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = effectiveBg
            style = Paint.Style.FILL
        }
        val isTopLeft = Math.abs(ox - originX) < cell * 0.5f && Math.abs(oy - originY) < cell * 0.5f
        val isTopRight = Math.abs(ox - (originX + (matrixSize - 7) * cell)) < cell * 0.5f && Math.abs(oy - originY) < cell * 0.5f
        val isBottomLeft = Math.abs(ox - originX) < cell * 0.5f && Math.abs(oy - (originY + (matrixSize - 7) * cell)) < cell * 0.5f

        val plateRect = when {
            isTopLeft -> RectF(ox, oy, ox + cell * 8, oy + cell * 8)
            isTopRight -> RectF(ox - cell, oy, ox + cell * 7, oy + cell * 8)
            isBottomLeft -> RectF(ox, oy - cell, ox + cell * 8, oy + cell * 7)
            else -> RectF(ox - cell * 0.5f, oy - cell * 0.5f, ox + cell * 7.5f, oy + cell * 7.5f)
        }
        val plateR = cell * 0.35f
        canvas.drawRoundRect(plateRect, plateR, plateR, platePaint)

        if (qrStyle.eyeShape == EyeShape.Target) {
            val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = eyeColor; style = Paint.Style.FILL }
            val midPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor; style = Paint.Style.FILL }
            val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ballColor; style = Paint.Style.FILL }
            val cx = ox + s / 2f
            val cy = oy + s / 2f
            canvas.drawCircle(cx, cy, s * 0.5f, outerPaint)
            canvas.drawCircle(cx, cy, s * (0.5f - 1f / 7f), midPaint)
            canvas.drawCircle(cx, cy, s * (1.5f / 7f), pupilPaint)
            return
        }

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = eyeColor
            this.style = Paint.Style.FILL
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            this.style = Paint.Style.FILL
        }
        val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ballColor
            this.style = Paint.Style.FILL
        }

        val bx = ox + cell * 2
        val by = oy + cell * 2
        val bSize = cell * 3
        val resolvedEffCol = if (effCol != 0) effCol else if (qrStyle.gradientType != GradientType.None) qrStyle.gradientTo else eyeColor
        val eyeIntensity = qrStyle.effectIntensity.coerceIn(0.4f, 2.5f)

        when (qrStyle.effect) {
            QrEffect.None -> {
                // Outer Ring (7x7)
                drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, outerPaint)

                // Middle Inset / Gap (5x5)
                drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)

                // Inner Ball / Pupil (3x3 modules)
                drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, ballPaint)
            }
            QrEffect.Raised3D -> {
                val sDist = (cell * 0.45f * eyeIntensity).coerceIn(4f, 22f)
                val bevelStrokeW = (cell * 0.28f).coerceIn(2.5f, 6.0f)
                val hlStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xDDFFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = bevelStrokeW
                }
                val shadeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x85000000.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = bevelStrokeW
                }
                val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x80000000.toInt(); style = Paint.Style.FILL }

                // 1. Outer ring extruded sidewall
                drawEyeLayer(canvas, ox + sDist * 0.5f, oy + sDist * 0.5f, s, qrStyle.eyeShape, sidePaint)

                // 2. Outer ring top face
                val ringFaceX = ox - sDist * 0.25f
                val ringFaceY = oy - sDist * 0.25f
                drawEyeLayer(canvas, ringFaceX, ringFaceY, s, qrStyle.eyeShape, outerPaint)

                // 3. Outer ring specular top-left highlight & bottom-right shadow contour
                drawEyeLayer(canvas, ringFaceX - sDist * 0.35f, ringFaceY - sDist * 0.35f, s, qrStyle.eyeShape, hlStrokePaint)
                drawEyeLayer(canvas, ringFaceX + sDist * 0.28f, ringFaceY + sDist * 0.28f, s, qrStyle.eyeShape, shadeStrokePaint)

                // 4. Middle gap (clean background)
                drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)

                // 5. Pupil extruded sidewall & top face
                drawEyeLayer(canvas, bx + sDist * 0.5f, by + sDist * 0.5f, bSize, qrStyle.ballShape, sidePaint)
                val pupilFaceX = bx - sDist * 0.25f
                val pupilFaceY = by - sDist * 0.25f
                drawEyeLayer(canvas, pupilFaceX, pupilFaceY, bSize, qrStyle.ballShape, ballPaint)

                // 6. Pupil specular highlight & center pip
                drawEyeLayer(canvas, pupilFaceX - sDist * 0.30f, pupilFaceY - sDist * 0.30f, bSize, qrStyle.ballShape, hlStrokePaint)
                val laserPip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEEFFFFFF.toInt(); style = Paint.Style.FILL }
                drawEyeLayer(canvas, pupilFaceX + bSize * 0.35f, pupilFaceY + bSize * 0.35f, bSize * 0.30f, qrStyle.ballShape, laserPip)
            }
            QrEffect.Engraved -> {
                val inDist = (cell * 0.36f * eyeIntensity).coerceIn(3.0f, 18f)

                // Recessed outer ring
                drawEyeLayer(canvas, ox + inDist * 0.25f, oy + inDist * 0.25f, s * 0.95f, qrStyle.eyeShape, outerPaint)

                // Middle gap
                drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)

                // Sunken pupil core
                drawEyeLayer(canvas, bx + inDist * 0.20f, by + inDist * 0.20f, bSize * 0.88f, qrStyle.ballShape, ballPaint)
            }
            QrEffect.Glow -> {
                val eyeGlowCol = getSlightlyLighterColor(qrStyle.eyeColor)
                val ballGlowCol = getSlightlyLighterColor(qrStyle.ballColor)

                // 1. Solid outer ring (7x7)
                drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, outerPaint)

                // 2. Neon luminous inner rim (strictly inside the 7x7 outer boundary)
                val strokeW = (cell * 0.12f).coerceIn(1.2f, 2.5f)
                val neonEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = (eyeGlowCol and 0x00FFFFFF) or 0xDD000000.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                drawEyeLayer(canvas, ox + strokeW * 0.5f, oy + strokeW * 0.5f, s - strokeW, qrStyle.eyeShape, neonEdge)

                // 3. Middle gap (100% clean background, exactly 5x5)
                drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)

                // 4. Solid pupil (100% solid, exactly 3x3)
                drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, ballPaint)

                // 5. Neon inner rim inside pupil (strictly inside the 3x3 pupil)
                val ballStrokeW = (cell * 0.10f).coerceIn(1.0f, 2.2f)
                val ballNeonEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = (ballGlowCol and 0x00FFFFFF) or 0xDD000000.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = ballStrokeW
                }
                drawEyeLayer(canvas, bx + ballStrokeW * 0.5f, by + ballStrokeW * 0.5f, bSize - ballStrokeW, qrStyle.ballShape, ballNeonEdge)

                // 6. Central laser pip in center of pupil
                val pupilPip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x88FFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                drawEyeLayer(canvas, bx + cell * 1.15f, by + cell * 1.15f, cell * 0.70f, EyeShape.Circle, pupilPip)
            }
            QrEffect.Shadow -> {
                drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, outerPaint)
                drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)
                drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, ballPaint)
            }
            QrEffect.Emboss -> {
                val eDist = (cell * 0.30f * eyeIntensity).coerceIn(2.5f, 15f)
                drawEyeLayer(canvas, ox, oy, s * 0.95f, qrStyle.eyeShape, outerPaint)
                drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)
                drawEyeLayer(canvas, bx, by, bSize * 0.90f, qrStyle.ballShape, ballPaint)
            }
            QrEffect.Outline -> {
                val strokeW = (cell * 0.35f * eyeIntensity).coerceIn(2.5f, 9f)
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xEEFFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, outlinePaint)
                drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, outerPaint)

                drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)

                drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, outlinePaint)
                drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, ballPaint)
            }
            QrEffect.Glassmorphism -> {
                val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88FFFFFF.toInt(); style = Paint.Style.FILL }
                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x65FFFFFF; style = Paint.Style.STROKE; strokeWidth = 2.0f }

                drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, outerPaint)
                drawEyeLayer(canvas, ox - cell * 0.15f, oy - cell * 0.25f, s * 0.55f, qrStyle.eyeShape, glossPaint)
                drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, rimPaint)

                drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)

                drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, ballPaint)
                drawEyeLayer(canvas, bx, by - cell * 0.18f, bSize * 0.55f, qrStyle.ballShape, glossPaint)
                drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, rimPaint)
            }
        }

        // If Ticks, draw the 4 cross-hair tick marks
        if (qrStyle.eyeShape == EyeShape.Ticks) {
            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = eyeColor
                style = Paint.Style.FILL
            }
            val t = cell * 1.35f
            val r = t * 0.3f
            canvas.drawRoundRect(RectF(ox + s / 2f - t / 2f, oy, ox + s / 2f + t / 2f, oy + t), r, r, tickPaint)
            canvas.drawRoundRect(RectF(ox + s / 2f - t / 2f, oy + s - t, ox + s / 2f + t / 2f, oy + s), r, r, tickPaint)
            canvas.drawRoundRect(RectF(ox, oy + s / 2f - t / 2f, ox + t, oy + s / 2f + t / 2f), r, r, tickPaint)
            canvas.drawRoundRect(RectF(ox + s - t, oy + s / 2f - t / 2f, ox + s, oy + s / 2f + t / 2f), r, r, tickPaint)
        }
    }

    private fun drawEyeLayer(canvas: Canvas, x: Float, y: Float, size: Float, shape: EyeShape, paint: Paint) {
        val cx = x + size / 2f
        val cy = y + size / 2f
        val r = size / 2f

        when (shape) {
            EyeShape.Square -> {
                canvas.drawRect(x, y, x + size, y + size, paint)
            }
            EyeShape.Rounded -> {
                canvas.drawRoundRect(RectF(x, y, x + size, y + size), size * 0.18f, size * 0.18f, paint)
            }
            EyeShape.ExtraRounded -> {
                canvas.drawRoundRect(RectF(x, y, x + size, y + size), size * 0.32f, size * 0.32f, paint)
            }
            EyeShape.Circle, EyeShape.Target -> {
                canvas.drawCircle(cx, cy, r, paint)
            }
            EyeShape.Diamond -> {
                val path = Path().apply {
                    moveTo(cx, y)
                    lineTo(x + size, cy)
                    lineTo(cx, y + size)
                    lineTo(x, cy)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            EyeShape.Hex -> {
                val path = Path()
                for (i in 0 until 6) {
                    val angle = (Math.PI / 3 * i - Math.PI / 6).toFloat()
                    val px = cx + cos(angle.toDouble()).toFloat() * r
                    val py = cy + sin(angle.toDouble()).toFloat() * r
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            EyeShape.Classy -> {
                // Diagonally rounded: top-right & bottom-left
                val cr = size * 0.38f
                val radii = floatArrayOf(0f, 0f, cr, cr, 0f, 0f, cr, cr)
                val path = Path().apply {
                    addRoundRect(RectF(x, y, x + size, y + size), radii, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
            }
            EyeShape.Leaf -> {
                // Diagonally rounded: top-left & bottom-right
                val cr = size * 0.42f
                val radii = floatArrayOf(cr, cr, 0f, 0f, cr, cr, 0f, 0f)
                val path = Path().apply {
                    addRoundRect(RectF(x, y, x + size, y + size), radii, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
            }
            EyeShape.Ticks -> {
                canvas.drawRoundRect(RectF(x, y, x + size, y + size), size * 0.18f, size * 0.18f, paint)
            }
        }
    }

    data class FrameInsets(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    fun calculateFrameInsets(frameStyle: FrameStyle, sizePx: Int): FrameInsets =
        calculateFrameInsets(frameStyle, null, sizePx)

    fun calculateFrameInsets(frameStyle: FrameStyle, artDirection: String?, sizePx: Int): FrameInsets {
        val s = sizePx.toFloat()
        if (!artDirection.isNullOrBlank()) {
            return FrameInsets(s * 0.020f, s * 0.020f, s * 0.020f, s * 0.020f)
        }
        return when (frameStyle) {
            FrameStyle.None -> FrameInsets(0f, 0f, 0f, 0f)
            FrameStyle.ModernPill,
            FrameStyle.Label,
            FrameStyle.Speech,
            FrameStyle.Bracket,
            FrameStyle.PhoneFrame -> FrameInsets(s * 0.020f, s * 0.095f, s * 0.020f, s * 0.020f)
            else -> FrameInsets(s * 0.020f, s * 0.020f, s * 0.020f, s * 0.095f)
        }
    }

    private fun drawBadgePill(
        canvas: Canvas,
        caption: String,
        defaultText: String,
        centerX: Float,
        centerY: Float,
        badgeWidth: Float,
        badgeHeight: Float,
        pillColor: Int,
        textColor: Int,
        s: Float
    ) {
        val textToDraw = if (caption.trim().isNotEmpty()) caption.trim() else defaultText

        val pillRect = RectF(
            centerX - badgeWidth / 2f,
            centerY - badgeHeight / 2f,
            centerX + badgeWidth / 2f,
            centerY + badgeHeight / 2f
        )

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pillColor
            style = Paint.Style.FILL
        }
        val cornerRadius = badgeHeight / 2f
        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.003f
        }
        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var targetTextSize = badgeHeight * 0.52f
        textPaint.textSize = targetTextSize

        val maxTextWidth = badgeWidth - s * 0.05f
        val measuredWidth = textPaint.measureText(textToDraw)
        if (measuredWidth > maxTextWidth && measuredWidth > 0f) {
            targetTextSize *= (maxTextWidth / measuredWidth)
            textPaint.textSize = targetTextSize
        }

        val fontMetrics = textPaint.fontMetrics
        val textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(textToDraw, centerX, textY, textPaint)
    }

    private fun drawFrameBackground(
        canvas: Canvas,
        frameStyle: FrameStyle,
        sizePx: Int,
        qrStyle: QrStyle
    ) {
        // Frames no longer draw opaque/clunky background shapes behind QR modules.
        // This keeps the QR code matrix 100% visible, uncluttered, and scannable.
    }

    private fun drawFrameForeground(
        canvas: Canvas,
        frameStyle: FrameStyle,
        caption: String,
        sizePx: Int,
        ox: Float,
        oy: Float,
        bodyPx: Float,
        qrStyle: QrStyle
    ) {
        val s = sizePx.toFloat()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = s * 0.038f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val primaryColor = if (qrStyle.gradientType != GradientType.None) qrStyle.gradientTo else qrStyle.fgColor

        when (frameStyle) {
            FrameStyle.BadgeScanMe -> {
                // Badge 1: Sleek Bottom Capsule
                drawBadgePill(
                    canvas = canvas,
                    caption = caption,
                    defaultText = "SCAN ME",
                    centerX = s / 2f,
                    centerY = s * 0.952f,
                    badgeWidth = s * 0.60f,
                    badgeHeight = s * 0.068f,
                    pillColor = qrStyle.fgColor,
                    textColor = 0xFFFFFFFF.toInt(),
                    s = s
                )
            }
            FrameStyle.ModernPill -> {
                // Badge 2: Sleek Top Capsule
                drawBadgePill(
                    canvas = canvas,
                    caption = caption,
                    defaultText = "SCAN ME",
                    centerX = s / 2f,
                    centerY = s * 0.048f,
                    badgeWidth = s * 0.60f,
                    badgeHeight = s * 0.068f,
                    pillColor = primaryColor,
                    textColor = 0xFFFFFFFF.toInt(),
                    s = s
                )
            }
            FrameStyle.Badge -> {
                // Badge 3: Angled Ribbon Badge
                val rw = s * 0.62f
                val rh = s * 0.070f
                val rx = (s - rw) / 2f
                val ry = s * 0.916f

                val ribbonPath = Path().apply {
                    moveTo(rx, ry)
                    lineTo(rx + rw, ry)
                    lineTo(rx + rw - s * 0.025f, ry + rh / 2f)
                    lineTo(rx + rw, ry + rh)
                    lineTo(rx, ry + rh)
                    lineTo(rx + s * 0.025f, ry + rh / 2f)
                    close()
                }

                val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    style = Paint.Style.FILL
                }
                canvas.drawPath(ribbonPath, ribbonPaint)

                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x66FFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.003f
                }
                canvas.drawPath(ribbonPath, strokePaint)

                textPaint.textSize = rh * 0.48f
                val textToDraw = if (caption.trim().isNotEmpty()) caption.trim() else "SCAN ME"
                val fontMetrics = textPaint.fontMetrics
                val textY = (ry + rh / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(textToDraw, s / 2f, textY, textPaint)
            }
            FrameStyle.Label -> {
                // Badge 4: Minimal Tag Badge
                val bw = s * 0.54f
                val bh = s * 0.064f
                val cx = s / 2f
                val cy = s * 0.048f
                val tagRect = RectF(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)

                val tagBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF1E293B.toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(tagRect, s * 0.018f, s * 0.018f, tagBg)

                val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx - bw / 2f + s * 0.03f, cy, s * 0.009f, dotP)

                drawBadgePill(
                    canvas = canvas,
                    caption = caption,
                    defaultText = "SCAN ME",
                    centerX = cx + s * 0.015f,
                    centerY = cy,
                    badgeWidth = bw - s * 0.08f,
                    badgeHeight = bh,
                    pillColor = 0x00000000,
                    textColor = 0xFFFFFFFF.toInt(),
                    s = s
                )
            }
            FrameStyle.Speech -> {
                // Badge 5: Speech Bubble Badge
                val bw = s * 0.56f
                val bh = s * 0.064f
                val cx = s / 2f
                val cy = s * 0.045f
                val bx = cx - bw / 2f
                val by = cy - bh / 2f

                val bPath = Path().apply {
                    addRoundRect(RectF(bx, by, bx + bw, by + bh), s * 0.020f, s * 0.020f, Path.Direction.CW)
                    moveTo(cx - s * 0.020f, by + bh)
                    lineTo(cx, by + bh + s * 0.015f)
                    lineTo(cx + s * 0.020f, by + bh)
                    close()
                }

                val bubbleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    style = Paint.Style.FILL
                }
                canvas.drawPath(bPath, bubbleP)

                textPaint.textSize = bh * 0.48f
                val textToDraw = if (caption.trim().isNotEmpty()) caption.trim() else "SCAN ME"
                val fontMetrics = textPaint.fontMetrics
                val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(textToDraw, cx, textY, textPaint)
            }
            FrameStyle.None -> {}
            else -> {
                // Fallback for legacy badges: clean bottom capsule
                drawBadgePill(
                    canvas = canvas,
                    caption = caption,
                    defaultText = "SCAN ME",
                    centerX = s / 2f,
                    centerY = s * 0.952f,
                    badgeWidth = s * 0.60f,
                    badgeHeight = s * 0.068f,
                    pillColor = primaryColor,
                    textColor = 0xFFFFFFFF.toInt(),
                    s = s
                )
            }
        }
    }

    private fun drawArtisticBorderDecorations(
        canvas: Canvas,
        artDirection: String?,
        sizePx: Int,
        originX: Float,
        originY: Float,
        bodyW: Float,
        qrStyle: QrStyle
    ) {
        if (artDirection.isNullOrBlank()) return
        val ad = artDirection.lowercase()
        if (ad.contains("frame") || ad.contains("bioluminescent") || ad.contains("botanical") || ad.contains("wood") || ad.contains("steel") || ad.contains("crown") || ad.contains("circuit") || ad.contains("wave") || ad.contains("border") || ad.contains("bracket") || ad.contains("stripes") || ad.contains("splash") || ad.contains("nebula")) return
        val s = sizePx.toFloat()
        val cornerSize = s * 0.12f

        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = qrStyle.gradientTo.takeIf { qrStyle.gradientType != GradientType.None } ?: qrStyle.fgColor
            style = Paint.Style.FILL
        }

        when (artDirection.lowercase()) {
            "snowflakes" -> {
                artPaint.color = 0xFF60A5FA.toInt()
                artPaint.style = Paint.Style.STROKE
                artPaint.strokeWidth = s * 0.008f
                drawSnowflake(canvas, s * 0.08f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawSnowflake(canvas, s * 0.92f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawSnowflake(canvas, s * 0.08f, s * 0.92f, cornerSize * 0.45f, artPaint)
                drawSnowflake(canvas, s * 0.92f, s * 0.92f, cornerSize * 0.45f, artPaint)
                drawSnowflake(canvas, s * 0.5f, s * 0.05f, cornerSize * 0.28f, artPaint)
                drawSnowflake(canvas, s * 0.5f, s * 0.95f, cornerSize * 0.28f, artPaint)
            }
            "music" -> {
                artPaint.color = 0xFF8B5CF6.toInt()
                artPaint.style = Paint.Style.FILL
                drawMusicNote(canvas, s * 0.07f, s * 0.07f, cornerSize * 0.4f, artPaint)
                drawMusicNote(canvas, s * 0.93f, s * 0.07f, cornerSize * 0.35f, artPaint)
                drawDoubleMusicNote(canvas, s * 0.07f, s * 0.93f, cornerSize * 0.45f, artPaint)
                drawMusicNote(canvas, s * 0.93f, s * 0.93f, cornerSize * 0.4f, artPaint)
                drawMusicNote(canvas, s * 0.5f, s * 0.05f, cornerSize * 0.3f, artPaint)
            }
            "stars", "star" -> {
                artPaint.color = 0xFFF59E0B.toInt()
                artPaint.style = Paint.Style.FILL
                drawStarSparkle(canvas, s * 0.07f, s * 0.07f, cornerSize * 0.45f, artPaint)
                drawStarSparkle(canvas, s * 0.93f, s * 0.07f, cornerSize * 0.45f, artPaint)
                drawStarSparkle(canvas, s * 0.07f, s * 0.93f, cornerSize * 0.45f, artPaint)
                drawStarSparkle(canvas, s * 0.93f, s * 0.93f, cornerSize * 0.45f, artPaint)
                drawStarSparkle(canvas, s * 0.5f, s * 0.05f, cornerSize * 0.3f, artPaint)
                drawStarSparkle(canvas, s * 0.5f, s * 0.95f, cornerSize * 0.3f, artPaint)
            }
            "floral", "spring", "sakura" -> {
                artPaint.color = 0xFFF472B6.toInt()
                artPaint.style = Paint.Style.FILL
                drawCherryBlossom(canvas, s * 0.08f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawCherryBlossom(canvas, s * 0.92f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawCherryBlossom(canvas, s * 0.08f, s * 0.92f, cornerSize * 0.45f, artPaint)
                drawCherryBlossom(canvas, s * 0.92f, s * 0.92f, cornerSize * 0.45f, artPaint)
            }
            "love" -> {
                artPaint.color = 0xFFFB7185.toInt()
                artPaint.style = Paint.Style.FILL
                drawHeart(canvas, s * 0.07f, s * 0.07f, cornerSize * 0.4f, artPaint)
                drawHeart(canvas, s * 0.93f, s * 0.07f, cornerSize * 0.4f, artPaint)
                drawHeart(canvas, s * 0.07f, s * 0.93f, cornerSize * 0.4f, artPaint)
                drawHeart(canvas, s * 0.93f, s * 0.93f, cornerSize * 0.4f, artPaint)
                drawHeart(canvas, s * 0.5f, s * 0.05f, cornerSize * 0.28f, artPaint)
                drawHeart(canvas, s * 0.5f, s * 0.95f, cornerSize * 0.28f, artPaint)
            }
            "halloween" -> {
                artPaint.color = 0xFFF97316.toInt()
                artPaint.style = Paint.Style.STROKE
                artPaint.strokeWidth = s * 0.007f
                drawSpiderWeb(canvas, 0f, 0f, cornerSize * 0.8f, artPaint)
                drawSpiderWeb(canvas, s, 0f, cornerSize * 0.8f, artPaint)
                artPaint.style = Paint.Style.FILL
                drawBat(canvas, s * 0.12f, s * 0.92f, cornerSize * 0.5f, artPaint)
                drawBat(canvas, s * 0.88f, s * 0.92f, cornerSize * 0.5f, artPaint)
            }
            "ramadan", "night-sky" -> {
                artPaint.color = 0xFFFBBF24.toInt()
                artPaint.style = Paint.Style.FILL
                drawCrescentMoon(canvas, s * 0.90f, s * 0.09f, cornerSize * 0.45f, artPaint)
                drawLantern(canvas, s * 0.10f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawStarSparkle(canvas, s * 0.08f, s * 0.92f, cornerSize * 0.35f, artPaint)
                drawStarSparkle(canvas, s * 0.92f, s * 0.92f, cornerSize * 0.35f, artPaint)
            }
            "autumn" -> {
                artPaint.color = 0xFFD97706.toInt()
                artPaint.style = Paint.Style.FILL
                drawMapleLeaf(canvas, s * 0.08f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawMapleLeaf(canvas, s * 0.92f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawMapleLeaf(canvas, s * 0.08f, s * 0.92f, cornerSize * 0.45f, artPaint)
                drawMapleLeaf(canvas, s * 0.92f, s * 0.92f, cornerSize * 0.45f, artPaint)
            }
            "ocean" -> {
                artPaint.color = 0xFF38BDF8.toInt()
                artPaint.style = Paint.Style.STROKE
                artPaint.strokeWidth = s * 0.012f
                drawWaveRibbon(canvas, s, s * 0.04f, artPaint)
                drawWaveRibbon(canvas, s, s * 0.96f, artPaint)
            }
            "summer" -> {
                artPaint.color = 0xFFF59E0B.toInt()
                artPaint.style = Paint.Style.FILL
                drawSunFlower(canvas, s * 0.08f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawSunFlower(canvas, s * 0.92f, s * 0.08f, cornerSize * 0.45f, artPaint)
                artPaint.color = 0xFF0284C7.toInt()
                artPaint.style = Paint.Style.STROKE
                artPaint.strokeWidth = s * 0.01f
                drawWaveRibbon(canvas, s, s * 0.96f, artPaint)
            }
            "nature" -> {
                artPaint.color = 0xFF22C55E.toInt()
                artPaint.style = Paint.Style.FILL
                drawLeafSprig(canvas, s * 0.08f, s * 0.08f, cornerSize * 0.45f, 45f, artPaint)
                drawLeafSprig(canvas, s * 0.92f, s * 0.08f, cornerSize * 0.45f, -45f, artPaint)
                drawLeafSprig(canvas, s * 0.08f, s * 0.92f, cornerSize * 0.45f, 135f, artPaint)
                drawLeafSprig(canvas, s * 0.92f, s * 0.92f, cornerSize * 0.45f, -135f, artPaint)
            }
            "arrows" -> {
                artPaint.color = 0xFFFB7185.toInt()
                artPaint.style = Paint.Style.FILL
                drawCornerArrow(canvas, s * 0.06f, s * 0.06f, cornerSize * 0.4f, 45f, artPaint)
                drawCornerArrow(canvas, s * 0.94f, s * 0.06f, cornerSize * 0.4f, 135f, artPaint)
                drawCornerArrow(canvas, s * 0.06f, s * 0.94f, cornerSize * 0.4f, -45f, artPaint)
                drawCornerArrow(canvas, s * 0.94f, s * 0.94f, cornerSize * 0.4f, -135f, artPaint)
            }
            "dot-ring" -> {
                val dotColors = listOf(0xFF10B981, 0xFF3B82F6, 0xFFEC4899, 0xFF8B5CF6, 0xFFF59E0B)
                val cx = s / 2f
                val cy = s / 2f
                val radius = s * 0.46f
                val dotCount = 28
                artPaint.style = Paint.Style.FILL
                for (i in 0 until dotCount) {
                    val angle = (2 * Math.PI / dotCount * i).toFloat()
                    val px = cx + cos(angle.toDouble()).toFloat() * radius
                    val py = cy + sin(angle.toDouble()).toFloat() * radius
                    artPaint.color = dotColors[i % dotColors.size].toInt()
                    canvas.drawCircle(px, py, s * 0.012f, artPaint)
                }
            }
            "vintage" -> {
                artPaint.color = 0xFF78350F.toInt()
                artPaint.style = Paint.Style.STROKE
                artPaint.strokeWidth = s * 0.008f
                drawVintageFiligree(canvas, s * 0.06f, s * 0.06f, cornerSize * 0.5f, 0f, artPaint)
                drawVintageFiligree(canvas, s * 0.94f, s * 0.06f, cornerSize * 0.5f, 90f, artPaint)
                drawVintageFiligree(canvas, s * 0.94f, s * 0.94f, cornerSize * 0.5f, 180f, artPaint)
                drawVintageFiligree(canvas, s * 0.06f, s * 0.94f, cornerSize * 0.5f, 270f, artPaint)
            }
            "christmas" -> {
                artPaint.color = 0xFF16A34A.toInt()
                artPaint.style = Paint.Style.FILL
                drawHollyLeaves(canvas, s * 0.08f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawHollyLeaves(canvas, s * 0.92f, s * 0.08f, cornerSize * 0.45f, artPaint)
                drawHollyLeaves(canvas, s * 0.08f, s * 0.92f, cornerSize * 0.45f, artPaint)
                drawHollyLeaves(canvas, s * 0.92f, s * 0.92f, cornerSize * 0.45f, artPaint)
            }
            "wedding" -> {
                artPaint.color = 0xFFBE185D.toInt()
                artPaint.style = Paint.Style.STROKE
                artPaint.strokeWidth = s * 0.008f
                drawWeddingRings(canvas, s * 0.08f, s * 0.08f, cornerSize * 0.4f, artPaint)
                drawWeddingRings(canvas, s * 0.92f, s * 0.08f, cornerSize * 0.4f, artPaint)
                artPaint.style = Paint.Style.FILL
                drawHeart(canvas, s * 0.08f, s * 0.92f, cornerSize * 0.35f, artPaint)
                drawHeart(canvas, s * 0.92f, s * 0.92f, cornerSize * 0.35f, artPaint)
            }
            "note", "plaque", "pentagon", "hexagon", "bucket", "arch", "cup", "seal", "globe" -> {
                // Dashed stitched border around card
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrStyle.fgColor
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.007f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.015f, s * 0.012f), 0f)
                }
                val inset = s * 0.045f
                canvas.drawRoundRect(RectF(inset, inset, s - inset, s - inset), s * 0.06f, s * 0.06f, stitchPaint)
            }
            "bioluminescent-botanical", "bioluminescent", "bioluminescent-wisteria", "wisteria" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "wisteria")
            }
            "bioluminescent-fiery-rose", "fiery-rose", "rose" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "fiery-rose")
            }
            "bioluminescent-coral-wildflower", "coral-wildflower", "coral" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "coral")
            }
            "bioluminescent-electric-teal", "electric-teal", "cyan-flora" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "electric-teal")
            }
            "bioluminescent-sakura", "sakura-blossom" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "sakura")
            }
            "bioluminescent-gold-filigree", "gold-filigree", "gold-flora" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "gold-filigree")
            }
            "bioluminescent-emerald-fern", "emerald-fern", "fern" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "emerald-fern")
            }
            "bioluminescent-icy-crystal", "icy-crystal", "frost" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "icy-crystal")
            }
            "bioluminescent-rainbow", "rainbow-flora" -> {
                drawBioluminescentBotanicalFrame(canvas, s, qrStyle, "rainbow")
            }
            "royal-crown" -> drawRoyalCrownFrame(canvas, s, qrStyle)
            "fluid-neon-border", "fluid-neon" -> drawFluidNeonFrame(canvas, s, qrStyle)
            "cyber-tech-circuit", "cyber-circuit" -> drawCyberCircuitFrame(canvas, s, qrStyle)
            "alpine-mountain" -> drawAlpineMountainFrame(canvas, s, qrStyle)
            "glass-liquid-bubbles", "glass-bubbles" -> drawGlassLiquidBubblesFrame(canvas, s, qrStyle)
            "zebra-stripe-border", "zebra-waves" -> drawZebraStripeFrame(canvas, s, qrStyle)
            "modern-tech-bracket", "modern-bracket" -> drawModernBracketFrame(canvas, s, qrStyle)
            "lush-green-leaves", "lush-leaves" -> drawLushLeavesFrame(canvas, s, qrStyle)
            "cosmic-nebula-ring", "cosmic-nebula" -> drawCosmicNebulaFrame(canvas, s, qrStyle)
            "red-matrix-cyber", "red-matrix" -> drawRedMatrixFrame(canvas, s, qrStyle)
            "armor-steel-plate", "armor-steel" -> drawArmorSteelFrame(canvas, s, qrStyle)
            "paint-splash-brush", "paint-splash" -> drawPaintSplashFrame(canvas, s, qrStyle)
            "retro-sunset-palm", "sunset-palm" -> drawSunsetPalmFrame(canvas, s, qrStyle)
            "neon-vortex-ring", "neon-vortex" -> drawNeonVortexFrame(canvas, s, qrStyle)
            "multicolor-geometric", "multicolor-geo" -> drawMulticolorGeoFrame(canvas, s, qrStyle)
        }
    }

    private fun drawSnowflake(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        for (i in 0 until 6) {
            val angle = (Math.PI / 3 * i).toFloat()
            val ex = cx + cos(angle.toDouble()).toFloat() * radius
            val ey = cy + sin(angle.toDouble()).toFloat() * radius
            canvas.drawLine(cx, cy, ex, ey, paint)
            // Branch bar
            val mx = cx + cos(angle.toDouble()).toFloat() * radius * 0.6f
            val my = cy + sin(angle.toDouble()).toFloat() * radius * 0.6f
            val bAngle = angle + (Math.PI / 4).toFloat()
            val bx = mx + cos(bAngle.toDouble()).toFloat() * radius * 0.3f
            val by = my + sin(bAngle.toDouble()).toFloat() * radius * 0.3f
            canvas.drawLine(mx, my, bx, by, paint)
        }
    }

    private fun drawMusicNote(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val headR = size * 0.28f
        canvas.drawOval(RectF(cx - headR, cy - headR * 0.7f, cx + headR, cy + headR * 0.7f), paint)
        val stemW = size * 0.12f
        val stemH = size * 0.9f
        canvas.drawRect(cx + headR * 0.5f, cy - stemH, cx + headR * 0.5f + stemW, cy, paint)
        // Flag
        val flagPath = Path().apply {
            moveTo(cx + headR * 0.5f + stemW, cy - stemH)
            cubicTo(cx + headR * 0.5f + stemW + size * 0.4f, cy - stemH + size * 0.2f, cx + headR * 0.5f + stemW + size * 0.2f, cy - stemH + size * 0.5f, cx + headR * 0.5f + stemW, cy - stemH + size * 0.4f)
            close()
        }
        canvas.drawPath(flagPath, paint)
    }

    private fun drawDoubleMusicNote(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val r = size * 0.22f
        canvas.drawCircle(cx - size * 0.3f, cy, r, paint)
        canvas.drawCircle(cx + size * 0.3f, cy - size * 0.15f, r, paint)
        val stemW = size * 0.1f
        val stemH = size * 0.75f
        canvas.drawRect(cx - size * 0.3f + r * 0.4f, cy - stemH, cx - size * 0.3f + r * 0.4f + stemW, cy, paint)
        canvas.drawRect(cx + size * 0.3f + r * 0.4f, cy - stemH - size * 0.15f, cx + size * 0.3f + r * 0.4f + stemW, cy - size * 0.15f, paint)
        // Cross bar
        canvas.drawRect(cx - size * 0.3f + r * 0.4f, cy - stemH, cx + size * 0.3f + r * 0.4f + stemW, cy - stemH + stemW * 1.5f, paint)
    }

    private fun drawStarSparkle(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - radius)
            quadTo(cx, cy, cx + radius, cy)
            quadTo(cx, cy, cx, cy + radius)
            quadTo(cx, cy, cx - radius, cy)
            quadTo(cx, cy, cx, cy - radius)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.drawCircle(cx, cy, radius * 0.25f, paint)
    }

    private fun drawCherryBlossom(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        for (i in 0 until 5) {
            val angle = (2 * Math.PI / 5 * i).toFloat()
            val px = cx + cos(angle.toDouble()).toFloat() * radius * 0.6f
            val py = cy + sin(angle.toDouble()).toFloat() * radius * 0.6f
            canvas.drawCircle(px, py, radius * 0.42f, paint)
        }
        val centerPaint = Paint(paint).apply { color = 0xFFFFFFFF.toInt() }
        canvas.drawCircle(cx, cy, radius * 0.22f, centerPaint)
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy + size * 0.45f)
            cubicTo(cx - size * 0.65f, cy + size * 0.1f, cx - size * 0.65f, cy - size * 0.45f, cx, cy - size * 0.15f)
            cubicTo(cx + size * 0.65f, cy - size * 0.45f, cx + size * 0.65f, cy + size * 0.1f, cx, cy + size * 0.45f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSpiderWeb(canvas: Canvas, ox: Float, oy: Float, size: Float, paint: Paint) {
        canvas.drawLine(ox, oy, ox + size, oy, paint)
        canvas.drawLine(ox, oy, ox, oy + size, paint)
        canvas.drawLine(ox, oy, ox + size * 0.7f, oy + size * 0.7f, paint)
        canvas.drawArc(RectF(ox - size * 0.5f, oy - size * 0.5f, ox + size * 0.5f, oy + size * 0.5f), 0f, 90f, false, paint)
        canvas.drawArc(RectF(ox - size, oy - size, ox + size, oy + size), 0f, 90f, false, paint)
    }

    private fun drawBat(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy)
            cubicTo(cx - size * 0.4f, cy - size * 0.3f, cx - size * 0.6f, cy - size * 0.1f, cx - size * 0.5f, cy + size * 0.15f)
            cubicTo(cx - size * 0.2f, cy + size * 0.05f, cx, cy + size * 0.25f, cx, cy)
            cubicTo(cx, cy + size * 0.25f, cx + size * 0.2f, cy + size * 0.05f, cx + size * 0.5f, cy + size * 0.15f)
            cubicTo(cx + size * 0.6f, cy - size * 0.1f, cx + size * 0.4f, cy - size * 0.3f, cx, cy)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCrescentMoon(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val path = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
            val cut = Path().apply { addCircle(cx - radius * 0.4f, cy - radius * 0.25f, radius * 0.85f, Path.Direction.CW) }
            op(cut, Path.Op.DIFFERENCE)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawLantern(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        canvas.drawLine(cx, cy - size * 0.6f, cx, cy - size * 0.3f, paint)
        val bodyRect = RectF(cx - size * 0.3f, cy - size * 0.3f, cx + size * 0.3f, cy + size * 0.3f)
        canvas.drawRoundRect(bodyRect, size * 0.1f, size * 0.1f, paint)
        canvas.drawLine(cx, cy + size * 0.3f, cx, cy + size * 0.55f, paint)
    }

    private fun drawMapleLeaf(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - radius)
            lineTo(cx + radius * 0.3f, cy - radius * 0.4f)
            lineTo(cx + radius * 0.85f, cy - radius * 0.3f)
            lineTo(cx + radius * 0.45f, cy + radius * 0.1f)
            lineTo(cx + radius * 0.65f, cy + radius * 0.6f)
            lineTo(cx, cy + radius * 0.3f)
            lineTo(cx - radius * 0.65f, cy + radius * 0.6f)
            lineTo(cx - radius * 0.45f, cy + radius * 0.1f)
            lineTo(cx - radius * 0.85f, cy - radius * 0.3f)
            lineTo(cx - radius * 0.3f, cy - radius * 0.4f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawWaveRibbon(canvas: Canvas, width: Float, y: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(0f, y)
            val segments = 8
            val segW = width / segments
            for (i in 0 until segments) {
                val startX = i * segW
                val midX = startX + segW / 2f
                val endX = startX + segW
                val amp = if (i % 2 == 0) width * 0.015f else -width * 0.015f
                quadTo(midX, y + amp, endX, y)
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSunFlower(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        for (i in 0 until 8) {
            val angle = (2 * Math.PI / 8 * i).toFloat()
            val px = cx + cos(angle.toDouble()).toFloat() * radius * 0.6f
            val py = cy + sin(angle.toDouble()).toFloat() * radius * 0.6f
            canvas.drawCircle(px, py, radius * 0.35f, paint)
        }
        val centerPaint = Paint(paint).apply { color = 0xFF78350F.toInt() }
        canvas.drawCircle(cx, cy, radius * 0.28f, centerPaint)
    }

    private fun drawLeafSprig(canvas: Canvas, cx: Float, cy: Float, size: Float, rotAngle: Float, paint: Paint) {
        canvas.save()
        canvas.rotate(rotAngle, cx, cy)
        canvas.drawLine(cx, cy + size * 0.5f, cx, cy - size * 0.5f, paint)
        canvas.drawOval(RectF(cx, cy - size * 0.4f, cx + size * 0.4f, cy - size * 0.1f), paint)
        canvas.drawOval(RectF(cx - size * 0.4f, cy - size * 0.1f, cx, cy + size * 0.2f), paint)
        canvas.restore()
    }

    private fun drawCornerArrow(canvas: Canvas, cx: Float, cy: Float, size: Float, rotAngle: Float, paint: Paint) {
        canvas.save()
        canvas.rotate(rotAngle, cx, cy)
        val path = Path().apply {
            moveTo(cx - size * 0.5f, cy - size * 0.5f)
            lineTo(cx, cy)
            lineTo(cx - size * 0.5f, cy + size * 0.5f)
            lineTo(cx - size * 0.25f, cy + size * 0.5f)
            lineTo(cx + size * 0.25f, cy)
            lineTo(cx - size * 0.25f, cy - size * 0.5f)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun drawVintageFiligree(canvas: Canvas, cx: Float, cy: Float, size: Float, rotAngle: Float, paint: Paint) {
        canvas.save()
        canvas.rotate(rotAngle, cx, cy)
        val path = Path().apply {
            moveTo(cx, cy)
            cubicTo(cx + size * 0.3f, cy, cx + size * 0.5f, cy + size * 0.2f, cx + size * 0.5f, cy + size * 0.5f)
            cubicTo(cx + size * 0.5f, cy + size * 0.3f, cx + size * 0.3f, cy + size * 0.5f, cx, cy + size * 0.5f)
        }
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun drawHollyLeaves(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        drawLeafSprig(canvas, cx, cy, radius, 30f, paint)
        val berryPaint = Paint(paint).apply { color = 0xFFDC2626.toInt() }
        canvas.drawCircle(cx - radius * 0.15f, cy, radius * 0.18f, berryPaint)
        canvas.drawCircle(cx + radius * 0.15f, cy - radius * 0.1f, radius * 0.18f, berryPaint)
        canvas.drawCircle(cx, cy + radius * 0.18f, radius * 0.18f, berryPaint)
    }

    private fun drawWeddingRings(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        canvas.drawCircle(cx - radius * 0.25f, cy, radius * 0.45f, paint)
        canvas.drawCircle(cx + radius * 0.25f, cy, radius * 0.45f, paint)
    }

    private data class FramePalette(
        val vineC: Long,
        val glowC: Long,
        val leafC: Long,
        val wisteriaC: Long,
        val hlC: Long,
        val filigreeC: Long,
        val sparkleC: Long,
        val bgC: Long
    )

    private fun drawBioluminescentBotanicalFrame(canvas: Canvas, s: Float, qrStyle: QrStyle, theme: String = "wisteria") {
        val margin = s * 0.055f

        // Theme palette configuration
        val palette = when (theme) {
            "fiery-rose" -> FramePalette(0xFFF59E0B, 0xFFF97316, 0xFFEF4444, 0xFFDC2626, 0xFFFDE047, 0xFFF59E0B, 0xFFFCD34D, 0xFF0F0404)
            "coral" -> FramePalette(0xFFFB923C, 0xFFFB7185, 0xFFF43F5E, 0xFFFB7185, 0xFFFECDD3, 0xFFFBBF24, 0xFFFEF08A, 0xFF0A0F1D)
            "electric-teal" -> FramePalette(0xFF06B6D4, 0xFF3B82F6, 0xFF22D3EE, 0xFF38BDF8, 0xFFE0F2FE, 0xFF60A5FA, 0xFF93C5FD, 0xFF030712)
            "sakura" -> FramePalette(0xFFF472B6, 0xFFE11D48, 0xFFFB7185, 0xFFF472B6, 0xFFFCE7F3, 0xFFF43F5E, 0xFFFBCFE8, 0xFF110414)
            "gold-filigree" -> FramePalette(0xFFEAB308, 0xFFD97706, 0xFFFACC15, 0xFFFDE047, 0xFFFEF9C3, 0xFFF59E0B, 0xFFFEF08A, 0xFF050505)
            "emerald-fern" -> FramePalette(0xFF10B981, 0xFF059669, 0xFF34D399, 0xFFA855F7, 0xFFE9D5FF, 0xFF6EE7B7, 0xFFFDE047, 0xFF02120A)
            "icy-crystal" -> FramePalette(0xFF38BDF8, 0xFF60A5FA, 0xFF7DD3FC, 0xFF93C5FD, 0xFFF0F9FF, 0xFFE0F2FE, 0xFFFFFFFF, 0xFF030A16)
            "rainbow" -> FramePalette(0xFFA78BFA, 0xFFF472B6, 0xFF38BDF8, 0xFFC084FC, 0xFFFDE047, 0xFF34D399, 0xFFFFFFFF, 0xFF090714)
            "bioluminescent-wisteria", "bioluminescent", "wisteria" -> FramePalette(0xFF00F0FF, 0xFF00FF87, 0xFF06B6D4, 0xFF2DD4BF, 0xFFE0F2FE, 0xFF38BDF8, 0xFF7DD3FC, 0xFF020914)
            else -> FramePalette(0xFF00F0FF, 0xFF00FF87, 0xFF06B6D4, 0xFF2DD4BF, 0xFFE0F2FE, 0xFF38BDF8, 0xFF7DD3FC, 0xFF020914)
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = qrStyle.bgColor.takeIf { it != 0 } ?: palette.bgC.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val vinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.vineC.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.005f
            strokeCap = Paint.Cap.ROUND
        }

        val glowVinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.glowC.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.007f
            strokeCap = Paint.Cap.ROUND
        }

        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.leafC.toInt()
            style = Paint.Style.FILL
        }

        val wisteriaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.wisteriaC.toInt()
            style = Paint.Style.FILL
        }

        val wisteriaHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.hlC.toInt()
            style = Paint.Style.FILL
        }

        val goldFiligreePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.filigreeC.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.004f
        }

        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.sparkleC.toInt()
            style = Paint.Style.FILL
        }

        // Outer Frame Lines
        val outerRect1 = RectF(s * 0.012f, s * 0.012f, s * 0.988f, s * 0.988f)
        val outerRect2 = RectF(s * 0.030f, s * 0.030f, s * 0.970f, s * 0.970f)
        canvas.drawRoundRect(outerRect1, s * 0.012f, s * 0.012f, vinePaint)
        canvas.drawRoundRect(outerRect2, s * 0.010f, s * 0.010f, glowVinePaint)

        // Vines & Blossoms
        val topVinePath = Path()
        val bottomVinePath = Path()
        val leftVinePath = Path()
        val rightVinePath = Path()

        val steps = 32
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = s * (0.04f + t * 0.92f)
            val wave1 = sin(t * Math.PI * 8).toFloat() * s * 0.006f
            val wave2 = cos(t * Math.PI * 6).toFloat() * s * 0.005f

            val topY = s * 0.020f + wave1
            val botY = s * 0.980f - wave1
            if (i == 0) {
                topVinePath.moveTo(x, topY)
                bottomVinePath.moveTo(x, botY)
            } else {
                topVinePath.lineTo(x, topY)
                bottomVinePath.lineTo(x, botY)
            }

            val y = s * (0.04f + t * 0.92f)
            val leftX = s * 0.020f + wave2
            val rightX = s * 0.980f - wave2
            if (i == 0) {
                leftVinePath.moveTo(leftX, y)
                rightVinePath.moveTo(rightX, y)
            } else {
                leftVinePath.lineTo(leftX, y)
                rightVinePath.lineTo(rightX, y)
            }

            if (i % 3 == 0) {
                drawMiniWisteriaCluster(canvas, x, topY + s * 0.010f, s * 0.016f, wisteriaPaint, wisteriaHighlight)
                drawTinyLeaf(canvas, x + s * 0.008f, topY - s * 0.004f, s * 0.009f, 45f, leafPaint)

                drawMiniWisteriaCluster(canvas, x, botY - s * 0.010f, s * 0.016f, wisteriaPaint, wisteriaHighlight)
                drawTinyLeaf(canvas, x - s * 0.008f, botY + s * 0.004f, s * 0.009f, 225f, leafPaint)

                drawMiniWisteriaCluster(canvas, leftX + s * 0.010f, y, s * 0.016f, wisteriaPaint, wisteriaHighlight)
                drawTinyLeaf(canvas, leftX - s * 0.004f, y + s * 0.008f, s * 0.009f, 135f, leafPaint)

                drawMiniWisteriaCluster(canvas, rightX - s * 0.010f, y, s * 0.016f, wisteriaPaint, wisteriaHighlight)
                drawTinyLeaf(canvas, rightX + s * 0.004f, y - s * 0.008f, s * 0.009f, 315f, leafPaint)
            }

            if (i % 2 == 1) {
                canvas.drawCircle(x, topY - s * 0.006f, s * 0.0025f, sparklePaint)
                canvas.drawCircle(x, botY + s * 0.006f, s * 0.0025f, sparklePaint)
                canvas.drawCircle(leftX - s * 0.006f, y, s * 0.0025f, sparklePaint)
                canvas.drawCircle(rightX + s * 0.006f, y, s * 0.0025f, sparklePaint)
            }
        }

        canvas.drawPath(topVinePath, vinePaint)
        canvas.drawPath(bottomVinePath, vinePaint)
        canvas.drawPath(leftVinePath, vinePaint)
        canvas.drawPath(rightVinePath, vinePaint)

        // Corner Clusters
        drawBotanicalCornerCluster(canvas, s * 0.030f, s * 0.030f, s * 0.022f, 0f, wisteriaPaint, wisteriaHighlight, leafPaint, goldFiligreePaint)
        drawBotanicalCornerCluster(canvas, s * 0.970f, s * 0.030f, s * 0.022f, 90f, wisteriaPaint, wisteriaHighlight, leafPaint, goldFiligreePaint)
        drawBotanicalCornerCluster(canvas, s * 0.970f, s * 0.970f, s * 0.022f, 180f, wisteriaPaint, wisteriaHighlight, leafPaint, goldFiligreePaint)
        drawBotanicalCornerCluster(canvas, s * 0.030f, s * 0.970f, s * 0.022f, 270f, wisteriaPaint, wisteriaHighlight, leafPaint, goldFiligreePaint)

        // Solid Central Void
        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = qrStyle.bgColor.takeIf { it != 0 } ?: palette.bgC.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)

        val keylinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = s * 0.002f
        }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), keylinePaint)
    }

    private data class Tuple8<A, B, C, D, E, F, G, H>(val _1: Long, val _2: Long, val _3: Long, val _4: Long, val _5: Long, val _6: Long, val _7: Long, val _8: Long)

    private fun drawRoyalCrownFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        // Black marble background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0A0A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Subtle gold marble veins
        val veinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33D97706
            style = Paint.Style.STROKE
            strokeWidth = s * 0.003f
        }
        val veinPath = Path().apply {
            moveTo(0f, s * 0.3f)
            cubicTo(s * 0.2f, s * 0.1f, s * 0.5f, s * 0.4f, s, s * 0.2f)
            moveTo(0f, s * 0.8f)
            cubicTo(s * 0.4f, s * 0.9f, s * 0.7f, s * 0.6f, s, s * 0.85f)
        }
        canvas.drawPath(veinPath, veinPaint)

        // Gold border keylines
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }
        canvas.drawRoundRect(RectF(s * 0.015f, s * 0.015f, s * 0.985f, s * 0.985f), s * 0.03f, s * 0.03f, goldPaint)
        canvas.drawRoundRect(RectF(s * 0.035f, s * 0.035f, s * 0.965f, s * 0.965f), s * 0.02f, s * 0.02f, goldPaint)

        // Crown emblem at top center
        val crownFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFBBF24.toInt(); style = Paint.Style.FILL }
        val crownPath = Path().apply {
            val cx = s / 2f
            val cy = s * 0.035f
            val cw = s * 0.06f
            val ch = s * 0.022f
            moveTo(cx - cw, cy + ch)
            lineTo(cx - cw, cy - ch * 0.5f)
            lineTo(cx - cw * 0.5f, cy + ch * 0.2f)
            lineTo(cx, cy - ch)
            lineTo(cx + cw * 0.5f, cy + ch * 0.2f)
            lineTo(cx + cw, cy - ch * 0.5f)
            lineTo(cx + cw, cy + ch)
            close()
        }
        canvas.drawPath(crownPath, crownFill)

        // Central Void
        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0A0A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawFluidNeonFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF06060E.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Fluid neon border paths
        val neonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.012f
            shader = LinearGradient(0f, 0f, s, s, intArrayOf(0xFFFF6B00.toInt(), 0xFFD946EF.toInt(), 0xFF06B6D4.toInt(), 0xFF8B5CF6.toInt()), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(s * 0.015f, s * 0.015f, s * 0.985f, s * 0.985f), s * 0.05f, s * 0.05f, neonPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF06060E.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawCyberCircuitFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF040914.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00F0FF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }
        val fillCyan = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.FILL }

        // Corner tech brackets
        val cLen = s * 0.08f
        val bi = s * 0.02f
        canvas.drawLine(bi, bi + cLen, bi, bi, cyanPaint)
        canvas.drawLine(bi, bi, bi + cLen, bi, cyanPaint)
        canvas.drawLine(s - bi - cLen, bi, s - bi, bi, cyanPaint)
        canvas.drawLine(s - bi, bi, s - bi, bi + cLen, cyanPaint)
        canvas.drawLine(bi, s - bi - cLen, bi, s - bi, cyanPaint)
        canvas.drawLine(bi, s - bi, bi + cLen, s - bi, cyanPaint)
        canvas.drawLine(s - bi - cLen, s - bi, s - bi, s - bi, cyanPaint)
        canvas.drawLine(s - bi, s - bi - cLen, s - bi, s - bi, cyanPaint)

        // Solder dots
        canvas.drawCircle(bi + cLen, bi, s * 0.005f, fillCyan)
        canvas.drawCircle(s - bi - cLen, bi, s * 0.005f, fillCyan)
        canvas.drawCircle(bi + cLen, s - bi, s * 0.005f, fillCyan)
        canvas.drawCircle(s - bi - cLen, s - bi, s * 0.005f, fillCyan)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF040914.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawAlpineMountainFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, 0xFF0284C7.toInt(), 0xFF064E3B.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, skyPaint)

        // Pine trees silhouette along bottom
        val treePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF022C22.toInt(); style = Paint.Style.FILL }
        val steps = 16
        for (i in 0 until steps) {
            val tx = s * (i.toFloat() / steps)
            val tw = s * 0.04f
            val th = s * 0.06f
            val treePath = Path().apply {
                moveTo(tx, s)
                lineTo(tx + tw / 2f, s - th)
                lineTo(tx + tw, s)
                close()
            }
            canvas.drawPath(treePath, treePaint)
        }

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF064E3B.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawGlassLiquidBubblesFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF8FAFC.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, cardPaint)

        // Translucent blue/purple glass bubble spheres
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAA2563EB.toInt()
            style = Paint.Style.FILL
        }
        val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEEFFFFFF.toInt(); style = Paint.Style.FILL }

        val bPositions = listOf(
            Pair(s * 0.03f, s * 0.03f), Pair(s * 0.97f, s * 0.03f),
            Pair(s * 0.03f, s * 0.97f), Pair(s * 0.97f, s * 0.97f),
            Pair(s * 0.5f, s * 0.02f), Pair(s * 0.5f, s * 0.98f)
        )
        for ((bx, by) in bPositions) {
            canvas.drawCircle(bx, by, s * 0.025f, bubblePaint)
            canvas.drawCircle(bx - s * 0.008f, by - s * 0.008f, s * 0.008f, hlPaint)
        }

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawZebraStripeFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF09090B.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }
        for (i in 0..12) {
            val offset = s * (i / 12f)
            canvas.drawLine(0f, offset, offset, 0f, stripePaint)
            canvas.drawLine(s - offset, s, s, s - offset, stripePaint)
        }

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawModernBracketFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1F5F9.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.010f
            strokeCap = Paint.Cap.ROUND
        }
        val bLen = s * 0.08f
        val bi = s * 0.02f
        canvas.drawLine(bi, bi + bLen, bi, bi, bracketPaint)
        canvas.drawLine(bi, bi, bi + bLen, bi, bracketPaint)
        canvas.drawLine(s - bi - bLen, bi, s - bi, bi, bracketPaint)
        canvas.drawLine(s - bi, bi, s - bi, bi + bLen, bracketPaint)
        canvas.drawLine(bi, s - bi - bLen, bi, s - bi, bracketPaint)
        canvas.drawLine(bi, s - bi, bi + bLen, s - bi, bracketPaint)
        canvas.drawLine(s - bi - bLen, s - bi, s - bi, s - bi, bracketPaint)
        canvas.drawLine(s - bi, s - bi - bLen, s - bi, s - bi, bracketPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawLushLeavesFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF02120A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10B981.toInt(); style = Paint.Style.FILL }
        val steps = 20
        for (i in 0 until steps) {
            val t = i.toFloat() / steps
            val pos = s * t
            drawTinyLeaf(canvas, pos, s * 0.02f, s * 0.015f, 90f, leafPaint)
            drawTinyLeaf(canvas, pos, s * 0.98f, s * 0.015f, 270f, leafPaint)
            drawTinyLeaf(canvas, s * 0.02f, pos, s * 0.015f, 0f, leafPaint)
            drawTinyLeaf(canvas, s * 0.98f, pos, s * 0.015f, 180f, leafPaint)
        }

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF02120A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawCosmicNebulaFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val spacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s / 2f, s / 2f, s * 0.7f, 0xFF7928CA.toInt(), 0xFF0A0A14.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, spacePaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC084FC.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }
        canvas.drawCircle(s / 2f, s / 2f, s * 0.47f, ringPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0A14.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawRedMatrixFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF050505.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEF4444.toInt(); style = Paint.Style.FILL }
        val redLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEF4444.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }

        val bi = s * 0.02f
        val bLen = s * 0.08f
        canvas.drawLine(bi, bi, bi + bLen, bi, redLine)
        canvas.drawLine(bi, bi, bi, bi + bLen, redLine)
        canvas.drawLine(s - bi, bi, s - bi - bLen, bi, redLine)
        canvas.drawLine(s - bi, bi, s - bi, bi + bLen, redLine)
        canvas.drawLine(bi, s - bi, bi + bLen, s - bi, redLine)
        canvas.drawLine(bi, s - bi, bi, s - bi - bLen, redLine)
        canvas.drawLine(s - bi, s - bi, s - bi - bLen, s - bi, redLine)
        canvas.drawLine(s - bi, s - bi, s - bi, s - bi - bLen, redLine)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF050505.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawArmorSteelFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val steelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF334155.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, steelPaint)

        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF94A3B8.toInt(); style = Paint.Style.FILL }
        val rivets = listOf(Pair(s * 0.03f, s * 0.03f), Pair(s * 0.97f, s * 0.03f), Pair(s * 0.03f, s * 0.97f), Pair(s * 0.97f, s * 0.97f))
        for ((rx, ry) in rivets) {
            canvas.drawCircle(rx, ry, s * 0.012f, boltPaint)
        }

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawPaintSplashFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E1B4B.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val splash1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF06B6D4.toInt(); style = Paint.Style.FILL }
        val splash2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD946EF.toInt(); style = Paint.Style.FILL }

        canvas.drawCircle(s * 0.05f, s * 0.05f, s * 0.08f, splash1)
        canvas.drawCircle(s * 0.95f, s * 0.05f, s * 0.08f, splash2)
        canvas.drawCircle(s * 0.05f, s * 0.95f, s * 0.08f, splash2)
        canvas.drawCircle(s * 0.95f, s * 0.95f, s * 0.08f, splash1)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawSunsetPalmFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val sunsetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, 0xFF4C1D95.toInt(), 0xFFEA580C.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, sunsetPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF18181B.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawNeonVortexFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF09090B.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val vortexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
            shader = SweepGradient(s / 2f, s / 2f, intArrayOf(0xFF00F0FF.toInt(), 0xFFA855F7.toInt(), 0xFFF43F5E.toInt(), 0xFF00F0FF.toInt()), null)
        }
        canvas.drawCircle(s / 2f, s / 2f, s * 0.47f, vortexPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF09090B.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawMulticolorGeoFrame(canvas: Canvas, s: Float, qrStyle: QrStyle) {
        val margin = s * 0.055f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val colors = listOf(0xFF0D9488, 0xFFEA580C, 0xFFE11D48, 0xFF7C3AED)
        for (i in 0 until 4) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors[i].toInt(); style = Paint.Style.FILL }
            val rect = when (i) {
                0 -> RectF(0f, 0f, s, s * 0.04f)
                1 -> RectF(0f, s * 0.96f, s, s)
                2 -> RectF(0f, 0f, s * 0.04f, s)
                else -> RectF(s * 0.96f, 0f, s, s)
            }
            canvas.drawRect(rect, p)
        }

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawMiniWisteriaCluster(canvas: Canvas, cx: Float, cy: Float, size: Float, mainPaint: Paint, hlPaint: Paint) {
        val r = size * 0.35f
        canvas.drawCircle(cx, cy, r, mainPaint)
        canvas.drawCircle(cx - r * 0.4f, cy + r * 0.5f, r * 0.7f, mainPaint)
        canvas.drawCircle(cx + r * 0.4f, cy + r * 0.5f, r * 0.7f, mainPaint)
        canvas.drawCircle(cx, cy + r * 1.0f, r * 0.5f, mainPaint)
        canvas.drawCircle(cx, cy, r * 0.35f, hlPaint)
    }

    private fun drawTinyLeaf(canvas: Canvas, cx: Float, cy: Float, size: Float, angleDeg: Float, paint: Paint) {
        canvas.save()
        canvas.rotate(angleDeg, cx, cy)
        val path = Path().apply {
            moveTo(cx, cy - size)
            quadTo(cx + size * 0.6f, cy, cx, cy + size)
            quadTo(cx - size * 0.6f, cy, cx, cy - size)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun drawBotanicalCornerCluster(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        rotDeg: Float,
        wisteriaP: Paint,
        hlP: Paint,
        leafP: Paint,
        filigreeP: Paint
    ) {
        canvas.save()
        canvas.rotate(rotDeg, cx, cy)
        val arcRect = RectF(cx - size, cy - size, cx + size, cy + size)
        canvas.drawArc(arcRect, 0f, 90f, false, filigreeP)

        drawMiniWisteriaCluster(canvas, cx, cy, size * 0.8f, wisteriaP, hlP)
        drawTinyLeaf(canvas, cx + size * 0.5f, cy, size * 0.5f, 30f, leafP)
        drawTinyLeaf(canvas, cx, cy + size * 0.5f, size * 0.5f, 60f, leafP)
        canvas.restore()
    }
}
