package org.jholsten.me2e.mock.exception

import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher

/**
 * Exception that occurs when a mock server verification was not successful.
 */
class VerificationException(message: String) : AssertionError(message) {
    companion object {
        /**
         * Returns [VerificationException] for the situation that a mock server did not receive the expected number of
         * requests to match the pattern represented by the [matcher].
         */
        internal fun forTimesNotMatching(
            mockServerName: String,
            times: Int,
            matcher: MockServerStubRequestMatcher,
            matchResults: List<ServeEvent>,
            receivedRequests: List<ServeEvent>,
        ): VerificationException {
            val message = renderDifference(
                "Expected $mockServerName to receive exactly $times number of requests to match the following patterns, but received ${matchResults.size}.",
                matcher = matcher,
                matchResults = matchResults,
                receivedRequests = receivedRequests,
            )
            return VerificationException(message)
        }

        /**
         * Returns [VerificationException] for the situation that a mock server did not receive at least one request
         * matching the pattern represented by the [matcher].
         */
        internal fun forNotReceivedAtLeastOnce(
            mockServerName: String,
            matcher: MockServerStubRequestMatcher,
            matchResults: List<ServeEvent>,
            receivedRequests: List<ServeEvent>,
        ): VerificationException {
            val message = renderDifference(
                "Expected $mockServerName to receive at least one request matching the following patterns.",
                matcher = matcher,
                matchResults = matchResults,
                receivedRequests = receivedRequests,
            )
            return VerificationException(message)
        }

        /**
         * Returns [VerificationException] for the situation that a mock server received requests other than
         * the ones matching the pattern represented by the [matcher].
         */
        internal fun forOtherRequests(
            mockServerName: String,
            matcher: MockServerStubRequestMatcher,
            matchResults: List<ServeEvent>,
            receivedRequests: List<ServeEvent>,
        ): VerificationException {
            val otherRequests = receivedRequests.size - matchResults.size
            val message = renderDifference(
                "Expected $mockServerName to only receive requests matching the following pattern, but received $otherRequests other requests.",
                matcher = matcher,
                matchResults = matchResults,
                receivedRequests = receivedRequests,
            )
            return VerificationException(message)
        }

        private fun renderDifference(
            message: String,
            matcher: MockServerStubRequestMatcher,
            matchResults: List<ServeEvent>,
            receivedRequests: List<ServeEvent>,
        ): String {
            val stringBuilder = StringBuilder()
            stringBuilder.appendLine(message)
            stringBuilder.appendLine("-".repeat(80))
            stringBuilder.appendLine("--> Expected request:")
            stringBuilder.appendLine(matcher)
            stringBuilder.appendLine()
            if (matchResults.isNotEmpty()) {
                stringBuilder.appendLine("--> Matched received requests:")
                for ((index, event) in matchResults.withIndex()) {
                    renderReceivedRequest(stringBuilder, index, event)
                }
            }
            if (receivedRequests.isNotEmpty()) {
                stringBuilder.appendLine("--> Received requests:")
                for ((index, event) in receivedRequests.withIndex()) {
                    renderReceivedRequest(stringBuilder, index, event)
                }
            }

            return stringBuilder.toString()
        }

        private fun renderReceivedRequest(stringBuilder: StringBuilder, index: Int, event: ServeEvent) {
            stringBuilder.appendLine()
            stringBuilder.appendLine("-- Request ${index + 1} --")
            stringBuilder.appendLine(event.request)
        }
    }
}
