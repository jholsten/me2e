package org.jholsten.me2e.request.mapper

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jholsten.me2e.request.model.HttpHeaders
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.model.HttpResponseBody
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.factory.Mappers
import java.io.IOException
import kotlin.Throws

@Mapper(uses = [HttpRequestMapper::class])
internal abstract class HttpResponseMapper {
    companion object {
        @JvmSynthetic
        val INSTANCE: HttpResponseMapper = Mappers.getMapper(HttpResponseMapper::class.java)
    }

    @Mapping(target = "request", source = "okHttpResponse", qualifiedByName = ["mapRequest"])
    @Mapping(target = "protocol", expression = "java(okHttpResponse.protocol().toString())")
    @Mapping(target = "message", expression = "java(okHttpResponse.message().toString())")
    @Mapping(target = "code", expression = "java(okHttpResponse.code())")
    @Mapping(target = "headers", source = "okHttpResponse", qualifiedByName = ["mapHeaders"])
    @Mapping(target = "body", source = "okHttpResponse", qualifiedByName = ["mapResponseBody"])
    internal abstract fun toInternalDto(okHttpResponse: Response): HttpResponse

    @Mapping(target = "protocol", expression = "java(Protocol.get(response.getProtocol()))")
    @Mapping(target = "headers", source = "response.headers", qualifiedByName = ["mapHeadersToOkHttp"])
    @Mapping(target = "body", source = "body", qualifiedByName = ["mapResponseBodyToOkHttp"])
    @Mapping(target = "handshake", ignore = true)
    @Mapping(target = "networkResponse", ignore = true)
    @Mapping(target = "cacheResponse", ignore = true)
    @Mapping(target = "priorResponse", ignore = true)
    @Mapping(target = "sentRequestAtMillis", ignore = true)
    @Mapping(target = "receivedResponseAtMillis", ignore = true)
    @Mapping(target = "exchange", ignore = true)
    @Throws(IOException::class)
    internal abstract fun toOkHttpResponse(response: HttpResponse): Response

    @Named("mapRequest")
    protected fun mapRequest(okHttpResponse: Response): HttpRequest {
        return HttpRequestMapper.INSTANCE.toInternalDto(okHttpResponse.request)
    }

    @Named("mapHeaders")
    protected fun mapHeaders(okHttpResponse: Response): HttpHeaders {
        return HttpRequestMapper.INSTANCE.mapHeaders(okHttpResponse.headers)
    }

    @Named("mapResponseBody")
    protected fun mapResponseBody(okHttpResponse: Response): HttpResponseBody? {
        val okHttpResponseBody = okHttpResponse.body ?: return null

        okHttpResponseBody.use { body ->
            val binaryContent = body.bytes()
            return HttpResponseBody(
                contentType = body.contentType()?.let { return@let MediaTypeMapper.INSTANCE.toInternalDto(it) },
                content = binaryContent,
            )
        }
    }

    @Named("mapResponseBodyToOkHttp")
    protected fun mapResponseBodyToOkHttp(responseBody: HttpResponseBody?): ResponseBody? {
        if (responseBody == null) {
            return null
        }

        val mediaType = responseBody.contentType?.value?.toMediaTypeOrNull()
        return responseBody.asBinary()?.toResponseBody(mediaType)
    }
}
