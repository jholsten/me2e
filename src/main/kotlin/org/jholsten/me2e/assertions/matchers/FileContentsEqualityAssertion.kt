package org.jholsten.me2e.assertions.matchers

import com.fasterxml.jackson.databind.JsonNode
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import java.io.File
import java.nio.charset.Charset
import java.util.Base64

/**
 * Assertion for checking whether an actual value is equal to the contents from a file.
 * Use in combination with [asString], [asJson], [asBinary] or [asBase64] for assertions.
 * Note that the file to be compared needs to be located in `resources` folder.
 * @throws java.io.FileNotFoundException if file with the given name does not exist.
 */
class FileContentsEqualityAssertion {
    private val file: File
    private val filename: String

    internal constructor(filename: String) {
        this.filename = filename
        this.file = FileUtils.getResourceAsFile(filename)
    }

    internal constructor(filename: String, file: File) {
        this.filename = filename
        this.file = file
    }

    /**
     * Parses string content from file using the given charset and returns assertion for
     * checking the equality of this parsed string and the actual string value.
     * @param charset Charset to use for decoding the file content to string.
     */
    @JvmOverloads
    fun asString(charset: Charset = Charsets.UTF_8): Assertable<String?> {
        val expected = file.readText(charset)
        return object : Assertable<String?>(
            assertion = { actual -> expected == actual },
            message = "to be equal to\n\t$expected",
        ) {
            override fun toString(): String = "equal to $expected"
        }
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
     * Parses string content from file as a JSON node and returns assertion for
     * checking the equality of this parsed instance and the actual JSON value.
     * @throws ParseException if string content from file could not be parsed to JSON node.
     */
    fun asJson(): Assertable<JsonNode?> {
        val content = file.readText()
        val expected = try {
            DeserializerFactory.getObjectMapper().readTree(content)
        } catch (e: Exception) {
            throw ParseException(e.message)
        }
        return object : Assertable<JsonNode?>(
            assertion = { actual -> expected == actual },
            message = "to be equal to\n\t$expected",
        ) {
            override fun toString(): String = "equal to $expected"
        }
    }

    /**
     * Parses binary content from file, encodes this value to Base64 and returns assertion
     * for checking the equality of this parsed Base64 string and the actual value.
     */
    fun asBase64(): Assertable<String?> {
        val expected = Base64.getEncoder().encodeToString(file.readBytes())
        return object : Assertable<String?>(
            assertion = { actual -> expected == actual },
            message = "to be equal to\n\t$expected",
        ) {
            override fun toString(): String = "equal to $expected"
        }
    }

    override fun toString(): String = "equal to file $filename"
}
