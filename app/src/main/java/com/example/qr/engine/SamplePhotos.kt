package com.example.qr.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

data class SamplePhotoItem(
    val id: String,
    val name: String,
    val description: String,
    val assetPath: String,
    val category: String = "Scenery"
)

object SamplePhotos {
    val list = listOf(
        // 30 Imported GitHub Frame Templates
        SamplePhotoItem("art-aurora-night", "Aurora Night", "Bioluminescent aurora night sky", "samples/aurora-night.png", "Art Frames"),
        SamplePhotoItem("art-aztec-sun", "Aztec Sun", "Ancient Aztec gold sun emblem", "samples/aztec-sun.png", "Art Frames"),
        SamplePhotoItem("art-coral-bloom", "Coral Bloom", "Vibrant coral floral burst", "samples/coral-bloom.png", "Art Frames"),
        SamplePhotoItem("art-coral-reef", "Coral Reef", "Deep sea aquatic coral reef", "samples/coral-reef.png", "Art Frames"),
        SamplePhotoItem("art-cosmic-nebula", "Cosmic Nebula", "Deep space stellar vortex", "samples/cosmic-nebula.png", "Art Frames"),
        SamplePhotoItem("art-crystal-ice", "Crystal Ice", "Frozen crystalline prism geometry", "samples/crystal-ice.png", "Art Frames"),
        SamplePhotoItem("art-deco-gold", "Deco Gold", "Art Deco gold filigree pattern", "samples/deco-gold.png", "Art Frames"),
        SamplePhotoItem("art-ember-roses", "Ember Roses", "Glowing fiery rose petals", "samples/ember-roses.png", "Art Frames"),
        SamplePhotoItem("art-emerald-fern", "Emerald Fern", "Lush tropical rainforest fern", "samples/emerald-fern.png", "Art Frames"),
        SamplePhotoItem("art-enchanted-forest", "Enchanted Forest", "Mystical moonlit woodland", "samples/enchanted-forest.png", "Art Frames"),
        SamplePhotoItem("art-festival-lights", "Festival Lights", "Festive glowing lantern bokeh", "samples/festival-lights.png", "Art Frames"),
        SamplePhotoItem("art-frost-blue", "Frost Blue", "Icy blue glacial crystal overlay", "samples/frost-blue.png", "Art Frames"),
        SamplePhotoItem("art-golden-lotus", "Golden Lotus", "Sacred golden lotus motif", "samples/golden-lotus.png", "Art Frames"),
        SamplePhotoItem("art-golden-roses", "Golden Roses", "Luxurious metallic gold roses", "samples/golden-roses.png", "Art Frames"),
        SamplePhotoItem("art-great-wave", "Great Wave", "Classic ukiyo-e ocean tsunami", "samples/great-wave.png", "Art Frames"),
        SamplePhotoItem("art-groovy-70s", "Groovy 70s", "Retro 1970s psychedelic waves", "samples/groovy-70s.png", "Art Frames"),
        SamplePhotoItem("art-holo-chrome", "Holo Chrome", "Holographic liquid mercury reflection", "samples/holo-chrome.png", "Art Frames"),
        SamplePhotoItem("art-kawaii-sweets", "Kawaii Sweets", "Playful pastel candy pattern", "samples/kawaii-sweets.png", "Art Frames"),
        SamplePhotoItem("art-marble-gold", "Marble Gold", "Carrara marble with gold veins", "samples/marble-gold.png", "Art Frames"),
        SamplePhotoItem("art-mosaic-lapis", "Mosaic Lapis", "Royal lapis lazuli mosaic tiles", "samples/mosaic-lapis.png", "Art Frames"),
        SamplePhotoItem("art-neon-grid", "Neon Grid", "Cyberpunk neon retrowave grid", "samples/neon-grid.png", "Art Frames"),
        SamplePhotoItem("art-phoenix-fire", "Phoenix Fire", "Majestic fiery phoenix wings", "samples/phoenix-fire.png", "Art Frames"),
        SamplePhotoItem("art-pixel-voxel", "Pixel Voxel", "3D isometric pixel block matrix", "samples/pixel-voxel.png", "Art Frames"),
        SamplePhotoItem("art-sakura-night", "Sakura Night", "Cherry blossom petals under starlight", "samples/sakura-night.png", "Art Frames"),
        SamplePhotoItem("art-silver-frost", "Silver Frost", "Metallic silver frost ornament", "samples/silver-frost.png", "Art Frames"),
        SamplePhotoItem("art-stained-glass", "Stained Glass", "Gothic cathedral stained glass window", "samples/stained-glass.png", "Art Frames"),
        SamplePhotoItem("art-steampunk", "Steampunk", "Victorian brass cogs & gears", "samples/steampunk.png", "Art Frames"),
        SamplePhotoItem("art-vintage-glam", "Vintage Glam", "Glamorous vintage golden flourish", "samples/vintage-glam.png", "Art Frames"),
        SamplePhotoItem("art-violet-stars", "Violet Stars", "Deep violet cosmic stardust", "samples/violet-stars.png", "Art Frames"),
        SamplePhotoItem("art-wisteria-glow", "Wisteria Glow", "Cascading glowing wisteria vines", "samples/wisteria-glow.png", "Art Frames"),

        // 20 Imported GitHub Frame Templates 2
        SamplePhotoItem("art-arabian-nights", "Arabian Nights", "Exotic star-lit desert nights", "samples/arabian-nights.png", "Art Frames"),
        SamplePhotoItem("art-autumn-harvest", "Autumn Harvest", "Golden autumn foliage and harvest theme", "samples/autumn-harvest.png", "Art Frames"),
        SamplePhotoItem("art-balloon-party", "Balloon Party", "Festive colorful celebration balloons", "samples/balloon-party.png", "Art Frames"),
        SamplePhotoItem("art-christmas-frost", "Christmas Frost", "Winter holiday snow and frosted ornament", "samples/christmas-frost.png", "Art Frames"),
        SamplePhotoItem("art-cocoa-cafe", "Cocoa Cafe", "Cozy warm coffee shop aesthetic", "samples/cocoa-cafe.png", "Art Frames"),
        SamplePhotoItem("art-dino-world", "Dino World", "Prehistoric Jurassic jungle theme", "samples/dino-world.png", "Art Frames"),
        SamplePhotoItem("art-fireworks-night", "Fireworks Night", "Sparkling night sky fireworks display", "samples/fireworks-night.png", "Art Frames"),
        SamplePhotoItem("art-halloween-night", "Halloween Night", "Spooky Halloween pumpkin and moonlight", "samples/halloween-night.png", "Art Frames"),
        SamplePhotoItem("art-moonlit-zodiac", "Moonlit Zodiac", "Celestial zodiac astrology constellation", "samples/moonlit-zodiac.png", "Art Frames"),
        SamplePhotoItem("art-music-groove", "Music Groove", "Rhythmic musical notes and soundwaves", "samples/music-groove.png", "Art Frames"),
        SamplePhotoItem("art-nautical-bay", "Nautical Bay", "Maritime anchor and sea harbor breeze", "samples/nautical-bay.png", "Art Frames"),
        SamplePhotoItem("art-pastel-dream", "Pastel Dream", "Soft pastel cloudscape dreamland", "samples/pastel-dream.png", "Art Frames"),
        SamplePhotoItem("art-rainy-april", "Rainy April", "Fresh spring raindrops and umbrellas", "samples/rainy-april.png", "Art Frames"),
        SamplePhotoItem("art-royal-baroque", "Royal Baroque", "Opulent royal baroque gold motif", "samples/royal-baroque.png", "Art Frames"),
        SamplePhotoItem("art-safari-savanna", "Safari Savanna", "African savanna sunset and wildlife", "samples/safari-savanna.png", "Art Frames"),
        SamplePhotoItem("art-sumi-ink", "Sumi Ink", "Traditional East Asian brush ink art", "samples/sumi-ink.png", "Art Frames"),
        SamplePhotoItem("art-travel-wonders", "Travel Wonders", "Global landmarks and travel adventure", "samples/travel-wonders.png", "Art Frames"),
        SamplePhotoItem("art-tropical-paradise", "Tropical Paradise", "Exotic palm beach and island vibes", "samples/tropical-paradise.png", "Art Frames"),
        SamplePhotoItem("art-wedding-rose", "Wedding Rose", "Elegant romantic wedding rose arch", "samples/wedding-rose.png", "Art Frames"),
        SamplePhotoItem("art-zen-mandala", "Zen Mandala", "Serene spiritual mandala pattern", "samples/zen-mandala.png", "Art Frames"),

        // Legacy / Alias mappings to ensure compatibility with preset IDs
        SamplePhotoItem("art-royal-gold", "Royal Gold", "Luxurious gold baroque filigree", "samples/deco-gold.png", "Art Frames"),
        SamplePhotoItem("art-cyberpunk", "Cyberpunk HUD", "Neon cyber circuit traces", "samples/neon-grid.png", "Art Frames"),
        SamplePhotoItem("art-jungle-ivy", "Jungle Ivy", "Lush botanical foliage", "samples/emerald-fern.png", "Art Frames"),
        SamplePhotoItem("art-cosmic-galaxy", "Cosmic Vortex", "Deep space nebula", "samples/cosmic-nebula.png", "Art Frames"),
        SamplePhotoItem("art-sakura-blossom", "Sakura Bloom", "Delicate pink cherry blossoms", "samples/sakura-night.png", "Art Frames"),
        SamplePhotoItem("art-neon-ring", "Neon Ring", "Vibrant circular light halo", "samples/festival-lights.png", "Art Frames"),
        SamplePhotoItem("art-ocean-waves", "Ocean Surf", "Dynamic cobalt wave swell", "samples/great-wave.png", "Art Frames"),
        SamplePhotoItem("art-mecha-steel", "Mecha Armor", "Industrial steel plates", "samples/steampunk.png", "Art Frames"),
        SamplePhotoItem("art-memphis-pop", "Memphis Pop", "Geometric color blocks", "samples/groovy-70s.png", "Art Frames"),
        SamplePhotoItem("art-retro-synthwave", "Synthwave Sunset", "Tropical palms & horizon", "samples/neon-grid.png", "Art Frames"),
        SamplePhotoItem("art-fiery-rose", "Ember Rose", "Glowing rose vines", "samples/ember-roses.png", "Art Frames"),
        SamplePhotoItem("art-purple-wisteria", "Purple Wisteria", "Cascading lavender blossoms", "samples/wisteria-glow.png", "Art Frames"),
        SamplePhotoItem("art-red-matrix", "Red Matrix", "Crimson sci-fi circuit HUD", "samples/neon-grid.png", "Art Frames"),
        SamplePhotoItem("art-3d-bubbles", "Liquid Bubbles", "Glossy glassmorphic spheres", "samples/holo-chrome.png", "Art Frames"),
        SamplePhotoItem("art-neon-voxels", "Neon Voxels", "Floating 3D isometric cubes", "samples/pixel-voxel.png", "Art Frames"),

        // Scenery & Textures
        SamplePhotoItem("mountain", "Summit", "Mountain peak against crisp alpine horizon", "samples/mountain.jpg", "Scenery"),
        SamplePhotoItem("lake", "Lake", "Tranquil crystal waters and subtle reflection", "samples/lake.jpg", "Scenery"),
        SamplePhotoItem("peony", "Peony", "Lush floral petal folds and rich velvet tones", "samples/peony.jpg", "Scenery"),
        SamplePhotoItem("ink", "Ink", "Deep Japanese sumi-e ink dispersion on washi paper", "samples/ink.jpg", "Scenery"),
        SamplePhotoItem("dunes", "Dunes", "Warm desert sand ridges and soft shadows", "samples/dunes.jpg", "Scenery"),
        SamplePhotoItem("dusk", "Dusk", "Golden hour sunset fading to indigo night", "samples/dusk.jpg", "Scenery"),
        SamplePhotoItem("mist", "Mist", "Ethereal morning forest vapor and quiet fog", "samples/mist.jpg", "Scenery"),
        SamplePhotoItem("aurora", "Aurora", "Bioluminescent arctic sky with polar radiance", "samples/aurora.jpg", "Scenery"),
        SamplePhotoItem("silk", "Silk", "Flowing draped silk textile folds", "samples/silk.jpg", "Scenery"),
        SamplePhotoItem("blossom", "Blossom", "Delicate spring cherry blossom branch", "samples/blossom.jpg", "Scenery"),
        SamplePhotoItem("marble", "Marble", "Polished white carrara marble stone veins", "samples/marble.jpg", "Scenery"),
        SamplePhotoItem("tide", "Tide", "Ocean swell foam and turquoise sea wash", "samples/tide.jpg", "Scenery")
    )

