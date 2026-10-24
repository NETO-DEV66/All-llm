package com.netodev66.alllm.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reported bug: from Settings, Back closed the whole app. These tests pin the
 * rule that Back unwinds one screen at a time, and that only Chat with an empty
 * history lets the system take Back and leave the app.
 */
class RootNavigatorStateTest {

    @Test
    fun `back from a secondary screen returns to the screen it was opened from`() {
        val nav = RootNavigatorState()
        nav.navigateTo(RootScreen.SETTINGS)

        assertTrue("Back must be handled in-app", nav.canGoBack())
        assertTrue(nav.pop())
        assertEquals(RootScreen.CHAT, nav.current)
        assertFalse("at Chat with empty history, Back leaves the app", nav.canGoBack())
    }

    @Test
    fun `back walks a chain one step at a time, in reverse order`() {
        val nav = RootNavigatorState()
        nav.navigateTo(RootScreen.PROJECTS)
        nav.navigateTo(RootScreen.SETTINGS)
        nav.navigateTo(RootScreen.SKILLS)

        assertEquals(RootScreen.SKILLS, nav.current)
        assertTrue(nav.pop())
        assertEquals(RootScreen.SETTINGS, nav.current)
        assertTrue(nav.pop())
        assertEquals(RootScreen.PROJECTS, nav.current)
        assertTrue(nav.pop())
        assertEquals(RootScreen.CHAT, nav.current)
        assertFalse(nav.pop())
    }

    @Test
    fun `back from Settings reached via the drawer returns to the screen that was showing`() {
        val nav = RootNavigatorState()
        nav.navigateTo(RootScreen.MARKETPLACE)
        nav.navigateTo(RootScreen.SETTINGS)

        nav.pop()
        assertEquals(RootScreen.MARKETPLACE, nav.current)
    }

    @Test
    fun `re-selecting the current screen does not push a duplicate`() {
        val nav = RootNavigatorState()
        nav.navigateTo(RootScreen.SETTINGS)
        nav.navigateTo(RootScreen.SETTINGS)

        assertTrue(nav.pop())
        assertEquals(RootScreen.CHAT, nav.current)
        assertFalse("only one entry must have been pushed", nav.pop())
    }

    @Test
    fun `returning to Chat clears the history, so a later back does not re-enter the branch`() {
        val nav = RootNavigatorState()
        nav.navigateTo(RootScreen.SETTINGS)
        nav.navigateTo(RootScreen.CHAT)

        assertFalse(nav.canGoBack())
        assertFalse(nav.pop())
    }

    @Test
    fun `pop reports failure instead of moving when there is no history`() {
        val nav = RootNavigatorState()
        assertFalse(nav.pop())
        assertEquals(RootScreen.CHAT, nav.current)
    }

    @Test
    fun `state survives a save and restore, so rotation keeps the back stack`() {
        val nav = RootNavigatorState()
        nav.navigateTo(RootScreen.PROJECTS)
        nav.navigateTo(RootScreen.SETTINGS)

        val restored = RootNavigatorState.restore(nav.save())

        assertEquals(RootScreen.SETTINGS, restored.current)
        assertTrue(restored.pop())
        assertEquals(RootScreen.PROJECTS, restored.current)
    }

    @Test
    fun `restore drops an unknown screen name rather than failing`() {
        val restored = RootNavigatorState.restore(listOf("SETTINGS", "NOT_A_SCREEN"))

        assertEquals(RootScreen.SETTINGS, restored.current)
        assertFalse(restored.pop())
    }

    @Test
    fun `an empty save restores to Chat`() {
        val restored = RootNavigatorState.restore(emptyList())
        assertEquals(RootScreen.CHAT, restored.current)
        assertFalse(restored.canGoBack())
    }

    @Test
    fun `every destination in the drawer can be backed out of`() {
        RootScreen.entries.filter { it != RootScreen.CHAT }.forEach { destination ->
            val nav = RootNavigatorState()
            nav.navigateTo(destination)
            assertTrue("Back must be handled on ${destination.name}", nav.canGoBack())
            nav.pop()
            assertEquals("Back from ${destination.name} must reach Chat", RootScreen.CHAT, nav.current)
        }
    }
}
