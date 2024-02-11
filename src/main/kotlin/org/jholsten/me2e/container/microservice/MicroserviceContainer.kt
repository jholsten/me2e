package org.jholsten.me2e.container.microservice

import com.fasterxml.jackson.annotation.JacksonInject
import org.jholsten.me2e.config.model.DockerConfig
import com.github.dockerjava.api.model.Container as DockerContainer
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.docker.DockerCompose
import org.jholsten.me2e.container.microservice.authentication.Authenticator
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.request.client.HttpClient
import org.jholsten.me2e.request.client.OkHttpClient
import org.jholsten.me2e.request.model.*
import org.jholsten.me2e.utils.logger
import org.jholsten.me2e.Me2eTestConfig
import org.jholsten.me2e.config.parser.deserializer.TestConfigDeserializer
import org.testcontainers.containers.ContainerState
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Representation of a Docker container which represents a microservice.
 * In this context, a Microservice is expected to offer a REST API. Therefore, this class offers commands
 * for executing HTTP requests towards the container.
 */
class MicroserviceContainer internal constructor(
    /**
     * Unique name of this container.
     */
    name: String,

    /**
     * Image to start the container from.
     * Corresponds to the value given for the `image` keyword in Docker-Compose.
     */
    image: String?,

    /**
     * Environment variables for this container.
     * Corresponds to the values given in the `environment` section of the Docker-Compose.
     */
    environment: Map<String, String>? = null,

    /**
     * URL where this container's REST API is accessible from localhost. Corresponds to the value of the label
     * `org.jholsten.me2e.url` in the Docker-Compose. If not set, the URL is composed of the Docker host and
     * the first publicly accessible port of this container.
     */
    private val predefinedUrl: String? = null,

    /**
     * Ports that should be exposed to localhost.
     * Corresponds to the `ports` section of the Docker-Compose.
     */
    ports: ContainerPortList = ContainerPortList(),

    /**
     * Configuration for executing HTTP requests.
     * Corresponds to the `requests` section of the ME2E configuration file.
     */
    @JacksonInject(TestConfigDeserializer.INJECTABLE_REQUEST_CONFIG_FIELD_NAME)
    private val requestConfig: RequestConfig,

    /**
     * Whether there is a healthcheck defined for this container in the Docker-Compose file.
     */
    hasHealthcheck: Boolean = false,

    /**
     * Pull policy for this Docker container.
     * If not overwritten in the label `org.jholsten.me2e.pull-policy` for this container, the global
     * pull policy [org.jholsten.me2e.config.model.DockerConfig.pullPolicy] is used.
     * @see org.jholsten.me2e.config.model.DockerConfig.pullPolicy
     */
    pullPolicy: DockerConfig.PullPolicy = DockerConfig.PullPolicy.MISSING,
) : Container(
    name = name,
    image = image,
    type = ContainerType.MICROSERVICE,
    environment = environment,
    ports = ports,
    hasHealthcheck = hasHealthcheck,
    pullPolicy = pullPolicy,
) {
    private val logger = logger<MicroserviceContainer>()

    /**
     * URL where microservice is accessible from localhost.
     * This value may either be set as the value of the label `org.jholsten.me2e.url` in the Docker-Compose file
     * (which will be deserialized to this instance's [predefinedUrl]), or it is automatically set by retrieving
     * the first exposed port.
     */
    var url: String? = null

    /**
     * HTTP client for executing requests towards this microservice.
     * Is initialized when the container was started in order to derive the base URL by the publicly accessible port.
     */
    var httpClient: HttpClient? = null

    /**
     * Performs authentication for requests to this microservice using the given [authenticator].
     * The [Authenticator.getRequestInterceptor] is executed for every subsequent request in the current test so that all
     * of the following requests are authenticated. Note that - unless deactivated in [Me2eTestConfig.StateReset.resetRequestInterceptors] -
     * all request interceptors are reset after each test so that the authentication only applies to the current test.
     * @param authenticator Authenticator to use for authenticating the HTTP requests.
     */
    fun authenticate(authenticator: Authenticator) {
        assertThatHttpClientIsInitialized()
        authenticator.initialize(this)
        httpClient!!.addRequestInterceptor(authenticator.getRequestInterceptor())
    }

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

    @JvmSynthetic
    override fun initializeOnContainerStarted(dockerContainer: DockerContainer, state: ContainerState, environment: DockerCompose) {
        super.initializeOnContainerStarted(dockerContainer, state, environment)
        val baseUrl = setBaseUrl()
        httpClient = OkHttpClient.Builder()
            .withBaseUrl(baseUrl)
            .withConnectTimeout(requestConfig.connectTimeout, TimeUnit.SECONDS)
            .withReadTimeout(requestConfig.readTimeout, TimeUnit.SECONDS)
            .withWriteTimeout(requestConfig.writeTimeout, TimeUnit.SECONDS)
            .build()
    }

    @JvmSynthetic
    override fun onRestart(timestamp: Instant) {
        super.onRestart(timestamp)
        val baseUrl = setBaseUrl()
        httpClient?.setBaseUrl(baseUrl)
    }

    /**
     * Resets all request interceptors of this microservice's HTTP client.
     */
    @JvmSynthetic
    internal fun resetRequestInterceptors() {
        if (httpClient != null) {
            httpClient!!.setRequestInterceptors(listOf())
        }
    }

    /**
     * Sets base URL of this microservice. If the URL is predefined in the label `org.jholsten.me2e.url`,
     * this value is used. Otherwise, the URL is composed of the Docker host and the first publicly accessible port
     * of this container.
     */
    private fun setBaseUrl(): Url {
        this.url = if (predefinedUrl == null) {
            val exposedPort = ports.findFirstExposed()
            requireNotNull(exposedPort) { "Microservice $name needs at least one exposed port or a URL defined." }
            "http://${dockerContainer!!.state.host}:${exposedPort.external}"
        } else {
            predefinedUrl
        }
        logger.info("Set base URL for microservice $name to $url.")
        return Url(this.url!!)
    }

    private fun assertThatHttpClientIsInitialized() {
        checkNotNull(httpClient) { "HTTP client was not properly initialized. Are you sure the container $name is started?" }
    }
}