    private val sampleThumbnails = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()

    fun getThumbnail(context: Context, id: String, sizePx: Int = 160): Bitmap {
        return sampleThumbnails.getOrPut(id) {
            loadSampleBitmap(context, id, sizePx)
        }
    }

    fun loadSampleBitmap(context: Context, id: String, sizePx: Int = 1024): Bitmap {
        val item = list.find { it.id == id } ?: list.first()
        try {
            // Step 1: Decode image dimensions with bounds check
            val options = BitmapFactory.Options()
            context.assets.open(item.assetPath).use { rawStream ->
                java.io.BufferedInputStream(rawStream).use { stream ->
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(stream, null, options)
                }
            }

            // Step 2: Compute inSampleSize based on requested sizePx
            val origW = options.outWidth
            val origH = options.outHeight
            var sampleSize = 1
            if (origW > sizePx || origH > sizePx) {
                val halfW = origW / 2
                val halfH = origH / 2
                while ((halfW / sampleSize) >= sizePx && (halfH / sampleSize) >= sizePx) {
                    sampleSize *= 2
                }
            }

            // Step 3: Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = if (sizePx <= 240) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            }

            val decoded = context.assets.open(item.assetPath).use { rawStream ->
                java.io.BufferedInputStream(rawStream).use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }

            if (decoded != null) {
                return if (sizePx <= 240 && (decoded.width > sizePx || decoded.height > sizePx)) {
                    Bitmap.createScaledBitmap(decoded, sizePx, sizePx, true)
                } else {
                    decoded
                }
            }
        } catch (_: Exception) {}

        // Fallback synthetic painterly canvas
        return createFallbackBitmap(id, sizePx)
    }

