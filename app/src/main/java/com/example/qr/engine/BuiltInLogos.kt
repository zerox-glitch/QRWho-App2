package com.example.qr.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

data class LogoItem(
    val id: String,
    val name: String,
    val category: String = "Popular Apps & Brands",
    val assetPath: String = ""
)

object BuiltInLogos {
    val list: List<LogoItem> = listOf(
        LogoItem("airbnb", "Airbnb", "Popular Apps & Brands", "airbnb.webp"),
        LogoItem("amazon", "Amazon", "Popular Apps & Brands", "amazon.webp"),
        LogoItem("android", "Android", "Popular Apps & Brands", "android.webp"),
        LogoItem("app-store", "App Store", "Popular Apps & Brands", "app-store.webp"),
        LogoItem("apple", "Apple", "Popular Apps & Brands", "apple.webp"),
        LogoItem("binance", "Binance", "Popular Apps & Brands", "binance.webp"),
        LogoItem("chrome", "Chrome", "Popular Apps & Brands", "chrome.webp"),
        LogoItem("discord", "Discord", "Popular Apps & Brands", "discord.webp"),
        LogoItem("duolingo", "Duolingo", "Popular Apps & Brands", "duolingo.webp"),
        LogoItem("epic-games", "Epic Games", "Popular Apps & Brands", "epic-games.webp"),
        LogoItem("etsy", "Etsy", "Popular Apps & Brands", "etsy.webp"),
        LogoItem("facebook", "Facebook", "Popular Apps & Brands", "facebook.webp"),
        LogoItem("facetime", "Facetime", "Popular Apps & Brands", "facetime.webp"),
        LogoItem("flower-calla-lily", "Calla Lily", "Flowers & Nature", "flower-calla-lily.webp"),
        LogoItem("flower-cherry-blossom", "Cherry Blossom", "Flowers & Nature", "flower-cherry-blossom.webp"),
        LogoItem("flower-crocus", "Crocus", "Flowers & Nature", "flower-crocus.webp"),
        LogoItem("flower-dahlia", "Dahlia", "Flowers & Nature", "flower-dahlia.webp"),
        LogoItem("flower-dandelion", "Dandelion", "Flowers & Nature", "flower-dandelion.webp"),
        LogoItem("flower-echinacea", "Echinacea", "Flowers & Nature", "flower-echinacea.webp"),
        LogoItem("flower-gerbera", "Gerbera", "Flowers & Nature", "flower-gerbera.webp"),
        LogoItem("flower-lavender", "Lavender", "Flowers & Nature", "flower-lavender.webp"),
        LogoItem("flower-lotus", "Lotus", "Flowers & Nature", "flower-lotus.webp"),
        LogoItem("flower-orchid", "Orchid", "Flowers & Nature", "flower-orchid.webp"),
        LogoItem("flower-periwinkle", "Periwinkle", "Flowers & Nature", "flower-periwinkle.webp"),
        LogoItem("flower-petunia", "Petunia", "Flowers & Nature", "flower-petunia.webp"),
        LogoItem("flower-pink-blossom", "Pink Blossom", "Flowers & Nature", "flower-pink-blossom.webp"),
        LogoItem("flower-plumeria", "Plumeria", "Flowers & Nature", "flower-plumeria.webp"),
        LogoItem("flower-red-ginger", "Red Ginger", "Flowers & Nature", "flower-red-ginger.webp"),
        LogoItem("flower-sunflowers-1", "Sunflowers 1", "Flowers & Nature", "flower-sunflowers-1.webp"),
        LogoItem("flower-sunflowers-2", "Sunflowers 2", "Flowers & Nature", "flower-sunflowers-2.webp"),
        LogoItem("flower-tulips", "Tulips", "Flowers & Nature", "flower-tulips.webp"),
        LogoItem("google-meet", "Google Meet", "Popular Apps & Brands", "google-meet.webp"),
        LogoItem("google-play", "Google Play", "Popular Apps & Brands", "google-play.webp"),
        LogoItem("google", "Google", "Popular Apps & Brands", "google.webp"),
        LogoItem("icon-black-at", "Black At", "Icons & Badges", "icon-black-at.webp"),
        LogoItem("icon-black-w", "Black W", "Icons & Badges", "icon-black-w.webp"),
        LogoItem("icon-blue-dashed-bubble", "Blue Dashed Bubble", "Icons & Badges", "icon-blue-dashed-bubble.webp"),
        LogoItem("icon-blue-m-circle", "Blue M Circle", "Icons & Badges", "icon-blue-m-circle.webp"),
        LogoItem("icon-blue-ring", "Blue Ring", "Icons & Badges", "icon-blue-ring.webp"),
        LogoItem("icon-color-blobs", "Color Blobs", "Icons & Badges", "icon-color-blobs.webp"),
        LogoItem("icon-green-up", "Green Up", "Icons & Badges", "icon-green-up.webp"),
        LogoItem("icon-key-mark-dark", "Key Mark Dark", "Icons & Badges", "icon-key-mark-dark.webp"),
        LogoItem("icon-navy-b-dot", "Navy B Dot", "Icons & Badges", "icon-navy-b-dot.webp"),
        LogoItem("icon-orange-swoosh", "Orange Swoosh", "Icons & Badges", "icon-orange-swoosh.webp"),
        LogoItem("icon-pink-f", "Pink F", "Icons & Badges", "icon-pink-f.webp"),
        LogoItem("icon-pink-g", "Pink G", "Icons & Badges", "icon-pink-g.webp"),
        LogoItem("icon-purple-m", "Purple M", "Icons & Badges", "icon-purple-m.webp"),
        LogoItem("icon-red-bag", "Red Bag", "Icons & Badges", "icon-red-bag.webp"),
        LogoItem("icon-red-burst", "Red Burst", "Icons & Badges", "icon-red-burst.webp"),
        LogoItem("icon-teal-k", "Teal K", "Icons & Badges", "icon-teal-k.webp"),
        LogoItem("icon-teal-tag", "Teal Tag", "Icons & Badges", "icon-teal-tag.webp"),
        LogoItem("instagram", "Instagram", "Popular Apps & Brands", "instagram.webp"),
        LogoItem("line", "Line", "Popular Apps & Brands", "line.webp"),
        LogoItem("linkedin", "Linkedin", "Popular Apps & Brands", "linkedin.webp"),
        LogoItem("messenger", "Messenger", "Popular Apps & Brands", "messenger.webp"),
        LogoItem("microsoft-store", "Microsoft Store", "Popular Apps & Brands", "microsoft-store.webp"),
        LogoItem("openai", "OpenAI", "Popular Apps & Brands", "openai.webp"),
        LogoItem("paypal", "Paypal", "Popular Apps & Brands", "paypal.webp"),
        LogoItem("pinterest", "Pinterest", "Popular Apps & Brands", "pinterest.webp"),
        LogoItem("playstation", "Playstation", "Popular Apps & Brands", "playstation.webp"),
        LogoItem("quora", "Quora", "Popular Apps & Brands", "quora.webp"),
        LogoItem("reddit", "Reddit", "Popular Apps & Brands", "reddit.webp"),
        LogoItem("shopify", "Shopify", "Popular Apps & Brands", "shopify.webp"),
        LogoItem("skype-classic", "Skype Classic", "Popular Apps & Brands", "skype-classic.webp"),
        LogoItem("skype-modern", "Skype Modern", "Popular Apps & Brands", "skype-modern.webp"),
        LogoItem("snapchat", "Snapchat", "Popular Apps & Brands", "snapchat.webp"),
        LogoItem("steam", "Steam", "Popular Apps & Brands", "steam.webp"),
        LogoItem("teams", "Teams", "Popular Apps & Brands", "teams.webp"),
        LogoItem("telegram", "Telegram", "Popular Apps & Brands", "telegram.webp"),
        LogoItem("tiktok", "Tiktok", "Popular Apps & Brands", "tiktok.webp"),
        LogoItem("tripadvisor", "Tripadvisor", "Popular Apps & Brands", "tripadvisor.webp"),
        LogoItem("tumblr", "Tumblr", "Popular Apps & Brands", "tumblr.webp"),
        LogoItem("twitch", "Twitch", "Popular Apps & Brands", "twitch.webp"),
        LogoItem("twitter", "Twitter", "Popular Apps & Brands", "twitter.webp"),
        LogoItem("viber", "Viber", "Popular Apps & Brands", "viber.webp"),
        LogoItem("vk", "VK", "Popular Apps & Brands", "vk.webp"),
        LogoItem("wechat", "Wechat", "Popular Apps & Brands", "wechat.webp"),
        LogoItem("whatsapp", "Whatsapp", "Popular Apps & Brands", "whatsapp.webp"),
        LogoItem("x", "X (Twitter)", "Popular Apps & Brands", "x.webp"),
        LogoItem("xbox", "Xbox", "Popular Apps & Brands", "xbox.webp"),
        LogoItem("zoom", "Zoom", "Popular Apps & Brands", "zoom.webp")
    )

    fun loadLogoBitmap(context: Context, logoId: String): Bitmap? {
        val item = list.find { it.id == logoId } ?: return null
        return try {
            val stream: InputStream = context.assets.open("logos/${item.assetPath}")
            val bmp = BitmapFactory.decodeStream(stream)
            stream.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }
}
