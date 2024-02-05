package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Configuration for all [org.jholsten.me2e.mock.MockServer] instances.
 */
data class MockServerConfig(
    /**
     * Path to the keystore containing the TLS keystore to use for the [org.jholsten.me2e.mock.MockServer] instances.
     * Can be either an absolute path to a file or a resource on the classpath.
     */
    @JsonProperty("keystore-path")
    val keystorePath: String? = null,

    /**
     * Password used to access the keystore. Required for TLS.
     */
    @JsonProperty("keystore-password")
    val keystorePassword: String? = null,

    /**
     * Password used to access individual keys in the keystore. Required for TLS.
     */
    @JsonProperty("key-manager-password")
    val keyManagerPassword: String? = null,

    /**
     * Type of the keystore. Required for TLS.
     */
    @JsonProperty("keystore-type")
    val keystoreType: String = "JKS",

    /**
     * Path to the truststore to use for the [org.jholsten.me2e.mock.MockServer] instances. Required for client authentication.
     * Can be either an absolute path to a file or a resource on the classpath.
     */
    @JsonProperty("truststore-path")
    val truststorePath: String? = null,

    /**
     * Password used to access the truststore. Required for client authentication.
     */
    @JsonProperty("truststore-password")
    val truststorePassword: String? = null,

    /**
     * Type of the truststore. Required for client authentication.
     */
    @JsonProperty("truststore-type")
    val truststoreType: String = "JKS",
)
