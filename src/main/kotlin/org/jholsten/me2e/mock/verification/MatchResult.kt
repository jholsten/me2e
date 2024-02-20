package org.jholsten.me2e.mock.verification

/**
 * Result of trying to match an expected request to an actual request.
 */
internal data class MatchResult(
    /**
     * Whether the expected request matches the actual request.
     */
    @JvmSynthetic
    val matches: Boolean,

    /**
     * Differences between the actual and the expected request.
     * Only non-empty if [matches] is false.
     */
    @JvmSynthetic
    val failures: List<String> = listOf(),
)
