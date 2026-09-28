package com.example.qr.engine

data class ShowcaseItem(
    val id: String,
    val title: String,
    val category: String,
    val tagline: String,
    val badge: String,
    val presetId: String,
    val defaultUrl: String = "https://qrwho.vercel.app"
)

object ShowcaseDesigns {
    val items = listOf(
        ShowcaseItem(
            id = "art-ukiyo",
            title = "Ukiyo Wave",
            category = "Japanese Woodblock",
            tagline = "Ocean indigo on handmade washi texture with leaf finders",
            badge = "Edo Aesthetic",
            presetId = "art-ukiyo"
        ),
        ShowcaseItem(
            id = "art-cyberpunk",
            title = "Cyberpunk 2099",
            category = "Sci-Fi & Tech",
            tagline = "Electric magenta to cyan neon data stream on dark onyx",
            badge = "High Voltage",
            presetId = "art-cyberpunk"
        ),
        ShowcaseItem(
            id = "art-vaporwave",
            title = "Vaporwave 1995",
            category = "Retro & Synth",
            tagline = "Pastel lilac, mint green and electric blue dreamscape",
            badge = "Retro Aesthetic",
            presetId = "art-vaporwave"
        ),
        ShowcaseItem(
            id = "art-royal",
            title = "Royal Gold",
            category = "Luxury & Fashion",
            tagline = "Obsidian black with polished gold gradient & classy finders",
            badge = "Editorial Luxe",
            presetId = "art-royal"
        ),
        ShowcaseItem(
            id = "art-sakura",
            title = "Sakura Bloom",
            category = "Weddings & Florals",
            tagline = "Two-lobe heart modules with emerald leaf eyes on blush cream",
            badge = "Romantic",
            presetId = "art-sakura"
        ),
        ShowcaseItem(
            id = "art-matcha",
            title = "Matcha Latte",
            category = "Café & Organic",
            tagline = "Earthy matcha green leaf modules on steamed cream",
            badge = "Organic",
            presetId = "art-matcha"
        ),
        ShowcaseItem(
            id = "art-solarpunk",
            title = "Solarpunk Dawn",
            category = "Green Tech & Nature",
            tagline = "Radiant lime & golden solar amber on forest night",
            badge = "Solarpunk",
            presetId = "art-solarpunk"
        ),
        ShowcaseItem(
            id = "art-neon-fungi",
            title = "Neon Fungi",
            category = "Creative & Music",
            tagline = "Bioluminescent emerald bubbles on pitch obsidian",
            badge = "Bioluminescent",
            presetId = "art-neon-fungi"
        ),
        ShowcaseItem(
            id = "art-mono",
            title = "Mono Luxe",
            category = "Swiss Minimalist",
            tagline = "Ultra-crisp geometric precision for architecture & design",
            badge = "Architectural",
            presetId = "art-mono"
        ),
        // 10 New Flagship Art Visuals
        ShowcaseItem(
            id = "art-aurora-borealis",
            title = "Aurora Borealis",
            category = "Nature & Arctic",
            tagline = "Emerald & violet northern lights over snowy mountain ridges",
            badge = "Arctic Glow",
            presetId = "art-aurora-borealis"
        ),
        ShowcaseItem(
            id = "art-cyber-samurai",
            title = "Cyber Katana",
            category = "Sci-Fi & Cyber",
            tagline = "Carbon armor with dual crimson and electric cyan blade slashes",
            badge = "Cyber Armor",
            presetId = "art-cyber-samurai"
        ),
        ShowcaseItem(
            id = "art-golden-kintsugi",
            title = "Golden Kintsugi",
            category = "Zen & Luxury",
            tagline = "Japanese wabi-sabi porcelain with shimmering molten gold seams",
            badge = "Imperial Gold",
            presetId = "art-golden-kintsugi"
        ),
        ShowcaseItem(
            id = "art-tropical-sunset",
            title = "Paradise Palm",
            category = "Travel & Resort",
            tagline = "Warm amber to magenta sunset with swaying palm silhouettes",
            badge = "Tropical",
            presetId = "art-tropical-sunset"
        ),
        ShowcaseItem(
            id = "art-midnight-lotus",
            title = "Midnight Lotus",
            category = "Botanical & Zen",
            tagline = "Glowing pink water lily with jade leaves on moonlit ripples",
            badge = "Sacred Bloom",
            presetId = "art-midnight-lotus"
        ),
        ShowcaseItem(
            id = "art-holo-prism",
            title = "Holo Prism",
            category = "Futuristic Art",
            tagline = "Iridescent pastel rainbow foil with diamond geometric facets",
            badge = "Prismatic",
            presetId = "art-holo-prism"
        ),
        ShowcaseItem(
            id = "art-steam-punk",
            title = "Clockwork Chrono",
            category = "Vintage & Steampunk",
            tagline = "Intricate Victorian brass gears, copper cogs, and riveted armor",
            badge = "Steampunk",
            presetId = "art-steam-punk"
        ),
        ShowcaseItem(
            id = "art-crystal-geode",
            title = "Amethyst Geode",
            category = "Minerals & Crystals",
            tagline = "Deep royal purple quartz crystal cavern with gold leaf edge",
            badge = "Amethyst",
            presetId = "art-crystal-geode"
        ),
        ShowcaseItem(
            id = "art-neon-noir",
            title = "Neon Tokyo Rain",
            category = "Cyberpunk Noir",
            tagline = "Rain-slicked asphalt reflections with vertical kanji neon signs",
            badge = "Tokyo Noir",
            presetId = "art-neon-noir"
        ),
        ShowcaseItem(
            id = "art-solar-flare",
            title = "Solar Eclipse",
            category = "Cosmic & Astral",
            tagline = "Dramatic celestial corona with radiant golden solar energy flares",
            badge = "Solar Corona",
            presetId = "art-solar-flare"
        )
    )
}
