package com.example.qr.engine

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity procedural renderer for Iconic Art Frames and Backgrounds.
 * Perfectly recreates the 16 floral, cyber, royal, anime, and luxury frames.
 */
object ArtFrameRenderer {

    fun isArtFrame(id: String): Boolean = id.startsWith("art-")

    fun render(id: String, canvas: Canvas, s: Float) {
        when (id) {
            "art-royal-gold" -> renderRoyalGold(canvas, s)
            "art-cyberpunk" -> renderCyberpunk(canvas, s)
            "art-jungle-ivy" -> renderJungleIvy(canvas, s)
            "art-cosmic-galaxy" -> renderCosmicGalaxy(canvas, s)
            "art-sakura-blossom" -> renderSakuraBlossom(canvas, s)
            "art-neon-ring" -> renderNeonRing(canvas, s)
            "art-ocean-waves" -> renderOceanWaves(canvas, s)
            "art-mecha-steel" -> renderMechaSteel(canvas, s)
            "art-memphis-pop" -> renderMemphisPop(canvas, s)
            "art-retro-synthwave" -> renderRetroSynthwave(canvas, s)
            "art-fiery-rose" -> renderFieryRose(canvas, s)
            "art-purple-wisteria" -> renderPurpleWisteria(canvas, s)
            "art-emerald-fern" -> renderEmeraldFern(canvas, s)
            "art-red-matrix" -> renderRedMatrix(canvas, s)
            "art-3d-bubbles" -> render3dBubbles(canvas, s)
            "art-neon-voxels" -> renderNeonVoxels(canvas, s)
            // 10 New Flagship Art Visuals & Backgrounds
            "art-aurora-borealis" -> renderAuroraBorealis(canvas, s)
            "art-cyber-samurai" -> renderCyberSamurai(canvas, s)
            "art-golden-kintsugi" -> renderGoldenKintsugi(canvas, s)
            "art-tropical-sunset" -> renderParadisePalm(canvas, s)
            "art-midnight-lotus" -> renderMidnightLotus(canvas, s)
            "art-holo-prism" -> renderHoloPrism(canvas, s)
            "art-steam-punk" -> renderClockworkChrono(canvas, s)
            "art-crystal-geode" -> renderAmethystGeode(canvas, s)
            "art-neon-noir" -> renderNeonNoir(canvas, s)
            "art-solar-flare" -> renderSolarFlare(canvas, s)
        }
    }

    private fun renderRoyalGold(canvas: Canvas, s: Float) {
        // Obsidian black marble background with subtle marble veins
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, 0xFF0D0B0A.toInt(), 0xFF171310.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Faint gold marble veining
        val veinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD4AF37.toInt()
            alpha = 35
            style = Paint.Style.STROKE
            strokeWidth = s * 0.003f
        }
        val veinPath = Path().apply {
            moveTo(0f, s * 0.2f)
            quadTo(s * 0.15f, s * 0.1f, s * 0.35f, s * 0.05f)
            moveTo(s * 0.8f, s)
            quadTo(s * 0.85f, s * 0.7f, s, s * 0.65f)
        }
        canvas.drawPath(veinPath, veinPaint)

