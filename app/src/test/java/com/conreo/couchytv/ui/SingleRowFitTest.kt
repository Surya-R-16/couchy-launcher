package com.conreo.couchytv.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The single-row layout's sizing maths.
 *
 * The invariant that matters: EVERY app stays on the home screen. A row that
 * silently drops apps hides them from the owner, and a launcher that hides
 * apps is a launcher with unreachable apps.
 */
class SingleRowFitTest {

    // The TV's usable width inside the scaled UI: 1200dp screen, minus the
    // row's outer margin and the glass panel's inner padding.
    private val available = 1200.dp - 48.dp - 24.dp
    private val maxIcon = 150.dp
    private val prefGap = 10.dp

    @Test
    fun `no apps means nothing to lay out`() {
        val fit = fitSingleRow(available, 0, maxIcon, prefGap)
        assertEquals(0, fit.visibleCount)
        assertEquals(0f, fit.icon.value, 0.01f)
    }

    @Test
    fun `a single app keeps the full icon size`() {
        val fit = fitSingleRow(available, 1, maxIcon, prefGap)
        assertEquals(maxIcon.value, fit.icon.value, 0.01f)
        assertEquals(0f, fit.gap.value, 0.01f)
        assertEquals(1, fit.visibleCount)
    }

    @Test
    fun `a row that fits keeps the requested icon size`() {
        val fit = fitSingleRow(available, 4, maxIcon, prefGap)
        assertEquals(maxIcon.value, fit.icon.value, 0.01f)
        assertEquals(4, fit.visibleCount)
    }

    @Test
    fun `the leftover width becomes extra gap, never wider than preferred`() {
        val fit = fitSingleRow(available, 4, maxIcon, prefGap)
        assertTrue("gap must not exceed the preferred gap", fit.gap.value <= prefGap.value + 0.01f)
    }

    @Test
    fun `every app survives a crowded row`() {
        // From a handful of apps to far more than one line could hold.
        for (n in listOf(6, 8, 12, 20, 25, 30, 40, 60, 90, 120)) {
            val fit = fitSingleRow(available, n, maxIcon, prefGap)
            assertEquals("all $n apps must stay in the row", n, fit.visibleCount)
        }
    }

    @Test
    fun `past the hard floor the row drops apps instead of overflowing`() {
        // Absurd app count: the row cannot hold them all at a visible size, so
        // it keeps the floor and shows what fits. It must never overflow.
        val fit = fitSingleRow(available, 400, maxIcon, prefGap)
        assertTrue(fit.visibleCount < 400)
        val used = fit.visibleCount * fit.icon.value + (fit.visibleCount - 1) * fit.gap.value
        assertTrue("row overflows: $used > ${available.value}", used <= available.value + 0.5f)
    }

    @Test
    fun `the row never overflows the available width`() {
        for (n in listOf(2, 5, 9, 15, 22, 35, 50, 80)) {
            val fit = fitSingleRow(available, n, maxIcon, prefGap)
            val used = n * fit.icon.value + (n - 1) * fit.gap.value
            assertTrue(
                "row of $n overflows: $used > ${available.value}",
                used <= available.value + 0.5f,
            )
        }
    }

    @Test
    fun `the icon shrinks as apps are added`() {
        val few = fitSingleRow(available, 6, maxIcon, prefGap)
        val many = fitSingleRow(available, 30, maxIcon, prefGap)
        assertTrue(many.icon < few.icon)
    }

    @Test
    fun `the icon never grows past the user's ceiling`() {
        val fit = fitSingleRow(available, 3, maxIcon, prefGap)
        assertTrue(fit.icon.value <= maxIcon.value + 0.01f)
    }

    @Test
    fun `a realistic app count lands on a readable icon`() {
        // 25 apps on this screen: the icons have to stay big enough to read
        // across a room.
        val fit = fitSingleRow(available, 25, maxIcon, prefGap)
        assertTrue("icon too small: ${fit.icon}", fit.icon.value >= 32f)
    }
}
