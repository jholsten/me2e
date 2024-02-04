package org.jholsten.me2e.container.model

/**
 * Result of the execution of a command inside a Docker container.
 */
data class ExecutionResult(
    /**
     * Exit code of the command.
     */
    val exitCode: Int,

    /**
     * Output of the command on STDOUT.
     */
    val stdout: String,

    /**
     * Output of the command on STDERR.
     */
    val stderr: String,
)
