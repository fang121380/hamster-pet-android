package com.andrew.hamsterpet

enum class PetMotionAction(val priority: Int) {
    IDLE(0),
    WALK(1),
    SLEEP(2),
    PAT(3),
    DRAG(4),
    FEED(5),
    MENU(6),
    CLOSE(7),
}

object PetMotionPolicy {
    fun canStart(next: PetMotionAction, current: PetMotionAction): Boolean {
        if (current == PetMotionAction.CLOSE) return next == PetMotionAction.CLOSE
        return next.priority >= current.priority || next == current
    }
}
