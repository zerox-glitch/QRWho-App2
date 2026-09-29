package com.example.qr.engine

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

object QrPresets {

    private val _presets = mutableListOf<QrPreset>()
    val list: List<QrPreset>
        get() = if (_presets.isNotEmpty()) _presets else fallbackList

    val categories: List<String> = listOf(
        "All",
        "🌟 Iconic Art Frames",
        "Botanical Art",
        "Neon & Tech",
        "Luxury & Fashion",
        "Minimal & Swiss",
        "Retro & Vintage",
        "Nature & Organic",
        "Creative Art"
    )

    fun initialize(context: Context) {
        if (_presets.isNotEmpty()) return
        try {
            val jsonString = context.assets.open("qrwho/presets.json").bufferedReader().use { it.readText() }
            val array = JSONArray(jsonString)
            val parsed = mutableListOf<QrPreset>()

            // Prepend flagship showcase presets
            parsed.addAll(fallbackList)

            val removedIds = setOf(
                "pro-alpine-mountain", "art-alpine-mountain",
                "pro-glass-bubbles", "art-glassmorphism-liquid",
                "pro-lush-leaves", "art-lush-jungle",
                "pro-red-matrix", "art-red-matrix",
                "pro-sunset-palm", "art-synthwave-sunset",
                "pro-neon-vortex", "art-neon-vortex",
                "pro-multicolor-geo", "art-multicolor-geometric"
            )

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "preset_$i")
                if (removedIds.contains(id)) continue

                val rawCat = obj.optString("category", "Creative Art")
                val category = normalizeCategory(rawCat)
                val name = obj.optString("name", "Preset $i")
                val blurb = obj.optString("blurb", "")
                val featured = obj.optBoolean("featured", false)

                val styleObj = obj.optJSONObject("style")
                val style = if (styleObj != null) parseStyle(styleObj) else QrStyle()

                parsed.add(QrPreset(id, name, category, blurb, style, featured))
            }

