package org.jholsten.me2e.request.mapper

import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.RequestMethod
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
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
        val INSTANCE: HttpRequestMapper = Mappers.getMapper(HttpRequestMapper::class.java)
    }

    @Mapping(target = "url", expression = "java(new Url(okHttpRequest.url().toString()))")
    @Mapping(target = "method", source = "okHttpRequest", qualifiedByName = ["mapHttpMethod"])
    @Mapping(target = "headers", source = "okHttpRequest", qualifiedByName = ["mapHeaders"])
    @Mapping(target = "body", source = "okHttpRequest", qualifiedByName = ["mapBody"])
    abstract fun toInternalDto(okHttpRequest: Request): HttpRequest

    @Mapping(target = "url", expression = "java(new Url(request.getAbsoluteUrl()))")
    @Mapping(target = "method", source = "request.method", qualifiedByName = ["mapHttpMethod"])
    @Mapping(target = "headers", source = "request.headers", qualifiedByName = ["mapHeaders"])
    @Mapping(target = "body", source = "request", qualifiedByName = ["mapBody"])
    abstract fun toInternalDto(request: com.github.tomakehurst.wiremock.http.Request): HttpRequest

    @Mapping(target = "url", expression = "java(HttpUrl.parse(request.getUrl().toString()))")
    @Mapping(target = "headers", source = "request.headers", qualifiedByName = ["mapHeadersToOkHttp"])
    @Mapping(target = "body", source = "body", qualifiedByName = ["mapBodyToOkHttp"])
    @Mapping(target = "tags\$okhttp", ignore = true)
    @Mapping(target = "tags", expression = "java(new java.util.HashMap<>())")
    abstract fun toOkHttpRequest(request: HttpRequest): Request

    @Named("mapHttpMethod")
    fun mapHttpMethod(okHttpRequest: Request): HttpMethod {
        return HttpMethodMapper.INSTANCE.toInternalDto(okHttpRequest.method)
    }

    @Named("mapHttpMethod")
    fun mapHttpMethod(requestMethod: RequestMethod): HttpMethod {
        return HttpMethodMapper.INSTANCE.toInternalDto(requestMethod.value())
    }

    @Named("mapHeaders")
    fun mapHeaders(okHttpRequest: Request): Map<String, List<String>> {
        return okHttpRequest.headers.toMultimap()
    }

    @Named("mapHeaders")
    fun mapHeaders(headers: HttpHeaders): Map<String, List<String>> {
        return headers.all().associate { it.key() to it.values() }
    }

    @Named("mapBody")
    fun mapBody(okHttpRequest: Request): HttpRequestBody? {
        val body = okHttpRequest.body ?: return null
        val buffer = Buffer()
        body.writeTo(buffer)
        return HttpRequestBody(
            buffer = buffer,
            contentType = body.contentType()?.let { return@let MediaTypeMapper.INSTANCE.toInternalDto(it) },
        )
    }

    @Named("mapBody")
    fun mapBody(request: com.github.tomakehurst.wiremock.http.Request): HttpRequestBody? {
        if (request.body == null || request.body.isEmpty()) {
            return null
        }
        return HttpRequestBody(
            content = request.bodyAsString,
            contentType = request.contentTypeHeader()?.let { return@let MediaTypeMapper.INSTANCE.toInternalDto(it) },
        )
    }

    @Named("mapHeadersToOkHttp")
    fun mapHeadersToOkHttp(headers: Map<String, List<String>>): Headers {
        val builder = Headers.Builder()
        for (header in headers.entries) {
            header.value.forEach { builder.add(header.key, it) }
        }

        return builder.build()
    }

    @Named("mapBodyToOkHttp")
    fun mapBodyToOkHttp(body: HttpRequestBody?): RequestBody? {
        if (body == null) {
            return null
        }

        val mediaType = body.contentType?.value?.toMediaTypeOrNull()
        return body.asBinary()?.toRequestBody(mediaType)
    }
}
