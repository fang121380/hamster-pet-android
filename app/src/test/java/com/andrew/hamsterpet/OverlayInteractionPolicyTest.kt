package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayInteractionPolicyTest {
    @Test
    fun pinch_scale_supports_thirty_five_to_two_hundred_eighty_percent() {
        assertEquals(.35f, OverlayInteractionPolicy.clampScale(.1f))
        assertEquals(1.42f, OverlayInteractionPolicy.clampScale(1.42f))
        assertEquals(2.8f, OverlayInteractionPolicy.clampScale(5f))
    }

    @Test
    fun window_grows_to_keep_large_sprite_visible() {
        val normal = OverlayInteractionPolicy.windowSize(scale = 1f, expanded = false, foodTrayVisible = false)
        val large = OverlayInteractionPolicy.windowSize(scale = 2.8f, expanded = false, foodTrayVisible = false)

        assertTrue(large.widthDp > normal.widthDp)
        assertTrue(large.heightDp > normal.heightDp)
        assertTrue(large.widthDp >= 408)
        assertTrue(large.heightDp >= 422)
    }

    @Test
    fun food_drop_requires_token_to_reach_the_mouth_area() {
        val mouth = OverlayPoint(150f, 120f)

        assertTrue(OverlayInteractionPolicy.isFoodDropAccepted(OverlayPoint(166f, 128f), mouth, 140f))
        assertFalse(OverlayInteractionPolicy.isFoodDropAccepted(OverlayPoint(50f, 30f), mouth, 140f))
    }

    @Test
    fun food_drop_minimum_radius_uses_density_independent_size() {
        val mouth = OverlayPoint(0f, 0f)

        assertTrue(OverlayInteractionPolicy.isFoodDropAccepted(OverlayPoint(50f, 0f), mouth, petWidth = 49f, density = 2f))
        assertFalse(OverlayInteractionPolicy.isFoodDropAccepted(OverlayPoint(80f, 0f), mouth, petWidth = 49f, density = 2f))
    }

    @Test
    fun eight_food_tray_reserves_two_visible_rows() {
        val menuOnly = OverlayInteractionPolicy.windowSize(scale = 1f, expanded = true, foodTrayVisible = false)
        val withFood = OverlayInteractionPolicy.windowSize(scale = 1f, expanded = true, foodTrayVisible = true)

        assertTrue(withFood.heightDp - menuOnly.heightDp >= 80)
    }
}
