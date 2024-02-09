package org.jholsten.me2e.request.model

import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.*
import java.nio.file.Files
import java.util.stream.Stream

internal class HttpRequestBodyTest {

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(StringBodyArgumentProvider::class)
    @Suppress("UNUSED_PARAMETER")
    fun `String content should be set in request body`(description: String, body: HttpRequestBody) {
        assertEquals("abc", body.asString())
        RecursiveComparison.assertEquals(byteArrayOf(97, 98, 99), body.asBinary())
        assertEquals("YWJj", body.asBase64())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `String content with special characters should be set in request body`() {
        val body = HttpRequestBody(
            content = "abcäÄöÖüÜèé%&~'",
            contentType = MediaType.TEXT_PLAIN_UTF8,
        )

        assertEquals("abcäÄöÖüÜèé%&~'", body.asString())
        RecursiveComparison.assertEquals(
            byteArrayOf(
                97, 98, 99, 195.toByte(), 164.toByte(), 195.toByte(), 132.toByte(), 195.toByte(), 182.toByte(),
                195.toByte(), 150.toByte(), 195.toByte(), 188.toByte(), 195.toByte(), 156.toByte(), 195.toByte(),
                168.toByte(), 195.toByte(), 169.toByte(), 37, 38, 126, 39
            ), body.asBinary()
        )
        assertEquals("YWJjw6TDhMO2w5bDvMOcw6jDqSUmfic=", body.asBase64())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Object should be serialized and content type should be set to JSON`() {
        val body = HttpRequestBody.Builder()
            .withJsonContent(Pair("value1", "value2"))
            .build()

        assertEquals("{\"first\":\"value1\",\"second\":\"value2\"}", body.asString())
        assertEquals("application/json; charset=utf-8", body.contentType?.value)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(FileBodyArgumentProvider::class)
    @Suppress("UNUSED_PARAMETER")
    fun `File content should be set in request body`(description: String, body: HttpRequestBody) {
        assertEquals("abc", body.asString())
        RecursiveComparison.assertEquals(byteArrayOf(97, 98, 99), body.asBinary())
        assertEquals("YWJj", body.asBase64())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(BinaryBodyArgumentProvider::class)
    @Suppress("UNUSED_PARAMETER")
    fun `Binary content should be set in request body`(description: String, body: HttpRequestBody) {
        assertEquals("abc", body.asString())
        RecursiveComparison.assertEquals(byteArrayOf(97, 98, 99), body.asBinary())
        assertEquals("YWJj", body.asBase64())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    class StringBodyArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(
                    "Constructor",
                    HttpRequestBody(
                        content = "abc",
                        contentType = MediaType.TEXT_PLAIN_UTF8,
                        charset = Charsets.UTF_8,
                    ),
                ),
                Arguments.of(
                    "Builder",
                    HttpRequestBody.Builder()
                        .withContent("abc")
                        .withContentType(MediaType.TEXT_PLAIN_UTF8)
                        .withCharset(Charsets.UTF_8)
                        .build()
                ),
            )
        }
    }

    class FileBodyArgumentProvider : ArgumentsProvider {
        private val file: File

        init {
            val path = Files.createTempFile("test", ".tmp")
            Files.write(path, "abc".toByteArray())
            file = path.toFile()
        }

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(
                    "Constructor",
                    HttpRequestBody(
                        content = file,
                        contentType = MediaType.TEXT_PLAIN_UTF8,
                    ),
                ),
                Arguments.of(
                    "Builder",
                    HttpRequestBody.Builder()
                        .withContent(file)
                        .withContentType(MediaType.TEXT_PLAIN_UTF8)
                        .build()
                ),
            )
        }
    }

    class BinaryBodyArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(
                    "Constructor",
                    HttpRequestBody(
                        content = byteArrayOf(97, 98, 99),
                        contentType = MediaType.TEXT_PLAIN_UTF8,
                    ),
                ),
                Arguments.of(
                    "Builder",
                    HttpRequestBody.Builder()
                        .withContent(byteArrayOf(97, 98, 99))
                        .withContentType(MediaType.TEXT_PLAIN_UTF8)
                        .build()
                ),
            )
        }
    }
}
