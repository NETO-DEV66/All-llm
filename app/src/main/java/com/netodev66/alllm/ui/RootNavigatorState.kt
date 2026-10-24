package com.netodev66.alllm.ui

/**
 * Screen plus the history of screens visited before it.
 *
 * A plain `var screen` value had nothing for the system Back button to unwind,
 * so Back from any secondary screen finished the activity and the app closed.
 * This keeps the visited order so Back steps one screen at a time, and it is a
 * plain class so that behaviour is testable without a Compose host.
 */
internal class RootNavigatorState(
    initial: RootScreen = RootScreen.CHAT,
    private val onChanged: () -> Unit = {},
) {
    var current: RootScreen = initial
        private set

    private val history = ArrayDeque<RootScreen>()

    /** Destinations visited before [current], oldest first. Exposed for tests. */
    val visited: List<RootScreen> get() = history.toList()

    /**
     * Moves to [destination], remembering where we came from. Re-selecting the
     * current screen is a no-op, so it does not push a duplicate.
     *
     * Returning to [RootScreen.CHAT] clears the history: Chat is the root, so
     * arriving there means the user is done with the branch they were in, and a
     * later Back should not walk back into it.
     */
    fun navigateTo(destination: RootScreen) {
        if (destination == current) return
        if (destination == RootScreen.CHAT) {
            history.clear()
        } else {
            history.addLast(current)
        }
        current = destination
        onChanged()
    }

    /** Steps back one screen. Returns false when there is nowhere to go. */
    fun pop(): Boolean {
        val previous = history.removeLastOrNull() ?: return false
        current = previous
        onChanged()
        return true
    }

    /** True while Back has somewhere to go inside the app. */
    fun canGoBack(): Boolean = current != RootScreen.CHAT || history.isNotEmpty()

    /** Snapshot for `rememberSaveable`, as enum names. */
    fun save(): List<String> = listOf(current.name) + history.map { it.name }

    companion object {
        /** Rebuilds state from [save]; unknown names are dropped. */
        fun restore(names: List<String>, onChanged: () -> Unit = {}): RootNavigatorState {
            val screens = names.mapNotNull { name -> RootScreen.entries.firstOrNull { it.name == name } }
            val state = RootNavigatorState(screens.firstOrNull() ?: RootScreen.CHAT, onChanged)
            screens.drop(1).forEach { state.history.addLast(it) }
            return state
        }
    }
}
