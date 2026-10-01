package com.example.qr.engine

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

object QrPresets {

    private val _presets = mutableListOf<QrPreset>()
    val list: List<QrPreset>
        get() = (if (_presets.isNotEmpty()) _presets else fallbackList).distinctBy { it.id }

    val mixedList: List<QrPreset>
        get() {
            val all = list.distinctBy { it.id }
            if (all.isEmpty()) return emptyList()
            val desiredCategoryOrder = listOf(
                "Minimal & Swiss",
                "Botanical Art",
                "Neon & Tech",
                "Luxury & Fashion",
                "Nature & Organic",
                "Retro & Vintage",
                "Creative Art",
                "🌟 Iconic Art Frames"
            )
            val grouped = all.groupBy { it.category }
            val categoryQueues = desiredCategoryOrder.mapNotNull { cat ->
                grouped[cat]?.toMutableList()
            }.toMutableList()

            grouped.keys.filter { it !in desiredCategoryOrder }.forEach { cat ->
                grouped[cat]?.toMutableList()?.let { categoryQueues.add(it) }
            }

            val result = ArrayList<QrPreset>(all.size)
            while (categoryQueues.isNotEmpty()) {
                val iterator = categoryQueues.iterator()
                while (iterator.hasNext()) {
                    val queue = iterator.next()
                    if (queue.isNotEmpty()) {
                        result.add(queue.removeAt(0))
                    }
                    if (queue.isEmpty()) {
                        iterator.remove()
                    }
                }
            }
            return result
        }

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
                "pro-multicolor-geo", "art-multicolor-geometric",
                "art-bioluminescent"
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
                _presets.addAll(parsed.distinctBy { it.id })
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

        val fgRaw = safeColor(fgStr, 0xFF0F172A.toInt())
        val bgRaw = safeColor(bgStr, 0xFFFFFFFF.toInt())
        val eyeRaw = safeColor(eyeColorStr, fgRaw)
        val ballRaw = safeColor(ballColorStr, fgRaw)

        val eyeIsLight = !QrGenerator.isDarkColor(eyeRaw) || !QrGenerator.isDarkColor(ballRaw)
        val bgIsDark = QrGenerator.isDarkColor(bgRaw)

        val finalBg = if (eyeIsLight) {
            // Rule: If eyes or pupils are light colors, the background should be dark!
            if (bgIsDark) bgRaw else 0xFF0B0F19.toInt()
        } else {
            // Rule: If eyes and pupils are dark, the background should be light!
            if (!bgIsDark) bgRaw else 0xFFFFFFFF.toInt()
        }

        var finalFg = fgRaw
        if (QrGenerator.getContrastRatio(finalFg, finalBg) < 2.8f) {
            finalFg = if (QrGenerator.isDarkColor(finalBg)) 0xFFFFFFFF.toInt() else 0xFF0F172A.toInt()
        }

        val (finalEye, finalBall) = QrGenerator.resolveEffectiveEyeColors(
            QrStyle(fgColor = finalFg, bgColor = finalBg, eyeColor = eyeRaw, ballColor = ballRaw),
            finalBg
        )

