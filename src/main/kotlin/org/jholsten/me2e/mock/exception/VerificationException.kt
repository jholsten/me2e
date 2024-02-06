package org.jholsten.me2e.mock.exception

import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher

/**
 * Exception that occurs when a Mock Server verification was not successful.
 */
class VerificationException(message: String) : AssertionError(message) {
    companion object {
        /**
         * Returns [VerificationException] for the situation that a Mock Server did not receive the expected number of
         * requests to match the pattern represented by the [matcher].
         * @param mockServerName Name of the Mock Server for which the expected number of requests did not match.
         * @param times Expected number of requests the Mock Server should have received.
         * @param matcher Request pattern of the matched requests.
         * @param matchResults Requests that the Mock Server received which match the pattern represented by the [matcher].
         * @param receivedRequests All requests that the Mock Server received.
         */
        @JvmSynthetic
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
         * Returns [VerificationException] for the situation that a Mock Server did not receive at least one request
         * matching the pattern represented by the [matcher].
         * @param mockServerName Name of the Mock Server which did not receive at least one request which matches the
         * pattern represented by the [matcher].
         * @param matcher Request pattern of the requests to match.
         * @param receivedRequests All requests that the Mock Server received.
         */
        @JvmSynthetic
        internal fun forNotReceivedAtLeastOnce(
            mockServerName: String,
            matcher: MockServerStubRequestMatcher,
            receivedRequests: List<ServeEvent>,
        ): VerificationException {
            val message = renderDifference(
                "Expected $mockServerName to receive at least one request matching the following patterns.",
                matcher = matcher,
                matchResults = listOf(),
                receivedRequests = receivedRequests,
            )
            return VerificationException(message)
        }

        /**
         * Returns [VerificationException] for the situation that a Mock Server received requests other than
         * the ones matching the pattern represented by the [matcher].
         * @param mockServerName Name of the Mock Server for which the expected number of requests did not match.
         * @param matcher Request pattern of the matched requests.
         * @param matchResults Requests that the Mock Server received which match the pattern represented by the [matcher].
         * @param receivedRequests All requests that the Mock Server received.
         */
        @JvmSynthetic
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

        /**
         * Generates string representation of the difference between the expected and actual requests received by a Mock Server.
         * Contains the following parts:
         * - Message describing the difference
         * - Requests that the Mock Server received which match the pattern represented by the [matcher].
         * - All requests that the Mock Server received.
         * @param message Message describing the difference between the expected and actual requests.
         * @param matcher Request pattern of the matched requests.
         * @param matchResults Requests that the Mock Server received which match the pattern represented by the [matcher].
         * @param receivedRequests All requests that the Mock Server received.
         */
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

        /**
         * Appends string representation of the given request received by the Mock Server to the given string builder.
         * @param stringBuilder String builder to append the string representation to.
         * @param index Index of this request in the list of all requests.
         * @param event Event registered by the Mock Server which contains the received request.
         */
        private fun renderReceivedRequest(stringBuilder: StringBuilder, index: Int, event: ServeEvent) {
            stringBuilder.appendLine()
            stringBuilder.appendLine("-- Request ${index + 1} --")
            stringBuilder.appendLine(event.request)
        }
    }
}
