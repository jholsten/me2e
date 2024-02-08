package org.jholsten.me2e.request.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import java.nio.charset.Charset
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
     * Returns response body content as string or `null`, if no content is present.
     * @param charset Charset to use for decoding the binary content to string.
     */
    @JvmOverloads
    fun asString(charset: Charset = Charsets.UTF_8): String? {
        return content?.let { String(it, charset) }
    }

    /**
     * Returns binary content of the response body or `null`, if no content is present.
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
     * @param type Class to which response body content should be parsed.
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
     * @param type Type reference to which response body content should be parsed.
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
     * @throws ParseException if content could not be parsed to instance of type [T].
     */
    inline fun <reified T> asObject(): T? {
        return asObject(object : TypeReference<T>() {})
    }
}
