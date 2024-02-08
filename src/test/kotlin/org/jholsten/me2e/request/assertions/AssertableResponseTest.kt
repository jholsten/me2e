package org.jholsten.me2e.request.assertions

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.every
import io.mockk.mockk
import org.jholsten.me2e.assertions.assertThat
import org.jholsten.me2e.assertions.containsString
import org.jholsten.me2e.assertions.containsNode
import org.jholsten.me2e.assertions.equalTo
import org.jholsten.me2e.request.model.*
import kotlin.test.*

internal class AssertableResponseTest {

    private val json = JsonNodeFactory.instance.objectNode()
        .put("title", "Developing, Verifying, and Maintaining High-Quality Automated Test Scripts")
        .set<ObjectNode>(
            "authors", JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("firstname", "Vahid").put("lastname", "Garousi"))
                .add(JsonNodeFactory.instance.objectNode().put("firstname", "Michael").put("lastname", "Felderer"))
        )
        .put("year", 2016)
        .set<ObjectNode>(
            "keywords", JsonNodeFactory.instance.arrayNode()
                .add("Software Testing")
                .add("Test Automation")
        )
        .set<ObjectNode>(
            "journal", JsonNodeFactory.instance.objectNode()
                .put("title", "IEEE Software")
                .put("volume", 33)
                .put("issue", 3)
        )

    private val responseBody = mockk<HttpResponseBody>()
    private val request = mockk<HttpRequest>()
    private val headers = mockk<HttpHeaders>()

    private val response = HttpResponse(
        request = request,
        protocol = "http/1.1",
        message = "Some Message",
        statusCode = 200,
        headers = headers,
        body = responseBody,
    )

    @BeforeTest
    fun beforeTest() {
        every { responseBody.asJson() } returns json
    }

    @Test
    fun `Asserting json body should not throw if assertion is satisfied`() {
        assertThat(response).jsonBody(containsNode("title").withValue(containsString("Automated Test Scripts")))
        assertThat(response).jsonBody(containsNode("authors[0].lastname").withValue(equalTo("Garousi")))
        assertThat(response).jsonBody(containsNode("authors[0]").withValue(equalTo("{\"firstname\":\"Vahid\",\"lastname\":\"Garousi\"}")))
        assertThat(response).jsonBody(containsNode("year").withValue(equalTo("2016")))
        assertThat(response).jsonBody(containsNode("keywords[1]").withValue(equalTo("Test Automation")))
        assertThat(response).jsonBody(containsNode("journal.title").withValue(equalTo("IEEE Software")))
    }
}
