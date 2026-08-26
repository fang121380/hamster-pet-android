package com.andrew.hamsterpet

enum class BgmTrack(
    val displayName: String,
    val author: String,
    val resourceId: Int,
    val sourceUrl: String,
    val license: String = "CC0",
) {
    MY_STREET(
        "街角漫步",
        "congusbongus",
        R.raw.bgm_my_street,
        "https://opengameart.org/content/my-street",
    ),
    DANCE_FIELD(
        "跳舞原野",
        "Centurion_of_war",
        R.raw.bgm_dance_field,
        "https://opengameart.org/content/dance-field",
    ),
    CONCENTRATION(
        "专注小循环",
        "cosmac",
        R.raw.bgm_concentration,
        "https://opengameart.org/content/8-bit-concentration-loop",
    ),
    DREAM_ALIVE(
        "梦想不停",
        "congusbongus",
        R.raw.bgm_dream_alive,
        "https://opengameart.org/content/keep-your-dream-alive-seamless-loop",
    ),
    ITEM_SHOP(
        "温暖商店",
        "congusbongus",
        R.raw.bgm_item_shop,
        "https://opengameart.org/content/welcome-to-the-item-shop",
    ),
    ;

    companion object {
        fun fromIndex(index: Int): BgmTrack = entries[Math.floorMod(index, entries.size)]
        fun nextIndex(index: Int): Int = Math.floorMod(index + 1, entries.size)
        fun clampVolume(volume: Int): Int = volume.coerceIn(0, 100)
        fun volumeGain(volume: Int): Float = clampVolume(volume) / 100f
    }
}
