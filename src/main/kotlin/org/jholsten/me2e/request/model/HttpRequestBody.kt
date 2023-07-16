package org.jholsten.me2e.request.model

import okio.Buffer
import okio.ByteString
import java.io.File

/**
 * Model representing the request body of an HTTP request.
 */
class HttpRequestBody {
    /**
     * The content of the body as a String.
     */
    val stringContent: String?

    /**
     * The content of the body as a file.
     * This value is ignored if the [stringContent] is also set.
     */
    val fileContent: File?

    /**
     * The content of the body as a byte string.
     * This value is ignored if the [stringContent] is also set.
     */
    val binaryContent: ByteString?

    /**
     * Content type of the request body.
     */
    val contentType: MediaType?

    constructor(content: String, contentType: MediaType) {
        this.stringContent = content
        this.fileContent = null
        this.binaryContent = null
        this.contentType = contentType
    }

    constructor(content: File, contentType: MediaType) {
        this.fileContent = content
        this.stringContent = null
        this.binaryContent = null
        this.contentType = contentType
    }

    constructor(content: ByteString, contentType: MediaType) {
        this.binaryContent = content
        this.stringContent = null
        this.fileContent = null
        this.contentType = contentType
    }

    internal constructor(buffer: Buffer, contentType: MediaType?) {
        if (contentType?.isStringType() == true) {
            this.stringContent = buffer.readUtf8()
            this.binaryContent = null
        } else {
            this.binaryContent = buffer.readByteString()
            this.stringContent = null
        }

        this.contentType = contentType
        this.fileContent = null
    }
}
