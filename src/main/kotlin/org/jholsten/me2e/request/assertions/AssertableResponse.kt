package org.jholsten.me2e.request.assertions

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.jholsten.me2e.assertions.AssertionFailure
import org.jholsten.me2e.assertions.matchers.Assertable
import org.jholsten.me2e.assertions.matchers.JsonNodeAssertion
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Model for asserting that the properties of the given [response] are as expected.
 *
 * Example Usage:
 * ```kotlin
 * import org.jholsten.me2e.assertions.*
 * import org.jholsten.me2e.Me2eTest
 * import org.jholsten.me2e.container.injection.InjectService
 * import org.jholsten.me2e.container.microservice.MicroserviceContainer
 *
 * class E2ETest : Me2eTest() {
 *     @InjectService
 *     private lateinit var api: MicroserviceContainer
 *
 *     @Test
 *     fun `Invoking endpoint should return expected request`() {
 *         val url = RelativeUrl.Builder().withPath("/books").withQueryParameter("id", "1234").build()
 *         val response = api.get(url)
 *
 *         assertThat(response)
 *             .statusCode(equalTo(200))
 *             .message(equalTo("OK"))
 *             .jsonBody(containsNode("$.journal.title").withValue(equalTo("IEEE Software")))
 *     }
 * }
 * ```
 * @see org.jholsten.me2e.assertions.assertThat
 */
class AssertableResponse internal constructor(private val response: HttpResponse) {

    /**
     * Asserts that the status code of the [response] satisfies the given assertion.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).statusCode(equalTo(200))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.statusCode].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun statusCode(expected: Assertable<Int?>): AssertableResponse = apply {
        expected.evaluate("status code", this.response.statusCode)
    }

    /**
     * Asserts that the protocol of the [response] satisfies the given assertion.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).protocol(equalTo("HTTP/1.1"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.protocol].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun protocol(expected: Assertable<String?>): AssertableResponse = apply {
        expected.evaluate("protocol", this.response.protocol)
    }

    /**
     * Asserts that the message of the [response] satisfies the given assertion.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).message(equalTo("OK"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.message].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun message(expected: Assertable<String?>): AssertableResponse = apply {
        expected.evaluate("message", this.response.message)
    }

    /**
     * Asserts that the headers of the [response] satisfy the given assertion.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).headers(containsKey("Content-Type").withValue(equalTo("application/json")))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.headers].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun headers(expected: Assertable<Map<String, List<*>>?>): AssertableResponse = apply {
        expected.evaluate("headers", this.response.headers.entries)
    }

    /**
     * Asserts that the content type of the [response] satisfies the given assertion.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).contentType(equalTo("application/json"))
     * ```
     * @param expected Expectation for the value of the [org.jholsten.me2e.request.model.HttpResponseBody.contentType].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if content type is not set or if assertion was not successful.
     */
    fun contentType(expected: Assertable<String?>): AssertableResponse = apply {
        expected.evaluate("content type", this.response.body?.contentType?.value)
    }

    /**
     * Asserts that the body of the [response], encoded as string, satisfies the given assertion.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).body(equalTo("Text Content"))
     * ```
     * @param expected Expectation for the string value of the [HttpResponse.body].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun body(expected: Assertable<String?>): AssertableResponse = apply {
        expected.evaluate("body", this.response.body?.asString())
    }

    /**
     * Asserts that the body of the [response], deserialized to [type], satisfies the given assertion.
     * For Kotlin, it is recommended to use the inline function [objectBody] instead.
     *
     * Example Usage:
     * ```java
     * assertThat(response).objectBody(MyClass.class, equalTo(obj));
     * ```
     * @param expected Expectation for the deserialized value of the [HttpResponse.body].
     * @param type Type to which the response body content should be deserialized.
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun <T> objectBody(type: Class<T>, expected: Assertable<T?>): AssertableResponse = apply {
        val obj = try {
            this.response.body?.asObject(type)
        } catch (e: ParseException) {
            throw AssertionFailure("Unable to deserialize body to instance of type $type: ${e.message}")
        }
        expected.evaluate("body", obj)
    }

    /**
     * Asserts that the body of the [response], deserialized to [type], satisfies the given assertion.
     * In Java, this is useful for deserializing lists of objects, for example.
     * For Kotlin, it is recommended to use the inline function [objectBody] instead.
     *
     * Example Usage:
     * ```java
     * assertThat(response).objectBody(new TypeReference<List<MyClass>>(){}, equalTo(list));
     * ```
     * @param expected Expectation for the deserialized value of the [HttpResponse.body].
     * @param type Type to which the response body content should be deserialized.
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun <T> objectBody(type: TypeReference<T>, expected: Assertable<T?>): AssertableResponse = apply {
        val obj = try {
            this.response.body?.asObject(type)
        } catch (e: ParseException) {
            throw AssertionFailure("Unable to deserialize body to instance of type $type: ${e.message}")
        }
        expected.evaluate("body", obj)
    }

    /**
     * Asserts that the body of the [response], deserialized to type [T], satisfies the given assertion.
     * Only available for Kotlin.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).objectBody<MyClass>(equalTo(list))
     * ```
     * @param expected Expectation for the deserialized value of the [HttpResponse.body].
     * @param T Type to which the response body content should be deserialized.
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    inline fun <reified T> objectBody(expected: Assertable<T?>): AssertableResponse = apply {
        objectBody(object : TypeReference<T>() {}, expected)
    }

    /**
     * Asserts that the body of the [response], encoded as byte array, satisfies the given assertion.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).binaryBody(equalTo(byteArrayOf(123, 34, 110)))
     * ```
     * @param expected Expectation for the binary value of the [HttpResponse.body].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun binaryBody(expected: Assertable<ByteArray?>): AssertableResponse = apply {
        expected.evaluate("binary body", this.response.body?.asBinary())
    }

    /**
     * Asserts that the body of the [response], encoded as base 64, satisfies the given assertion.
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).base64Body(equalTo("YWRtaW46c2VjcmV0"))
     * ```
     * @param expected Expectation for the base 64 encoded value of the [HttpResponse.body].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun base64Body(expected: Assertable<String?>): AssertableResponse = apply {
        expected.evaluate("base 64 body", this.response.body?.asBase64())
    }

    /**
     * Asserts that the body of the [response], parsed as JSON, satisfies the given assertion.
     * See [JsonNodeAssertion] for detailed information on the format of the [JsonNodeAssertion.expectedPath].
     *
     * Example Usage:
     * ```kotlin
     * assertThat(response).jsonBody(containsNode("$.journal.title").withValue(equalTo("IEEE Software")))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.body].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ExpectedResponse].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun jsonBody(expected: Assertable<JsonNode?>): AssertableResponse = apply {
        val json = try {
            this.response.body?.asJson()
        } catch (e: Exception) {
            throw AssertionFailure("Unable to parse body as JSON: ${e.message}")
        }

        expected.evaluate("json body", json)
    }

    /**
     * Asserts that the [response] conforms to the given specification. Evaluates assertions for all properties.
     * @param specification Expectation for the [response].
     * @throws AssertionFailure if at least one assertion was not successful.
     */
    fun conformsTo(specification: ExpectedResponse) {
        specification.evaluate(this)
    }
}
