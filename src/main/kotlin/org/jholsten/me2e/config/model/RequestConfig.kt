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
    val connectTimeout: Long = 10,

    /**
     * Read timeout in seconds.
     */
    @JsonProperty("read-timeout")
    val readTimeout: Long = 10,

    /**
     * Write timeout in seconds.
     */
    @JsonProperty("write-timeout")
    val writeTimeout: Long = 10,
)
