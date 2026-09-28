package com.example.qr.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.LruCache
import kotlin.math.cos
import kotlin.math.sin

data class RasterFrameInfo(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val bgColor: Int
)

object RasterFrameGenerator {

    private val frameCache = LruCache<String, Bitmap>(30)

    val list = listOf(
        RasterFrameInfo("mahogany_wood", "Polished Mahogany Wood", "Wooden", "Rich dark mahogany wood grain with brass corner brackets", 0xFF2B140E.toInt()),
        RasterFrameInfo("rustic_oak", "Rustic Oak Wood", "Wooden", "Natural warm oak wood planks with carved bevel border", 0xFF6E3F23.toInt()),
        RasterFrameInfo("bioluminescent_wisteria", "Bioluminescent Wisteria", "Floral", "Glowing purple wisteria flowers & cyan vines on pitch obsidian", 0xFF0B0C10.toInt()),
        RasterFrameInfo("fiery_ember_rose", "Fiery Ember Rose", "Floral", "Glowing fiery orange and red roses with burning embers", 0xFF0F0404.toInt()),
        RasterFrameInfo("coral_wildflower", "Coral Wildflower", "Floral", "Luminous coral pink wildflowers on deep navy", 0xFF0A0F1D.toInt()),
        RasterFrameInfo("electric_cyan_flora", "Electric Cyan Flora", "Floral", "Neon cyan and sapphire blue glowing flowers", 0xFF030712.toInt()),
        RasterFrameInfo("sakura_blossom", "Sakura Cherry Blossom", "Floral", "Floating painterly pink sakura blossoms with magenta petals", 0xFF110414.toInt()),
        RasterFrameInfo("cyberpunk_grid", "Cyberpunk 2099 Grid", "Cyberpunk", "High-tech perspective neon laser grid with circuit nodes", 0xFF040914.toInt()),
        RasterFrameInfo("gold_marble", "Black Gold Marble", "Art", "Polished black carrara marble stone with 24k gold leaf veins", 0xFF0E0F17.toInt()),
        RasterFrameInfo("oil_canvas", "Impasto Oil Painting", "Art", "Textured oil painting canvas with rich cobalt & gold paint strokes", 0xFF1E1B4B.toInt()),
        RasterFrameInfo("jungle_nature", "Lush Jungle Nature", "Nature", "Tropical monstera leaves & fern fronds in emerald green", 0xFF02120A.toInt()),
        RasterFrameInfo("japanese_washi", "Japanese Washi & Gold", "Art", "Handmade fibrous washi paper texture with gold foil flakes", 0xFFFAF7F2.toInt()),
        RasterFrameInfo("cosmic_nebula", "Deep Cosmic Nebula", "Art", "Deep galaxy space nebula with violet gas clouds and stardust", 0xFF0A0A14.toInt()),
        RasterFrameInfo("industrial_steel", "Industrial Steel & Leather", "Cyberpunk", "Embossed dark leather with riveted brushed steel armor plates", 0xFF0F172A.toInt())
    )

    fun getFrameBitmap(id: String, sizePx: Int = 1024): Bitmap {
        val cacheKey = "${id}_$sizePx"
        val cached = frameCache.get(cacheKey)
        if (cached != null) return cached

        val bitmap = generateFrameBitmapInternal(id, sizePx)
        frameCache.put(cacheKey, bitmap)
        return bitmap
    }

    private fun generateFrameBitmapInternal(id: String, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = sizePx.toFloat()
        val margin = s * 0.055f // Outer 5.5% margin reserved for frame art

        when (id) {
            "mahogany_wood" -> drawMahoganyWoodFrame(canvas, s, margin)
            "rustic_oak" -> drawRusticOakFrame(canvas, s, margin)
            "bioluminescent_wisteria" -> drawBioluminescentFloraFrame(canvas, s, margin, "wisteria")
            "fiery_ember_rose" -> drawBioluminescentFloraFrame(canvas, s, margin, "fiery-rose")
            "coral_wildflower" -> drawBioluminescentFloraFrame(canvas, s, margin, "coral")
            "electric_cyan_flora" -> drawBioluminescentFloraFrame(canvas, s, margin, "electric-teal")
            "sakura_blossom" -> drawBioluminescentFloraFrame(canvas, s, margin, "sakura")
            "cyberpunk_grid" -> drawCyberpunkGridFrame(canvas, s, margin)
            "gold_marble" -> drawGoldMarbleFrame(canvas, s, margin)
            "oil_canvas" -> drawOilCanvasFrame(canvas, s, margin)
            "jungle_nature" -> drawJungleNatureFrame(canvas, s, margin)
            "japanese_washi" -> drawJapaneseWashiFrame(canvas, s, margin)
            "cosmic_nebula" -> drawCosmicNebulaFrame(canvas, s, margin)
            "industrial_steel" -> drawIndustrialSteelFrame(canvas, s, margin)
            else -> drawMahoganyWoodFrame(canvas, s, margin)
        }

        return bitmap
    }

