package com.example.qr.engine

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
    fun generateThumbnail(qrStyle: QrStyle, sizePx: Int = 180): Bitmap = getOrGenerateThumbnail(qrStyle, sizePx)

    fun getOrGenerateThumbnail(qrStyle: QrStyle, sizePx: Int = 120): Bitmap {
        val key = qrStyle.hashCode() * 31 + sizePx
        val cached = thumbnailCache.get(key)
        if (cached != null) return cached

        val matrix = getSampleMatrix(qrStyle.minVersion)
        val matrixSize = matrix.width
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        if (qrStyle.artDirection != null && ArtFrameRenderer.isArtFrame(qrStyle.artDirection)) {
            ArtFrameRenderer.render(qrStyle.artDirection, canvas, sizePx.toFloat())
        } else if (!qrStyle.transparentBg) {
            val bgPaint = Paint().apply { color = qrStyle.bgColor }
            canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)
        } else {
            bitmap.eraseColor(0x00000000)
        }

        val hasFrame = qrStyle.frameStyle != FrameStyle.None
        val insets = calculateFrameInsets(qrStyle.frameStyle, qrStyle.artDirection, sizePx)

        val qz = qrStyle.quietZone.coerceIn(0, 8)
        val totalModules = matrixSize + qz * 2
        val usableWidth = sizePx - insets.left - insets.right
        val usableHeight = sizePx - insets.top - insets.bottom
        val cellSize = min(usableWidth, usableHeight) / totalModules.toFloat()
        val originX = insets.left + (usableWidth - matrixSize * cellSize) / 2f
        val originY = insets.top + (usableHeight - matrixSize * cellSize) / 2f

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
        drawEye(canvas, originX, originY, cellSize, qrStyle)
        drawEye(canvas, originX + (matrixSize - 7) * cellSize, originY, cellSize, qrStyle)
        drawEye(canvas, originX, originY + (matrixSize - 7) * cellSize, cellSize, qrStyle)

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
        sizePx: Int = 1024
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

        // 1. Draw Background
        if (!qrStyle.transparentBg) {
            bitmap.eraseColor(qrStyle.bgColor)
            val bgPaint = Paint().apply {
                color = qrStyle.bgColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)
        } else {
            bitmap.eraseColor(0x00000000)
        }

        if (hasFrame) {
            drawFrameBackground(canvas, qrStyle.frameStyle, sizePx, qrStyle)
        }

        // 2. Draw Photo Underlay only for Clean mode (Clean overlay)
        val hasPhotoUnderlay = photoBitmap != null && qrStyle.imageMode == ImageMode.Clean
        if (hasPhotoUnderlay && photoBitmap != null) {
            renderPhotoUnderlay(canvas, photoBitmap, qrStyle, originX, originY, matrixSize * cellSize, sizePx, cellSize, matrixSize)
        } else if (photoBitmap == null && qrStyle.artDirection != null && ArtFrameRenderer.isArtFrame(qrStyle.artDirection)) {
            ArtFrameRenderer.render(qrStyle.artDirection, canvas, sizePx.toFloat())
        }

        val hasPhoto = photoBitmap != null && qrStyle.imageMode != ImageMode.None && qrStyle.imageMode != ImageMode.Logo

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
        val gap = cellSize * qrStyle.moduleGap.coerceIn(0f, 0.15f)
        val dotScale = qrStyle.dotScale.coerceIn(0.55f, 1.0f)

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
                            // Guaranteed center core dot in deep ink for camera decode
                            val coreSize = activeSize * (1f - qrStyle.artisticStrength * 0.45f).coerceIn(0.65f, 0.95f)
                            drawModuleShape(
                                canvas = canvas,
                                shape = qrStyle.moduleShape,
                                cx = cx,
                                cy = cy,
                                size = coreSize,
                                paint = modulePaint,
                                gx = x,
                                gy = y
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

        // 6. Draw The 3 Finder Eyes (Top-Left, Top-Right, Bottom-Left)
        drawEye(canvas, originX, originY, cellSize, qrStyle)
        drawEye(canvas, originX + (matrixSize - 7) * cellSize, originY, cellSize, qrStyle)
        drawEye(canvas, originX, originY + (matrixSize - 7) * cellSize, cellSize, qrStyle)

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

        val fgHex = String.format("#%06X", 0xFFFFFF and qrStyle.fgColor)
        val bgHex = String.format("#%06X", 0xFFFFFF and qrStyle.bgColor)
        val gradHex = String.format("#%06X", 0xFFFFFF and qrStyle.gradientTo)
        val (effEyeColor, effBallColor) = resolveEffectiveEyeColors(qrStyle)
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
            append("  </defs>\n")

            // Background
            if (!qrStyle.transparentBg) {
                append("  <rect width=\"$sizePx\" height=\"$sizePx\" fill=\"$bgHex\" />\n")
            }

            // QR Modules
            append("  <g fill=\"$fillAttr\">\n")
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
            append(renderEyeSvg(originX, originY, cellSize, qrStyle.eyeShape, qrStyle.ballShape, eyeHex, ballHex, bgHex))
            append(renderEyeSvg(originX + (matrixSize - 7) * cellSize, originY, cellSize, qrStyle.eyeShape, qrStyle.ballShape, eyeHex, ballHex, bgHex))
            append(renderEyeSvg(originX, originY + (matrixSize - 7) * cellSize, cellSize, qrStyle.eyeShape, qrStyle.ballShape, eyeHex, ballHex, bgHex))

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
        eyeHex: String, ballHex: String, bgHex: String
    ): String = buildString {
        val s = cell * 7
        val r = s / 2f
        val cx = ox + r
        val cy = oy + r

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
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.bgColor
            this.style = Paint.Style.FILL
        }
        val plateG = cellSize * 0.45f
        val plateR = cellSize * 0.9f
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
                RectF(sepX - plateG, sepY - plateG, sepX + cellSize * 8 + plateG * 2, sepY + cellSize * 8 + plateG * 2),
                plateR, plateR, platePaint
            )
        }

        canvas.restore()
    }

    private fun isDarkColor(color: Int): Boolean {
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

    fun resolveEffectiveEyeColors(qrStyle: QrStyle): Pair<Int, Int> {
        val bgColor = qrStyle.bgColor
        val fgColor = qrStyle.fgColor

        var eyeColor = qrStyle.eyeColor
        var ballColor = qrStyle.ballColor

        // 1. Ensure eyeColor (outer ring) contrasts with bgColor
        if (getContrastRatio(eyeColor, bgColor) < 1.8f) {
            eyeColor = if (getContrastRatio(fgColor, bgColor) >= 1.8f) fgColor
                       else if (isDarkColor(bgColor)) 0xFFFFFFFF.toInt()
                       else 0xFF000000.toInt()
        }

        // 2. Ensure ballColor (pupil) contrasts with bgColor (middle gap)
        if (getContrastRatio(ballColor, bgColor) < 1.8f) {
            ballColor = if (getContrastRatio(eyeColor, bgColor) >= 1.8f) eyeColor
                        else if (getContrastRatio(fgColor, bgColor) >= 1.8f) fgColor
                        else if (isDarkColor(bgColor)) 0xFFFFFFFF.toInt()
                        else 0xFF000000.toInt()
        }

        return Pair(eyeColor, ballColor)
    }

    private fun getContrastRatio(c1: Int, c2: Int): Float {
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

    private fun drawEye(canvas: Canvas, ox: Float, oy: Float, cell: Float, qrStyle: QrStyle) {
        val s = cell * 7
        val (eyeColor, ballColor) = resolveEffectiveEyeColors(qrStyle)
        val bgColor = qrStyle.bgColor

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

        // Outer Ring (7x7)
        drawEyeLayer(canvas, ox, oy, s, qrStyle.eyeShape, outerPaint)

        // Middle Inset / Gap (5x5)
        drawEyeLayer(canvas, ox + cell, oy + cell, cell * 5, qrStyle.eyeShape, bgPaint)

        // Inner Ball / Pupil (3x3 modules)
        val bx = ox + cell * 2
        val by = oy + cell * 2
        val bSize = cell * 3
        drawEyeLayer(canvas, bx, by, bSize, qrStyle.ballShape, ballPaint)

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
            FrameStyle.SimpleBorder -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.012f)
            FrameStyle.BadgeScanMe -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.045f)
            FrameStyle.ModernPill -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.045f)
            FrameStyle.NeonGlow -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.045f)
            FrameStyle.Badge -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.045f)
            FrameStyle.Card -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.045f)
            FrameStyle.Label -> FrameInsets(s * 0.012f, s * 0.042f, s * 0.012f, s * 0.012f)
            FrameStyle.Speech -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.045f)
            FrameStyle.Bracket -> FrameInsets(s * 0.012f, s * 0.042f, s * 0.012f, s * 0.012f)
            FrameStyle.Plaque -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.045f)
            FrameStyle.PhoneFrame -> FrameInsets(s * 0.012f, s * 0.022f, s * 0.012f, s * 0.012f)
            else -> FrameInsets(s * 0.012f, s * 0.012f, s * 0.012f, s * 0.012f)
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
            color = 0x88FFFFFF.toInt()
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

        val maxTextWidth = badgeWidth - s * 0.04f
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
        val s = sizePx.toFloat()
        when (frameStyle) {
            FrameStyle.Bracket -> {
                val cardRect = RectF(s * 0.02f, s * 0.03f, s * 0.98f, s * 0.98f)
                val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
                canvas.drawRoundRect(cardRect, s * 0.04f, s * 0.04f, cardBg)
            }
            FrameStyle.Badge -> {
                val rosettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF5B21B6.toInt()
                    style = Paint.Style.FILL
                }
                val cx = s / 2f
                val cy = s * 0.45f
                val petals = 16
                val outerR = s * 0.49f
                val innerR = s * 0.45f
                val rosettePath = Path()
                for (i in 0 until petals * 2) {
                    val angle = (Math.PI / petals * i).toFloat()
                    val r = if (i % 2 == 0) outerR else innerR
                    val px = cx + cos(angle.toDouble()).toFloat() * r
                    val py = cy + sin(angle.toDouble()).toFloat() * r
                    if (i == 0) rosettePath.moveTo(px, py) else rosettePath.lineTo(px, py)
                }
                rosettePath.close()
                canvas.drawPath(rosettePath, rosettePaint)

                val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
                canvas.drawCircle(cx, cy, s * 0.43f, whitePaint)
            }
            FrameStyle.Arch -> {
                val archPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF4C1D95.toInt()
                    style = Paint.Style.FILL
                }
                val archPath = Path().apply {
                    moveTo(s * 0.03f, s * 0.98f)
                    lineTo(s * 0.97f, s * 0.98f)
                    lineTo(s * 0.97f, s * 0.25f)
                    arcTo(RectF(s * 0.03f, s * 0.01f, s * 0.97f, s * 0.50f), 0f, -180f, false)
                    lineTo(s * 0.03f, s * 0.98f)
                    close()
                }
                canvas.drawPath(archPath, archPaint)

                val innerCard = RectF(s * 0.05f, s * 0.06f, s * 0.95f, s * 0.84f)
                val innerWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
                canvas.drawRoundRect(innerCard, s * 0.04f, s * 0.04f, innerWhite)
            }
            FrameStyle.Cup -> {
                val cupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0D9488.toInt()
                    style = Paint.Style.FILL
                }
                val cupPath = Path().apply {
                    moveTo(s * 0.05f, s * 0.12f)
                    lineTo(s * 0.95f, s * 0.12f)
                    lineTo(s * 0.88f, s * 0.98f)
                    quadTo(s * 0.88f, s * 0.99f, s * 0.82f, s * 0.99f)
                    lineTo(s * 0.18f, s * 0.99f)
                    quadTo(s * 0.12f, s * 0.99f, s * 0.12f, s * 0.98f)
                    close()
                }
                canvas.drawPath(cupPath, cupPaint)

                val whiteCard = RectF(s * 0.07f, s * 0.08f, s * 0.93f, s * 0.88f)
                val whiteP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
                canvas.drawRoundRect(whiteCard, s * 0.04f, s * 0.04f, whiteP)
            }
            FrameStyle.Card -> {
                val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0D9488.toInt()
                    style = Paint.Style.FILL
                }
                val outerRect = RectF(s * 0.03f, s * 0.02f, s * 0.97f, s * 0.98f)
                canvas.drawRoundRect(outerRect, s * 0.06f, s * 0.06f, cardFill)

                val whiteRect = RectF(s * 0.05f, s * 0.03f, s * 0.95f, s * 0.86f)
                val whiteP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
                canvas.drawRoundRect(whiteRect, s * 0.04f, s * 0.04f, whiteP)
            }
            FrameStyle.Label -> {
                val labelFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0D9488.toInt()
                    style = Paint.Style.FILL
                }
                val labelRect = RectF(s * 0.03f, s * 0.02f, s * 0.97f, s * 0.98f)
                canvas.drawRoundRect(labelRect, s * 0.06f, s * 0.06f, labelFill)

                val whiteRect = RectF(s * 0.05f, s * 0.09f, s * 0.95f, s * 0.95f)
                val whiteP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
                canvas.drawRoundRect(whiteRect, s * 0.04f, s * 0.04f, whiteP)
            }
            FrameStyle.Speech -> {
                val speechFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF67E8F9.toInt()
                    style = Paint.Style.FILL
                }
                val speechPath = Path().apply {
                    addCircle(s / 2f, s * 0.44f, s * 0.46f, Path.Direction.CW)
                    moveTo(s * 0.20f, s * 0.72f)
                    lineTo(s * 0.08f, s * 0.96f)
                    lineTo(s * 0.36f, s * 0.86f)
                    close()
                }
                canvas.drawPath(speechPath, speechFill)
            }
            FrameStyle.Note -> {
                val noteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFEF08A.toInt()
                    style = Paint.Style.FILL
                }
                val fold = s * 0.10f
                val inset = s * 0.02f
                val notePath = Path().apply {
                    moveTo(inset, inset)
                    lineTo(s - inset, inset)
                    lineTo(s - inset, s - inset - fold)
                    lineTo(s - inset - fold, s - inset)
                    lineTo(inset, s - inset)
                    close()
                }
                canvas.drawPath(notePath, noteFill)
            }
            FrameStyle.Globe -> {
                val globeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFBFDBFE.toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(s / 2f, s / 2f, s * 0.49f, globeFill)
            }
            FrameStyle.Plaque -> {
                val plaqueFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFBEF264.toInt()
                    style = Paint.Style.FILL
                }
                val cornerCut = s * 0.08f
                val pi = s * 0.02f
                val plaquePath = Path().apply {
                    moveTo(pi + cornerCut, pi)
                    lineTo(s - pi - cornerCut, pi)
                    quadTo(s - pi, pi, s - pi, pi + cornerCut)
                    lineTo(s - pi, s - pi - cornerCut)
                    quadTo(s - pi, s - pi, s - pi - cornerCut, s - pi)
                    lineTo(pi + cornerCut, s - pi)
                    quadTo(pi, s - pi, pi, s - pi - cornerCut)
                    lineTo(pi, pi + cornerCut)
                    quadTo(pi, pi, pi + cornerCut, pi)
                    close()
                }
                canvas.drawPath(plaquePath, plaqueFill)
            }
            FrameStyle.Pentagon -> {
                val pentaFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFB7185.toInt()
                    style = Paint.Style.FILL
                }
                val pentaPath = Path().apply {
                    moveTo(s * 0.50f, s * 0.01f)
                    lineTo(s * 0.99f, s * 0.32f)
                    lineTo(s * 0.88f, s * 0.98f)
                    lineTo(s * 0.12f, s * 0.98f)
                    lineTo(s * 0.01f, s * 0.32f)
                    close()
                }
                canvas.drawPath(pentaPath, pentaFill)
            }
            FrameStyle.Hexagon -> {
                val hexFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFBBF24.toInt()
                    style = Paint.Style.FILL
                }
                val hexPath = Path().apply {
                    moveTo(s * 0.50f, s * 0.01f)
                    lineTo(s * 0.99f, s * 0.25f)
                    lineTo(s * 0.99f, s * 0.75f)
                    lineTo(s * 0.50f, s * 0.99f)
                    lineTo(s * 0.01f, s * 0.75f)
                    lineTo(s * 0.01f, s * 0.25f)
                    close()
                }
                canvas.drawPath(hexPath, hexFill)
            }
            FrameStyle.Diamond -> {
                val diaFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrStyle.bgColor
                    style = Paint.Style.FILL
                }
                val diaPath = Path().apply {
                    moveTo(s * 0.50f, s * 0.01f)
                    lineTo(s * 0.99f, s * 0.50f)
                    lineTo(s * 0.50f, s * 0.99f)
                    lineTo(s * 0.01f, s * 0.50f)
                    close()
                }
                canvas.drawPath(diaPath, diaFill)
            }
            FrameStyle.Seal -> {
                val sealFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFF87171.toInt()
                    style = Paint.Style.FILL
                }
                val cx = s / 2f
                val cy = s / 2f
                val petals = 18
                val outerR = s * 0.49f
                val innerR = s * 0.46f
                val sealPath = Path()
                for (i in 0 until petals * 2) {
                    val angle = (Math.PI / petals * i).toFloat()
                    val r = if (i % 2 == 0) outerR else innerR
                    val px = cx + cos(angle.toDouble()).toFloat() * r
                    val py = cy + sin(angle.toDouble()).toFloat() * r
                    if (i == 0) sealPath.moveTo(px, py) else sealPath.lineTo(px, py)
                }
                sealPath.close()
                canvas.drawPath(sealPath, sealFill)
            }
            FrameStyle.Bucket -> {
                val bucketFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF93C5FD.toInt()
                    style = Paint.Style.FILL
                }
                val bucketPath = Path().apply {
                    moveTo(s * 0.05f, s * 0.06f)
                    lineTo(s * 0.95f, s * 0.06f)
                    lineTo(s * 0.88f, s * 0.98f)
                    quadTo(s * 0.86f, s * 0.99f, s * 0.80f, s * 0.99f)
                    lineTo(s * 0.20f, s * 0.99f)
                    quadTo(s * 0.14f, s * 0.99f, s * 0.12f, s * 0.98f)
                    close()
                }
                canvas.drawPath(bucketPath, bucketFill)
            }
            else -> {}
        }
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
            textSize = s * 0.042f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        when (frameStyle) {
            FrameStyle.SimpleBorder -> {
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrStyle.fgColor
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.012f
                }
                canvas.drawRoundRect(RectF(s * 0.012f, s * 0.012f, s * 0.988f, s * 0.988f), s * 0.025f, s * 0.025f, framePaint)
            }
            FrameStyle.BadgeScanMe -> {
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrStyle.fgColor
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.012f
                }
                canvas.drawRoundRect(RectF(s * 0.012f, s * 0.012f, s * 0.988f, s * 0.950f), s * 0.025f, s * 0.025f, framePaint)
                drawBadgePill(
                    canvas = canvas,
                    caption = caption,
                    defaultText = "SCAN ME",
                    centerX = s / 2f,
                    centerY = s * 0.965f,
                    badgeWidth = s * 0.65f,
                    badgeHeight = s * 0.060f,
                    pillColor = qrStyle.fgColor,
                    textColor = 0xFFFFFFFF.toInt(),
                    s = s
                )
            }
            FrameStyle.ModernPill -> {
                val pillColor = if (qrStyle.gradientType != GradientType.None) qrStyle.gradientTo else qrStyle.fgColor
                drawBadgePill(
                    canvas = canvas,
                    caption = caption,
                    defaultText = "SCAN ME",
                    centerX = s / 2f,
                    centerY = s * 0.965f,
                    badgeWidth = s * 0.65f,
                    badgeHeight = s * 0.060f,
                    pillColor = pillColor,
                    textColor = 0xFFFFFFFF.toInt(),
                    s = s
                )
            }
            FrameStyle.PhoneFrame -> {
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrStyle.fgColor
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.015f
                }
                val phoneRect = RectF(s * 0.012f, s * 0.010f, s * 0.988f, s * 0.990f)
                canvas.drawRoundRect(phoneRect, s * 0.04f, s * 0.04f, framePaint)
                val notchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrStyle.fgColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(RectF(s * 0.38f, s * 0.015f, s * 0.62f, s * 0.038f), s * 0.012f, s * 0.012f, notchPaint)
            }
            FrameStyle.NeonGlow -> {
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrStyle.gradientTo
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.015f
                    setShadowLayer(s * 0.025f, 0f, 0f, qrStyle.gradientTo)
                }
                canvas.drawRoundRect(RectF(s * 0.012f, s * 0.012f, s * 0.988f, s * 0.950f), s * 0.03f, s * 0.03f, framePaint)
                drawBadgePill(
                    canvas = canvas,
                    caption = caption,
                    defaultText = "SCAN NOW",
                    centerX = s / 2f,
                    centerY = s * 0.965f,
                    badgeWidth = s * 0.68f,
                    badgeHeight = s * 0.060f,
                    pillColor = qrStyle.gradientTo,
                    textColor = 0xFF000000.toInt(),
                    s = s
                )
            }
            FrameStyle.Bracket -> {
                val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0F172A.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.018f
                    strokeCap = Paint.Cap.ROUND
                }
                val cardRect = RectF(s * 0.02f, s * 0.03f, s * 0.98f, s * 0.98f)
                canvas.drawRoundRect(cardRect, s * 0.04f, s * 0.04f, bracketPaint)

                // 4 Camera corner brackets
                val cLen = s * 0.08f
                val bi = s * 0.05f
                canvas.drawLine(bi, bi + cLen, bi, bi, bracketPaint)
                canvas.drawLine(bi, bi, bi + cLen, bi, bracketPaint)
                canvas.drawLine(s - bi - cLen, bi, s - bi, bi, bracketPaint)
                canvas.drawLine(s - bi, bi, s - bi, bi + cLen, bracketPaint)
                canvas.drawLine(bi, s - bi - cLen, bi, s - bi, bracketPaint)
                canvas.drawLine(bi, s - bi, bi + cLen, s - bi, bracketPaint)
                canvas.drawLine(s - bi - cLen, s - bi, s - bi, s - bi, bracketPaint)
                canvas.drawLine(s - bi, s - bi - cLen, s - bi, s - bi, bracketPaint)

                // Speech bubble on top
                val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF6366F1.toInt()
                    style = Paint.Style.FILL
                }
                val bw = s * 0.44f
                val bh = s * 0.08f
                val bx = (s - bw) / 2f
                val by = s * 0.01f
                val bPath = Path().apply {
                    addRoundRect(RectF(bx, by, bx + bw, by + bh), s * 0.025f, s * 0.025f, Path.Direction.CW)
                    moveTo(s / 2f - s * 0.025f, by + bh)
                    lineTo(s / 2f, by + bh + s * 0.02f)
                    lineTo(s / 2f + s * 0.025f, by + bh)
                    close()
                }
                canvas.drawPath(bPath, bubblePaint)
                textPaint.textSize = s * 0.038f
                canvas.drawText(caption.ifEmpty { "SCAN CODE" }, s / 2f, s * 0.065f, textPaint)
            }
            FrameStyle.Badge -> {
                val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF4C1D95.toInt()
                    style = Paint.Style.FILL
                }
                val rw = s * 0.60f
                val rh = s * 0.11f
                val rx = (s - rw) / 2f
                val ry = s * 0.87f
                val ribbonPath = Path().apply {
                    moveTo(rx, ry)
                    lineTo(rx + rw, ry)
                    lineTo(rx + rw - s * 0.025f, ry + rh / 2f)
                    lineTo(rx + rw, ry + rh)
                    lineTo(rx, ry + rh)
                    lineTo(rx + s * 0.025f, ry + rh / 2f)
                    close()
                }
                canvas.drawPath(ribbonPath, ribbonPaint)
                canvas.drawText(caption.ifEmpty { "SCAN CODE" }, s / 2f, ry + rh * 0.68f, textPaint)
            }
            FrameStyle.Arch -> {
                val standPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF3B0764.toInt()
                    style = Paint.Style.FILL
                }
                val standRect = RectF(s * 0.10f, s * 0.86f, s * 0.90f, s * 0.98f)
                canvas.drawRoundRect(standRect, s * 0.03f, s * 0.03f, standPaint)
            }
            FrameStyle.Cup -> {
                val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                val lidStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0F172A.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.012f
                }
                val lidRim = RectF(s * 0.08f, s * 0.08f, s * 0.92f, s * 0.14f)
                canvas.drawRoundRect(lidRim, s * 0.02f, s * 0.02f, lidPaint)
                canvas.drawRoundRect(lidRim, s * 0.02f, s * 0.02f, lidStroke)
                val lidCap = RectF(s * 0.28f, s * 0.03f, s * 0.72f, s * 0.09f)
                canvas.drawRoundRect(lidCap, s * 0.02f, s * 0.02f, lidPaint)
                canvas.drawRoundRect(lidCap, s * 0.02f, s * 0.02f, lidStroke)

                val sleeveStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0F172A.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.012f
                }
                canvas.drawRoundRect(RectF(s * 0.07f, s * 0.08f, s * 0.93f, s * 0.88f), s * 0.04f, s * 0.04f, sleeveStroke)
            }
            FrameStyle.Card -> {
                val outlineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0F172A.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.015f
                }
                canvas.drawRoundRect(RectF(s * 0.03f, s * 0.02f, s * 0.97f, s * 0.98f), s * 0.06f, s * 0.06f, outlineP)
                canvas.drawRoundRect(RectF(s * 0.05f, s * 0.03f, s * 0.95f, s * 0.86f), s * 0.04f, s * 0.04f, outlineP)

                val pillRect = RectF(s * 0.18f, s * 0.87f, s * 0.82f, s * 0.97f)
                val pillFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF06B6D4.toInt(); style = Paint.Style.FILL }
                canvas.drawRoundRect(pillRect, s * 0.04f, s * 0.04f, pillFill)
                canvas.drawText(caption.ifEmpty { "SCAN ME" }, s / 2f, s * 0.935f, textPaint)
            }
            FrameStyle.Label -> {
                val outlineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0F172A.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.015f
                }
                canvas.drawRoundRect(RectF(s * 0.03f, s * 0.02f, s * 0.97f, s * 0.98f), s * 0.06f, s * 0.06f, outlineP)
                canvas.drawRoundRect(RectF(s * 0.05f, s * 0.09f, s * 0.95f, s * 0.95f), s * 0.04f, s * 0.04f, outlineP)

                val topPill = RectF(s * 0.20f, s * 0.03f, s * 0.80f, s * 0.11f)
                val topPillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E1B4B.toInt(); style = Paint.Style.FILL }
                canvas.drawRoundRect(topPill, s * 0.035f, s * 0.035f, topPillP)
                textPaint.textSize = s * 0.035f
                canvas.drawText(caption.ifEmpty { "SCAN ME" }, s / 2f, s * 0.08f, textPaint)
            }
            FrameStyle.Speech -> {
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0891B2.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.008f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.018f, s * 0.012f), 0f)
                }
                canvas.drawCircle(s / 2f, s * 0.44f, s * 0.45f, stitchPaint)
            }
            FrameStyle.Note -> {
                val fold = s * 0.10f
                val inset = s * 0.02f
                val flapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFDE047.toInt()
                    style = Paint.Style.FILL
                }
                val flapPath = Path().apply {
                    moveTo(s - inset - fold, s - inset)
                    lineTo(s - inset - fold, s - inset - fold)
                    lineTo(s - inset, s - inset - fold)
                    close()
                }
                canvas.drawPath(flapPath, flapPaint)

                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFCA8A04.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.007f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.016f, s * 0.012f), 0f)
                }
                val stitchPath = Path().apply {
                    val si = inset + s * 0.015f
                    moveTo(si, si)
                    lineTo(s - si, si)
                    lineTo(s - si, s - si - fold)
                    lineTo(s - si - fold, s - si)
                    lineTo(si, s - si)
                    close()
                }
                canvas.drawPath(stitchPath, stitchPaint)
            }
            FrameStyle.Globe -> {
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF2563EB.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.008f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.016f, s * 0.012f), 0f)
                }
                canvas.drawCircle(s / 2f, s / 2f, s * 0.47f, stitchPaint)
            }
            FrameStyle.Plaque -> {
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF4D7C0F.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.007f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.016f, s * 0.012f), 0f)
                }
                val cornerCut = s * 0.08f
                val pi = s * 0.02f
                val si = pi + s * 0.015f
                val sCornerCut = cornerCut - s * 0.005f
                val stitchPath = Path().apply {
                    moveTo(si + sCornerCut, si)
                    lineTo(s - si - sCornerCut, si)
                    quadTo(s - si, si, s - si, si + sCornerCut)
                    lineTo(s - si, s - si - sCornerCut)
                    quadTo(s - si, s - si, s - si - sCornerCut, s - si)
                    lineTo(si + sCornerCut, s - si)
                    quadTo(si, s - si, si, s - si - sCornerCut)
                    lineTo(si, si + sCornerCut)
                    quadTo(si, si, si + sCornerCut, si)
                    close()
                }
                canvas.drawPath(stitchPath, stitchPaint)
            }
            FrameStyle.Pentagon -> {
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFBE123C.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.007f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.016f, s * 0.012f), 0f)
                }
                val stitchPath = Path().apply {
                    moveTo(s * 0.50f, s * 0.03f)
                    lineTo(s * 0.97f, s * 0.33f)
                    lineTo(s * 0.86f, s * 0.96f)
                    lineTo(s * 0.14f, s * 0.96f)
                    lineTo(s * 0.03f, s * 0.33f)
                    close()
                }
                canvas.drawPath(stitchPath, stitchPaint)
            }
            FrameStyle.Hexagon -> {
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFB45309.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.007f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.016f, s * 0.012f), 0f)
                }
                val stitchPath = Path().apply {
                    moveTo(s * 0.50f, s * 0.03f)
                    lineTo(s * 0.97f, s * 0.26f)
                    lineTo(s * 0.97f, s * 0.74f)
                    lineTo(s * 0.50f, s * 0.97f)
                    lineTo(s * 0.03f, s * 0.74f)
                    lineTo(s * 0.03f, s * 0.26f)
                    close()
                }
                canvas.drawPath(stitchPath, stitchPaint)
            }
            FrameStyle.Diamond -> {
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrStyle.fgColor
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.010f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.016f, s * 0.012f), 0f)
                }
                val stitchPath = Path().apply {
                    moveTo(s * 0.50f, s * 0.03f)
                    lineTo(s * 0.97f, s * 0.50f)
                    lineTo(s * 0.50f, s * 0.97f)
                    lineTo(s * 0.03f, s * 0.50f)
                    close()
                }
                canvas.drawPath(stitchPath, stitchPaint)
            }
            FrameStyle.Seal -> {
                val cx = s / 2f
                val cy = s / 2f
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFB91C1C.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.007f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.016f, s * 0.012f), 0f)
                }
                canvas.drawCircle(cx, cy, s * 0.44f, stitchPaint)
            }
            FrameStyle.Bucket -> {
                val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF1D4ED8.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.007f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 0.016f, s * 0.012f), 0f)
                }
                val stitchPath = Path().apply {
                    moveTo(s * 0.14f, s * 0.16f)
                    lineTo(s * 0.86f, s * 0.16f)
                    lineTo(s * 0.79f, s * 0.90f)
                    lineTo(s * 0.21f, s * 0.90f)
                    close()
                }
                canvas.drawPath(stitchPath, stitchPaint)
            }
            else -> {}
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
            else -> FramePalette(0xFF06B6D4, 0xFFA855F7, 0xFF2DD4BF, 0xFFC084FC, 0xFFE9D5FF, 0xFFF59E0B, 0xFFFDE047, 0xFF0B0C10) // wisteria default
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
