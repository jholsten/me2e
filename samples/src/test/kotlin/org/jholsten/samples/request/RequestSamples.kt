package org.jholsten.samples.request

import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.model.*


/**
 * Sample of a [RequestInterceptor] which adds a basic authentication header to all HTTP requests.
 */
fun basicAuthenticationRequestInterceptor(): RequestInterceptor {
    return object : RequestInterceptor {
        override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
            val request = chain.getRequest().newBuilder()
                .addHeader("Authorization", "Basic YWRtaW46c2VjcmV0")
                .build()
            return chain.proceed(request)
        }
    }
}

fun httpRequestWithHeaders(): HttpRequest {
    val headers = HttpHeaders.Builder()
        .add("Custom-Key", "Some Value")
        .build()

    return HttpRequest.Builder()
        .withUrl(Url("https://example.com"))
        .withMethod(HttpMethod.GET)
        .withHeaders(headers)
        .build()
}

fun httpRequestBodyWithStringContent(): HttpRequestBody {
    return HttpRequestBody.Builder()
        .withContent("Hello World")
        .withContentType(ContentType.TEXT_PLAIN_UTF8)
        .build()
}

fun httpRequestBodyWithJsonContent(): HttpRequestBody {
    return HttpRequestBody.Builder()
        .withJsonContent(Pair("value1", "value2"))
        .build()
}

fun url(): Url {
    return Url("https://example.com/search?q=xyz&q=abc&id=12#p=42")
}

fun urlWithBuilder(): Url {
    return Url.Builder()
        .withScheme(Url.Scheme.HTTPS)
        .withHost("example.com")
        .withPath("/search")
        .withQueryParameter("q", "xyz")
        .withQueryParameter("q", "abc")
        .withQueryParameter("id", "12")
        .withFragment("p=42")
        .build()
}

fun relativeUrl(): RelativeUrl {
    return RelativeUrl("/search?q=xyz&q=abc&id=12#p=42")
}

fun relativeUrlWithBuilder(): RelativeUrl {
    return RelativeUrl.Builder()
        .withPath("/search")
        .withQueryParameter("q", "xyz")
        .withQueryParameter("q", "abc")
        .withQueryParameter("id", "12")
        .withFragment("p=42")
        .build()
}
