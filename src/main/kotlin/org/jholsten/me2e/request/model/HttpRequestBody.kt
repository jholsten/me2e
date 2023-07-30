package org.jholsten.me2e.request.model

import okio.Buffer
import java.io.File
import java.util.*

/**
 * Model representing the request body of an HTTP request.
 */
class HttpRequestBody {
    /**
     * Content type of the request body.
     */
    val contentType: MediaType?

    /**
     * Content of the request body.
     */
    private val content: ByteArray?

    constructor(content: String, contentType: MediaType?) {
        this.content = content.toByteArray()
        this.contentType = contentType
    }

    constructor(content: File, contentType: MediaType?) {
        this.content = content.readBytes()
        this.contentType = contentType
    }

    constructor(content: ByteArray, contentType: MediaType?) {
        this.content = content
        this.contentType = contentType
    }

    internal constructor(buffer: Buffer, contentType: MediaType?) {
        this.content = buffer.readByteArray()
        this.contentType = contentType
    }

    /**
     * Returns request body content as string or null, if no content is present.
     */
    fun asString(): String? {
        return content?.decodeToString()
    }

    /**
     * Returns binary content of the request body or null, if no content is present.
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
