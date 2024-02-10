package org.jholsten.me2e.mock.stubbing

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.LocalNotifier
import com.github.tomakehurst.wiremock.core.Admin
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import com.github.tomakehurst.wiremock.verification.NearMiss
import com.github.tomakehurst.wiremock.verification.notmatched.NotMatchedRenderer
import com.google.common.net.HttpHeaders
import de.m3y.kformat.Table
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import de.m3y.kformat.Table.Hints
import de.m3y.kformat.table
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMapper.Companion.METADATA_MATCHER_KEY
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import kotlin.math.max

/**
 * Utility class that outputs a body containing information about an unmatched request.
 * The [render] method of the class is invoked when the Mock Server receives a request for which no matching stub is found.
 * Within the method, first, the closest stub is identified and a comparison of this stub with the actual request is rendered.
 * This body is both printed in the logs and returned as a response body to the request.
 * @see [com.github.tomakehurst.wiremock.verification.notmatched.PlainTextStubNotMatchedRenderer]
 */
internal class MockServerStubNotMatchedRenderer : NotMatchedRenderer() {
    override fun render(admin: Admin, request: Request): ResponseDefinition {
        val loggedRequest = LoggedRequest.createFrom(request.originalRequest.or(request))
        val nearMisses = admin.findTopNearMissesFor(loggedRequest).nearMisses

        val body = when {
            nearMisses.isEmpty() -> "No response could be served as there are no stubs registered for the Mock Server."
            else -> renderDifference(nearMisses[0])
        }

        LocalNotifier.notifier().error(body)

        return ResponseDefinitionBuilder.responseDefinition()
            .withStatus(404)
            .withHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .withBody(body)
            .build()
    }

    /**
     * Builds string representation of the given nearest miss, which is the stub that is closest to the request received.
     * The string representation describes the differences between the nearest stub and the actual request.
     * @param nearestMiss Nearest miss as the stub that is closest to the request received.
     */
    private fun renderDifference(nearestMiss: NearMiss): String {
        val requestMatcher = nearestMiss.stubMapping.metadata[METADATA_MATCHER_KEY] as MockServerStubRequestMatcher
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("Request was not matched.")

        table {
            header("Attribute", "Closest Stub", "Actual Request")

            row("Hostname", requestMatcher.hostname, nearestMiss.request.host)
            row("Method", requestMatcher.method ?: "ANY", nearestMiss.request.method)
            multilineRow(
                "Path",
                stubEntries = requestMatcher.path?.toStringLines(),
                requestEntries = listOf(nearestMiss.request.url),
            )
            multilineRow(
                "Headers",
                stubEntries = requestMatcher.headers?.toStringLines(),
                requestEntries = nearestMiss.request.headers.all().map { it.toString() },
            )
            multilineRow(
                "Query-Parameters",
                stubEntries = requestMatcher.queryParameters?.toStringLines(),
                requestEntries = nearestMiss.request.queryParams.map { it.toString() },
            )

            hints {
                postfix("Attribute", ":")
                borderStyle = Table.BorderStyle.SINGLE_LINE
                defaultAlignment = Hints.Alignment.LEFT
            }
        }.render(stringBuilder)

        if (requestMatcher.bodyPatterns != null || (nearestMiss.request.body != null && nearestMiss.request.body.isNotEmpty())) {
            renderBody(stringBuilder, requestMatcher, nearestMiss.request)
        }

        return stringBuilder.toString()
    }

    /**
     * Appends string representation of the request body of the actual request to the given string builder.
     * @param stringBuilder String builder to append the string representation to.
     * @param requestMatcher Request matcher of the nearest stub.
     * @param request Actual request that the Mock Server received.
     */
    private fun renderBody(stringBuilder: StringBuilder, requestMatcher: MockServerStubRequestMatcher, request: LoggedRequest) {
        stringBuilder.appendLine()
        stringBuilder.appendLine("--> Body Patterns of the closest stub:")
        stringBuilder.appendLine(requestMatcher.bodyPatterns ?: "ANY")
        stringBuilder.appendLine()
        stringBuilder.appendLine("-".repeat(80))
        stringBuilder.appendLine()
        stringBuilder.appendLine("--> Actual Body of the request:")
        stringBuilder.appendLine(request.bodyAsString)
    }

    /**
     * Appends a row for which some cells may span over multiple lines to the given table.
     * @param header Header of the row, i.e. the name to display in the first column.
     * @param stubEntries Lines containing the content of the stubbed request.
     * @param requestEntries Lines containing the content of the actual request.
     */
    private fun Table.multilineRow(header: String, stubEntries: List<String>?, requestEntries: List<String>?) {
        val stubLines = if (stubEntries.isNullOrEmpty()) listOf("ANY") else stubEntries
        val requestLines = if (requestEntries.isNullOrEmpty()) listOf("{}") else requestEntries
        for (i in 0 until max(stubLines.size, requestLines.size)) {
            val headerValue = when {
                i == 0 -> header
                else -> ""
            }
            val stubValue = when {
                i < stubLines.size -> stubLines[i]
                else -> ""
            }
            val requestValue = when {
                i < requestLines.size -> requestLines[i]
                else -> ""
            }
            row(headerValue, stubValue, requestValue)
        }
    }

    /**
     * Generates string representation of the given string matcher with each property in a separate line.
     * @return Lines representing the properties of this string matcher.
     */
    private fun StringMatcher.toStringLines(): List<String> {
        val lines = listOf(
            "  equals" to equals,
            "  matches" to matches,
            "  notMatches" to notMatches,
            "  contains" to contains,
            "  notContains" to notContains,
            "  ignoreCase" to ignoreCase,
        ).filter { it.second != null }.map { "${it.first}: ${it.second}" }
        return when {
            lines.isNotEmpty() -> listOf("{") + lines + listOf("}")
            else -> listOf("{}")
        }
    }

    /**
     * Generates string representation of the given map of key and string matcher with each key and
     * each property in a separate line. Adds indentation to string matchers for a key.
     * @return Lines representing this map.
     */
    private fun Map<String, StringMatcher>.toStringLines(): List<String> {
        return entries.map { it.key to it.value.toStringLines() }.flatMap { (key, lines) ->
            listOf("\"$key\": {", *lines.takeLast(lines.size - 1).toTypedArray())
        }
    }
}
