package org.jholsten.me2e.request.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
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

    /**
     * Returns binary content parsed as JSON node or null, if no content is present.
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
     * Returns binary content parsed as object of the given type or null, if no content is present.
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
     * Returns binary content parsed as object of the given type or null, if no content is present.
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
     * Returns binary content parsed as object of the given type or null, if no content is present.
     * @throws ParseException if content could not be parsed to instance of type [T].
     */
    inline fun <reified T> asObject(): T? {
        return asObject(object : TypeReference<T>() {})
    }
}