        return QrStyle(
            moduleShape = ModuleShape.fromString(moduleShapeStr),
            eyeShape = EyeShape.fromString(eyeShapeStr),
            ballShape = EyeShape.fromString(ballShapeStr),
            fgColor = finalFg,
            bgColor = finalBg,
            eyeColor = finalEye,
            ballColor = finalBall,
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
            artDirection = o.optString("artDirection").takeIf { it.isNotBlank() },
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
                eyeColor = 0xFF831843.toInt(),
                ballColor = 0xFF701A75.toInt(),
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
                eyeColor = 0xFF0E7490.toInt(),
                ballColor = 0xFF9F1239.toInt(),
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
            name = "Wisteria Violet Glow",
            category = "🌟 Iconic Art Frames",
            description = "Imperial amethyst & lavender cascading wisteria arbour with royal gold filigree accents",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFE879F9.toInt(),
                bgColor = 0xFF1A0B2E.toInt(),
                eyeColor = 0xFFFFD700.toInt(),
                ballColor = 0xFFC084FC.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF9333EA.toInt(),
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
                eyeColor = 0xFF451A03.toInt(),
                ballColor = 0xFF78350F.toInt(),
                gradientType = GradientType.Radial,
                gradientTo = 0xFF451A03.toInt(),
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
                fgColor = 0xFF0F051D.toInt(),
                bgColor = 0xFFFFF7ED.toInt(),
                eyeColor = 0xFF180A2A.toInt(),
                ballColor = 0xFF180A2A.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFF3B0764.toInt(),
                quietZone = 3,
                dotScale = 0.92f,
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

        QrPreset(
            id = "art-aurora-night",
            name = "Aurora Night Sky",
            category = "🌟 Iconic Art Frames",
            description = "Bioluminescent arctic aurora curtains dancing over obsidian waters",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00FFCC.toInt(),
                bgColor = 0xFF030712.toInt(),
                eyeColor = 0xFF00FFCC.toInt(),
                ballColor = 0xFF8B5CF6.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF9933FF.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-aurora-night"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-aztec-sun",
            name = "Aztec Sun Gold",
            category = "🌟 Iconic Art Frames",
            description = "Ancient Aztec radiant gold sun emblem on deep obsidian amber",
            style = QrStyle(
                moduleShape = ModuleShape.Diamond,
                eyeShape = EyeShape.Diamond,
                ballShape = EyeShape.Diamond,
                fgColor = 0xFFFFD700.toInt(),
                bgColor = 0xFF120802.toInt(),
                eyeColor = 0xFFFFB300.toInt(),
                ballColor = 0xFFFFE082.toInt(),
                gradientType = GradientType.Radial,
                gradientTo = 0xFFFF9800.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-aztec-sun"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-coral-bloom",
            name = "Coral Floral Bloom",
            category = "🌟 Iconic Art Frames",
            description = "Vibrant coral botanical burst and delicate petals on velvety noir",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFF6F61.toInt(),
                bgColor = 0xFF1A0A0E.toInt(),
                eyeColor = 0xFFFF5252.toInt(),
                ballColor = 0xFFFF8A80.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFFF9E80.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-coral-bloom"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-coral-reef",
            name = "Deep Coral Reef",
            category = "🌟 Iconic Art Frames",
            description = "Submerged oceanic marine reef with bioluminescent cyan & aquamarine",
            style = QrStyle(
                moduleShape = ModuleShape.Bubbles,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00E5FF.toInt(),
                bgColor = 0xFF011627.toInt(),
                eyeColor = 0xFF00E5FF.toInt(),
                ballColor = 0xFF1DE9B6.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFF00B0FF.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-coral-reef"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-cosmic-nebula",
            name = "Cosmic Stellar Nebula",
            category = "🌟 Iconic Art Frames",
            description = "Interstellar ultraviolet nebula clouds and celestial starlight vortex",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFE040FB.toInt(),
                bgColor = 0xFF0B001A.toInt(),
                eyeColor = 0xFF00E5FF.toInt(),
                ballColor = 0xFFE040FB.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF7C4DFF.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-cosmic-nebula"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-crystal-ice",
            name = "Crystal Glacial Ice",
            category = "🌟 Iconic Art Frames",
            description = "Frozen crystalline prism facets and shimmering arctic glacial geometry",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFE0F7FA.toInt(),
                bgColor = 0xFF04101A.toInt(),
                eyeColor = 0xFF00E5FF.toInt(),
                ballColor = 0xFFB2EBF2.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF80DEEA.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-crystal-ice"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-deco-gold",
            name = "Art Deco Gold Filigree",
            category = "🌟 Iconic Art Frames",
            description = "Opulent Roaring Twenties Gatsby gold geometric filigree on black onyx",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Classy,
                fgColor = 0xFFFFD700.toInt(),
                bgColor = 0xFF0B0907.toInt(),
                eyeColor = 0xFFFFD700.toInt(),
                ballColor = 0xFFFFF8E1.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFD4AF37.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-deco-gold"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-ember-roses",
            name = "Ember Rose Petals",
            category = "🌟 Iconic Art Frames",
            description = "Glowing crimson and molten amber rose vines with drifting sparks",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFF5722.toInt(),
                bgColor = 0xFF100305.toInt(),
                eyeColor = 0xFFFF1744.toInt(),
                ballColor = 0xFFFFAB40.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFE91E63.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-ember-roses"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-enchanted-forest",
            name = "Enchanted Woodland Forest",
            category = "🌟 Iconic Art Frames",
            description = "Mystical emerald rainforest canopy with glowing jade moss and orchids",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Leaf,
                fgColor = 0xFF10B981.toInt(),
                bgColor = 0xFF02130A.toInt(),
                eyeColor = 0xFF34D399.toInt(),
                ballColor = 0xFF6EE7B7.toInt(),
                gradientType = GradientType.Linear,
                gradientTo = 0xFF059669.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-enchanted-forest"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-festival-lights",
            name = "Festival Lantern Bokeh",
            category = "🌟 Iconic Art Frames",
            description = "Festive golden festival lantern bokeh and warm celebratory glow",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFCA28.toInt(),
                bgColor = 0xFF120A04.toInt(),
                eyeColor = 0xFFFFB300.toInt(),
                ballColor = 0xFFFFE082.toInt(),
                gradientType = GradientType.Radial,
                gradientTo = 0xFFFF7043.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-festival-lights"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-frost-blue",
            name = "Glacial Frost Blue",
            category = "🌟 Iconic Art Frames",
            description = "Deep sapphire ice crystals and glacial frost overlay on midnight blue",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF38BDF8.toInt(),
                bgColor = 0xFF030D1A.toInt(),
                eyeColor = 0xFF38BDF8.toInt(),
                ballColor = 0xFFBAE6FD.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF0284C7.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-frost-blue"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-golden-lotus",
            name = "Sacred Golden Lotus",
            category = "🌟 Iconic Art Frames",
            description = "Spiritual blooming golden lotus with celestial aura on obsidian water",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFFD700.toInt(),
                bgColor = 0xFF0A0802.toInt(),
                eyeColor = 0xFFFFC107.toInt(),
                ballColor = 0xFFFFE082.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFF59E0B.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-golden-lotus"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-golden-roses",
            name = "Golden Rose Luxury",
            category = "🌟 Iconic Art Frames",
            description = "Luxurious metallic champagne gold roses with ornate botanical flourishes",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Classy,
                fgColor = 0xFFFFD54F.toInt(),
                bgColor = 0xFF140D05.toInt(),
                eyeColor = 0xFFFFC107.toInt(),
                ballColor = 0xFFFFECB3.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFFFB74D.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-golden-roses"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-great-wave",
            name = "Ukiyo-e Great Wave",
            category = "🌟 Iconic Art Frames",
            description = "Classic woodblock ocean tsunami with foam crests on washi paper",
            style = QrStyle(
                moduleShape = ModuleShape.Fluid,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1E3A8A.toInt(),
                bgColor = 0xFFFAF7F2.toInt(),
                eyeColor = 0xFF0C4A6E.toInt(),
                ballColor = 0xFF38BDF8.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF0284C7.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-great-wave"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-groovy-70s",
            name = "Groovy 70s Waves",
            category = "🌟 Iconic Art Frames",
            description = "Psychedelic 1970s retro curves in sunset orange, mustard, and avocado",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFEA580C.toInt(),
                bgColor = 0xFFFFFBEB.toInt(),
                eyeColor = 0xFFC2410C.toInt(),
                ballColor = 0xFFD97706.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFCA8A04.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-groovy-70s"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-holo-chrome",
            name = "Holo Chrome Mercury",
            category = "🌟 Iconic Art Frames",
            description = "Liquid mercury reflection with iridescent holographic chromatic luster",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFE2E8F0.toInt(),
                bgColor = 0xFF0F172A.toInt(),
                eyeColor = 0xFF38BDF8.toInt(),
                ballColor = 0xFFC084FC.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF94A3B8.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-holo-chrome"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-kawaii-sweets",
            name = "Kawaii Pastel Sweets",
            category = "🌟 Iconic Art Frames",
            description = "Playful pastel cotton candy and sugar confections on soft blush",
            style = QrStyle(
                moduleShape = ModuleShape.Bubbles,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF472B6.toInt(),
                bgColor = 0xFFFFF1F2.toInt(),
                eyeColor = 0xFFEC4899.toInt(),
                ballColor = 0xFF3B82F6.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF60A5FA.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-kawaii-sweets"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-marble-gold",
            name = "Carrara Marble Gold",
            category = "🌟 Iconic Art Frames",
            description = "Polished Italian white marble with rich molten gold veining",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF1E293B.toInt(),
                bgColor = 0xFFFAF9F6.toInt(),
                eyeColor = 0xFFB45309.toInt(),
                ballColor = 0xFFD97706.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF334155.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-marble-gold"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-mosaic-lapis",
            name = "Lapis Lazuli Mosaic",
            category = "🌟 Iconic Art Frames",
            description = "Byzantine royal lapis lazuli mosaic tiles with golden tesserae",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFF1D4ED8.toInt(),
                bgColor = 0xFF0A1026.toInt(),
                eyeColor = 0xFFF59E0B.toInt(),
                ballColor = 0xFF60A5FA.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF1E40AF.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-mosaic-lapis"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-neon-grid",
            name = "Retro Neon Gridwave",
            category = "🌟 Iconic Art Frames",
            description = "Cyberpunk outrun neon cyan and synthwave magenta wireframe horizon",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00F0FF.toInt(),
                bgColor = 0xFF080014.toInt(),
                eyeColor = 0xFF00F0FF.toInt(),
                ballColor = 0xFFFF007F.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFFF007F.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-neon-grid"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-phoenix-fire",
            name = "Phoenix Solar Fire",
            category = "🌟 Iconic Art Frames",
            description = "Majestic blazing phoenix wings with fiery solar plumage on obsidian",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFF3D00.toInt(),
                bgColor = 0xFF120300.toInt(),
                eyeColor = 0xFFFF1744.toInt(),
                ballColor = 0xFFFF9100.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFFFD600.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-phoenix-fire"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-pixel-voxel",
            name = "3D Pixel Voxel Matrix",
            category = "🌟 Iconic Art Frames",
            description = "Isometric 8-bit voxel cube blocks in vibrant neon emerald and cyan",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFF10B981.toInt(),
                bgColor = 0xFF090D16.toInt(),
                eyeColor = 0xFFF59E0B.toInt(),
                ballColor = 0xFF10B981.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF06B6D4.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-pixel-voxel"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-sakura-night",
            name = "Midnight Sakura Blossom",
            category = "🌟 Iconic Art Frames",
            description = "Cherry blossom petals drifting under starry moonlit indigo sky",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Leaf,
                fgColor = 0xFFF472B6.toInt(),
                bgColor = 0xFF0F071A.toInt(),
                eyeColor = 0xFFF43F5E.toInt(),
                ballColor = 0xFFE879F9.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFC084FC.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-sakura-night"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-silver-frost",
            name = "Platinum Silver Frost",
            category = "🌟 Iconic Art Frames",
            description = "Delicate metallic platinum filigree and sparkling silver frost",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFE2E8F0.toInt(),
                bgColor = 0xFF0B0F14.toInt(),
                eyeColor = 0xFF94A3B8.toInt(),
                ballColor = 0xFFF8FAFC.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFCBD5E1.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-silver-frost"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-stained-glass",
            name = "Gothic Cathedral Glass",
            category = "🌟 Iconic Art Frames",
            description = "Sacred cathedral leaded stained glass with ruby, amber, and cobalt light",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFFDC2626.toInt(),
                bgColor = 0xFF080812.toInt(),
                eyeColor = 0xFFF59E0B.toInt(),
                ballColor = 0xFF9333EA.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF2563EB.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-stained-glass"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-steampunk",
            name = "Brass Horology Steampunk",
            category = "🌟 Iconic Art Frames",
            description = "Victorian brass cogs, copper gears, and riveted clockwork mechanisms",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFD97706.toInt(),
                bgColor = 0xFF150D06.toInt(),
                eyeColor = 0xFFF59E0B.toInt(),
                ballColor = 0xFF92400E.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFB45309.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-steampunk"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-vintage-glam",
            name = "Vintage Hollywood Glamour",
            category = "🌟 Iconic Art Frames",
            description = "Golden age Hollywood luxury with ornate baroque flourishes on champagne",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Classy,
                fgColor = 0xFFFFD700.toInt(),
                bgColor = 0xFF0D0A08.toInt(),
                eyeColor = 0xFFFFE082.toInt(),
                ballColor = 0xFFFFD54F.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFF59E0B.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-vintage-glam"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-violet-stars",
            name = "Violet Celestial Stardust",
            category = "🌟 Iconic Art Frames",
            description = "Deep violet cosmic galaxy with sparkling diamond stardust cluster",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFC084FC.toInt(),
                bgColor = 0xFF0E041A.toInt(),
                eyeColor = 0xFFE879F9.toInt(),
                ballColor = 0xFFDDD6FE.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFA855F7.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-violet-stars"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-wisteria-glow",
            name = "Luminescent Wisteria Arbour",
            category = "🌟 Iconic Art Frames",
            description = "Cascading lavender and periwinkle wisteria blooms with glowing tendrils",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFD8B4FE.toInt(),
                bgColor = 0xFF0C071C.toInt(),
                eyeColor = 0xFFA78BFA.toInt(),
                ballColor = 0xFFC4B5FD.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF818CF8.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-wisteria-glow"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-arabian-nights",
            name = "Mystic Arabian Nights",
            category = "🌟 Iconic Art Frames",
            description = "Desert star-lit dunes beneath golden crescent moon and sapphire sky",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFF59E0B.toInt(),
                bgColor = 0xFF080D21.toInt(),
                eyeColor = 0xFF38BDF8.toInt(),
                ballColor = 0xFFFBBF24.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFD97706.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-arabian-nights"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-autumn-harvest",
            name = "Golden Autumn Harvest",
            category = "🌟 Iconic Art Frames",
            description = "Warm russet maple leaves, golden pumpkin amber, and harvest forest",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Leaf,
                fgColor = 0xFFEA580C.toInt(),
                bgColor = 0xFF140702.toInt(),
                eyeColor = 0xFFB45309.toInt(),
                ballColor = 0xFFF97316.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFD97706.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-autumn-harvest"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-balloon-party",
            name = "Celebration Balloon Confetti",
            category = "🌟 Iconic Art Frames",
            description = "Joyful multicolored festival celebration balloons and floating confetti",
            style = QrStyle(
                moduleShape = ModuleShape.Bubbles,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFEC4899.toInt(),
                bgColor = 0xFFFAFAFA.toInt(),
                eyeColor = 0xFF10B981.toInt(),
                ballColor = 0xFFF59E0B.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF3B82F6.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-balloon-party"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-christmas-frost",
            name = "Festive Holiday Frost",
            category = "🌟 Iconic Art Frames",
            description = "Holiday winter evergreen, frosted cranberries, and sparkling snowfall",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Rounded,
                fgColor = 0xFFEF4444.toInt(),
                bgColor = 0xFF02140D.toInt(),
                eyeColor = 0xFFEF4444.toInt(),
                ballColor = 0xFFFBBF24.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF10B981.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-christmas-frost"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-cocoa-cafe",
            name = "Artisan Cocoa Cafe",
            category = "🌟 Iconic Art Frames",
            description = "Cozy roasted coffee beans, warm cocoa, and creamy espresso froth",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Rounded,
                fgColor = 0xFF78350F.toInt(),
                bgColor = 0xFFFFFBEB.toInt(),
                eyeColor = 0xFF92400E.toInt(),
                ballColor = 0xFFB45309.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF451A03.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-cocoa-cafe"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-dino-world",
            name = "Jurassic Prehistoric World",
            category = "🌟 Iconic Art Frames",
            description = "Prehistoric Jurassic jungle canopy with ancient amber and fossils",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF15803D.toInt(),
                bgColor = 0xFF061408.toInt(),
                eyeColor = 0xFF16A34A.toInt(),
                ballColor = 0xFFF59E0B.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFD97706.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-dino-world"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-fireworks-night",
            name = "Midnight Sky Fireworks",
            category = "🌟 Iconic Art Frames",
            description = "Radiant pyrotechnic bursts of turquoise, crimson, and golden sparks",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFF0055.toInt(),
                bgColor = 0xFF050510.toInt(),
                eyeColor = 0xFF00F0FF.toInt(),
                ballColor = 0xFFFF0055.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFFFD700.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-fireworks-night"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-halloween-night",
            name = "Haunted Halloween Night",
            category = "🌟 Iconic Art Frames",
            description = "Spooky jack-o-lantern glowing orange with haunted purple mist",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFFF7700.toInt(),
                bgColor = 0xFF0B0410.toInt(),
                eyeColor = 0xFFFF5500.toInt(),
                ballColor = 0xFFA855F7.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF9333EA.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-halloween-night"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-moonlit-zodiac",
            name = "Celestial Moonlit Zodiac",
            category = "🌟 Iconic Art Frames",
            description = "Mystical zodiac constellation map with silver star charts and moon",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFE0E7FF.toInt(),
                bgColor = 0xFF03071E.toInt(),
                eyeColor = 0xFF6366F1.toInt(),
                ballColor = 0xFFC7D2FE.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF818CF8.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-moonlit-zodiac"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-music-groove",
            name = "Rhythmic Music Groove",
            category = "🌟 Iconic Art Frames",
            description = "Neon soundwaves and rhythmic vinyl audio grooves with pulsing beats",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF06B6D4.toInt(),
                bgColor = 0xFF0A0A14.toInt(),
                eyeColor = 0xFFEC4899.toInt(),
                ballColor = 0xFF06B6D4.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF8B5CF6.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-music-groove"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-nautical-bay",
            name = "Maritime Harbor Bay",
            category = "🌟 Iconic Art Frames",
            description = "Crisp ocean harbor breeze with navy anchor and brass nautical accents",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0369A1.toInt(),
                bgColor = 0xFFF0F9FF.toInt(),
                eyeColor = 0xFF075985.toInt(),
                ballColor = 0xFFBAE6FD.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF0284C7.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-nautical-bay"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-pastel-dream",
            name = "Pastel Cloudscape Dream",
            category = "🌟 Iconic Art Frames",
            description = "Dreamy iridescent pastel sky with soft lavender, mint, and blush clouds",
            style = QrStyle(
                moduleShape = ModuleShape.Bubbles,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF8B5CF6.toInt(),
                bgColor = 0xFFFAF5FF.toInt(),
                eyeColor = 0xFF6366F1.toInt(),
                ballColor = 0xFFF472B6.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFEC4899.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-pastel-dream"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-rainy-april",
            name = "Rainy Spring Blossom",
            category = "🌟 Iconic Art Frames",
            description = "Fresh spring raindrops falling on translucent umbrellas and teal puddles",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0D9488.toInt(),
                bgColor = 0xFFF0FDFA.toInt(),
                eyeColor = 0xFF0F766E.toInt(),
                ballColor = 0xFF2DD4BF.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF0284C7.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-rainy-april"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-royal-baroque",
            name = "Versailles Royal Baroque",
            category = "🌟 Iconic Art Frames",
            description = "Opulent gold leaf cartouches on burgundy velvet palatial backdrop",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Classy,
                fgColor = 0xFFFFD700.toInt(),
                bgColor = 0xFF1A050D.toInt(),
                eyeColor = 0xFFE11D48.toInt(),
                ballColor = 0xFFFFE082.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFF59E0B.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-royal-baroque"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-safari-savanna",
            name = "Savanna Sunset Safari",
            category = "🌟 Iconic Art Frames",
            description = "Serengeti acacia silhouettes against radiant copper and amber sunset",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Rounded,
                fgColor = 0xFFEA580C.toInt(),
                bgColor = 0xFF170802.toInt(),
                eyeColor = 0xFFD97706.toInt(),
                ballColor = 0xFFF97316.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFB45309.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-safari-savanna"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-sumi-ink",
            name = "Traditional Sumi-e Brush Ink",
            category = "🌟 Iconic Art Frames",
            description = "Expressive Japanese sumi-e ink washes and brushwork on fibrous washi",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0F172A.toInt(),
                bgColor = 0xFFF5F3EF.toInt(),
                eyeColor = 0xFF1E293B.toInt(),
                ballColor = 0xFF475569.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF334155.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-sumi-ink"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-travel-wonders",
            name = "Global Travel Explorer",
            category = "🌟 Iconic Art Frames",
            description = "Vintage world exploration atlas with nautical compass and parchment",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Rounded,
                fgColor = 0xFF0F766E.toInt(),
                bgColor = 0xFFFDFBF7.toInt(),
                eyeColor = 0xFF047857.toInt(),
                ballColor = 0xFFB45309.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFD97706.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-travel-wonders"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-tropical-paradise",
            name = "Exotic Island Paradise",
            category = "🌟 Iconic Art Frames",
            description = "Tropical turquoise lagoon waters and vibrant pink hibiscus blossoms",
            style = QrStyle(
                moduleShape = ModuleShape.Rounded,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF06B6D4.toInt(),
                bgColor = 0xFF02171A.toInt(),
                eyeColor = 0xFF10B981.toInt(),
                ballColor = 0xFFF43F5E.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFEC4899.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-tropical-paradise"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-wedding-rose",
            name = "Romantic Wedding Rose Arch",
            category = "🌟 Iconic Art Frames",
            description = "Romantic bridal blush roses, ivory satin ribbons, and delicate greenery",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFBE185D.toInt(),
                bgColor = 0xFFFFF1F2.toInt(),
                eyeColor = 0xFF9D174D.toInt(),
                ballColor = 0xFFF472B6.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFDB2777.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-wedding-rose"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-zen-mandala",
            name = "Sacred Zen Mandala",
            category = "🌟 Iconic Art Frames",
            description = "Serene spiritual mandala geometry with sacred lotus ring and indigo aura",
            style = QrStyle(
                moduleShape = ModuleShape.Dots,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF6366F1.toInt(),
                bgColor = 0xFF090919.toInt(),
                eyeColor = 0xFFA855F7.toInt(),
                ballColor = 0xFF818CF8.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF8B5CF6.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-zen-mandala"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-iconic-ukiyo",
            name = "Edo Woodblock Wave",
            category = "🌟 Iconic Art Frames",
            description = "Indigo ocean surf and foaming white sea spray on handmade washi",
            style = QrStyle(
                moduleShape = ModuleShape.Fluid,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF0369A1.toInt(),
                bgColor = 0xFFFDFBF7.toInt(),
                eyeColor = 0xFF075985.toInt(),
                ballColor = 0xFF38BDF8.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF0C4A6E.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-ukiyo"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-iconic-royal",
            name = "Imperial Royal Sovereign",
            category = "🌟 Iconic Art Frames",
            description = "Imperial crown crest and baroque gold scrollwork on dark marble",
            style = QrStyle(
                moduleShape = ModuleShape.Classy,
                eyeShape = EyeShape.Classy,
                ballShape = EyeShape.Classy,
                fgColor = 0xFFFFD700.toInt(),
                bgColor = 0xFF0B0907.toInt(),
                eyeColor = 0xFFFFD700.toInt(),
                ballColor = 0xFFFFDF73.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFD4AF37.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-royal"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-iconic-sakura",
            name = "Spring Sakura Bloom",
            category = "🌟 Iconic Art Frames",
            description = "Delicate pink cherry blossom petals falling against satin blush",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Leaf,
                ballShape = EyeShape.Leaf,
                fgColor = 0xFFE11D48.toInt(),
                bgColor = 0xFFFFF5F7.toInt(),
                eyeColor = 0xFFBE185D.toInt(),
                ballColor = 0xFFFB7185.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFFF43F5E.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-sakura"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-iconic-matcha",
            name = "Ceremonial Matcha Botanical",
            category = "🌟 Iconic Art Frames",
            description = "Vibrant ceremonial green tea leaves framing crisp emerald modules",
            style = QrStyle(
                moduleShape = ModuleShape.Leaf,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF15803D.toInt(),
                bgColor = 0xFFF7FEE7.toInt(),
                eyeColor = 0xFF166534.toInt(),
                ballColor = 0xFF4ADE80.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF16A34A.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-matcha"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-iconic-solarpunk",
            name = "Solarpunk Amber Canopy",
            category = "🌟 Iconic Art Frames",
            description = "Lush solarpunk greenery with glowing golden amber solar motifs",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFFD97706.toInt(),
                bgColor = 0xFF0B1A0E.toInt(),
                eyeColor = 0xFF10B981.toInt(),
                ballColor = 0xFFFBBF24.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF059669.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-solarpunk"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-iconic-neon-fungi",
            name = "Neon Bioluminescent Fungi",
            category = "🌟 Iconic Art Frames",
            description = "Bioluminescent emerald and cyan fungal spores glowing on midnight",
            style = QrStyle(
                moduleShape = ModuleShape.Bubbles,
                eyeShape = EyeShape.Circle,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF10B981.toInt(),
                bgColor = 0xFF02120C.toInt(),
                eyeColor = 0xFF06B6D4.toInt(),
                ballColor = 0xFF6EE7B7.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF06B6D4.toInt(),
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-neon-fungi"
            ),
            featured = true
        ),
        QrPreset(
            id = "art-iconic-mono",
            name = "Monochrome Luxe Precision",
            category = "🌟 Iconic Art Frames",
            description = "High-contrast architectural black and white luxury geometric styling",
            style = QrStyle(
                moduleShape = ModuleShape.Square,
                eyeShape = EyeShape.Square,
                ballShape = EyeShape.Square,
                fgColor = 0xFF09090B.toInt(),
                bgColor = 0xFFFAFAFA.toInt(),
                eyeColor = 0xFF18181B.toInt(),
                ballColor = 0xFF27272A.toInt(),
                gradientType = GradientType.None,
                quietZone = 3,
                dotScale = 0.90f,
                ecc = "H",
                artDirection = "art-mono"
            ),
            featured = true
        ),

        // --- BOTANICAL ART CATEGORY ---
        QrPreset(
            id = "pro-wisteria",
            name = "Bioluminescent Wisteria",
            category = "Botanical Art",
            description = "Bioluminescent electric cyan & neon mint glowing vines on pitch obsidian void",
            style = QrStyle(
                moduleShape = ModuleShape.Squircle,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF00F0FF.toInt(),
                bgColor = 0xFF020914.toInt(),
                eyeColor = 0xFF00F0FF.toInt(),
                ballColor = 0xFF00FF87.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF00FF87.toInt(),
                quietZone = 1,
                moduleGap = 0.02f,
                dotScale = 0.90f,
                ecc = "H",
                effect = QrEffect.Glow,
                effectIntensity = 1.35f,
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
            name = "Bioluminescent Lagoon",
            category = "Creative Art",
            description = "Deep ocean bioluminescent aqua algae and neon emerald sea coral on midnight abyss",
            style = QrStyle(
                moduleShape = ModuleShape.Fluid,
                eyeShape = EyeShape.Rounded,
                ballShape = EyeShape.Circle,
                fgColor = 0xFF06B6D4.toInt(),
                bgColor = 0xFF030C16.toInt(),
                eyeColor = 0xFF38BDF8.toInt(),
                ballColor = 0xFF10B981.toInt(),
                gradientType = GradientType.Diagonal,
                gradientTo = 0xFF10B981.toInt(),
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
