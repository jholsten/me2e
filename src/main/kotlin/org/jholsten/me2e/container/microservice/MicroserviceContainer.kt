package org.jholsten.me2e.container.microservice

import com.fasterxml.jackson.annotation.JacksonInject
import org.jholsten.me2e.config.model.DockerConfig
import com.github.dockerjava.api.model.Container as DockerContainer
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.request.client.OkHttpClient
import org.jholsten.me2e.request.model.*
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

    override fun initialize(dockerConfig: DockerConfig, dockerContainer: DockerContainer, dockerContainerState: ContainerState) {
        super.initialize(dockerConfig, dockerContainer, dockerContainerState)
        if (url == null) {
            val exposedPort = ports.findFirstExposed()
            requireNotNull(exposedPort) { "Microservice $name needs at least one exposed port or a URL defined." }
            url = "http://${dockerConfig.dockerHost}:${exposedPort.external}"
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
     * Executes an HTTP GET request to this microservice. Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the microservice's base URL.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    @JvmOverloads
    fun get(relativeUrl: RelativeUrl, headers: HttpHeaders = HttpHeaders.empty()): HttpResponse {
        assertThatHttpClientIsInitialized()
        return httpClient!!.get(relativeUrl, headers)
    }

    /**
     * Executes an HTTP POST request to this microservice. Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the microservice's base URL.
     * @param body Request body to set in the request.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    @JvmOverloads
    fun post(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders = HttpHeaders.empty()): HttpResponse {
        assertThatHttpClientIsInitialized()
        return httpClient!!.post(relativeUrl, body, headers)
    }

    /**
     * Executes an HTTP PUT request to this microservice. Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the microservice's base URL.
     * @param body Request body to set in the request.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    @JvmOverloads
    fun put(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders = HttpHeaders.empty()): HttpResponse {
        assertThatHttpClientIsInitialized()
        return httpClient!!.put(relativeUrl, body, headers)
    }

    /**
     * Executes an HTTP PATCH request to this microservice. Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the microservice's base URL.
     * @param body Request body to set in the request.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    @JvmOverloads
    fun patch(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders = HttpHeaders.empty()): HttpResponse {
        assertThatHttpClientIsInitialized()
        return httpClient!!.patch(relativeUrl, body, headers)
    }

    /**
     * Executes an HTTP DELETE request to this microservice. Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the microservice's base URL.
     * @param body Request body to set in the request.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    @JvmOverloads
    fun delete(relativeUrl: RelativeUrl, body: HttpRequestBody? = null, headers: HttpHeaders = HttpHeaders.empty()): HttpResponse {
        assertThatHttpClientIsInitialized()
        return httpClient!!.delete(relativeUrl, body, headers)
    }

    private fun assertThatHttpClientIsInitialized() {
        checkNotNull(httpClient) { "HTTP client was not properly initialized." }
    }

    /**
     * TODO: Desired interface:
     * @Microservice("backend")
     * private val backend: MicroserviceContainer
     */
}