    private fun createFallbackBitmap(id: String, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = sizePx.toFloat()

        if (ArtFrameRenderer.isArtFrame(id)) {
            ArtFrameRenderer.render(id, canvas, s)
            return bitmap
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (id) {
            "mountain", "dusk" -> {
                paint.shader = LinearGradient(0f, 0f, 0f, s, 0xFFFF7E5F.toInt(), 0xFFFEB47B.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
                val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF1C5.toInt() }
                canvas.drawCircle(s * 0.5f, s * 0.45f, s * 0.16f, sunPaint)
                val mtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2D142C.toInt() }
                val path = Path().apply {
                    moveTo(0f, s)
                    lineTo(0f, s * 0.7f)
                    lineTo(s * 0.35f, s * 0.45f)
                    lineTo(s * 0.65f, s * 0.62f)
                    lineTo(s * 0.85f, s * 0.5f)
                    lineTo(s, s * 0.68f)
                    lineTo(s, s)
                    close()
                }
                canvas.drawPath(path, mtnPaint)
            }
            "lake" -> {
                paint.shader = LinearGradient(0f, 0f, 0f, s, 0xFF1A365D.toInt(), 0xFF319795.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
                val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6FFFA.toInt(); alpha = 160 }
                canvas.drawCircle(s * 0.5f, s * 0.85f, s * 0.45f, wavePaint)
            }
            "peony", "blossom" -> {
                paint.shader = LinearGradient(0f, 0f, s, s, 0xFF831843.toInt(), 0xFFF472B6.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
                val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFCE7F3.toInt(); alpha = 200 }
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.3f, petalPaint)
            }
            "ink" -> {
                paint.shader = LinearGradient(0f, 0f, s, s, 0xFFF4EFE6.toInt(), 0xFFE2D9C8.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
                val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111827.toInt(); alpha = 230 }
                canvas.drawCircle(s * 0.45f, s * 0.5f, s * 0.28f, inkPaint)
                canvas.drawCircle(s * 0.65f, s * 0.4f, s * 0.18f, inkPaint)
            }
            "dunes" -> {
                paint.shader = LinearGradient(0f, 0f, 0f, s, 0xFFB45309.toInt(), 0xFFFDE68A.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
            }
            "mist" -> {
                paint.shader = LinearGradient(0f, 0f, 0f, s, 0xFF374151.toInt(), 0xFF9CA3AF.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
            }
            "aurora" -> {
                paint.shader = LinearGradient(0f, 0f, 0f, s, 0xFF050B14.toInt(), 0xFF0D253A.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
                val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(s * 0.5f, s * 0.4f, s * 0.6f, 0xFF00FF88.toInt(), 0x00000000, Shader.TileMode.CLAMP)
                    alpha = 180
                }
                canvas.drawCircle(s * 0.5f, s * 0.4f, s * 0.6f, wavePaint)
            }
            "silk" -> {
                paint.shader = LinearGradient(0f, 0f, s, s, 0xFF4A044E.toInt(), 0xFFC084FC.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
            }
            "marble" -> {
                paint.shader = LinearGradient(0f, 0f, s, s, 0xFFE2E8F0.toInt(), 0xFFFFFFFF.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
            }
            "tide" -> {
                paint.shader = LinearGradient(0f, 0f, 0f, s, 0xFF0C4A6E.toInt(), 0xFF38BDF8.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
            }
            else -> {
                paint.shader = LinearGradient(0f, 0f, s, s, 0xFF2C3E50.toInt(), 0xFF3498DB.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, s, s, paint)
            }
        }
        return bitmap
    }
}
