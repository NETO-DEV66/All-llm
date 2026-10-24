package com.netodev66.alllm.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every tappable in the app must give the same press feedback. `Modifier.clickable`
 * only ripples; `bounceClick` scales the surface through the app's bounce spring.
 *
 * This test exists because a partial conversion looks finished. A grep for
 * `.clickable` returned 19 hits after the first pass and a grep for `bounceClick`
 * looked healthy, but whole files (AgentScreen's model pickers, the browser's tab
 * strip and history rows) had never been touched. Nothing failed, because a row
 * with a ripple is not a compile error — it is just an inconsistent tap.
 *
 * The scan covers every source package, not just `ui/`: the same drift had already
 * happened in `git/ui`, `pentest/ui` and `agent/team/ui`, which the first version
 * of this test never looked at.
 *
 * The allow-list is deliberately tiny and each entry is a scrim: a full-screen
 * dimmed backdrop that dismisses on tap. A scrim must not shrink under the
 * finger, so it is the one control that is correct without bounce.
 */
class BounceCoverageTest {

    private val sourceRoot: File = run {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "app/build.gradle.kts").isFile) dir = dir.parentFile
        requireNotNull(dir) { "could not locate the repo root from ${System.getProperty("user.dir")}" }
        File(dir, "app/src/main/java/com/netodev66/alllm")
    }

    private val uiDir: File get() = File(sourceRoot, "ui")

    /** file name -> number of plain-clickable calls that are legitimately scrims. */
    private val allowedScrims = mapOf(
        "PocketComponents.kt" to 2, // the bounceClick definition itself + the drawer scrim
        "MarketplaceScreen.kt" to 1, // dimmed backdrop behind the marketplace search sheet
        "PocketDevApp.kt" to 1, // dimmed backdrop behind the bottom sheet
    )

    /** Material's chips hardcode their ripple; only the Pocket* wrappers may be used. */
    private val chipWrappers = mapOf(
        "FilterChip(" to "PocketFilterChip(",
        "AssistChip(" to "PocketAssistChip(",
    )

    @Test
    fun `every tappable in the app uses bounceClick`() {
        val offenders = mutableListOf<String>()
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                val count = Regex("""\.clickable[ (]""").findAll(text).count()
                val allowed = allowedScrims[file.name] ?: 0
                if (count > allowed) {
                    val rel = file.relativeTo(sourceRoot)
                    offenders += "$rel: $count plain .clickable (allowed $allowed)"
                }
            }
        assertTrue(
            "These files still call Modifier.clickable, so those taps ripple instead of " +
                "bouncing. Convert them to bounceClick, or add the file to allowedScrims if " +
                "the call is a dismiss scrim:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `every chip goes through a bounce wrapper`() {
        val offenders = mutableListOf<String>()
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "PocketButtons.kt" }
            .forEach { file ->
                val text = file.readText()
                chipWrappers.forEach { (material, wrapper) ->
                    val count = Regex("""(?<![\w.])${Regex.escape(material)}""").findAll(text).count()
                    if (count > 0) {
                        offenders += "${file.relativeTo(sourceRoot)}: $count $material (use $wrapper)"
                    }
                }
            }
        assertTrue(
            "Material's chips ripple on press instead of bouncing. Use the Pocket wrapper:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `bounceClick is the single press-feedback implementation`() {
        val components = File(uiDir, "theme/PocketComponents.kt").readText()
        // A second hand-rolled copy of the scale+clickable pair would drift from
        // this one; PocketNavDrawerItem used to be exactly that.
        assertTrue(
            "bounceClick must be declared exactly once, in PocketComponents.kt",
            Regex("""fun Modifier\.bounceClick\(""").findAll(components).count() == 1,
        )
    }
}
