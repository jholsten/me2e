package org.jholsten.me2e.request.mapper

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
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

    @Mapping(target = "url", expression = "java(okHttpRequest.url().toString())")
    @Mapping(target = "method", source = "okHttpRequest", qualifiedByName = ["mapHttpMethod"])
    @Mapping(target = "headers", source = "okHttpRequest", qualifiedByName = ["mapHeaders"])
    @Mapping(target = "body", source = "okHttpRequest", qualifiedByName = ["mapBody"])
    abstract fun toInternalDto(okHttpRequest: Request): HttpRequest

    @Mapping(target = "url", expression = "java(HttpUrl.parse(request.getUrl()))")
    @Mapping(target = "headers", source = "request.headers", qualifiedByName = ["mapHeadersToOkHttp"])
    @Mapping(target = "body", source = "body", qualifiedByName = ["mapBodyToOkHttp"])
    @Mapping(target = "tags\$okhttp", ignore = true)
    @Mapping(target = "tags", ignore = true)
    abstract fun toOkHttpRequest(request: HttpRequest): Request

    @Named("mapHttpMethod")
    fun mapHttpMethod(okHttpRequest: Request): HttpMethod {
        return HttpMethodMapper.INSTANCE.toInternalDto(okHttpRequest.method)
    }

    @Named("mapHeaders")
    fun mapHeaders(okHttpRequest: Request): Map<String, List<String>> {
        return okHttpRequest.headers.toMultimap()
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
        return when {
            body.stringContent != null -> body.stringContent.toRequestBody(mediaType)
            body.binaryContent != null -> body.binaryContent.toRequestBody(mediaType)
            body.fileContent != null -> body.fileContent.asRequestBody(mediaType)
            else -> null
        }
    }
}
