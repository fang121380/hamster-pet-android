package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Test

class PetServiceActionsTest {
    @Test
    fun stop_action_is_stable() {
        assertEquals("com.andrew.hamsterpet.action.STOP", PetServiceActions.STOP)
    }
}
