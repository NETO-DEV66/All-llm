package com.netodev66.alllm.release

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Play derives "What's new" from `changelogs/<versionCode>.txt`. Two releases
 * shipped a changelog named after the *version name* (10.txt for v3.9.0,
 * 11.txt for v3.9.1), so the store listing would have shown an empty note and
 * the files would have been ignored by the upload. Nothing failed, because a
 * stray changelog file is not an error to any tool — only to the user reading
 * an empty release note.
 */
class FastlaneChangelogTest {

    private val repoRoot: File = run {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "app/build.gradle.kts").isFile) dir = dir.parentFile
        requireNotNull(dir) { "could not locate the repo root from ${System.getProperty("user.dir")}" }
    }

    private val changelogDir = File(repoRoot, "fastlane/metadata/android/en-US/changelogs")

    private fun currentVersionCode(): Long {
        val gradle = File(repoRoot, "app/build.gradle.kts").readText()
        val match = Regex("""versionCode = (\d+)""").find(gradle)
        return requireNotNull(match) { "versionCode not found in app/build.gradle.kts" }.groupValues[1].toLong()
    }

    @Test
    fun `every changelog is named after a version code`() {
        val files = changelogDir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }.orEmpty()
        assertTrue("no changelog files found in $changelogDir", files.isNotEmpty())
        files.forEach { file ->
            val name = file.nameWithoutExtension
            assertTrue(
                "changelog ${file.name} must be named <versionCode>.txt; Play looks up " +
                    "the file by version code and ignores anything else",
                name.toIntOrNull() != null,
            )
        }
    }

    @Test
    fun `the current version code has a changelog`() {
        val expected = File(changelogDir, "${currentVersionCode()}.txt")
        assertTrue(
            "missing ${expected.name}: this release would publish an empty " +
                "\"What's new\" note to Play",
            expected.isFile,
        )
    }

    @Test
    fun `changelogs are non-empty and Play-length`() {
        val files = changelogDir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }.orEmpty()
        files.forEach { file ->
            val text = file.readText().trim()
            assertTrue("changelog ${file.name} is empty", text.isNotEmpty())
            // Play rejects a release note longer than 500 characters.
            assertTrue(
                "changelog ${file.name} is ${text.length} characters; Play caps notes at 500",
                text.length <= 500,
            )
        }
    }
}
