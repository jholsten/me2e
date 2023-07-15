package org.jholsten.me2e.request.mapper

import okhttp3.Request
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
}
