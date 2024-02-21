package org.jholsten.samples

import org.jholsten.me2e.Me2eTest
import org.jholsten.me2e.Me2eTestConfig
import org.jholsten.me2e.assertions.*
import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.container.injection.InjectService
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.verification.ExpectedRequest
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.RelativeUrl
import kotlin.test.Test

@Me2eTestConfig(
    config = "me2e-config-test.yaml",
    format = ConfigFormat.YAML,
)
class Me2eTestSample : Me2eTest() {

    @InjectService
    private lateinit var backendApi: MicroserviceContainer

    @InjectService
    private lateinit var paymentService: MockServer

    @Test
    fun `Invoking Mock Server in container should succeed`() {
        val relativeUrl = RelativeUrl.Builder()
            .withPath("/search")
            .withQueryParameter("id", "xyz")
            .build()

        val response = backendApi.get(relativeUrl)

        assertThat(response)
            .statusCode(equalTo(200))
            .message(equalTo("OK"))
            .jsonBody(equalToContentsFromFile("responses/expected_response.json").asJson())

        assertThat(paymentService).receivedRequest(
            ExpectedRequest()
                .withPath(equalTo("/search"))
                .withMethod(equalTo(HttpMethod.POST))
                .withQueryParameters(containsKey("id").withValue(equalTo("xyz")))
                .withJsonBody(containsNode("$.auth").withValue(equalTo("secret")))
                .andNoOther()
        )
    }
}
