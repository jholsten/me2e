package org.jholsten.me2e.assertions.matchers

import com.fasterxml.jackson.core.type.TypeReference
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import java.io.File
import java.nio.charset.Charset
import java.util.Base64

/**
 * Assertion for checking whether an actual value is equal to the contents from a file.
 * Use in combination with [asString], [asJson], [asBinary], [asBase64] or [asObject] for assertions.
 * Note that the file to be compared needs to be located in `resources` folder.
 * @throws java.io.FileNotFoundException if file with the given name does not exist.
 */
class FileContentsEqualityAssertion internal constructor(private val filename: String) {
    private val file: File = FileUtils.getResourceAsFile(filename)

    /**
     * Parses string content from file using the given charset and returns assertion for
     * checking the equality of this parsed string and the actual string value.
     * @param charset Charset to use for decoding the file content to string.
     */
    @JvmOverloads
    fun asString(charset: Charset = Charsets.UTF_8): Assertable<String?> {
        val expected = file.readText(charset)
        return EqualityAssertion(expected)
    }

    /**
     * Parses binary content from file and returns assertion for checking the equality of
     * this parsed binary content and the actual binary value.
     */
    fun asBinary(): Assertable<ByteArray?> {
        val expected = file.readBytes()
        return object : Assertable<ByteArray?>(
            assertion = { actual -> expected.contentEquals(actual) },
            message = "to be equal to\n\t$expected",
        ) {
            override fun toString(): String = "equal to $expected"
        }
    }

    /**
     * Parses string content from file as a JSON object and returns assertion for
     * checking the equality of this parsed instance and the actual JSON object.
     *
     * To exclude certain nodes when comparing the objects, use [JsonBodyEqualityAssertion.whenIgnoringNodes].
     * @throws ParseException if string content from file could not be parsed to JSON node.
     */
    fun asJson(): JsonBodyEqualityAssertion {
        val content = file.readText()
        val expected = try {
            DeserializerFactory.getObjectMapper().readTree(content)
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
        return JsonBodyEqualityAssertion(expected)
    }

    /**
     * Parses binary content from file, encodes this value to Base64 and returns assertion
     * for checking the equality of this parsed Base64 string and the actual value.
     */
    fun asBase64(): Assertable<String?> {
        val expected = Base64.getEncoder().encodeToString(file.readBytes())
        return EqualityAssertion(expected)
    }

    /**
     * Deserializes string content from file to an instance of the given type and returns assertion for
     * checking the equality of this deserialized instance with the actual object.
     * @param type Class to which response body content should be parsed.
     * @throws ParseException if content from file could not be deserialized to an instance of type [T].
     */
    fun <T> asObject(type: Class<T>): Assertable<T?> {
        val content = file.readText()
        val expected = try {
            DeserializerFactory.getObjectMapper().readValue(content, type)
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
        return EqualityAssertion(expected)
    }

    /**
     * Deserializes string content from file to an instance of the given type and returns assertion for
     * checking the equality of this deserialized instance with the actual object.
     *
     * In Java, this is useful for deserializing lists of objects, for example.
     * For Kotlin, it is recommended to use the inline function [asObject] instead.
     *
     * Example Usage:
     * ```java
     * equalToContentsFromFile("expected.json").asObject(new TypeReference<List<MyClass>>(){});
     * ```
     * @param type Class to which response body content should be parsed.
     * @throws ParseException if content from file could not be deserialized to an instance of type [T].
     */
    fun <T> asObject(type: TypeReference<T>): Assertable<T?> {
        val content = file.readText()
        val expected = try {
            DeserializerFactory.getObjectMapper().readValue(content, type)
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
        return EqualityAssertion(expected)
    }

    /**
     * Deserializes string content from file to an instance of the given type and returns assertion for
     * checking the equality of this deserialized instance with the actual object.
     * Only available for Kotlin.
     *
     * Example Usage:
     * ```kotlin
     * equalToContentsFromFile("expected.json").asObject<MyClass>()
     * equalToContentsFromFile("expected_list.json").asObject<Array<BodyClass>>()
     * equalToContentsFromFile("expected_list.json").asObject<List<MyClass>>()
     * ```
     * @param T Class to which response body content should be parsed.
     * @throws ParseException if content from file could not be deserialized to an instance of type [T].
     */
    inline fun <reified T> asObject(): Assertable<T?> {
        return asObject(object : TypeReference<T>() {})
    }

    override fun toString(): String = "equal to file $filename"
}
