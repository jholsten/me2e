package org.jholsten.me2e.request.model

import com.fasterxml.jackson.databind.JsonNode
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.utils.logger
import java.io.File
import java.nio.charset.Charset
import java.util.*

/**
 * Model representing the request body of an HTTP request.
 * Stores binary content of the body along with its content type.
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

    /**
     * Instantiates a new request body with the given String content and the given content type.
     * The contents of the body are encoded to a byte array using the given charset.
     * @param content String content of the request body.
     * @param contentType Content type of the request body.
     * @param charset Charset to use for encoding the string content to a byte array.
     */
    @JvmOverloads
    constructor(content: String, contentType: MediaType?, charset: Charset = Charsets.UTF_8) {
        this.content = content.toByteArray(charset)
        this.contentType = contentType
    }

    /**
     * Instantiates a new request body with the given File content and the given content type.
     * The content of the body is encoded to a byte array.
     * @param content File content of the request body.
     * @param contentType Content type of the request body.
     */
    constructor(content: File, contentType: MediaType?) {
        this.content = content.readBytes()
        this.contentType = contentType
    }

    /**
     * Instantiates a new request body with the given byte array content and the given content type.
     * @param content Binary content of the request body.
     * @param contentType Content type of the request body.
     */
    constructor(content: ByteArray, contentType: MediaType?) {
        this.content = content
        this.contentType = contentType
    }

    class Builder {
        private var contentType: MediaType? = null
        private var stringContent: String? = null
        private var charset: Charset = Charsets.UTF_8
        private var fileContent: File? = null
        private var binaryContent: ByteArray? = null

        private val logger = logger(this)

        /**
         * Sets the given String content for the request body.
         * The contents of the body are encoded to a byte array using [Charsets.UTF_8] by default.
         * To change the charset to use for encoding, call [withCharset].
         * @param content String content of the request body to set.
         * @return This builder instance, to use for chaining.
         */
        fun withContent(content: String) = apply {
            this.stringContent = content
        }

        /**
         * Sets the given file content for the request body.
         * The content of the body is encoded to a byte array.
         * @param content File content of the request body to set.
         * @return This builder instance, to use for chaining.
         */
        fun withContent(content: File) = apply {
            this.fileContent = content
        }

        /**
         * Sets the given binary content for the request body.
         * @param content Binary content of the request body to set.
         * @return This builder instance, to use for chaining.
         */
        fun withContent(content: ByteArray) = apply {
            this.binaryContent = content
        }

        /**
         * Serializes the given object to a JSON string and sets this as the string content of the request body.
         * @param content Object to serialize.
         * @return This builder instance, to use for chaining.
         * @throws java.io.IOException if content could not be serialized.
         */
        fun <T> withJsonContent(content: T) = apply {
            this.stringContent = DeserializerFactory.getObjectMapper().writeValueAsString(content)
            this.contentType = MediaType.JSON_UTF8
        }

        /**
         * Sets the given content type for the request body.
         * @param contentType Content type of the request body to set.
         * @return This builder instance, to use for chaining.
         */
        fun withContentType(contentType: MediaType?) = apply {
            this.contentType = contentType
        }

        /**
         * Sets the given charset to use for encoding the string content to binary content.
         * By default, [Charsets.UTF_8] is used. Only applicable if string content was set.
         * @param charset Charset to use for encoding the string content.
         * @return This builder instance, to use for chaining.
         */
        fun withCharset(charset: Charset) = apply {
            this.charset = charset
        }

        /**
         * Builds an instance of the [HttpRequestBody] using the properties set in this builder.
         * Only one of the contents is read, all others are ignored.
         */
        fun build(): HttpRequestBody {
            val setContents = listOfNotNull(stringContent, fileContent, binaryContent)
            require(setContents.isNotEmpty()) { "Content of the request body needs to be set." }
            if (setContents.size > 1) {
                logger.warn("Request body contents of multiple types are set. Only one of the contents is read.")
            }
            return when (val content = setContents.first()) {
                is String -> HttpRequestBody(
                    content = content,
                    contentType = contentType,
                    charset = charset,
                )

                is File -> HttpRequestBody(
                    content = content,
                    contentType = contentType,
                )

                else -> HttpRequestBody(
                    content = content as ByteArray,
                    contentType = contentType,
                )
            }
        }
    }

    /**
     * Returns request body content as string or `null`, if no content is present.
     * @param charset Charset to use for decoding the binary content to string.
     */
    @JvmOverloads
    fun asString(charset: Charset = Charsets.UTF_8): String? {
        return content?.let { String(it, charset) }
    }

    /**
     * Returns binary content of the request body or `null`, if no content is present.
     */
    fun asBinary(): ByteArray? {
        return content
    }

    /**
     * Returns binary content encoded as Base 64 or `null`, if no content is present.
     */
    fun asBase64(): String? {
        return content?.let { Base64.getEncoder().encodeToString(it) }
    }

    /**
     * Returns binary content parsed as JSON node or `null`, if no content is present.
     * @throws ParseException if content could not be parsed to JSON.
     */
    fun asJson(): JsonNode? {
        try {
            return content?.let { DeserializerFactory.getObjectMapper().readTree(it) }
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
    }
}