            if (parsed.isNotEmpty()) {
                _presets.clear()
                _presets.addAll(parsed)
            }
        } catch (_: Exception) {
            // Fallback is used automatically
        }
    }

    private fun normalizeCategory(cat: String): String {
        return when (cat.trim().lowercase()) {
            "botanical art", "botanical", "flowers", "flora" -> "Botanical Art"
            "neon & tech", "neon", "tech", "cyber", "pulse", "grid", "frames", "pro frames" -> "Neon & Tech"
            "luxury & fashion", "luxury", "gold", "fashion", "luxe", "editorial" -> "Luxury & Fashion"
            "minimal & swiss", "minimal", "classic", "clean", "mono", "architectural" -> "Minimal & Swiss"
            "retro & vintage", "retro", "vintage", "synth", "vaporwave" -> "Retro & Vintage"
            "nature & organic", "nature", "scenes", "photo", "ember", "organic", "cafe" -> "Nature & Organic"
            else -> "Creative Art"
        }
    }

    private fun parseStyle(o: JSONObject): QrStyle {
        val moduleShapeStr = o.optString("moduleShape", "rounded")
        val eyeShapeStr = o.optString("eyeShape", "rounded")
        val ballShapeStr = o.optString("ballShape", "circle")
        val fgStr = o.optString("fg", "#0f172a")
        val bgStr = o.optString("bg", "#ffffff")
        val eyeColorStr = o.optString("eyeColor", fgStr)
        val ballColorStr = o.optString("ballColor", fgStr)
        val gradTypeStr = o.optString("gradientType", "none")
        val gradToStr = o.optString("gradientTo", fgStr)

        val frameStyleStr = o.optString("frameStyle", "none")
        val frameCaptionStr = o.optString("frameCaption", "SCAN ME")

        return QrStyle(
            moduleShape = ModuleShape.fromString(moduleShapeStr),
            eyeShape = EyeShape.fromString(eyeShapeStr),
            ballShape = EyeShape.fromString(ballShapeStr),
            fgColor = safeColor(fgStr, 0xFF0F172A.toInt()),
            bgColor = safeColor(bgStr, 0xFFFFFFFF.toInt()),
            eyeColor = safeColor(eyeColorStr, 0xFF0F172A.toInt()),
            ballColor = safeColor(ballColorStr, 0xFF0F172A.toInt()),
            gradientType = GradientType.fromString(gradTypeStr),
            gradientTo = safeColor(gradToStr, 0xFF7A5AF8.toInt()),
            quietZone = o.optInt("quietZone", 3),
            moduleGap = o.optDouble("moduleGap", 0.04).toFloat(),
            imageMode = ImageMode.fromString(o.optString("imageMode", "none")),
            imageOpacity = o.optDouble("imageOpacity", 0.86).toFloat(),
            photoZoom = o.optDouble("photoZoom", 1.0).toFloat(),
            dotScale = o.optDouble("dotScale", 0.88).toFloat(),
            contrast = o.optDouble("contrast", 1.0).toFloat(),
            logoScale = o.optDouble("logoScale", 0.22).toFloat(),
            ecc = o.optString("ecc", "H"),
            artisticStrength = o.optDouble("artisticStrength", 0.5).toFloat(),
            artDirection = o.optString("artDirection", null),
            effect = QrEffect.fromString(o.optString("effect", "none")),
            effectIntensity = o.optDouble("effectIntensity", 1.0).toFloat(),
            frameStyle = FrameStyle.fromString(frameStyleStr),
            frameCaption = frameCaptionStr
        )
    }

    private fun safeColor(hex: String?, defaultColor: Int): Int {
        if (hex.isNullOrBlank()) return defaultColor
        return try {
            Color.parseColor(hex.trim())
        } catch (_: Exception) {
            defaultColor
        }
    }

    fun findById(id: String): QrPreset? = list.find { it.id == id }

    fun filter(query: String, category: String): List<QrPreset> {
        val q = query.trim().lowercase()
        return list.filter { preset ->
            val matchesCategory = category == "All" || preset.category.equals(category, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                    preset.name.lowercase().contains(q) ||
                    preset.description.lowercase().contains(q) ||
                    preset.category.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

    // Curated fallback including the flagship QRWho art showcase styles
    val fallbackList = listOf(
        // --- 🌟 ICONIC ART FRAMES (MATCHING USER'S SHOWCASE DESIGNS) ---
        QrPreset(
            id = "art-royal-gold",
            name = "Royal Gold Crown",
            category = "🌟 Iconic Art Frames",
            description = "Luxurious gold baroque filigree with royal crown on dark marble",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFD700.toInt(),
                bgColor = 0xFF0D0B0A.toInt(),
                eyeColor = 0xFFFFD700.toInt(),
                ballColor = 0xFFFFE082.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFF59E0B.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-royal-gold"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-cyberpunk",
            name = "Cyberpunk HUD Tech",
            category = "🌟 Iconic Art Frames",
            description = "Neon cyan & magenta glowing sci-fi circuit board with tech corners",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00F0FF.toInt(),
                bgColor = 0xFF050813.toInt(),
                eyeColor = 0xFF00F0FF.toInt(),
                ballColor = 0xFFFF007F.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFFD946EF.toInt(),
                quietZone = 3,
                dotScale = 0.88f,
                ecc = "H",
                artDirection = "art-cyberpunk"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-jungle-ivy",
            name = "Lush Jungle Ivy",
            category = "🌟 Iconic Art Frames",
            description = "Lush botanical climbing ivy foliage framing crisp white modules",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF0B2113.toInt(),
                eyeColor = 0xFF22C55E.toInt(),
                ballColor = 0xFF4ADE80.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.92f,
                ecc = "H",
                artDirection = "art-jungle-ivy"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-cosmic-galaxy",
            name = "Cosmic Galaxy Vortex",
            category = "🌟 Iconic Art Frames",
            description = "Deep space swirling nebula vortex with starry orbital ring",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF190633.toInt(),
                eyeColor = 0xFFA855F7.toInt(),
                ballColor = 0xFF06B6D4.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-cosmic-galaxy"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-sakura-blossom",
            name = "Sakura Cherry Blossom",
            category = "🌟 Iconic Art Frames",
            description = "Delicate Japanese pink cherry blossoms and floating petals on cream",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF2B1810.toInt(),
                bgColor = 0xFFFFF9F9.toInt(),
                eyeColor = 0xFFEC4899.toInt(),
                ballColor = 0xFFBE185D.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.92f,
                ecc = "H",
                artDirection = "art-sakura-blossom"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-neon-ring",
            name = "Neon Vortex Light Ring",
            category = "🌟 Iconic Art Frames",
            description = "Vibrant circular rainbow vortex halo around pure white modules",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF060608.toInt(),
                eyeColor = 0xFF00E5FF.toInt(),
                ballColor = 0xFFE040FB.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.88f,
                ecc = "H",
                artDirection = "art-neon-ring"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-ocean-waves",
            name = "Swirling Ocean Surf",
            category = "🌟 Iconic Art Frames",
            description = "Dynamic cobalt blue and turquoise wave crests with foam ripples",
            style = QrStyle(
                moduleShape = ModuleShape.Bubbles,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF02162E.toInt(),
                eyeColor = 0xFF38BDF8.toInt(),
                ballColor = 0xFF0284C7.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.92f,
                ecc = "H",
                artDirection = "art-ocean-waves"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-mecha-steel",
            name = "Industrial Mecha Armor",
            category = "🌟 Iconic Art Frames",
            description = "Heavy brushed steel armor plating with bevels and rivets",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF101418.toInt(),
                eyeColor = 0xFF94A3B8.toInt(),
                ballColor = 0xFFE2E8F0.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.92f,
                ecc = "H",
                artDirection = "art-mecha-steel"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-memphis-pop",
            name = "Memphis Pop Geometric",
            category = "🌟 Iconic Art Frames",
            description = "Playful angular color blocks in cyan, coral, yellow, and magenta",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0F172A.toInt(),
                bgColor = 0xFFF8FAFC.toInt(),
                eyeColor = 0xFF06B6D4.toInt(),
                ballColor = 0xFFF43F5E.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-memphis-pop"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-retro-synthwave",
            name = "Retro Synthwave Sunset",
            category = "🌟 Iconic Art Frames",
            description = "Tropical palm trees silhouette against glowing sun and neon sky",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1E0528.toInt(),
                bgColor = 0xFFFFF0F5.toInt(),
                eyeColor = 0xFFFF5E36.toInt(),
                ballColor = 0xFFFF007F.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-retro-synthwave"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-fiery-rose",
            name = "Burning Ember Rose",
            category = "🌟 Iconic Art Frames",
            description = "Glowing fiery orange and red rose vines with burning embers",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF59E0B.toInt(),
                bgColor = 0xFF0F0404.toInt(),
                eyeColor = 0xFFEF4444.toInt(),
                ballColor = 0xFFFBBF24.toInt(),
                gradientType = GradientType.Vertical,
                gradientTo = 0xFFEF4444.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-fiery-rose"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-purple-wisteria",
            name = "Glowing Purple Wisteria",
            category = "🌟 Iconic Art Frames",
            description = "Cascading wisteria blossoms and starlight floral vines on midnight indigo",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFC084FC.toInt(),
                bgColor = 0xFF0B061A.toInt(),
                eyeColor = 0xFFA855F7.toInt(),
                ballColor = 0xFFE879F9.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF818CF8.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-purple-wisteria"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-emerald-fern",
            name = "Bioluminescent Fern",
            category = "🌟 Iconic Art Frames",
            description = "Glowing mint and emerald rainforest ferns with exotic orchids",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF34D399.toInt(),
                bgColor = 0xFF04120C.toInt(),
                eyeColor = 0xFF10B981.toInt(),
                ballColor = 0xFF6EE7B7.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFF06B6D4.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-emerald-fern"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-red-matrix",
            name = "Red Cyber Matrix",
            category = "🌟 Iconic Art Frames",
            description = "Futuristic crimson red circuit traces on matte black void",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFEF4444.toInt(),
                bgColor = 0xFF080101.toInt(),
                eyeColor = 0xFFDC2626.toInt(),
                ballColor = 0xFFF87171.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFB91C1C.toInt(),
                quietZone = 3,
                dotScale = 0.92f,
                ecc = "H",
                artDirection = "art-red-matrix"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-3d-bubbles",
            name = "Glassmorphic Bubbles",
            category = "🌟 Iconic Art Frames",
            description = "Translucent 3D liquid bubbles in sky blue and purple on satin white",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF3B82F6.toInt(),
                bgColor = 0xFFF5F7FA.toInt(),
                eyeColor = 0xFF6366F1.toInt(),
                ballColor = 0xFF06B6D4.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF8B5CF6.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-3d-bubbles"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-neon-voxels",
            name = "3D Floating Voxels",
            category = "🌟 Iconic Art Frames",
            description = "Floating 3D isometric glowing cyan and magenta cube voxels",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF080612.toInt(),
                eyeColor = 0xFF06B6D4.toInt(),
                ballColor = 0xFFD946EF.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-neon-voxels"
            ),
            featured = true
        ),
        // 10 New Flagship Art Visual Presets
        QrPreset(
            id = "art-aurora-borealis",
            name = "Aurora Lights",
            category = "🌟 Iconic Art Frames",
            description = "Northern lights emerald and violet curtain over snowy peaks and starry night",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00FF87.toInt(),
                bgColor = 0xFF020617.toInt(),
                eyeColor = 0xFF00FF87.toInt(),
                ballColor = 0xFF38BDF8.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF8B5CF6.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-aurora-borealis"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-cyber-samurai",
            name = "Cyber Katana",
            category = "🌟 Iconic Art Frames",
            description = "Obsidian carbon fiber armor with neon crimson and electric cyan blade slashes",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00F0FF.toInt(),
                bgColor = 0xFF07090E.toInt(),
                eyeColor = 0xFFFF0055.toInt(),
                ballColor = 0xFF00F0FF.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFFF0055.toInt(),
                quietZone = 3,
                dotScale = 0.88f,
                ecc = "H",
                artDirection = "art-cyber-samurai"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-golden-kintsugi",
            name = "Golden Kintsugi",
            category = "🌟 Iconic Art Frames",
            description = "Japanese wabi-sabi crackled ivory porcelain with molten gold veins and dust",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF2A241F.toInt(),
                bgColor = 0xFFFAF7F2.toInt(),
                eyeColor = 0xFFD4AF37.toInt(),
                ballColor = 0xFFB45309.toInt(),
                gradientType = GradientType.Radial,
                gradientTo = 0xFFD4AF37.toInt(),
                quietZone = 3,
                dotScale = 0.92f,
                ecc = "H",
                artDirection = "art-golden-kintsugi"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-tropical-sunset",
            name = "Paradise Palm",
            category = "🌟 Iconic Art Frames",
            description = "Radiant tropical sunset gradient with swaying palm tree silhouettes",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFF176.toInt(),
                bgColor = 0xFF180A2A.toInt(),
                eyeColor = 0xFFFF4500.toInt(),
                ballColor = 0xFFFF758C.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFFFF4500.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-tropical-sunset"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-midnight-lotus",
            name = "Midnight Lotus",
            category = "🌟 Iconic Art Frames",
            description = "Luminescent pink lotus petals and jade leaves floating on moonlit obsidian waters",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF43F5E.toInt(),
                bgColor = 0xFF030712.toInt(),
                eyeColor = 0xFF10B981.toInt(),
                ballColor = 0xFFF43F5E.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF06B6D4.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-midnight-lotus"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-holo-prism",
            name = "Holographic Prism",
            category = "🌟 Iconic Art Frames",
            description = "Iridescent chromatic rainbow sheen with geometric diamond facet shards",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF0F172A.toInt(),
                eyeColor = 0xFFB967FF.toInt(),
                ballColor = 0xFF01CDFE.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF05FFA1.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-holo-prism"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-steam-punk",
            name = "Clockwork Chrono",
            category = "🌟 Iconic Art Frames",
            description = "Victorian steampunk brass and copper cogwheels with riveted armor boiler plate",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFD97706.toInt(),
                bgColor = 0xFF1C130D.toInt(),
                eyeColor = 0xFFF59E0B.toInt(),
                ballColor = 0xFFB45309.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF92400E.toInt(),
                quietZone = 3,
                dotScale = 0.92f,
                ecc = "H",
                artDirection = "art-steam-punk"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-crystal-geode",
            name = "Amethyst Geode",
            category = "🌟 Iconic Art Frames",
            description = "Royal purple quartz crystalline cavern with glittering amethyst facets and gold leaf",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFC084FC.toInt(),
                bgColor = 0xFF0F051D.toInt(),
                eyeColor = 0xFFA855F7.toInt(),
                ballColor = 0xFFFBBF24.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF7E22CE.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-crystal-geode"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-neon-noir",
            name = "Neon Tokyo Rain",
            category = "🌟 Iconic Art Frames",
            description = "Rain-slicked cyberpunk street reflections with glowing cyan and pink kanji neon signs",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00E5FF.toInt(),
                bgColor = 0xFF080811.toInt(),
                eyeColor = 0xFFFF1493.toInt(),
                ballColor = 0xFF00E5FF.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFFFF1493.toInt(),
                quietZone = 3,
                dotScale = 0.88f,
                ecc = "H",
                artDirection = "art-neon-noir"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-solar-flare",
            name = "Solar Eclipse",
            category = "🌟 Iconic Art Frames",
            description = "Celestial eclipse with blazing solar corona flare filaments and cosmic ring",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFD54F.toInt(),
                bgColor = 0xFF040308.toInt(),
                eyeColor = 0xFFFFA000.toInt(),
                ballColor = 0xFFFFB300.toInt(),
                gradientType = GradientType.Radial,
                gradientTo = 0xFFFF5722.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-solar-flare"
            ),
            featured = true
        ),

        // --- BOTANICAL ART CATEGORY ---
        QrPreset(
            id = "pro-wisteria",
            name = "Wisteria Violet Glow",
            category = "Botanical Art",
            description = "Bioluminescent purple wisteria vines with zero-interference dark void",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF38BDF8.toInt(),
                bgColor = 0xFF0B0C10.toInt(),
                eyeColor = 0xFFC084FC.toInt(),
                ballColor = 0xFFF59E0B.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFA855F7.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-wisteria"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-fiery-rose",
            name = "Fiery Ember Rose",
            category = "Botanical Art",
            description = "Glowing fiery orange and red rose vines with burning embers",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF59E0B.toInt(),
                bgColor = 0xFF0F0404.toInt(),
                eyeColor = 0xFFF97316.toInt(),
                ballColor = 0xFFEF4444.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFDC2626.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-fiery-rose"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-coral-wildflower",
            name = "Coral Wildflower",
            category = "Botanical Art",
            description = "Luminous coral pink and orange wildflowers on deep navy",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFB7185.toInt(),
                bgColor = 0xFF0A0F1D.toInt(),
                eyeColor = 0xFFFB923C.toInt(),
                ballColor = 0xFFFBBF24.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFF43F5E.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-coral-wildflower"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-electric-teal",
            name = "Electric Teal Flora",
            category = "Botanical Art",
            description = "Neon cyan and sapphire blue glowing flowers",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF22D3EE.toInt(),
                bgColor = 0xFF030712.toInt(),
                eyeColor = 0xFF3B82F6.toInt(),
                ballColor = 0xFF60A5FA.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF2563EB.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-electric-teal"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-sakura-blossom",
            name = "Sakura Cherry Blossom",
            category = "Botanical Art",
            description = "Floating pink sakura cherry blossoms with magenta petals",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF472B6.toInt(),
                bgColor = 0xFF110414.toInt(),
                eyeColor = 0xFFE11D48.toInt(),
                ballColor = 0xFFC084FC.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFDB2777.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-sakura"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-gold-filigree",
            name = "Gold Filigree Flora",
            category = "Botanical Art",
            description = "Shimmering golden sunflowers and delicate filigree",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFEAB308.toInt(),
                bgColor = 0xFF050505.toInt(),
                eyeColor = 0xFFFACC15.toInt(),
                ballColor = 0xFFD97706.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFFF59E0B.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-gold-filigree"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-emerald-fern",
            name = "Emerald Fern & Orchid",
            category = "Botanical Art",
            description = "Glowing green fern fronds with orchid purple accents",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF10B981.toInt(),
                bgColor = 0xFF02120A.toInt(),
                eyeColor = 0xFF34D399.toInt(),
                ballColor = 0xFFA855F7.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF059669.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-emerald-fern"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-icy-crystal",
            name = "Icy Crystal Frost",
            category = "Botanical Art",
            description = "Frost blue crystal flowers and silver filigree",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF38BDF8.toInt(),
                bgColor = 0xFF030A16.toInt(),
                eyeColor = 0xFF7DD3FC.toInt(),
                ballColor = 0xFFE0F2FE.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF0284C7.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-icy-crystal"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-rainbow-starburst",
            name = "Rainbow Starburst Flora",
            category = "Botanical Art",
            description = "Pastel rainbow glowing flowers with sparkling stardust",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFA78BFA.toInt(),
                bgColor = 0xFF090714.toInt(),
                eyeColor = 0xFF38BDF8.toInt(),
                ballColor = 0xFFF472B6.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFEC4899.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-rainbow"
            ),
            featured = true
        ),

        // --- PRO FRAMES CATEGORY ---
        QrPreset(
            id = "pro-royal-crown",
            name = "Royal Gold Crown",
            category = "Luxury & Fashion",
            description = "Polished gold crown with black marble veins and corner filigree",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFD97706.toInt(),
                bgColor = 0xFF0A0A0A.toInt(),
                eyeColor = 0xFFFBBF24.toInt(),
                ballColor = 0xFFF59E0B.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFFF59E0B.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "royal-crown"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-fluid-neon",
            name = "Fluid Wave Neon",
            category = "Neon & Tech",
            description = "Vibrant liquid neon orange to purple gradient border",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00F0FF.toInt(),
                bgColor = 0xFF06060E.toInt(),
                eyeColor = 0xFFA855F7.toInt(),
                ballColor = 0xFFFFFFFF.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFD946EF.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "fluid-neon-border"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-cyber-circuit",
            name = "Cyber Tech Circuit",
            category = "Neon & Tech",
            description = "Neon cyan circuit board border with corner tech nodes",
            style = QrStyle(
                moduleShape = ModuleShape.Dash,
                eyeShape = EyeShape.Ticks,
                ballShape = EyeShape.Square,
                fgColor = 0xFF00F0FF.toInt(),
                bgColor = 0xFF040914.toInt(),
                eyeColor = 0xFF22D3EE.toInt(),
                ballColor = 0xFFFFFFFF.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF0284C7.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "cyber-tech-circuit"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-zebra-waves",
            name = "Zebra Monochromatic",
            category = "Minimal & Swiss",
            description = "Dynamic black and white wavy zebra stripe border",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFF09090B.toInt(),
                bgColor = 0xFFFFFFFF.toInt(),
                eyeColor = 0xFF18181B.toInt(),
                ballColor = 0xFF18181B.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "zebra-stripe-border"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-modern-bracket",
            name = "Modern Tech Bracket",
            category = "Minimal & Swiss",
            description = "Sleek slate card with dark charcoal corner brackets",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Target,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0F172A.toInt(),
                bgColor = 0xFFF1F5F9.toInt(),
                eyeColor = 0xFF1E293B.toInt(),
                ballColor = 0xFF0F172A.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "modern-tech-bracket"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-cosmic-nebula",
            name = "Cosmic Nebula Galaxy",
            category = "Creative Art",
            description = "Deep space galaxy nebula with glowing violet cosmic ring",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF0A0A14.toInt(),
                eyeColor = 0xFF38BDF8.toInt(),
                ballColor = 0xFFEC4899.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "cosmic-nebula-ring"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-armor-steel",
            name = "Armor Steel Plate",
            category = "Minimal & Swiss",
            description = "Industrial metallic steel armor plate with corner rivets",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFFFFFFFF.toInt(),
                bgColor = 0xFF0F172A.toInt(),
                eyeColor = 0xFF334155.toInt(),
                ballColor = 0xFF94A3B8.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "armor-steel-plate"
            ),
            featured = true
        ),
        QrPreset(
            id = "pro-paint-splash",
            name = "Paint Splash Brush",
            category = "Creative Art",
            description = "Vibrant cyan, magenta and yellow paint brush stroke frame",
            style = QrStyle(
                moduleShape = ModuleShape.Fluid,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1E1B4B.toInt(),
                bgColor = 0xFFFFFFFF.toInt(),
                eyeColor = 0xFF4338CA.toInt(),
                ballColor = 0xFFD946EF.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "paint-splash-brush"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-bioluminescent",
            name = "Bioluminescent Wisteria",
            category = "Art",
            description = "Glow-in-the-dark wisteria botanical frame with zero-interference pitch obsidian void",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF38BDF8.toInt(),
                bgColor = 0xFF0B0C10.toInt(),
                eyeColor = 0xFFC084FC.toInt(),
                ballColor = 0xFFF59E0B.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFA855F7.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "bioluminescent-botanical"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-ukiyo",
            name = "Ukiyo Wave",
            category = "Art",
            description = "Ocean indigo on handmade washi texture with leaf finders",
            style = QrStyle(
                moduleShape = ModuleShape.Fluid,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Leaf,
                fgColor = 0xFF164E63.toInt(),
                bgColor = 0xFFF4EFE6.toInt(),
                eyeColor = 0xFF0E3A4A.toInt(),
                ballColor = 0xFF0E3A4A.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF1D7A8C.toInt(),
                moduleGap = 0.02f,
                dotScale = 0.88f,
                ecc = "H",
                artDirection = "ukiyo-e"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-cyberpunk",
            name = "Cyberpunk 2099",
            category = "Neon",
            description = "Electric magenta to cyan neon data stream on dark onyx",
            style = QrStyle(
                moduleShape = ModuleShape.Dash,
                eyeShape = EyeShape.Ticks,
                ballShape = EyeShape.Square,
                fgColor = 0xFFFF2A85.toInt(),
                bgColor = 0xFF0A0A14.toInt(),
                eyeColor = 0xFF00F0FF.toInt(),
                ballColor = 0xFF00F0FF.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF7928CA.toInt(),
                moduleGap = 0.04f,
                dotScale = 0.88f,
                ecc = "H",
                artDirection = "cyberpunk-pink"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-vaporwave",
            name = "Vaporwave 1995",
            category = "Retro",
            description = "Pastel lilac, mint green and electric blue dreamscape",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFFFF8AD4.toInt(),
                bgColor = 0xFF120A24.toInt(),
                eyeColor = 0xFF5FF0E6.toInt(),
                ballColor = 0xFFF4E9FF.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF8D72FF.toInt(),
                moduleGap = 0.05f,
                dotScale = 0.90f,
                ecc = "Q",
                artDirection = "vaporwave"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-royal",
            name = "Royal Gold",
            category = "Luxury",
            description = "Obsidian black with polished gold gradient & classy finders",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Rounded,
                fgColor = 0xFFC6A25A.toInt(),
                bgColor = 0xFF16140F.toInt(),
                eyeColor = 0xFFE8D09A.toInt(),
                ballColor = 0xFFF0E4C0.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFFF0E4C0.toInt(),
                moduleGap = 0.02f,
                dotScale = 0.88f,
                ecc = "H"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-sakura",
            name = "Sakura Bloom",
            category = "Pastel",
            description = "Two-lobe heart modules with emerald leaf eyes on blush cream",
            style = QrStyle(
                moduleShape = ModuleShape.Heart,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFD4457F.toInt(),
                bgColor = 0xFFFDF4F1.toInt(),
                eyeColor = 0xFFB8477C.toInt(),
                ballColor = 0xFFB8477C.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFFBE4A82.toInt(),
                moduleGap = 0.04f,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "sakura"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-matcha",
            name = "Matcha Latte",
            category = "Nature",
            description = "Earthy matcha green leaf modules on steamed cream",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Leaf,
                fgColor = 0xFF41644A.toInt(),
                bgColor = 0xFFF7F9F4.toInt(),
                eyeColor = 0xFF263E2D.toInt(),
                ballColor = 0xFF263E2D.toInt(),
                gradientType = GradientType.None,
                moduleGap = 0.04f,
                dotScale = 0.88f,
                ecc = "H"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-solarpunk",
            name = "Solarpunk Dawn",
            category = "Nature",
            description = "Radiant lime & golden solar amber on forest night",
            style = QrStyle(
                moduleShape = ModuleShape.Fluid,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Rounded,
                fgColor = 0xFF84CC16.toInt(),
                bgColor = 0xFF0C1708.toInt(),
                eyeColor = 0xFFFACC15.toInt(),
                ballColor = 0xFF65A30D.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFEAB308.toInt(),
                moduleGap = 0.02f,
                dotScale = 0.88f,
                ecc = "H"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-neon-fungi",
            name = "Neon Fungi",
            category = "Neon",
            description = "Bioluminescent emerald bubbles on pitch obsidian",
            style = QrStyle(
                moduleShape = ModuleShape.Bubbles,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF3DBA8B.toInt(),
                bgColor = 0xFF0B1210.toInt(),
                eyeColor = 0xFFD8F5EA.toInt(),
                ballColor = 0xFF22C3D6.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF22C3D6.toInt(),
                moduleGap = 0.02f,
                dotScale = 0.88f,
                ecc = "H"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-mono",
            name = "Mono Luxe",
            category = "Minimal",
            description = "Ultra-crisp geometric precision for architecture & design",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFF0E0E0E.toInt(),
                bgColor = 0xFFF7F7F5.toInt(),
                eyeColor = 0xFF0E0E0E.toInt(),
                ballColor = 0xFF0E0E0E.toInt(),
                gradientType = GradientType.None,
                moduleGap = 0.02f,
                dotScale = 0.88f,
                ecc = "H"
            ),
            featured = true
        ),
        // --- Custom Templates requested in UI Screenshots ---
        QrPreset(
            id = "tpl-classic",
            name = "Classic",
            category = "Templates",
            description = "Crisp high-contrast standard QR code style",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFF000000.toInt(),
                bgColor = 0xFFFFFFFF.toInt(),
                eyeColor = 0xFF000000.toInt(),
                ballColor = 0xFF000000.toInt(),
                gradientType = GradientType.None
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-globe",
            name = "Globe",
            category = "Templates",
            description = "Circular world globe frame with ocean blue tones",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1976D2.toInt(),
                bgColor = 0xFFE3F2FD.toInt(),
                eyeColor = 0xFF1565C0.toInt(),
                ballColor = 0xFF1565C0.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Globe,
                artDirection = "globe"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-ocean-blue",
            name = "Ocean Blue",
            category = "Templates",
            description = "Vibrant azure blue dots on soft cyan backdrop",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Rounded,
                fgColor = 0xFF1565C0.toInt(),
                bgColor = 0xFFFFFFFF.toInt(),
                eyeColor = 0xFF0D47A1.toInt(),
                ballColor = 0xFF0D47A1.toInt(),
                gradientType = GradientType.None
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-floral",
            name = "Floral",
            category = "Templates",
            description = "Soft rose pink modules with elegant floral corner accents",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFEC4899.toInt(),
                bgColor = 0xFFFFF0F5.toInt(),
                eyeColor = 0xFFDB2777.toInt(),
                ballColor = 0xFFDB2777.toInt(),
                gradientType = GradientType.None,
                artDirection = "floral"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-note",
            name = "Note",
            category = "Templates",
            description = "Sticky notebook page style with golden stitched border",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1E3A8A.toInt(),
                bgColor = 0xFFFEF08A.toInt(),
                eyeColor = 0xFF1E3A8A.toInt(),
                ballColor = 0xFF1E3A8A.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Note,
                artDirection = "note"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-love",
            name = "Love",
            category = "Templates",
            description = "Romantic pink theme with floating hearts",
            style = QrStyle(
                moduleShape = ModuleShape.Heart,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF43F5E.toInt(),
                bgColor = 0xFFFFF1F2.toInt(),
                eyeColor = 0xFFE11D48.toInt(),
                ballColor = 0xFFE11D48.toInt(),
                gradientType = GradientType.None,
                artDirection = "love"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-stars",
            name = "Stars",
            category = "Templates",
            description = "Starlight constellation theme with golden star finders",
            style = QrStyle(
                moduleShape = ModuleShape.Star,
                eyeShape = EyeShape.Diamond,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF64748B.toInt(),
                bgColor = 0xFFFFFFFF.toInt(),
                eyeColor = 0xFF475569.toInt(),
                ballColor = 0xFFF59E0B.toInt(),
                gradientType = GradientType.None,
                artDirection = "stars"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-seal",
            name = "Seal",
            category = "Templates",
            description = "Crimson red stamp badge with scalloped edge",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Hex,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1E3A8A.toInt(),
                bgColor = 0xFFF87171.toInt(),
                eyeColor = 0xFF1E3A8A.toInt(),
                ballColor = 0xFF1E3A8A.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Seal,
                artDirection = "seal"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-christmas",
            name = "Christmas",
            category = "Templates",
            description = "Festive pine green & holly berry red holiday theme",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF166534.toInt(),
                bgColor = 0xFFF0FDF4.toInt(),
                eyeColor = 0xFFDC2626.toInt(),
                ballColor = 0xFFDC2626.toInt(),
                gradientType = GradientType.None,
                artDirection = "christmas"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-birthday",
            name = "Birthday",
            category = "Templates",
            description = "Pastel celebration sprinkled with party confetti",
            style = QrStyle(
                moduleShape = ModuleShape.Confetti,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF9333EA.toInt(),
                bgColor = 0xFFFAF5FF.toInt(),
                eyeColor = 0xFF7E22CE.toInt(),
                ballColor = 0xFF7E22CE.toInt(),
                gradientType = GradientType.None,
                artDirection = "birthday"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-diamond",
            name = "Diamond",
            category = "Templates",
            description = "Rotated diamond frame motif with lavender purple theme",
            style = QrStyle(
                moduleShape = ModuleShape.Diamond,
                eyeShape = EyeShape.Diamond,
                ballShape = EyeShape.Diamond,
                fgColor = 0xFF1E3A8A.toInt(),
                bgColor = 0xFFDDD6FE.toInt(),
                eyeColor = 0xFF1E3A8A.toInt(),
                ballColor = 0xFF1E3A8A.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Diamond,
                artDirection = "diamond"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-scan-me",
            name = "Scan Me",
            category = "Templates",
            description = "Clean black & slate code with top SCAN ME banner",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFF334155.toInt(),
                bgColor = 0xFFF8FAFC.toInt(),
                eyeColor = 0xFF1E293B.toInt(),
                ballColor = 0xFF1E293B.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.BadgeScanMe,
                frameCaption = "SCAN ME"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-night-sky",
            name = "Night Sky",
            category = "Templates",
            description = "Midnight navy backdrop with glowing gold star lattice",
            style = QrStyle(
                moduleShape = ModuleShape.Star,
                eyeShape = EyeShape.Hex,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF59E0B.toInt(),
                bgColor = 0xFF0F172A.toInt(),
                eyeColor = 0xFFFBBF24.toInt(),
                ballColor = 0xFFFBBF24.toInt(),
                gradientType = GradientType.None,
                artDirection = "night-sky"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-hexagon",
            name = "Hexagon",
            category = "Templates",
            description = "Honeycomb amber orange container with hexagonal modules",
            style = QrStyle(
                moduleShape = ModuleShape.Hex,
                eyeShape = EyeShape.Hex,
                ballShape = EyeShape.Hex,
                fgColor = 0xFF1E3A8A.toInt(),
                bgColor = 0xFFFBBF24.toInt(),
                eyeColor = 0xFF1E3A8A.toInt(),
                ballColor = 0xFF1E3A8A.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Hexagon,
                artDirection = "hexagon"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-nature",
            name = "Nature",
            category = "Templates",
            description = "Fresh mint botanical leaves theme with forest green",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Leaf,
                fgColor = 0xFF15803D.toInt(),
                bgColor = 0xFFF0FDF4.toInt(),
                eyeColor = 0xFF166534.toInt(),
                ballColor = 0xFF166534.toInt(),
                gradientType = GradientType.None,
                artDirection = "nature"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-arrows",
            name = "Arrows",
            category = "Templates",
            description = "Inward pointing corner arrows with vibrant rose accent",
            style = QrStyle(
                moduleShape = ModuleShape.Cross,
                eyeShape = EyeShape.Ticks,
                ballShape = EyeShape.Square,
                fgColor = 0xFFF43F5E.toInt(),
                bgColor = 0xFFFFF1F2.toInt(),
                eyeColor = 0xFFE11D48.toInt(),
                ballColor = 0xFFE11D48.toInt(),
                gradientType = GradientType.None,
                artDirection = "arrows"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-speech",
            name = "Speech",
            category = "Templates",
            description = "Chat speech bubble frame with ocean cyan modules",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0284C7.toInt(),
                bgColor = 0xFF67E8F9.toInt(),
                eyeColor = 0xFF0369A1.toInt(),
                ballColor = 0xFF0369A1.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Speech,
                artDirection = "speech"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-wedding",
            name = "Wedding",
            category = "Templates",
            description = "Soft pastel pink wedding elegance with classy eyes",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFBE185D.toInt(),
                bgColor = 0xFFFDF2F8.toInt(),
                eyeColor = 0xFF9D174D.toInt(),
                ballColor = 0xFF9D174D.toInt(),
                gradientType = GradientType.None,
                artDirection = "wedding"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-summer",
            name = "Summer",
            category = "Templates",
            description = "Sunny golden yellow & ocean blue beach wave theme",
            style = QrStyle(
                moduleShape = ModuleShape.Fluid,
                eyeShape = EyeShape.ExtraRounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFD97706.toInt(),
                bgColor = 0xFFFFFBEB.toInt(),
                eyeColor = 0xFFB45309.toInt(),
                ballColor = 0xFFB45309.toInt(),
                gradientType = GradientType.None,
                artDirection = "summer"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-pentagon",
            name = "Pentagon",
            category = "Templates",
            description = "5-sided pentagon frame design in warm coral red",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1E3A8A.toInt(),
                bgColor = 0xFFFB7185.toInt(),
                eyeColor = 0xFF1E3A8A.toInt(),
                ballColor = 0xFF1E3A8A.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Pentagon,
                artDirection = "pentagon"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-autumn",
            name = "Autumn",
            category = "Templates",
            description = "Warm golden fall leaves and beige autumn palette",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFB45309.toInt(),
                bgColor = 0xFFFEF3C7.toInt(),
                eyeColor = 0xFF92400E.toInt(),
                ballColor = 0xFF92400E.toInt(),
                gradientType = GradientType.None,
                artDirection = "autumn"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-halloween",
            name = "Halloween",
            category = "Templates",
            description = "Spooky midnight violet backdrop with pumpkin orange dots",
            style = QrStyle(
                moduleShape = ModuleShape.Plus,
                eyeShape = EyeShape.Ticks,
                ballShape = EyeShape.Square,
                fgColor = 0xFFF97316.toInt(),
                bgColor = 0xFF18181B.toInt(),
                eyeColor = 0xFFEA580C.toInt(),
                ballColor = 0xFFEA580C.toInt(),
                gradientType = GradientType.None,
                artDirection = "halloween"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-bucket",
            name = "Bucket",
            category = "Templates",
            description = "Tapered bucket container frame with crisp blue modules",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1D4ED8.toInt(),
                bgColor = 0xFF93C5FD.toInt(),
                eyeColor = 0xFF1E40AF.toInt(),
                ballColor = 0xFF1E40AF.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Bucket,
                artDirection = "bucket"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-ramadan",
            name = "Ramadan",
            category = "Templates",
            description = "Deep midnight navy with golden crescent moon & star lattice",
            style = QrStyle(
                moduleShape = ModuleShape.Star,
                eyeShape = EyeShape.Hex,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF59E0B.toInt(),
                bgColor = 0xFF0F172A.toInt(),
                eyeColor = 0xFFFBBF24.toInt(),
                ballColor = 0xFFFBBF24.toInt(),
                gradientType = GradientType.None,
                artDirection = "ramadan"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-ocean",
            name = "Ocean",
            category = "Templates",
            description = "Wave patterns header and footer with marine blue modules",
            style = QrStyle(
                moduleShape = ModuleShape.Fluid,
                eyeShape = EyeShape.ExtraRounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0284C7.toInt(),
                bgColor = 0xFFE0F2FE.toInt(),
                eyeColor = 0xFF0369A1.toInt(),
                ballColor = 0xFF0369A1.toInt(),
                gradientType = GradientType.None,
                artDirection = "ocean"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-plaque",
            name = "Plaque",
            category = "Templates",
            description = "Green plaque badge container frame with stitched border",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF3F6212.toInt(),
                bgColor = 0xFFBEF264.toInt(),
                eyeColor = 0xFF365314.toInt(),
                ballColor = 0xFF365314.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Plaque,
                artDirection = "plaque"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-snowflakes",
            name = "Snowflakes",
            category = "Templates",
            description = "Ice blue background with falling snowflake star dots",
            style = QrStyle(
                moduleShape = ModuleShape.Star,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF3B82F6.toInt(),
                bgColor = 0xFFEFF6FF.toInt(),
                eyeColor = 0xFF2563EB.toInt(),
                ballColor = 0xFF2563EB.toInt(),
                gradientType = GradientType.None,
                artDirection = "snowflakes"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-vintage",
            name = "Vintage",
            category = "Templates",
            description = "Parchment beige with retro antique corner flourishes",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Square,
                fgColor = 0xFF78350F.toInt(),
                bgColor = 0xFFFEF3C7.toInt(),
                eyeColor = 0xFF451A03.toInt(),
                ballColor = 0xFF451A03.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.SimpleBorder,
                artDirection = "vintage"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-card",
            name = "Card",
            category = "Templates",
            description = "Rounded card container with bottom scan me pill",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0F766E.toInt(),
                bgColor = 0xFF2DD4BF.toInt(),
                eyeColor = 0xFF115E59.toInt(),
                ballColor = 0xFF115E59.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Card,
                frameCaption = "SCAN ME"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-spring",
            name = "Spring",
            category = "Templates",
            description = "Pastel spring garden theme with blooming floral dots",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFDB2777.toInt(),
                bgColor = 0xFFFDF2F8.toInt(),
                eyeColor = 0xFFBE185D.toInt(),
                ballColor = 0xFFBE185D.toInt(),
                gradientType = GradientType.None,
                artDirection = "spring"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-music",
            name = "Music",
            category = "Templates",
            description = "Lavender purple theme with rhythm bubble notes",
            style = QrStyle(
                moduleShape = ModuleShape.Bubbles,
                eyeShape = EyeShape.ExtraRounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF7C3AED.toInt(),
                bgColor = 0xFFF3E8FF.toInt(),
                eyeColor = 0xFF6D28D9.toInt(),
                ballColor = 0xFF6D28D9.toInt(),
                gradientType = GradientType.None,
                artDirection = "music"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-label",
            name = "Label",
            category = "Templates",
            description = "Pill label container frame with scan me badge",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.ExtraRounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0284C7.toInt(),
                bgColor = 0xFF06B6D4.toInt(),
                eyeColor = 0xFF0E7490.toInt(),
                ballColor = 0xFF0E7490.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Label,
                frameCaption = "SCAN ME"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-dot-ring",
            name = "Dot Ring",
            category = "Templates",
            description = "Playful dotted ring frame surrounding the code",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF10B981.toInt(),
                bgColor = 0xFFFFFFFF.toInt(),
                eyeColor = 0xFF059669.toInt(),
                ballColor = 0xFF059669.toInt(),
                gradientType = GradientType.None,
                artDirection = "dot-ring"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-bracket",
            name = "Bracket",
            category = "Templates",
            description = "Camera bracket frame with speech-bubble scan code badge",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFF1E293B.toInt(),
                bgColor = 0xFFFFFFFF.toInt(),
                eyeColor = 0xFF0F172A.toInt(),
                ballColor = 0xFF0F172A.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Bracket,
                frameCaption = "SCAN CODE"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-badge",
            name = "Badge",
            category = "Templates",
            description = "Circular scalloped badge with bottom ribbon",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.ExtraRounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF6D28D9.toInt(),
                bgColor = 0xFFC4B5FD.toInt(),
                eyeColor = 0xFF5B21B6.toInt(),
                ballColor = 0xFF5B21B6.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Badge,
                frameCaption = "SCAN CODE"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-arch",
            name = "Arch",
            category = "Templates",
            description = "Archway dome container shape in regal purple",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.ExtraRounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF581C87.toInt(),
                bgColor = 0xFFDDD6FE.toInt(),
                eyeColor = 0xFF3B0764.toInt(),
                ballColor = 0xFF3B0764.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Arch,
                artDirection = "arch"
            ),
            featured = true
        ),
        QrPreset(
            id = "tpl-cup",
            name = "Cup",
            category = "Templates",
            description = "Takeaway coffee cup shape container in fresh teal",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0D9488.toInt(),
                bgColor = 0xFF14B8A6.toInt(),
                eyeColor = 0xFF0F766E.toInt(),
                ballColor = 0xFF0F766E.toInt(),
                gradientType = GradientType.None,
                frameStyle = FrameStyle.Cup,
                artDirection = "cup"
            ),
            featured = true
        )
    )
}
