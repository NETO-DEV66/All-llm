package com.netodev66.alllm.session

import com.netodev66.alllm.core.result.OperationResult
import com.netodev66.alllm.core.result.OperationStatus

/** One pass of the verify → diagnose → fix cycle. */
data class VerificationAttempt(
    val iteration: Int,
    val command: String,
    val success: Boolean,
    val output: String,
    val diagnosis: ErrorDiagnosis? = null,
    /** Null until a fix was attempted; false means the fix could not be applied. */
    val fixApplied: Boolean? = null,
)

/** Honest outcome of a self-verifying run. */
data class VerificationRunResult(
    val success: Boolean,
    val command: String,
    val attempts: List<VerificationAttempt>,
    val stoppedBecause: String,
) {
    val iterations: Int get() = attempts.size
    val lastDiagnosis: ErrorDiagnosis? get() = attempts.lastOrNull()?.diagnosis
}

/**
 * Bounded verify → diagnose → fix → retest loop.
 *
 * The point of this class is that a green result can only come from a command
 * that really exited zero. Three rules keep it honest:
 *
 * 1. A run that never executed (no toolchain installed, nothing configured)
 *    stops the loop immediately. Retrying cannot install a compiler, so burning
 *    agent turns on it would only produce noise.
 * 2. A failure with no fix applied stops the loop. Retesting the identical tree
 *    would reproduce the identical error.
 * 3. Success requires exit code 0 from the real command. A repair that "looks
 *    right" but still fails to build is reported as a failure.
 *
 * Everything is injected, so tests drive the whole loop with fakes and the app
 * drives it with the real Gradle-in-PRoot runner and the real agent.
 */
class VerificationLoopRunner(
    private val maxIterations: Int = DEFAULT_MAX_ITERATIONS,
    private val diagnose: (String) -> ErrorDiagnosis = { output -> SelfHealingLoop().diagnose(output) },
    private val onProgress: (VerificationAttempt) -> Unit = {},
) {

    init {
        require(maxIterations >= 1) { "maxIterations must be at least 1" }
    }

    suspend fun run(
        command: String,
        runBuild: suspend (iteration: Int) -> OperationResult,
        applyFix: suspend (ErrorDiagnosis) -> Boolean,
    ): VerificationRunResult {
        val attempts = mutableListOf<VerificationAttempt>()
        var iteration = 0

        while (iteration < maxIterations) {
            iteration++
            val outcome = runBuild(iteration)
            val output = outcome.logs.joinToString("\n")

            if (outcome.isSuccess) {
                val attempt = VerificationAttempt(iteration, command, true, output)
                attempts += attempt
                onProgress(attempt)
                return VerificationRunResult(
                    success = true,
                    command = command,
                    attempts = attempts,
                    stoppedBecause = "Verified: exit 0 on attempt $iteration of $maxIterations",
                )
            }

            if (outcome.isNonExecuting) {
                val attempt = VerificationAttempt(iteration, command, false, output)
                attempts += attempt
                onProgress(attempt)
                return VerificationRunResult(
                    success = false,
                    command = command,
                    attempts = attempts,
                    stoppedBecause = "Could not run the build, so there is nothing to repair: " +
                        (outcome.error ?: outcome.status.name),
                )
            }

            val diagnosis = diagnose(buildFailureText(command, outcome))
            val fixApplied = if (iteration < maxIterations) applyFix(diagnosis) else null
            val attempt = VerificationAttempt(
                iteration = iteration,
                command = command,
                success = false,
                output = output,
                diagnosis = diagnosis,
                fixApplied = fixApplied,
            )
            attempts += attempt
            onProgress(attempt)

            if (fixApplied == false) {
                return VerificationRunResult(
                    success = false,
                    command = command,
                    attempts = attempts,
                    stoppedBecause = "No fix could be applied for: ${diagnosis.rootCause}",
                )
            }
        }

        return VerificationRunResult(
            success = false,
            command = command,
            attempts = attempts,
            stoppedBecause = "Still failing after $maxIterations attempts; last error: " +
                (attempts.lastOrNull()?.diagnosis?.rootCause ?: "unknown"),
        )
    }

    /**
     * The command and the real output, so the diagnosis sees file:line pairs. A
     * bare exit code is not diagnosable, and [OperationResult.error] is where the
     * Gradle runner puts its tail when it fails.
     */
    private fun buildFailureText(command: String, outcome: OperationResult): String = buildString {
        appendLine("Command: $command")
        appendLine("Exit code: ${outcome.exitCode ?: "unknown"}")
        outcome.error?.let { appendLine(it) }
        appendLine(outcome.logs.joinToString("\n"))
    }

    companion object {
        const val DEFAULT_MAX_ITERATIONS = 3

        /** True only for a result that genuinely ran and genuinely failed. */
        fun isRepairableFailure(result: OperationResult): Boolean =
            result.status == OperationStatus.FAILED
    }
}
