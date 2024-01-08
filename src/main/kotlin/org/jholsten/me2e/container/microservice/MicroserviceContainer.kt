package org.jholsten.me2e.container.microservice

import com.fasterxml.jackson.annotation.JacksonInject
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.request.client.OkHttpClient
import org.jholsten.me2e.request.model.HttpHeaders
import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.model.RelativeUrl
import org.jholsten.me2e.request.model.Url
import org.testcontainers.containers.ContainerState
import java.util.concurrent.TimeUnit

/**
 * Model representing one Microservice container.
 * In this context, a Microservice is expected to offer a REST API.
 */
class MicroserviceContainer(
    name: String,
    image: String,
    environment: Map<String, String>? = null,
    url: String? = null,
    ports: ContainerPortList = ContainerPortList(),
    @JacksonInject("requestConfig")
    private val requestConfig: RequestConfig,
    hasHealthcheck: Boolean = false,
) : Container(
    name = name,
    image = image,
    type = ContainerType.MICROSERVICE,
    environment = environment,
    ports = ports,
    hasHealthcheck = hasHealthcheck,
) {
    /**
     * URL where microservice is accessible from localhost.
     * This value may either be set in the Docker-Compose file, or it is automatically set by retrieving the first exposed port.
     */
    private var url: String? = url

    /**
     * HTTP client for executing requests towards this microservice.
     * Is initialized when the container was started in order to derive the base URL by the publicly accessible port.
     */
    private var httpClient: OkHttpClient? = null

    override fun initialize(dockerContainer: com.github.dockerjava.api.model.Container, dockerContainerState: ContainerState) {
        super.initialize(dockerContainer, dockerContainerState)
        if (url == null) {
            val exposedPort = ports.findFirstExposed()
            requireNotNull(exposedPort) { "Microservices need at least one exposed port." }
            url = "http://localhost:${exposedPort.external}"
        }

        this.httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url(url!!))
            .withConnectTimeout(requestConfig.connectTimeout, TimeUnit.SECONDS)
            .withReadTimeout(requestConfig.readTimeout, TimeUnit.SECONDS)
            .withWriteTimeout(requestConfig.writeTimeout, TimeUnit.SECONDS)
            .build()
    }

    // TODO
    fun authenticate() {
        //httpClient.setRequestInterceptors(Auth())
    }

    /*
    TODO: Authentication:

    backend.authenticate(UsernamePasswordAuthentication("user", "password"))

    This will lead to all requests having the token as header
     */

    /**
     * Sends a GET request over HTTP to this microservice.
     * TODO
     */
    fun get(relativeUrl: RelativeUrl, headers: HttpHeaders): HttpResponse {
        checkNotNull(httpClient) { "HTTP client was not properly initialized." }
        return httpClient!!.get(relativeUrl, headers)
    }

    fun get(relativeUrl: RelativeUrl): HttpResponse {
        checkNotNull(httpClient) { "HTTP client was not properly initialized." }
        return httpClient!!.get(relativeUrl, HttpHeaders.empty())
    }

    /**
     * TODO: Desired interface:
     * @Microservice("backend")
     * private val backend: MicroserviceContainer
     *
     * backend.get(...)
     *  .expect()
     *      .statusCode(200)
     *      .responseBody(...)
     */
}
