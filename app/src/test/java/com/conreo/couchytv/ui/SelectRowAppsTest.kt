package com.conreo.couchytv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.conreo.couchytv.data.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which apps the single row shows.
 *
 * The invariant: the home screen is never empty. A launcher with nothing on it
 * is broken, so both "no pins" and "pins that no longer resolve" fall back to
 * the full list.
 */
class SelectRowAppsTest {

    private fun app(pkg: String) = AppEntry(
        pkg = pkg,
        label = pkg,
        banner = null,
        icon = null,
        autoCategory = "",
        tile = Color.Gray,
        stamp = 0L,
        firstInstall = 0L,
    )

    private val all = listOf(app("a"), app("b"), app("c"))

    @Test
    fun `no pins shows every app`() {
        val row = selectRowApps(all, all, emptyList())
        assertEquals(3, row.size)
    }

    @Test
    fun `pins select only the pinned apps`() {
        val row = selectRowApps(all, all, listOf("a", "c"))
        assertEquals(listOf("a", "c"), row.map { it.pkg })
    }

    @Test
    fun `the pin order is the row order`() {
        val row = selectRowApps(all, all, listOf("c", "a"))
        assertEquals(listOf("c", "a"), row.map { it.pkg })
    }

    @Test
    fun `an uninstalled pin is skipped, not fatal`() {
        val row = selectRowApps(all, all, listOf("a", "gone"))
        assertEquals(listOf("a"), row.map { it.pkg })
    }

    @Test
    fun `pins that all vanished fall back to every app`() {
        val row = selectRowApps(all, all, listOf("gone1", "gone2"))
        assertEquals(3, row.size)
    }

    @Test
    fun `the row is never empty`() {
        assertEquals(3, selectRowApps(all, all, emptyList()).size)
        assertEquals(3, selectRowApps(all, all, listOf("nope")).size)
    }

    @Test
    fun `a hidden app is gone from the row, not greyed out`() {
        val row = selectRowApps(all, all, listOf("a", "b", "c"), hidden = setOf("b"))
        assertEquals(listOf("a", "c"), row.map { it.pkg })
    }

    @Test
    fun `a hidden app is gone from the fallback list too`() {
        val row = selectRowApps(all, all, emptyList(), hidden = setOf("b"))
        assertEquals(listOf("a", "c"), row.map { it.pkg })
    }

    @Test
    fun `hiding every pinned app falls back to the visible ones`() {
        val row = selectRowApps(all, all, listOf("a", "b"), hidden = setOf("a", "b"))
        assertEquals(listOf("c"), row.map { it.pkg })
    }
}
