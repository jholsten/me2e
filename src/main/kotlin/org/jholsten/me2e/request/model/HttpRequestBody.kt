package org.jholsten.me2e.request.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.utils.logger
import java.io.File
import java.nio.charset.Charset
import java.util.*

/**
 * Model representing the request body of an HTTP request.
 * Stores binary content of the body along with its content type.
 * @sample org.jholsten.samples.request.httpRequestBodyWithStringContent
 * @sample org.jholsten.samples.request.httpRequestBodyWithJsonContent
 */
class HttpRequestBody {
    /**
     * Content type of the request body.
     */
    val contentType: ContentType?

    /**
     * Content of the request body.
     */
    private val content: ByteArray?

    /**
     * Instantiates a new request body with the given String content and the given content type.
     * The contents of the body are encoded to a byte array using the given charset.
     *
     * To set the properties of the request body individually, use the [Builder] instead.
     * @param content String content of the request body.
     * @param contentType Content type of the request body.
     * @param charset Charset to use for encoding the string content to a byte array.
     */
    @JvmOverloads
    constructor(content: String, contentType: ContentType?, charset: Charset = Charsets.UTF_8) {
        this.content = content.toByteArray(charset)
        this.contentType = contentType
    }

    /**
     * Instantiates a new request body with the given File content and the given content type.
     * The content of the body is encoded to a byte array.
     *
     * To set the properties of the request body individually, use the [Builder] instead.
     * @param content File content of the request body.
     * @param contentType Content type of the request body.
     */
    constructor(content: File, contentType: ContentType?) {
        this.content = content.readBytes()
        this.contentType = contentType
    }

    /**
     * Instantiates a new request body with the given byte array content and the given content type.
     *
     * To set the properties of the request body individually, use the [Builder] instead.
     * @param content Binary content of the request body.
     * @param contentType Content type of the request body.
     */
    constructor(content: ByteArray, contentType: ContentType?) {
        this.content = content
        this.contentType = contentType
    }

    /**
     * Builder for instantiating instances of [HttpRequestBody].
     * @constructor Instantiates a new builder instance for constructing an [HttpRequestBody].
     */
    class Builder {
        private var contentType: ContentType? = null
        private var stringContent: String? = null
        private var charset: Charset = Charsets.UTF_8
        private var fileContent: File? = null
        private var binaryContent: ByteArray? = null

        private val logger = logger<Builder>()

        /**
         * Sets the given String content for the request body.
         * The contents of the body are encoded to a byte array using [Charsets.UTF_8] by default.
         * To change the charset to use for encoding, call [withCharset].
         * @param content String content of the request body to set.
         * @return This builder instance, to use for chaining.
         */
        fun withContent(content: String): Builder = apply {
            this.stringContent = content
        }

        /**
         * Sets the given file content for the request body.
         * The content of the body is encoded to a byte array.
         * @param content File content of the request body to set.
         * @return This builder instance, to use for chaining.
         */
        fun withContent(content: File): Builder = apply {
            this.fileContent = content
        }

        /**
         * Sets the given binary content for the request body.
         * @param content Binary content of the request body to set.
         * @return This builder instance, to use for chaining.
         */
        fun withContent(content: ByteArray): Builder = apply {
            this.binaryContent = content
        }

        /**
         * Reads the content from the file with the given name, relative to this project's
         * `resources` folder, and sets these contents, encoded as a byte array, for this request body.
         * @param filename Name of the file whose contents should be read. Needs to be located in `resources` folder.
         * @return This builder instance, to use for chaining.
         * @throws java.io.FileNotFoundException if file with the given name could not be found.
         */
        fun withContentFromFile(filename: String): Builder = apply {
            this.fileContent = FileUtils.getResourceAsFile(filename)
        }

        /**
         * Serializes the given object to a JSON string and sets this as the string content of the request body.
         * @param content Object to serialize.
         * @return This builder instance, to use for chaining.
         * @throws java.io.IOException if content could not be serialized.
         */
        fun <T> withJsonContent(content: T): Builder = apply {
            this.stringContent = DeserializerFactory.getObjectMapper().writeValueAsString(content)
            this.contentType = ContentType.JSON_UTF8
        }

        /**
         * Sets the given content type for the request body.
         * @param contentType Content type of the request body to set.
         * @return This builder instance, to use for chaining.
         */
        fun withContentType(contentType: ContentType?): Builder = apply {
            this.contentType = contentType
        }

        /**
         * Sets the given charset to use for encoding the string content to binary content.
         * By default, [Charsets.UTF_8] is used. Only applicable if string content was set.
         * @param charset Charset to use for encoding the string content.
         * @return This builder instance, to use for chaining.
         */
        fun withCharset(charset: Charset): Builder = apply {
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

    /**
     * Returns binary content deserialized as object of the given type or `null`, if no content is present.
     * For Kotlin, it is recommended to use the inline function [asObject] instead.
     *
     * Example Usage:
     * ```java
     * MyClass obj = body.asObject(MyClass.class);
     * MyClass[] arr = body.asObject(MyClass[].class);
     * ```
     * @param type Class to which request body content should be parsed.
     * @throws ParseException if content could not be parsed to instance of type [T].
     */
    fun <T> asObject(type: Class<T>): T? {
        try {
            return content?.let { DeserializerFactory.getObjectMapper().readValue(it, type) }
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
    }

    /**
     * Returns binary content deserialized as object of the given type or `null`, if no content is present.
     * In Java, this is useful for deserializing lists of objects, for example.
     * For Kotlin, it is recommended to use the inline function [asObject] instead.
     *
     * Example Usage:
     * ```java
     * List<MyClass> list = body.asObject(new TypeReference<List<MyClass>>(){});
     * ```
     * @param type Type reference to which request body content should be parsed.
     * @throws ParseException if content could not be parsed to instance of type [T].
     */
    fun <T> asObject(type: TypeReference<T>): T? {
        try {
            return content?.let { DeserializerFactory.getObjectMapper().readValue(it, type) }
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
    }

    /**
     * Returns binary content deserialized as object of the given type or `null`, if no content is present.
     * Only available for Kotlin.
     *
     * Example Usage:
     * ```kotlin
     * val obj = body.asObject<MyClass>()
     * val arr = body.asObject<Array<BodyClass>>()
     * val list = body.asObject<List<MyClass>>()
     * ```
     * @param T Class to which request body content should be parsed.
     * @throws ParseException if content could not be parsed to instance of type [T].
     */
    inline fun <reified T> asObject(): T? {
        return asObject(object : TypeReference<T>() {})
    }
}
