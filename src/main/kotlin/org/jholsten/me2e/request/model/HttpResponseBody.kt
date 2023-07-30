package org.jholsten.me2e.request.model

import java.util.*

/**
 * Model representing the response body of an HTTP response.
 */
class HttpResponseBody internal constructor(
    /**
     * Content type of the response body.
     */
    val contentType: MediaType?,

    /**
     * Content of the response body.
     */
    private val content: ByteArray?,
) {
    /**
     * Returns response body content as string or null, if no content is present.
     */
    fun asString(): String? {
        return content?.decodeToString()
    }

    /**
     * Returns binary content of the response body or null, if no content is present.
     */
    fun asBinary(): ByteArray? {
        return content
    }

    /**
     * Returns binary content encoded as Base 64 or null, if no content is present.
     */
    fun asBase64(): String? {
        return content?.let { Base64.getEncoder().encodeToString(it) }
    }
}
