package com.conreo.couchytv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reordering the curated row: hold OK → Move → Left/Right.
 *
 * The row has no wrap-around — an app cannot be pushed off the edge of the
 * screen, so a move past either end is refused.
 */
class MoveWithinTest {

    private val list = listOf("a", "b", "c", "d")

    @Test
    fun `moves one place to the right`() {
        assertEquals(listOf("b", "a", "c", "d"), moveWithin(list, "a", 1))
    }

    @Test
    fun `moves one place to the left`() {
        // A move, not a swap in name only: the app lands one place earlier and
        // the one it passed shifts back. For a single step that reads as a swap.
        assertEquals(listOf("b", "a", "c", "d"), moveWithin(list, "b", -1))
    }

    @Test
    fun `moves several places at once`() {
        assertEquals(listOf("b", "c", "d", "a"), moveWithin(list, "a", 3))
    }

    @Test
    fun `refuses to move past the start`() {
        assertEquals(list, moveWithin(list, "a", -1))
    }

    @Test
    fun `refuses to move past the end`() {
        assertEquals(list, moveWithin(list, "d", 1))
    }

    @Test
    fun `ignores an app that is not in the list`() {
        assertEquals(list, moveWithin(list, "zz", 1))
    }

    @Test
    fun `the order is otherwise preserved`() {
        val moved = moveWithin(list, "c", -2)
        assertEquals(listOf("c", "a", "b", "d"), moved)
    }
}