    // --- 1. POLISHED MAHOGANY WOOD FRAME ---
    private fun drawMahoganyWoodFrame(canvas: Canvas, s: Float, margin: Float) {
        // Base dark wood mahogany
        val woodBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, 0xFF2B140E.toInt(), 0xFF140804.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, woodBg)

        // Wood grain rings & fibers
        val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x223D1F16
            style = Paint.Style.STROKE
            strokeWidth = s * 0.003f
        }
        val ringCount = 28
        for (i in 1..ringCount) {
            val r = s * 0.7f * (i.toFloat() / ringCount)
            canvas.drawCircle(s / 2f, s / 2f, r, grainPaint)
        }

        // Sapwood fiber streaks
        val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x1A421E14
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }
        val path = Path()
        for (i in 0..16) {
            val y = s * (i / 16f)
            path.moveTo(0f, y)
            path.cubicTo(s * 0.3f, y + s * 0.02f, s * 0.7f, y - s * 0.02f, s, y)
        }
        canvas.drawPath(path, streakPaint)

        // Brass corner plates with rivets
        val brassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s * 0.08f, s * 0.08f, 0xFFF59E0B.toInt(), 0xFF78350F.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        val rivetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFEF08A.toInt(); style = Paint.Style.FILL }

        val cornerSize = s * 0.045f
        val corners = listOf(
            Pair(0f, 0f),
            Pair(s - cornerSize, 0f),
            Pair(0f, s - cornerSize),
            Pair(s - cornerSize, s - cornerSize)
        )
        for ((cx, cy) in corners) {
            canvas.drawRect(RectF(cx, cy, cx + cornerSize, cy + cornerSize), brassPaint)
            canvas.drawCircle(cx + cornerSize / 2f, cy + cornerSize / 2f, s * 0.004f, rivetPaint)
        }

        // Inner Wood Void
        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF140804.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)

        // Polished brass keyline
        val keylinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.003f
        }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), keylinePaint)
    }

    // --- 2. RUSTIC OAK WOOD FRAME ---
    private fun drawRusticOakFrame(canvas: Canvas, s: Float, margin: Float) {
        val oakBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, 0xFF8C5A3C.toInt(), 0xFF543422.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, oakBg)

        // Plank lines
        val plankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3D2114.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.004f
        }
        canvas.drawLine(0f, s * 0.25f, s, s * 0.25f, plankPaint)
        canvas.drawLine(0f, s * 0.50f, s, s * 0.50f, plankPaint)
        canvas.drawLine(0f, s * 0.75f, s, s * 0.75f, plankPaint)

        // Wood knot swirls
        val knotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x333D2114
            style = Paint.Style.STROKE
            strokeWidth = s * 0.002f
        }
        canvas.drawOval(RectF(s * 0.02f, s * 0.1f, s * 0.05f, s * 0.2f), knotPaint)
        canvas.drawOval(RectF(s * 0.95f, s * 0.8f, s * 0.98f, s * 0.9f), knotPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3D2114.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private data class FloraPalette(
        val vineC: Long,
        val glowC: Long,
        val leafC: Long,
        val wisteriaC: Long,
        val hlC: Long,
        val filigreeC: Long,
        val sparkleC: Long,
        val bgC: Long
    )

    // --- 3. BIOLUMINESCENT FLORAL RASTER FRAME ---
    private fun drawBioluminescentFloraFrame(canvas: Canvas, s: Float, margin: Float, theme: String) {
        val palette = when (theme) {
            "fiery-rose" -> FloraPalette(0xFFF59E0B, 0xFFF97316, 0xFFEF4444, 0xFFDC2626, 0xFFFDE047, 0xFFF59E0B, 0xFFFCD34D, 0xFF0F0404)
            "coral" -> FloraPalette(0xFFFB923C, 0xFFFB7185, 0xFFF43F5E, 0xFFFB7185, 0xFFFECDD3, 0xFFFBBF24, 0xFFFEF08A, 0xFF0A0F1D)
            "electric-teal" -> FloraPalette(0xFF06B6D4, 0xFF3B82F6, 0xFF22D3EE, 0xFF38BDF8, 0xFFE0F2FE, 0xFF60A5FA, 0xFF93C5FD, 0xFF030712)
            "sakura" -> FloraPalette(0xFFF472B6, 0xFFE11D48, 0xFFFB7185, 0xFFF472B6, 0xFFFCE7F3, 0xFFF43F5E, 0xFFFBCFE8, 0xFF110414)
            else -> FloraPalette(0xFF06B6D4, 0xFFA855F7, 0xFF2DD4BF, 0xFFC084FC, 0xFFE9D5FF, 0xFFF59E0B, 0xFFFDE047, 0xFF0B0C10)
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.bgC.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Outer glow halos
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s / 2f, s / 2f, s * 0.7f, palette.glowC.toInt(), palette.bgC.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
            alpha = 40
        }
        canvas.drawRect(0f, 0f, s, s, glowPaint)

        val vinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.vineC.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.005f
            strokeCap = Paint.Cap.ROUND
        }

        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.leafC.toInt(); style = Paint.Style.FILL }
        val floralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.wisteriaC.toInt(); style = Paint.Style.FILL }
        val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.hlC.toInt(); style = Paint.Style.FILL }
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.sparkleC.toInt(); style = Paint.Style.FILL }

        // Continuous delicate vine wave paths
        val steps = 32
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = s * (0.04f + t * 0.92f)
            val wave = sin(t * Math.PI * 8).toFloat() * s * 0.006f

            val topY = s * 0.020f + wave
            val botY = s * 0.980f - wave

            if (i % 3 == 0) {
                // Top blossoms
                canvas.drawCircle(x, topY + s * 0.010f, s * 0.012f, floralPaint)
                canvas.drawCircle(x, topY + s * 0.010f, s * 0.005f, hlPaint)
                // Bottom blossoms
                canvas.drawCircle(x, botY - s * 0.010f, s * 0.012f, floralPaint)
                canvas.drawCircle(x, botY - s * 0.010f, s * 0.005f, hlPaint)
            }

            if (i % 2 == 1) {
                canvas.drawCircle(x, topY - s * 0.005f, s * 0.0025f, sparklePaint)
                canvas.drawCircle(x, botY + s * 0.005f, s * 0.0025f, sparklePaint)
            }
        }

        // Inner Void
        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.bgC.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)

        val keylinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = s * 0.002f
        }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), keylinePaint)
    }

    // --- 4. CYBERPUNK 2099 GRID FRAME ---
    private fun drawCyberpunkGridFrame(canvas: Canvas, s: Float, margin: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF040914.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Neon laser perspective grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x4400F0FF
            style = Paint.Style.STROKE
            strokeWidth = s * 0.002f
        }
        for (i in 0..20) {
            val pos = s * (i / 20f)
            canvas.drawLine(pos, 0f, pos, margin, gridPaint)
            canvas.drawLine(pos, s - margin, pos, s, gridPaint)
            canvas.drawLine(0f, pos, margin, pos, gridPaint)
            canvas.drawLine(s - margin, pos, s, pos, gridPaint)
        }

        // Outer neon cyan border
        val neonCyan = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00F0FF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.006f
        }
        canvas.drawRect(RectF(s * 0.01f, s * 0.01f, s * 0.99f, s * 0.99f), neonCyan)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF040914.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    // --- 5. BLACK GOLD MARBLE FRAME ---
    private fun drawGoldMarbleFrame(canvas: Canvas, s: Float, margin: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, 0xFF0E0F17.toInt(), 0xFF06070B.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Gold leaf foil veining
        val goldVein = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, 0f, 0xFFF59E0B.toInt(), 0xFFFEF08A.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.STROKE
            strokeWidth = s * 0.004f
        }
        val veinPath = Path().apply {
            moveTo(0f, s * 0.2f)
            cubicTo(s * 0.3f, s * 0.05f, s * 0.6f, s * 0.35f, s, s * 0.15f)
            moveTo(0f, s * 0.85f)
            cubicTo(s * 0.4f, s * 0.95f, s * 0.7f, s * 0.65f, s, s * 0.80f)
        }
        canvas.drawPath(veinPath, goldVein)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF06070B.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)

        val goldRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF59E0B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.003f
        }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), goldRim)
    }

    // --- 6. IMPASTO OIL CANVAS FRAME ---
    private fun drawOilCanvasFrame(canvas: Canvas, s: Float, margin: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, 0xFF1E1B4B.toInt(), 0xFF0284C7.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Thick oil paint brush strokes
        val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x44F59E0B
            style = Paint.Style.STROKE
            strokeWidth = s * 0.02f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(0f, s * 0.03f, s, s * 0.03f, brushPaint)
        canvas.drawLine(0f, s * 0.97f, s, s * 0.97f, brushPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    // --- 7. LUSH JUNGLE NATURE FRAME ---
    private fun drawJungleNatureFrame(canvas: Canvas, s: Float, margin: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF02120A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10B981.toInt(); style = Paint.Style.FILL }
        val steps = 24
        for (i in 0 until steps) {
            val pos = s * (i.toFloat() / steps)
            drawLeafShape(canvas, pos, s * 0.02f, s * 0.015f, leafPaint)
            drawLeafShape(canvas, pos, s * 0.98f, s * 0.015f, leafPaint)
            drawLeafShape(canvas, s * 0.02f, pos, s * 0.015f, leafPaint)
            drawLeafShape(canvas, s * 0.98f, pos, s * 0.015f, leafPaint)
        }

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF02120A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    // --- 8. JAPANESE WASHI PAPER FRAME ---
    private fun drawJapaneseWashiFrame(canvas: Canvas, s: Float, margin: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFAF7F2.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        // Fibrous speckles
        val speckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x228C5A3C; style = Paint.Style.FILL }
        for (i in 0..40) {
            val sx = (Math.random() * s).toFloat()
            val sy = (Math.random() * s).toFloat()
            canvas.drawCircle(sx, sy, s * 0.002f, speckPaint)
        }

        // Indigo border keyline
        val indigoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E3A8A.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.005f
        }
        canvas.drawRect(RectF(s * 0.015f, s * 0.015f, s * 0.985f, s * 0.985f), indigoPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFAF7F2.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    // --- 9. COSMIC NEBULA GALAXY FRAME ---
    private fun drawCosmicNebulaFrame(canvas: Canvas, s: Float, margin: Float) {
        val spacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s / 2f, s / 2f, s * 0.7f, 0xFF7928CA.toInt(), 0xFF0A0A14.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, s, s, spacePaint)

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
        for (i in 0..30) {
            val sx = (Math.random() * s).toFloat()
            val sy = (Math.random() * s).toFloat()
            canvas.drawCircle(sx, sy, s * 0.002f, starPaint)
        }

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0A14.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    // --- 10. INDUSTRIAL STEEL & LEATHER FRAME ---
    private fun drawIndustrialSteelFrame(canvas: Canvas, s: Float, margin: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, s, s, bgPaint)

        val steelPlate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF334155.toInt(); style = Paint.Style.FILL }
        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF94A3B8.toInt(); style = Paint.Style.FILL }

        val cs = s * 0.045f
        canvas.drawRect(RectF(0f, 0f, cs, cs), steelPlate)
        canvas.drawRect(RectF(s - cs, 0f, s, cs), steelPlate)
        canvas.drawRect(RectF(0f, s - cs, cs, s), steelPlate)
        canvas.drawRect(RectF(s - cs, s - cs, s, s), steelPlate)

        canvas.drawCircle(cs / 2f, cs / 2f, s * 0.005f, boltPaint)
        canvas.drawCircle(s - cs / 2f, cs / 2f, s * 0.005f, boltPaint)
        canvas.drawCircle(cs / 2f, s - cs / 2f, s * 0.005f, boltPaint)
        canvas.drawCircle(s - cs / 2f, s - cs / 2f, s * 0.005f, boltPaint)

        val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(RectF(margin, margin, s - margin, s - margin), voidPaint)
    }

    private fun drawLeafShape(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - size)
            quadTo(cx + size * 0.6f, cy, cx, cy + size)
            quadTo(cx - size * 0.6f, cy, cx, cy - size)
            close()
        }
        canvas.drawPath(path, paint)
    }
}
