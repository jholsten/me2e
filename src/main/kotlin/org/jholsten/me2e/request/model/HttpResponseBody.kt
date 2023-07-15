package org.jholsten.me2e.request.model

import okio.ByteString

/**
 * Model representing the response body of an HTTP response.
 */
class HttpResponseBody internal constructor(
    /**
     * Content type of the response body.
     */
    val contentType: MediaType?,

    /**
     * Length of the content in number of bytes.
     */
    val contentLength: Long,

    /**
     * Content of the response body as string value.
     */
    val stringContent: String?,

    /**
     * Content of the response body as a byte string.
     */
    val binaryContent: ByteString?,
)