        // Polished Gold Outer & Inner Frame Borders
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD700.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.012f
        }
        canvas.drawRoundRect(RectF(s * 0.025f, s * 0.025f, s * 0.975f, s * 0.975f), s * 0.04f, s * 0.04f, goldPaint)

        val thinGold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.004f
        }
        canvas.drawRoundRect(RectF(s * 0.042f, s * 0.042f, s * 0.958f, s * 0.958f), s * 0.03f, s * 0.03f, thinGold)

        // Baroque Corner Filigree Scrollwork (4 corners)
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFDF73.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
            strokeCap = Paint.Cap.ROUND
        }
        val cornerPoints = listOf(
            Pair(0f, 0f), Pair(s, 0f), Pair(0f, s), Pair(s, s)
        )
        for ((cx, cy) in cornerPoints) {
            val sx = if (cx == 0f) 1f else -1f
            val sy = if (cy == 0f) 1f else -1f
            val p = Path().apply {
                moveTo(cx + sx * s * 0.03f, cy + sy * s * 0.12f)
                quadTo(cx + sx * s * 0.09f, cy + sy * s * 0.09f, cx + sx * s * 0.12f, cy + sy * s * 0.03f)
                moveTo(cx + sx * s * 0.05f, cy + sy * s * 0.15f)
                quadTo(cx + sx * s * 0.12f, cy + sy * s * 0.12f, cx + sx * s * 0.15f, cy + sy * s * 0.05f)
            }
            canvas.drawPath(p, cornerPaint)
        }

        // Royal Golden Crown at Top Center
        val crownFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD700.toInt()
            style = Paint.Style.FILL
        }
        val midX = s * 0.5f
        val crownY = s * 0.042f
        val crownW = s * 0.09f
        val crownH = s * 0.032f
        val crownPath = Path().apply {
            moveTo(midX - crownW / 2f, crownY + crownH)
            lineTo(midX - crownW / 2f, crownY)
            lineTo(midX - crownW * 0.25f, crownY + crownH * 0.45f)
            lineTo(midX, crownY - crownH * 0.2f)
            lineTo(midX + crownW * 0.25f, crownY + crownH * 0.45f)
            lineTo(midX + crownW / 2f, crownY)
            lineTo(midX + crownW / 2f, crownY + crownH)
            close()
        }
        canvas.drawPath(crownPath, crownFill)
    }

    private fun renderCyberpunk(canvas: Canvas, s: Float) {
        // Deep cyber dark navy background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF050813.toInt()
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Outer Cyan & Magenta Glow Borders
        val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00F0FF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }
        val magentaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF007F.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }

        // 45° Chamfered Tech Corners
        val cut = s * 0.08f
        val pad = s * 0.025f
        val techBorder = Path().apply {
            moveTo(pad + cut, pad)
            lineTo(s - pad - cut, pad)
            lineTo(s - pad, pad + cut)
            lineTo(s - pad, s - pad - cut)
            lineTo(s - pad - cut, s - pad)
            lineTo(pad + cut, s - pad)
            lineTo(pad, s - pad - cut)
            lineTo(pad, pad + cut)
            close()
        }
        canvas.drawPath(techBorder, cyanPaint)

        // Secondary Magenta Inner Brackets
        val inPad = s * 0.045f
        val inCut = s * 0.06f
        val inBorder = Path().apply {
            moveTo(inPad + inCut, inPad)
            lineTo(s - inPad - inCut, inPad)
            lineTo(s - inPad, inPad + inCut)
            lineTo(s - inPad, s - inPad - inCut)
            lineTo(s - inPad - inCut, s - inPad)
            lineTo(inPad + inCut, s - inPad)
            lineTo(inPad, s - inPad - inCut)
            lineTo(inPad, inPad + inCut)
            close()
        }
        canvas.drawPath(inBorder, magentaPaint)

        // Circuit Nodes & Data Traces
        val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00F0FF.toInt()
            style = Paint.Style.FILL
        }
        val nodes = listOf(
            Pair(s * 0.15f, pad), Pair(s * 0.85f, pad),
            Pair(s * 0.15f, s - pad), Pair(s * 0.85f, s - pad),
            Pair(pad, s * 0.5f), Pair(s - pad, s * 0.5f)
        )
        for ((nx, ny) in nodes) {
            canvas.drawCircle(nx, ny, s * 0.007f, nodePaint)
        }
    }

    private fun renderJungleIvy(canvas: Canvas, s: Float) {
        // Deep foliage dark green background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s * 0.5f, s * 0.5f, s * 0.65f, 0xFF0B2113.toInt(), 0xFF040D07.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val leafVeinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x88FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.002f
        }

        // Draw multiple lush ivy leaves along each of the 4 borders
        val borderLeaves = 9
        for (i in 0..borderLeaves) {
            val t = i.toFloat() / borderLeaves
            // Top border
            drawIvyLeaf(canvas, s * t, s * (0.02f + (i % 2) * 0.03f), s * 0.045f, (i * 45f), leafPaint, leafVeinPaint, i)
            // Bottom border
            drawIvyLeaf(canvas, s * t, s * (0.98f - (i % 2) * 0.03f), s * 0.045f, (180f + i * 35f), leafPaint, leafVeinPaint, i + 1)
            // Left border
            drawIvyLeaf(canvas, s * (0.02f + (i % 2) * 0.03f), s * t, s * 0.045f, (-90f + i * 40f), leafPaint, leafVeinPaint, i + 2)
            // Right border
            drawIvyLeaf(canvas, s * (0.98f - (i % 2) * 0.03f), s * t, s * 0.045f, (90f + i * 40f), leafPaint, leafVeinPaint, i + 3)
        }
    }

    private fun drawIvyLeaf(canvas: Canvas, x: Float, y: Float, size: Float, angleDeg: Float, fill: Paint, vein: Paint, seed: Int) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(angleDeg)
        val green = if (seed % 3 == 0) 0xFF22C55E.toInt() else if (seed % 3 == 1) 0xFF16A34A.toInt() else 0xFF4ADE80.toInt()
        fill.color = green
        val path = Path().apply {
            moveTo(0f, -size)
            quadTo(size * 0.8f, -size * 0.4f, size * 0.5f, size * 0.6f)
            lineTo(0f, size)
            lineTo(-size * 0.5f, size * 0.6f)
            quadTo(-size * 0.8f, -size * 0.4f, 0f, -size)
            close()
        }
        canvas.drawPath(path, fill)
        canvas.drawLine(0f, -size * 0.8f, 0f, size * 0.8f, vein)
        canvas.restore()
    }

    private fun renderCosmicGalaxy(canvas: Canvas, s: Float) {
        // Deep space void
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s * 0.5f, s * 0.5f, s * 0.7f, 0xFF190633.toInt(), 0xFF04010A.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Swirling Nebula Spiral Arms along border
        val nebulaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.035f
        }
        nebulaPaint.shader = LinearGradient(0f, 0f, s, s, 0xAAEC4899.toInt(), 0xAA06B6D4.toInt(), Shader.TileMode.CLAMP)
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.46f, nebulaPaint)

        // Starlight Orbital Ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            alpha = 180
            style = Paint.Style.STROKE
            strokeWidth = s * 0.004f
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.475f, ringPaint)

        // Twinkling Starlight Cross Stars
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        val starPositions = listOf(
            Pair(s * 0.1f, s * 0.1f), Pair(s * 0.9f, s * 0.1f),
            Pair(s * 0.1f, s * 0.9f), Pair(s * 0.9f, s * 0.9f),
            Pair(s * 0.5f, s * 0.03f), Pair(s * 0.5f, s * 0.97f)
        )
        for ((sx, sy) in starPositions) {
            val r = s * 0.015f
            val star = Path().apply {
                moveTo(sx, sy - r)
                quadTo(sx, sy, sx + r, sy)
                quadTo(sx, sy, sx, sy + r)
                quadTo(sx, sy, sx - r, sy)
                quadTo(sx, sy, sx, sy - r)
                close()
            }
            canvas.drawPath(star, starPaint)
        }
    }

    private fun renderSakuraBlossom(canvas: Canvas, s: Float) {
        // Soft Ivory/Cream background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF9F9.toInt() }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Delicate Sakura Branches
        val branchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5C3D2E.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.007f
            strokeCap = Paint.Cap.ROUND
        }
        val branch1 = Path().apply {
            moveTo(0f, s * 0.15f)
            quadTo(s * 0.08f, s * 0.08f, s * 0.22f, s * 0.04f)
        }
        val branch2 = Path().apply {
            moveTo(s, s * 0.85f)
            quadTo(s * 0.92f, s * 0.92f, s * 0.78f, s * 0.96f)
        }
        canvas.drawPath(branch1, branchPaint)
        canvas.drawPath(branch2, branchPaint)

        // 5-Petal Sakura Blossoms along the frame
        val blossomPoints = listOf(
            Pair(s * 0.08f, s * 0.08f), Pair(s * 0.16f, s * 0.05f),
            Pair(s * 0.88f, s * 0.90f), Pair(s * 0.80f, s * 0.94f),
            Pair(s * 0.05f, s * 0.50f), Pair(s * 0.95f, s * 0.50f),
            Pair(s * 0.50f, s * 0.04f), Pair(s * 0.50f, s * 0.96f)
        )
        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF472B6.toInt() }
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFBE185D.toInt() }
        for ((bx, by) in blossomPoints) {
            val r = s * 0.024f
            for (p in 0..4) {
                val rad = (Math.PI * 2 / 5 * p).toFloat()
                val px = bx + cos(rad.toDouble()).toFloat() * r
                val py = by + sin(rad.toDouble()).toFloat() * r
                canvas.drawCircle(px, py, r * 0.6f, petalPaint)
            }
            canvas.drawCircle(bx, by, r * 0.35f, centerPaint)
        }
    }

    private fun renderNeonRing(canvas: Canvas, s: Float) {
        // Obsidian black background
        canvas.drawColor(0xFF060608.toInt())

        // Glowing concentric laser vortex rings (rainbow neon)
        val outerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.018f
            shader = LinearGradient(0f, 0f, s, s, 0xFF00E5FF.toInt(), 0xFFE040FB.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.46f, outerRing)

        val innerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.010f
            shader = LinearGradient(s, 0f, 0f, s, 0xFFFFAB00.toInt(), 0xFFFF1744.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.44f, innerRing)
    }

    private fun renderOceanWaves(canvas: Canvas, s: Float) {
        // Deep marine navy background
        canvas.drawColor(0xFF02162E.toInt())

        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.015f
            shader = LinearGradient(0f, 0f, s, s, 0xFF0284C7.toInt(), 0xFF38BDF8.toInt(), Shader.TileMode.CLAMP)
        }
        // Swirling crests along borders
        val crest1 = Path().apply {
            moveTo(0f, s * 0.08f)
            quadTo(s * 0.25f, 0f, s * 0.5f, s * 0.06f)
            quadTo(s * 0.75f, s * 0.12f, s, s * 0.04f)
        }
        val crest2 = Path().apply {
            moveTo(0f, s * 0.92f)
            quadTo(s * 0.25f, s, s * 0.5f, s * 0.94f)
            quadTo(s * 0.75f, s * 0.88f, s, s * 0.96f)
        }
        canvas.drawPath(crest1, wavePaint)
        canvas.drawPath(crest2, wavePaint)

        // Foam splashes
        val foamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCBAE6FD.toInt(); style = Paint.Style.FILL }
        canvas.drawCircle(s * 0.25f, s * 0.05f, s * 0.012f, foamPaint)
        canvas.drawCircle(s * 0.75f, s * 0.95f, s * 0.012f, foamPaint)
    }

    private fun renderMechaSteel(canvas: Canvas, s: Float) {
        // Carbon dark background
        canvas.drawColor(0xFF101418.toInt())

        // Metallic Steel Armor Frame
        val steelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.038f
            shader = LinearGradient(0f, 0f, s, s, 0xFF64748B.toInt(), 0xFF334155.toInt(), Shader.TileMode.CLAMP)
        }
        val pad = s * 0.024f
        val rect = RectF(pad, pad, s - pad, s - pad)
        canvas.drawRoundRect(rect, s * 0.04f, s * 0.04f, steelPaint)

        // Rivets/Bolts on perimeter
        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCBD5E1.toInt() }
        val boltShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt() }
        val boltPositions = listOf(
            Pair(s * 0.15f, pad), Pair(s * 0.50f, pad), Pair(s * 0.85f, pad),
            Pair(s * 0.15f, s - pad), Pair(s * 0.50f, s - pad), Pair(s * 0.85f, s - pad),
            Pair(pad, s * 0.50f), Pair(s - pad, s * 0.50f)
        )
        for ((bx, by) in boltPositions) {
            canvas.drawCircle(bx, by, s * 0.007f, boltShadow)
            canvas.drawCircle(bx - s * 0.001f, by - s * 0.001f, s * 0.005f, boltPaint)
        }
    }

    private fun renderMemphisPop(canvas: Canvas, s: Float) {
        // Crisp White background
        canvas.drawColor(0xFFF8FAFC.toInt())

        val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF06B6D4.toInt(); style = Paint.Style.FILL }
        val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF43F5E.toInt(); style = Paint.Style.FILL }
        val p3 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFBBF24.toInt(); style = Paint.Style.FILL }
        val p4 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B5CF6.toInt(); style = Paint.Style.FILL }

        // Bold angular color blocks around the edges
        val path1 = Path().apply {
            moveTo(0f, 0f); lineTo(s * 0.28f, 0f); lineTo(0f, s * 0.28f); close()
        }
        val path2 = Path().apply {
            moveTo(s, 0f); lineTo(s - s * 0.25f, 0f); lineTo(s, s * 0.25f); close()
        }
        val path3 = Path().apply {
            moveTo(0f, s); lineTo(s * 0.25f, s); lineTo(0f, s - s * 0.25f); close()
        }
        val path4 = Path().apply {
            moveTo(s, s); lineTo(s - s * 0.30f, s); lineTo(s, s - s * 0.30f); close()
        }
        canvas.drawPath(path1, p1)
        canvas.drawPath(path2, p2)
        canvas.drawPath(path3, p3)
        canvas.drawPath(path4, p4)
    }

    private fun renderRetroSynthwave(canvas: Canvas, s: Float) {
        // Sunset sky gradient
        val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, 0xFF1E052D.toInt(), 0xFFFF5E36.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, skyPaint)

        // Glowing striped sunset sun
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD700.toInt() }
        canvas.drawCircle(s * 0.5f, s * 0.88f, s * 0.22f, sunPaint)

        // Silhouette Palm trees at left & right edges
        val palmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0D0214.toInt()
            style = Paint.Style.FILL
        }
        // Palm trunk left
        canvas.drawLine(s * 0.05f, s, s * 0.08f, s * 0.7f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0D0214.toInt(); strokeWidth = s * 0.012f
        })
        canvas.drawCircle(s * 0.08f, s * 0.7f, s * 0.035f, palmPaint)

        // Palm trunk right
        canvas.drawLine(s * 0.95f, s, s * 0.92f, s * 0.7f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0D0214.toInt(); strokeWidth = s * 0.012f
        })
        canvas.drawCircle(s * 0.92f, s * 0.7f, s * 0.035f, palmPaint)
    }

    private fun renderFieryRose(canvas: Canvas, s: Float) {
        // Charcoal black void
        canvas.drawColor(0xFF0F0404.toInt())

        // Thorny flame vines
        val vinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }
        val v1 = Path().apply {
            moveTo(s * 0.04f, s * 0.04f)
            lineTo(s * 0.96f, s * 0.04f)
            lineTo(s * 0.96f, s * 0.96f)
            lineTo(s * 0.04f, s * 0.96f)
            close()
        }
        canvas.drawPath(v1, vinePaint)

        // Burning ember roses at corners
        val rosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEF4444.toInt() }
        val emberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFBBF24.toInt() }
        val corners = listOf(Pair(s * 0.08f, s * 0.08f), Pair(s * 0.92f, s * 0.08f), Pair(s * 0.08f, s * 0.92f), Pair(s * 0.92f, s * 0.92f))
        for ((rx, ry) in corners) {
            canvas.drawCircle(rx, ry, s * 0.035f, rosePaint)
            canvas.drawCircle(rx, ry, s * 0.018f, emberPaint)
        }
    }

    private fun renderPurpleWisteria(canvas: Canvas, s: Float) {
        // Midnight twilight indigo
        canvas.drawColor(0xFF0B061A.toInt())

        val vinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA855F7.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.005f
        }
        canvas.drawRoundRect(RectF(s * 0.035f, s * 0.035f, s * 0.965f, s * 0.965f), s * 0.04f, s * 0.04f, vinePaint)

        // Hanging Wisteria blossoms along top
        val blossomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC084FC.toInt() }
        for (i in 0..12) {
            val wx = s * (0.08f + i * 0.07f)
            val dropLen = if (i % 2 == 0) s * 0.08f else s * 0.05f
            for (step in 1..4) {
                canvas.drawCircle(wx, s * 0.04f + dropLen * (step / 4f), s * 0.008f, blossomPaint)
            }
        }
    }

    private fun renderEmeraldFern(canvas: Canvas, s: Float) {
        // Deep enchanted rainforest dark
        canvas.drawColor(0xFF04120C.toInt())

        val fernPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10B981.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }
        canvas.drawRoundRect(RectF(s * 0.035f, s * 0.035f, s * 0.965f, s * 0.965f), s * 0.04f, s * 0.04f, fernPaint)

        // Glowing mint orchids at corners
        val orchidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF34D399.toInt() }
        val corners = listOf(Pair(s * 0.06f, s * 0.06f), Pair(s * 0.94f, s * 0.06f), Pair(s * 0.06f, s * 0.94f), Pair(s * 0.94f, s * 0.94f))
        for ((ox, oy) in corners) {
            canvas.drawCircle(ox, oy, s * 0.026f, orchidPaint)
        }
    }

    private fun renderRedMatrix(canvas: Canvas, s: Float) {
        canvas.drawColor(0xFF080101.toInt())

        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEF4444.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }
        val cut = s * 0.07f
        val pad = s * 0.025f
        val path = Path().apply {
            moveTo(pad + cut, pad); lineTo(s - pad - cut, pad)
            lineTo(s - pad, pad + cut); lineTo(s - pad, s - pad - cut)
            lineTo(s - pad - cut, s - pad); lineTo(pad + cut, s - pad)
            lineTo(pad, s - pad - cut); lineTo(pad, pad + cut)
            close()
        }
        canvas.drawPath(path, redPaint)
    }

    private fun render3dBubbles(canvas: Canvas, s: Float) {
        canvas.drawColor(0xFFF5F7FA.toInt())

        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s * 0.1f, s * 0.1f, s * 0.08f, 0xFF93C5FD.toInt(), 0x003B82F6, Shader.TileMode.CLAMP)
        }
        val bubblePoints = listOf(
            Pair(s * 0.08f, s * 0.08f), Pair(s * 0.92f, s * 0.08f),
            Pair(s * 0.08f, s * 0.92f), Pair(s * 0.92f, s * 0.92f),
            Pair(s * 0.50f, s * 0.04f), Pair(s * 0.50f, s * 0.96f)
        )
        for ((bx, by) in bubblePoints) {
            canvas.drawCircle(bx, by, s * 0.04f, bubblePaint)
            canvas.drawCircle(bx - s * 0.01f, by - s * 0.01f, s * 0.012f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAFFFFFF.toInt() })
        }
    }

    private fun renderNeonVoxels(canvas: Canvas, s: Float) {
        canvas.drawColor(0xFF080612.toInt())

        val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF06B6D4.toInt() }
        val magPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD946EF.toInt() }

        // Floating 3D voxels (isometric cubes)
        for (i in 0..10) {
            val t = s * (0.05f + i * 0.09f)
            val paint = if (i % 2 == 0) cyanPaint else magPaint
            canvas.drawRect(t, s * 0.02f, t + s * 0.03f, s * 0.05f, paint)
            canvas.drawRect(t, s * 0.95f, t + s * 0.03f, s * 0.98f, paint)
        }
    }

    // 1. Aurora Borealis - Northern Lights with mountain silhouettes & polar radiance
    private fun renderAuroraBorealis(canvas: Canvas, s: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, 0xFF020617.toInt(), 0xFF064E3B.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Shimmering Aurora ribbons
        val aurora1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, s * 0.1f, s, s * 0.6f, 0x8800FF87.toInt(), 0x118B5CF6.toInt(), Shader.TileMode.CLAMP)
        }
        val p1 = Path().apply {
            moveTo(0f, s * 0.35f)
            cubicTo(s * 0.25f, s * 0.15f, s * 0.6f, s * 0.5f, s, s * 0.2f)
            lineTo(s, s * 0.55f)
            cubicTo(s * 0.7f, s * 0.65f, s * 0.3f, s * 0.35f, 0f, s * 0.55f)
            close()
        }
        canvas.drawPath(p1, aurora1)

        val aurora2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(s, 0f, 0f, s * 0.7f, 0x7738BDF8.toInt(), 0x1110B981.toInt(), Shader.TileMode.CLAMP)
        }
        val p2 = Path().apply {
            moveTo(0f, s * 0.2f)
            cubicTo(s * 0.35f, s * 0.45f, s * 0.7f, s * 0.15f, s, s * 0.38f)
            lineTo(s, s * 0.48f)
            cubicTo(s * 0.65f, s * 0.3f, s * 0.25f, s * 0.6f, 0f, s * 0.35f)
            close()
        }
        canvas.drawPath(p2, aurora2)

        // Starlight dots
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEEFFFFFF.toInt() }
        val stars = listOf(
            Pair(0.12f, 0.08f), Pair(0.28f, 0.05f), Pair(0.48f, 0.12f), Pair(0.72f, 0.07f),
            Pair(0.88f, 0.14f), Pair(0.15f, 0.25f), Pair(0.85f, 0.28f), Pair(0.6f, 0.04f)
        )
        for ((sx, sy) in stars) {
            canvas.drawCircle(s * sx, s * sy, s * 0.005f, starPaint)
        }

        // Silhouette of snowy mountains at bottom
        val mtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF020B14.toInt() }
        val mtnPath = Path().apply {
            moveTo(0f, s)
            lineTo(0f, s * 0.90f)
            lineTo(s * 0.22f, s * 0.82f)
            lineTo(s * 0.45f, s * 0.88f)
            lineTo(s * 0.70f, s * 0.80f)
            lineTo(s * 0.88f, s * 0.86f)
            lineTo(s, s * 0.83f)
            lineTo(s, s)
            close()
        }
        canvas.drawPath(mtnPath, mtnPaint)

        // Glowing ice-emerald outer border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00FF87.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.007f
        }
        canvas.drawRoundRect(RectF(s * 0.028f, s * 0.028f, s * 0.972f, s * 0.972f), s * 0.035f, s * 0.035f, borderPaint)
    }

    // 2. Cyber Katana - Obsidian carbon armor with neon crimson and electric cyan cuts
    private fun renderCyberSamurai(canvas: Canvas, s: Float) {
        canvas.drawColor(0xFF07090E.toInt())

        // Carbon weave line traces
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF141A29.toInt()
            strokeWidth = s * 0.002f
        }
        for (i in 1..14) {
            val step = s * (i * 0.07f)
            canvas.drawLine(step, 0f, step, s, gridPaint)
            canvas.drawLine(0f, step, s, step, gridPaint)
        }

        // Angular Cyber Edge Chamfers (Dual neon: Crimson & Cyan)
        val crimsonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF0055.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.009f
            strokeCap = Paint.Cap.SQUARE
        }
        val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00F0FF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.009f
            strokeCap = Paint.Cap.SQUARE
        }

        val cut = s * 0.08f
        val pad = s * 0.026f

        // Top & Right in Cyan
        val pathCyan = Path().apply {
            moveTo(pad, pad + cut)
            lineTo(pad + cut, pad)
            lineTo(s - pad - cut, pad)
            lineTo(s - pad, pad + cut)
            lineTo(s - pad, s * 0.5f)
        }
        canvas.drawPath(pathCyan, cyanPaint)

        // Bottom & Left in Crimson
        val pathCrimson = Path().apply {
            moveTo(s - pad, s * 0.5f)
            lineTo(s - pad, s - pad - cut)
            lineTo(s - pad - cut, s - pad)
            lineTo(pad + cut, s - pad)
            lineTo(pad, s - pad - cut)
            lineTo(pad, pad + cut)
        }
        canvas.drawPath(pathCrimson, crimsonPaint)

        // Katana slash accents at corners
        val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF0055.toInt()
            strokeWidth = s * 0.004f
        }
        canvas.drawLine(s * 0.06f, s * 0.12f, s * 0.12f, s * 0.06f, slashPaint)
        canvas.drawLine(s * 0.88f, s * 0.94f, s * 0.94f, s * 0.88f, Paint(slashPaint).apply { color = 0xFF00F0FF.toInt() })
    }

    // 3. Golden Kintsugi - Japanese cracked porcelain with radiant molten gold veins
    private fun renderGoldenKintsugi(canvas: Canvas, s: Float) {
        // Warm ceramic ivory background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, 0xFFFAF7F2.toInt(), 0xFFEFE8DD.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Radiant Molten Gold Veins (Kintsugi repair lines)
        val goldVein = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD4AF37.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val goldHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF2A8.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.0025f
            strokeCap = Paint.Cap.ROUND
        }

        val v1 = Path().apply {
            moveTo(0f, s * 0.28f)
            cubicTo(s * 0.15f, s * 0.32f, s * 0.25f, s * 0.18f, s * 0.38f, s * 0.24f)
            lineTo(s * 0.52f, s * 0.12f)
            cubicTo(s * 0.68f, s * 0.18f, s * 0.82f, s * 0.08f, s, s * 0.15f)
        }
        canvas.drawPath(v1, goldVein)
        canvas.drawPath(v1, goldHighlight)

        val v2 = Path().apply {
            moveTo(s * 0.38f, s * 0.24f)
            cubicTo(s * 0.42f, s * 0.48f, s * 0.30f, s * 0.68f, s * 0.45f, s * 0.82f)
            lineTo(s * 0.60f, s)
        }
        canvas.drawPath(v2, goldVein)
        canvas.drawPath(v2, goldHighlight)

        val v3 = Path().apply {
            moveTo(s * 0.45f, s * 0.82f)
            cubicTo(s * 0.65f, s * 0.78f, s * 0.78f, s * 0.88f, s, s * 0.75f)
        }
        canvas.drawPath(v3, goldVein)
        canvas.drawPath(v3, goldHighlight)

        // Gold dust specks
        val dustPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCD4AF37.toInt() }
        val specks = listOf(
            Pair(0.20f, 0.28f), Pair(0.40f, 0.20f), Pair(0.55f, 0.15f),
            Pair(0.43f, 0.52f), Pair(0.36f, 0.72f), Pair(0.68f, 0.82f), Pair(0.85f, 0.72f)
        )
        for ((sx, sy) in specks) {
            canvas.drawCircle(s * sx, s * sy, s * 0.007f, dustPaint)
            canvas.drawCircle(s * (sx + 0.015f), s * (sy - 0.01f), s * 0.004f, dustPaint)
        }

        // Minimalist Zen Gold Frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD4AF37.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.005f
        }
        canvas.drawRoundRect(RectF(s * 0.026f, s * 0.026f, s * 0.974f, s * 0.974f), s * 0.03f, s * 0.03f, framePaint)
    }

    // 4. Paradise Palm - Glowing synth-sunset gradient with swaying tropical palms
    private fun renderParadisePalm(canvas: Canvas, s: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, 0xFFFF4500.toInt(), 0xFF7928CA.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Warm Golden Sun Disc
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s * 0.5f, s * 0.42f, s * 0.24f, 0xFFFFF176.toInt(), 0x00FF8E53, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(s * 0.5f, s * 0.42f, s * 0.24f, sunPaint)

        // Silhouetted Palm Tree Fronds (Top-Right and Top-Left)
        val palmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF180A2A.toInt()
            style = Paint.Style.FILL
        }

        // Right palm branch
        val palmPathR = Path().apply {
            moveTo(s, s * 0.05f)
            quadTo(s * 0.8f, s * 0.12f, s * 0.65f, s * 0.28f)
            quadTo(s * 0.78f, s * 0.22f, s, s * 0.05f)
        }
        canvas.drawPath(palmPathR, palmPaint)
        // Frond leaves right
        for (i in 0..5) {
            val t = 0.08f + i * 0.035f
            val leaf = Path().apply {
                moveTo(s * (1f - t * 0.8f), s * (0.05f + t * 0.9f))
                quadTo(s * (1f - t * 0.8f - 0.06f), s * (0.05f + t * 0.9f + 0.05f), s * (1f - t * 0.8f - 0.08f), s * (0.05f + t * 0.9f + 0.09f))
                quadTo(s * (1f - t * 0.8f - 0.02f), s * (0.05f + t * 0.9f + 0.05f), s * (1f - t * 0.8f), s * (0.05f + t * 0.9f))
            }
            canvas.drawPath(leaf, palmPaint)
        }

        // Left palm frond
        val palmPathL = Path().apply {
            moveTo(0f, s * 0.08f)
            quadTo(s * 0.22f, s * 0.15f, s * 0.35f, s * 0.30f)
            quadTo(s * 0.20f, s * 0.24f, 0f, s * 0.08f)
        }
        canvas.drawPath(palmPathL, palmPaint)

        // Warm Coral Border with Sunset Glow
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF758C.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.007f
        }
        canvas.drawRoundRect(RectF(s * 0.025f, s * 0.025f, s * 0.975f, s * 0.975f), s * 0.035f, s * 0.035f, borderPaint)
    }

    // 5. Midnight Lotus - Sacred neon lotus blossoms floating on moonlit obsidian waters
    private fun renderMidnightLotus(canvas: Canvas, s: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, 0xFF030712.toInt(), 0xFF06202A.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Concentric Water Ripples
        val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF06B6D4.toInt()
            alpha = 40
            style = Paint.Style.STROKE
            strokeWidth = s * 0.003f
        }
        canvas.drawOval(RectF(s * 0.2f, s * 0.72f, s * 0.8f, s * 0.96f), ripplePaint)
        canvas.drawOval(RectF(s * 0.1f, s * 0.65f, s * 0.9f, s * 0.99f), ripplePaint)

        // Sacred Lotus Petals at Bottom Center
        val petalPink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF43F5E.toInt()
            style = Paint.Style.FILL
        }
        val petalWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF1F2.toInt()
            style = Paint.Style.FILL
        }

        val cx = s * 0.5f
        val cy = s * 0.92f

        // Outer Jade Leaves
        val leafJade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF059669.toInt() }
        canvas.drawOval(RectF(cx - s * 0.22f, cy - s * 0.02f, cx - s * 0.04f, cy + s * 0.05f), leafJade)
        canvas.drawOval(RectF(cx + s * 0.04f, cy - s * 0.02f, cx + s * 0.22f, cy + s * 0.05f), leafJade)

        // Lotus Petals Layer
        for (i in -2..2) {
            val offset = i * s * 0.035f
            val p = Path().apply {
                moveTo(cx + offset, cy + s * 0.02f)
                cubicTo(cx + offset * 1.5f, cy - s * 0.04f, cx + offset * 0.8f, cy - s * 0.07f, cx, cy - s * 0.09f)
                cubicTo(cx - offset * 0.8f, cy - s * 0.07f, cx - offset * 1.5f, cy - s * 0.04f, cx + offset, cy + s * 0.02f)
            }
            canvas.drawPath(p, if (i == 0) petalWhite else petalPink)
        }

        // Firefly / Bioluminescent motes
        val firefly = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s * 0.2f, s * 0.3f, s * 0.04f, 0xFF34D399.toInt(), 0x0034D399, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(s * 0.2f, s * 0.3f, s * 0.04f, firefly)
        canvas.drawCircle(s * 0.8f, s * 0.4f, s * 0.035f, firefly)

        // Jade Lotus Frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10B981.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }
        canvas.drawRoundRect(RectF(s * 0.026f, s * 0.026f, s * 0.974f, s * 0.974f), s * 0.035f, s * 0.035f, borderPaint)
    }

    // 6. Holographic Prism - Iridescent chromatic rainbow sheen with diamond facet refraction
    private fun renderHoloPrism(canvas: Canvas, s: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s,
                intArrayOf(0xFFB967FF.toInt(), 0xFF01CDFE.toInt(), 0xFF05FFA1.toInt(), 0xFFFFFB96.toInt(), 0xFFFF71CE.toInt()),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Geometric Diamond Shard Facets
        val shardPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF.toInt()
            style = Paint.Style.FILL
        }
        val shardPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22000000.toInt()
            style = Paint.Style.FILL
        }

        // Diagonal geometric cuts across the perimeter
        val sh1 = Path().apply {
            moveTo(0f, 0f); lineTo(s * 0.45f, 0f); lineTo(0f, s * 0.45f); close()
        }
        canvas.drawPath(sh1, shardPaint1)

        val sh2 = Path().apply {
            moveTo(s, s); lineTo(s * 0.55f, s); lineTo(s, s * 0.55f); close()
        }
        canvas.drawPath(sh2, shardPaint2)

        val sh3 = Path().apply {
            moveTo(s, 0f); lineTo(s * 0.7f, 0f); lineTo(s, s * 0.3f); close()
        }
        canvas.drawPath(sh3, shardPaint1)

        // Metallic Silver Holographic Frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }
        canvas.drawRoundRect(RectF(s * 0.026f, s * 0.026f, s * 0.974f, s * 0.974f), s * 0.03f, s * 0.03f, borderPaint)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x88000000.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.003f
        }
        canvas.drawRoundRect(RectF(s * 0.04f, s * 0.04f, s * 0.96f, s * 0.96f), s * 0.02f, s * 0.02f, innerPaint)
    }

    // 7. Clockwork Chrono - Victorian steampunk brass & copper cogs with riveted armor
    private fun renderClockworkChrono(canvas: Canvas, s: Float) {
        // Antique Mahogany/Brass background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, 0xFF1C130D.toInt(), 0xFF2F1D13.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Steampunk Cogwheel Helper
        fun drawCog(cx: Float, cy: Float, radius: Float, teeth: Int, color: Int) {
            val cogPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, radius * 0.85f, cogPaint)
            for (i in 0 until teeth) {
                val angle = (i * 2.0 * Math.PI / teeth).toFloat()
                val tx = cx + cos(angle) * radius
                val ty = cy + sin(angle) * radius
                canvas.drawCircle(tx, ty, radius * 0.16f, cogPaint)
            }
            // Center axle hole
            val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = 0xFF1C130D.toInt()
            }
            canvas.drawCircle(cx, cy, radius * 0.35f, holePaint)
        }

        // Intricate Brass and Copper cogs in corners
        drawCog(s * 0.08f, s * 0.08f, s * 0.09f, 8, 0xFFD97706.toInt()) // Brass
        drawCog(s * 0.92f, s * 0.08f, s * 0.075f, 6, 0xFFB45309.toInt()) // Copper
        drawCog(s * 0.08f, s * 0.92f, s * 0.075f, 6, 0xFFB45309.toInt())
        drawCog(s * 0.92f, s * 0.92f, s * 0.09f, 8, 0xFFD97706.toInt())

        // Boiler-plate riveted border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }
        canvas.drawRoundRect(RectF(s * 0.026f, s * 0.026f, s * 0.974f, s * 0.974f), s * 0.03f, s * 0.03f, borderPaint)

        // Copper Rivets on border
        val rivetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFCD34D.toInt() }
        val rivets = listOf(
            Pair(0.25f, 0.026f), Pair(0.50f, 0.026f), Pair(0.75f, 0.026f),
            Pair(0.25f, 0.974f), Pair(0.50f, 0.974f), Pair(0.75f, 0.974f),
            Pair(0.026f, 0.25f), Pair(0.026f, 0.50f), Pair(0.026f, 0.75f),
            Pair(0.974f, 0.25f), Pair(0.974f, 0.50f), Pair(0.974f, 0.75f)
        )
        for ((rx, ry) in rivets) {
            canvas.drawCircle(s * rx, s * ry, s * 0.007f, rivetPaint)
        }
    }

    // 8. Amethyst Geode - Deep royal purple crystalline cavern with glittering quartz facets
    private fun renderAmethystGeode(canvas: Canvas, s: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s * 0.5f, s * 0.5f, s * 0.7f, 0xFF2E1065.toInt(), 0xFF0F051D.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Jagged Amethyst Crystal Clusters at Corners
        fun drawCrystalCluster(cx: Float, cy: Float, sx: Float, sy: Float) {
            val deepPurple = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7E22CE.toInt() }
            val brightViolet = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA855F7.toInt() }
            val quartzLavender = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE9D5FF.toInt() }

            val p = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + sx * s * 0.12f, cy + sy * s * 0.04f)
                lineTo(cx + sx * s * 0.08f, cy + sy * s * 0.12f)
                close()
            }
            canvas.drawPath(p, deepPurple)

            val p2 = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + sx * s * 0.08f, cy + sy * s * 0.12f)
                lineTo(cx + sx * s * 0.04f, cy + sy * s * 0.15f)
                close()
            }
            canvas.drawPath(p2, brightViolet)

            val p3 = Path().apply {
                moveTo(cx + sx * s * 0.05f, cy + sy * s * 0.05f)
                lineTo(cx + sx * s * 0.09f, cy + sy * s * 0.03f)
                lineTo(cx + sx * s * 0.06f, cy + sy * s * 0.09f)
                close()
            }
            canvas.drawPath(p3, quartzLavender)
        }

        drawCrystalCluster(0f, 0f, 1f, 1f)
        drawCrystalCluster(s, 0f, -1f, 1f)
        drawCrystalCluster(0f, s, 1f, -1f)
        drawCrystalCluster(s, s, -1f, -1f)

        // Gold leaf trim edge
        val goldBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBBF24.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }
        canvas.drawRoundRect(RectF(s * 0.026f, s * 0.026f, s * 0.974f, s * 0.974f), s * 0.035f, s * 0.035f, goldBorder)

        // Shimmering diamond glints
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        canvas.drawCircle(s * 0.15f, s * 0.12f, s * 0.007f, glintPaint)
        canvas.drawCircle(s * 0.85f, s * 0.88f, s * 0.007f, glintPaint)
    }

    // 9. Neon Tokyo Rain - Rain-slicked cyber cityscape with vertical kanji neon glow
    private fun renderNeonNoir(canvas: Canvas, s: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, 0xFF080811.toInt(), 0xFF131127.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Wet asphalt reflection puddle at bottom
        val puddle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, s * 0.85f, 0f, s, 0x1100F0FF.toInt(), 0x66FF007F.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, s * 0.82f, s, s, puddle)

        // Diagonal Rain Streaks
        val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33A5F3FC.toInt()
            strokeWidth = s * 0.002f
        }
        for (i in 0..16) {
            val rx = s * (i * 0.06f)
            canvas.drawLine(rx, 0f, rx + s * 0.04f, s * 0.35f, rainPaint)
            canvas.drawLine(rx, s * 0.45f, rx + s * 0.04f, s * 0.85f, rainPaint)
        }

        // Vertical Neon Sign Glow bars on side margins
        val cyanNeon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00E5FF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.005f
        }
        val pinkNeon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF1493.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.005f
        }

        // Left Neon Sign
        canvas.drawLine(s * 0.04f, s * 0.18f, s * 0.04f, s * 0.45f, cyanNeon)
        canvas.drawLine(s * 0.04f, s * 0.55f, s * 0.04f, s * 0.78f, pinkNeon)

        // Right Neon Sign
        canvas.drawLine(s * 0.96f, s * 0.18f, s * 0.96f, s * 0.45f, pinkNeon)
        canvas.drawLine(s * 0.96f, s * 0.55f, s * 0.96f, s * 0.78f, cyanNeon)

        // Framing brackets
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00E5FF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.008f
        }
        val p = Path().apply {
            moveTo(s * 0.026f, s * 0.10f); lineTo(s * 0.026f, s * 0.026f); lineTo(s * 0.10f, s * 0.026f)
            moveTo(s * 0.90f, s * 0.026f); lineTo(s * 0.974f, s * 0.026f); lineTo(s * 0.974f, s * 0.10f)
            moveTo(s * 0.026f, s * 0.90f); lineTo(s * 0.026f, s * 0.974f); lineTo(s * 0.10f, s * 0.974f)
            moveTo(s * 0.90f, s * 0.974f); lineTo(s * 0.974f, s * 0.974f); lineTo(s * 0.974f, s * 0.90f)
        }
        canvas.drawPath(p, cornerPaint)
    }

    // 10. Solar Eclipse - Radiant celestial corona with blazing solar flares & particle sparks
    private fun renderSolarFlare(canvas: Canvas, s: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF040308.toInt()
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Radiant Solar Corona Flares (Center Eclipse)
        val coronaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s * 0.5f, s * 0.5f, s * 0.48f, 0xFFFFA000.toInt(), 0x00FF3D00, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.48f, coronaPaint)

        // Solar Flare Filaments shooting out radially
        val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD54F.toInt()
            strokeWidth = s * 0.004f
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 18) {
            val angle = (i * 2.0 * Math.PI / 18).toFloat()
            val r1 = s * 0.38f
            val r2 = s * (0.44f + (i % 3) * 0.035f)
            canvas.drawLine(
                s * 0.5f + cos(angle) * r1, s * 0.5f + sin(angle) * r1,
                s * 0.5f + cos(angle) * r2, s * 0.5f + sin(angle) * r2,
                flarePaint
            )
        }

        // Dark Eclipse Moon Sphere (Center)
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF09080F.toInt()
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.36f, moonPaint)

        // Golden Solar Corona Outer Frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFB300.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.007f
        }
        canvas.drawRoundRect(RectF(s * 0.026f, s * 0.026f, s * 0.974f, s * 0.974f), s * 0.035f, s * 0.035f, borderPaint)

        // Solar sparks at corners
        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFE082.toInt() }
        canvas.drawCircle(s * 0.08f, s * 0.08f, s * 0.01f, sparkPaint)
        canvas.drawCircle(s * 0.92f, s * 0.08f, s * 0.01f, sparkPaint)
        canvas.drawCircle(s * 0.08f, s * 0.92f, s * 0.01f, sparkPaint)
        canvas.drawCircle(s * 0.92f, s * 0.92f, s * 0.01f, sparkPaint)
    }
}

