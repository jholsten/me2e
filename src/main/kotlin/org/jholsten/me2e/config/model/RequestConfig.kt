package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Configuration for all HTTP requests to [org.jholsten.me2e.container.microservice.MicroserviceContainer] instances.
 */
class RequestConfig(
    /**
     * Connect timeout in seconds.
     */
    @JsonProperty("connect-timeout")
    val connectTimeout: Int,

    /**
     * Read timeout in seconds.
     */
    @JsonProperty("read-timeout")
    val readTimeout: Int,

    /**
     * Write timeout in seconds.
     */
    @JsonProperty("write-timeout")
    val writeTimeout: Int,
)
