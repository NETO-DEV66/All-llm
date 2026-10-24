package com.netodev66.alllm.session

import com.netodev66.alllm.core.result.OperationResult
import com.netodev66.alllm.core.result.OperationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The loop's whole value is that a green result cannot be faked. These tests
 * drive it with real [OperationResult] values and assert the stopping rules, so
 * the honesty guarantees are pinned rather than assumed.
 */
class VerificationLoopRunnerTest {

    private fun failure(exitCode: Int = 1, output: String = ""): OperationResult = OperationResult(
        status = OperationStatus.FAILED,
        exitCode = exitCode,
        error = output.ifBlank { null },
        logs = if (output.isBlank()) emptyList() else listOf(output),
        finishedAt = System.currentTimeMillis(),
    )

    private fun success(output: String = "BUILD SUCCESSFUL"): OperationResult = OperationResult(
        status = OperationStatus.SUCCESS,
        exitCode = 0,
        logs = listOf(output),
        verificationEvidence = "exit=0",
        finishedAt = System.currentTimeMillis(),
    )

    @Test
    fun `green on the first attempt stops without applying any fix`() = runBlocking {
        var fixes = 0
        val runner = VerificationLoopRunner(maxIterations = 3, diagnose = { errorDiagnosis() })

        val result = runner.run(
            command = "gradle assembleDebug",
            runBuild = { success() },
            applyFix = { fixes++; true },
        )

        assertTrue(result.success)
        assertEquals(1, result.iterations)
        assertEquals(0, fixes)
        assertTrue(result.stoppedBecause.startsWith("Verified"))
    }

    @Test
    fun `a failing build is diagnosed, fixed, then verified`() = runBlocking {
        val runs = mutableListOf<Int>()
        val fixes = mutableListOf<ErrorDiagnosis>()
        val runner = VerificationLoopRunner(
            maxIterations = 3,
            diagnose = { output ->
                assertTrue("diagnosis must see the real compiler error", output.contains("Unresolved reference"))
                errorDiagnosis()
            },
        )

        val result = runner.run(
            command = "gradle assembleDebug",
            runBuild = { iteration ->
                runs += iteration
                if (iteration == 1) failure(output = "e: app/Main.kt:12:5 Unresolved reference 'foo'") else success()
            },
            applyFix = { fixes += it; true },
        )

        assertTrue(result.success)
        assertEquals(listOf(1, 2), runs)
        assertEquals(1, fixes.size)
        assertEquals(2, result.iterations)
        assertEquals(true, result.attempts.first().fixApplied)
        assertFalse(result.attempts.first().success)
    }

    @Test
    fun `stops when the build cannot be run at all`() = runBlocking {
        var attempts = 0
        var fixes = 0
        val runner = VerificationLoopRunner(maxIterations = 3, diagnose = { errorDiagnosis() })

        val result = runner.run(
            command = "gradle assembleDebug",
            runBuild = {
                attempts++
                OperationResult.notConfigured("Linux runtime is not installed; cannot run Gradle build/test.")
            },
            applyFix = { fixes++; true },
        )

        assertFalse(result.success)
        assertEquals("a missing toolchain must not be retried", 1, attempts)
        assertEquals(0, fixes)
        assertTrue(result.stoppedBecause.contains("Could not run the build"))
    }

    @Test
    fun `stops when no fix can be applied instead of retesting the same tree`() = runBlocking {
        var attempts = 0
        val runner = VerificationLoopRunner(maxIterations = 3, diagnose = { errorDiagnosis() })

        val result = runner.run(
            command = "gradle assembleDebug",
            runBuild = { attempts++; failure(output = "error: cannot find symbol") },
            applyFix = { false },
        )

        assertFalse(result.success)
        assertEquals(1, attempts)
        assertEquals(1, result.iterations)
        assertTrue(result.stoppedBecause.contains("No fix could be applied"))
    }

    @Test
    fun `exhausts the iteration budget and reports the last real error`() = runBlocking {
        var attempts = 0
        val runner = VerificationLoopRunner(
            maxIterations = 3,
            diagnose = { errorDiagnosis(rootCause = "type mismatch in Main.kt") },
        )

        val result = runner.run(
            command = "gradle assembleDebug",
            runBuild = { attempts++; failure(output = "e: Main.kt:3:1 type mismatch") },
            applyFix = { true },
        )

        assertFalse(result.success)
        assertEquals(3, attempts)
        assertEquals(3, result.iterations)
        assertEquals("type mismatch in Main.kt", result.lastDiagnosis?.rootCause)
        assertTrue(result.stoppedBecause.contains("Still failing after 3 attempts"))
        // The final attempt is never followed by a fix it cannot retest.
        assertNull(result.attempts.last().fixApplied)
    }

    @Test
    fun `progress is reported for every attempt so the UI can narrate the run`() = runBlocking {
        val seen = mutableListOf<VerificationAttempt>()
        val runner = VerificationLoopRunner(
            maxIterations = 2,
            diagnose = { errorDiagnosis() },
            onProgress = { seen += it },
        )

        runner.run(
            command = "gradle assembleDebug",
            runBuild = { failure(output = "e: Main.kt:1:1 unresolved") },
            applyFix = { true },
        )

        assertEquals(2, seen.size)
        assertEquals(listOf(1, 2), seen.map { it.iteration })
    }

    @Test
    fun `only a genuinely failed execution counts as repairable`() {
        assertTrue(VerificationLoopRunner.isRepairableFailure(failure()))
        assertFalse(VerificationLoopRunner.isRepairableFailure(success()))
        assertFalse(VerificationLoopRunner.isRepairableFailure(OperationResult.notConfigured("nope")))
    }

    private fun errorDiagnosis(
        rootCause: String = "Unresolved reference 'foo'",
    ) = ErrorDiagnosis(
        type = ErrorType.COMPILATION_ERROR,
        affectedFiles = listOf("app/Main.kt"),
        rootCause = rootCause,
        suggestedFix = "Import or declare foo",
    )
}
