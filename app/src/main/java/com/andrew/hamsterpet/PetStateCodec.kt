package com.andrew.hamsterpet

import org.json.JSONArray
import org.json.JSONObject

object PetStateCodec {
    private const val VERSION = 4

    fun encode(state: PetState): String = JSONObject().apply {
        put("version", VERSION)
        put("satiety", state.satiety)
        put("affection", state.affection)
        put("carePoints", state.carePoints)
        put("feedingCount", state.feedingCount)
        put("interactionCount", state.interactionCount)
        put("nestLevel", state.nestLevel)
        put("overlayRunning", state.overlayRunning)
        put("motionMode", state.motionMode.name)
        put("skin", state.skin.name)
        put("furTint", state.furTint.name)
        put("petScale", state.petScale.toDouble())
        put("musicEnabled", state.musicEnabled)
        put("bgmTrackIndex", state.bgmTrackIndex)
        put("bgmVolume", state.bgmVolume)
        put("lastSatietyUpdateAt", state.lastSatietyUpdateAt)
        put("lastAffectionUpdateAt", state.lastAffectionUpdateAt)
        put("lastCarePointAt", state.lastCarePointAt)
        put("breedingStartedAt", state.breedingStartedAt)
        put("babies", JSONArray().apply {
            state.babies.forEach { baby ->
                put(JSONObject().apply {
                    put("id", baby.id)
                    put("variant", baby.variant.name)
                    put("bornAt", baby.bornAt)
                    put("feedingCount", baby.feedingCount)
                    put("playCount", baby.playCount)
                })
            }
        })
    }.toString()

    fun decode(raw: String?, now: Long): PetState {
        if (raw.isNullOrBlank()) return PetGameEngine.sanitize(PetState(), now)
        return runCatching {
            val json = JSONObject(raw)
            val babiesJson = json.optJSONArray("babies") ?: JSONArray()
            val babies = buildList {
                for (index in 0 until babiesJson.length()) {
                    val item = babiesJson.optJSONObject(index) ?: continue
                    val variant = runCatching {
                        BabyVariant.valueOf(item.optString("variant"))
                    }.getOrNull() ?: continue
                    val legacyCare = item.optInt("careCount", 0)
                    add(
                        BabyState(
                            id = item.optLong("id", -1L),
                            variant = variant,
                            bornAt = item.optLong("bornAt", now),
                            feedingCount = item.optInt("feedingCount", legacyCare),
                            playCount = item.optInt("playCount", legacyCare / 2),
                        ),
                    )
                }
            }
            val motionMode = runCatching {
                MotionMode.valueOf(json.optString("motionMode", MotionMode.STANDARD.name))
            }.getOrDefault(MotionMode.STANDARD)
            val skin = runCatching {
                HamsterSkin.valueOf(json.optString("skin", HamsterSkin.CLASSIC.name))
            }.getOrDefault(HamsterSkin.CLASSIC)
            val furTint = runCatching {
                FurTint.valueOf(json.optString("furTint", FurTint.NATURAL.name))
            }.getOrDefault(FurTint.NATURAL)
            PetGameEngine.sanitize(
                PetState(
                    satiety = json.optInt("satiety", 70),
                    affection = json.optInt("affection", 35),
                    carePoints = json.optInt("carePoints", 0),
                    feedingCount = json.optInt("feedingCount", json.optInt("carePoints", 0)),
                    interactionCount = json.optInt("interactionCount", 0),
                    nestLevel = json.optInt("nestLevel", 0),
                    overlayRunning = json.optBoolean("overlayRunning", false),
                    motionMode = motionMode,
                    skin = skin,
                    furTint = furTint,
                    petScale = json.optDouble("petScale", 1.0).toFloat(),
                    musicEnabled = json.optBoolean("musicEnabled", true),
                    bgmTrackIndex = json.optInt("bgmTrackIndex", 0),
                    bgmVolume = json.optInt("bgmVolume", 42),
                    lastSatietyUpdateAt = json.optLong("lastSatietyUpdateAt", now),
                    lastAffectionUpdateAt = json.optLong("lastAffectionUpdateAt", now),
                    lastCarePointAt = json.optLong("lastCarePointAt", 0L),
                    breedingStartedAt = json.optLong("breedingStartedAt", 0L),
                    babies = babies,
                ),
                now,
            )
        }.getOrElse {
            PetGameEngine.sanitize(PetState(), now)
        }
    }
}
