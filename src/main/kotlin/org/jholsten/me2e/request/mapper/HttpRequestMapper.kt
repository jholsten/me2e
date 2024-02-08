package org.jholsten.me2e.request.mapper

import com.github.tomakehurst.wiremock.http.HttpHeaders as WireMockHttpHeaders
import com.github.tomakehurst.wiremock.http.RequestMethod as WireMockHttpMethod
import com.github.tomakehurst.wiremock.http.Request as WireMockRequest
import okhttp3.Headers as OkHttpHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody as OkHttpRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.jholsten.me2e.request.model.HttpHeaders
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class HttpRequestMapper {
    companion object {
        @JvmSynthetic
        val INSTANCE: HttpRequestMapper = Mappers.getMapper(HttpRequestMapper::class.java)
    }

    @Mapping(target = "url", expression = "java(new Url(okHttpRequest.url().toString()))")
    @Mapping(target = "method", source = "okHttpRequest", qualifiedByName = ["mapHttpMethod"])
    @Mapping(target = "headers", source = "okHttpRequest", qualifiedByName = ["mapHeaders"])
    @Mapping(target = "body", source = "okHttpRequest", qualifiedByName = ["mapBody"])
    internal abstract fun toInternalDto(okHttpRequest: OkHttpRequest): HttpRequest

    @Mapping(target = "url", expression = "java(new Url(request.getAbsoluteUrl()))")
    @Mapping(target = "method", source = "request.method", qualifiedByName = ["mapHttpMethod"])
    @Mapping(target = "headers", source = "request.headers", qualifiedByName = ["mapHeaders"])
    @Mapping(target = "body", source = "request", qualifiedByName = ["mapBody"])
    internal abstract fun toInternalDto(request: WireMockRequest): HttpRequest

    @Mapping(target = "url", expression = "java(HttpUrl.parse(request.getUrl().toString()))")
    @Mapping(target = "headers", source = "request.headers", qualifiedByName = ["mapHeadersToOkHttp"])
    @Mapping(target = "body", source = "body", qualifiedByName = ["mapBodyToOkHttp"])
    @Mapping(target = "tags\$okhttp", ignore = true)
    @Mapping(target = "tags", expression = "java(new java.util.HashMap<>())")
    internal abstract fun toOkHttpRequest(request: HttpRequest): OkHttpRequest

    @Named("mapHttpMethod")
    protected fun mapHttpMethod(okHttpRequest: OkHttpRequest): HttpMethod {
        return HttpMethodMapper.INSTANCE.toInternalDto(okHttpRequest.method)
    }

    @Named("mapHttpMethod")
    protected fun mapHttpMethod(requestMethod: WireMockHttpMethod): HttpMethod {
        return HttpMethodMapper.INSTANCE.toInternalDto(requestMethod.value())
    }

    @Named("mapHeaders")
    protected fun mapHeaders(okHttpRequest: OkHttpRequest): HttpHeaders {
        return mapHeaders(okHttpRequest.headers)
    }

    @Named("mapHeaders")
    internal fun mapHeaders(headers: OkHttpHeaders): HttpHeaders {
        val builder = HttpHeaders.Builder()
        for (i in 0 until headers.size) {
            builder.add(headers.name(i), headers.value(i))
        }
        return builder.build()
    }

    @Named("mapHeaders")
    protected fun mapHeaders(headers: WireMockHttpHeaders): HttpHeaders {
        return HttpHeaders(headers.all().associate { it.key() to it.values() })
    }

    @Named("mapBody")
    protected fun mapBody(okHttpRequest: OkHttpRequest): HttpRequestBody? {
        val body = okHttpRequest.body ?: return null
        val buffer = Buffer()
        body.writeTo(buffer)
        return HttpRequestBody(
            content = buffer.readByteArray(),
            contentType = body.contentType()?.let { return@let MediaTypeMapper.INSTANCE.toInternalDto(it) },
        )
    }

    @Named("mapBody")
    protected fun mapBody(request: WireMockRequest): HttpRequestBody? {
        if (request.body == null || request.body.isEmpty()) {
            return null
        }
        return HttpRequestBody(
            content = request.bodyAsString,
            contentType = request.contentTypeHeader()?.let { return@let MediaTypeMapper.INSTANCE.toInternalDto(it) },
        )
    }

    @Named("mapHeadersToOkHttp")
    protected fun mapHeadersToOkHttp(headers: HttpHeaders): OkHttpHeaders {
        val builder = OkHttpHeaders.Builder()
        for (header in headers.entries) {
            header.value.forEach { builder.add(header.key, it) }
        }

        return builder.build()
    }

    @Named("mapBodyToOkHttp")
    protected fun mapBodyToOkHttp(body: HttpRequestBody?): OkHttpRequestBody? {
        if (body == null) {
            return null
        }

        val mediaType = body.contentType?.value?.toMediaTypeOrNull()
        return body.asBinary()?.toRequestBody(mediaType)
    }
}
