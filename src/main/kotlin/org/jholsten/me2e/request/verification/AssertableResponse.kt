package org.jholsten.me2e.request.verification

import org.jholsten.me2e.request.model.HttpResponse

// TODO: Do not inherit from Response (does not make sense)
class AssertableResponse internal constructor(response: HttpResponse) : HttpResponse(
    request = response.request,
    protocol = response.protocol,
    message = response.message,
    code = response.code,
    headers = response.headers, // TODO: AssertableHeaders
    body = response.body, // TODO: AssertableBody
) {
}

// TODO: Define one Class Assertions with all methods, e.g.
/*
isEqualTo(expected: String): StringMatcher
isEqualTo(expected: Int): NumberMatcher
isEqualTo(expected: Any): ValueMatcher
 */

/*
backend.get("/search")
    .assertThat() // Or maybe: do not allow this and only enable the syntax below (as AssertJ)
        .statusCode(isEqualTo(200))
        .protocol(isEqualTo("http/1.1.")
        .headers(contains("key").withValue("abc"))
        .headers(contains("Content-Type").withValue("abc"))
        .contentType(isEqualTo("JSON"))
        .jsonBody("lastname", isEqualTo("Doe"))
        .body(isEqualTo("Some Response")
        .body(contains("file.txt"))

backend.get(RelativeUrl("/search")).assertThat().conformsTo(specification)

val response = backend.get("/search")

assertThat(response).statusCode(isEqualTo(200))
assertThat(response).conformsTo(specification)

val specification = ResponseSpecification.Builder()
    .expectStatusCode(isEqualTo(200))
    .expectBody(isEqualTo("Some Response"))
    .expectHeaders(contains("key").withValue("abc"))
    .build()
 */
