package com.netodev66.alllm.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the two UI defects that a compiler cannot see: a touch target that was
 * sized below the platform minimum, and a font weight that was never bundled.
 *
 * Both are silent. `Modifier.size(40.dp)` on a Material `IconButton` looks
 * deliberate and renders fine — it just makes the tappable area 40dp instead of
 * the 48dp the platform guarantees, so the control is fine with a fingertip and
 * misses with a thumb. Material's `IconButton` does apply
 * `minimumInteractiveComponentSize()`, but an explicit `size()` on the caller's
 * modifier is applied *after* it and wins, which is why the defect survives
 * review.
 *
 * `FontWeight.ExtraBold` is the same class of bug: `InterFamily` bundles only
 * Normal/Medium/SemiBold/Bold, and Compose does not synthesise a missing weight
 * reliably, so a declared-but-absent weight silently falls back. Its own KDoc
 * warns about this and the app still shipped one.
 *
 * The retired status literals are checked here too, because the palette migration
 * that removed them left two behind and nothing failed.
 */
class UiHygieneTest {

    private val sourceRoot: File = run {
        val start: String = System.getProperty("user.dir") ?: "."
        var dir: File? = File(start).absoluteFile
        while (dir != null && !File(dir, "app/build.gradle.kts").isFile) dir = dir.parentFile
        requireNotNull(dir) { "could not locate the repo root from $start" }
        File(dir, "app/src/main/java/com/netodev66/alllm")
    }

    private val MIN_TOUCH_DP = 48

    /** Colours that the semantic palette replaced; none may return to a call site. */
    private val retiredStatusLiterals = listOf(
        "0xFF2E7D32", // the old "success" green, unreadable on a dark pane
        "0xFF81C784",
        "0xFFA5D6A7",
        "0xFFF57C00", // warning, now statusColors.warning
        "0xFFE65100",
        "0xFFB71C1C", // error banner text, now statusColors.onDangerContainer
        "0xFF1B5E20", // build-success card fill, now statusColors.successContainer
        "0xFFFFEBEE",
        "0xFFFFF3E0",
    )

    private fun kotlinSources(): List<File> =
        sourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun `icon buttons keep the platform minimum touch target`() {
        val offenders = mutableListOf<String>()
        // Only the button's own argument list counts. The trailing lambda may size
        // the icon inside the button, and that is not the tap target, so the scan
        // stops at the matching close paren rather than reading to the next line.
        val callStart = Regex("""Pocket(?:IconButton|HamburgerButton)\(""")
        val ownModifier = Regex("""modifier\s*=\s*Modifier\.size\((\d+)\.dp\)""")
        kotlinSources().forEach { file ->
            val text = file.readText()
            callStart.findAll(text).forEach { call ->
                var i = call.range.last
                var depth = 0
                var end = -1
                while (i < text.length) {
                    when (text[i]) {
                        '(' -> depth++
                        ')' -> {
                            depth--
                            if (depth == 0) { end = i; break }
                        }
                    }
                    i++
                }
                val args = if (end > call.range.last) text.substring(call.range.last, end) else ""
                ownModifier.find(args)?.let { match ->
                    val dp = match.groupValues[1].toInt()
                    if (dp < MIN_TOUCH_DP) {
                        val line = text.take(call.range.first).count { it == '\n' } + 1
                        offenders += "${file.relativeTo(sourceRoot)}:$line: Modifier.size(${dp}.dp)"
                    }
                }
            }
        }
        assertTrue(
            "These icon buttons are sized under ${MIN_TOUCH_DP}dp, so the tappable area is " +
                "smaller than the platform minimum. Drop the explicit size (the button " +
                "defaults to 48dp) or raise it to ${MIN_TOUCH_DP}dp:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `no unbundled font weight is used`() {
        // InterFamily bundles Normal, Medium, SemiBold and Bold only.
        val unbundled = listOf("Thin", "ExtraLight", "Light", "ExtraBold", "Black")
        val offenders = mutableListOf<String>()
        kotlinSources().forEach { file ->
            val text = file.readText()
            unbundled.forEach { weight ->
                val count = Regex("""FontWeight\.$weight\b""").findAll(text).count()
                if (count > 0) {
                    offenders += "${file.relativeTo(sourceRoot)}: $count FontWeight.$weight"
                }
            }
        }
        assertTrue(
            "These weights have no font file in res/font, so Compose falls back to " +
                "another weight at runtime. Use Normal, Medium, SemiBold or Bold:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `retired status colours do not reappear at call sites`() {
        val offenders = mutableListOf<String>()
        kotlinSources()
            .filter { it.parentFile?.name != "theme" }
            .forEach { file ->
                // Comments legitimately describe what a colour used to be; only
                // real code counts.
                val code = file.readLines().joinToString("\n") { it.substringBefore("//") }
                retiredStatusLiterals.forEach { literal ->
                    val count = Regex(Regex.escape(literal)).findAll(code).count()
                    if (count > 0) {
                        offenders += "${file.relativeTo(sourceRoot)}: $count $literal"
                    }
                }
            }
        assertTrue(
            "These status colours were replaced by the semantic palette in " +
                "ui/theme/StatusColors.kt. Use statusColors.* instead of a literal:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }
}
