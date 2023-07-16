package org.jholsten.me2e.request.mapper

import okhttp3.Response
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.model.HttpResponseBody
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.factory.Mappers
import java.nio.charset.Charset

@Mapper
internal abstract class HttpResponseMapper {
    companion object {
        val INSTANCE: HttpResponseMapper = Mappers.getMapper(HttpResponseMapper::class.java)
    }

    @Mapping(target = "request", source = "okHttpResponse", qualifiedByName = ["mapRequest"])
    @Mapping(target = "protocol", expression = "java(okHttpResponse.protocol().toString())")
    @Mapping(target = "message", expression = "java(okHttpResponse.message().toString())")
    @Mapping(target = "code", expression = "java(okHttpResponse.code())")
    @Mapping(target = "headers", expression = "java(okHttpResponse.headers().toMultimap())")
    @Mapping(target = "body", source = "okHttpResponse", qualifiedByName = ["mapResponseBody"])
    abstract fun toInternalDto(okHttpResponse: Response): HttpResponse

    @Named("mapRequest")
    fun mapRequest(okHttpResponse: Response): HttpRequest {
        return HttpRequestMapper.INSTANCE.toInternalDto(okHttpResponse.request)
    }

    @Named("mapResponseBody")
    fun mapResponseBody(okHttpResponse: Response): HttpResponseBody? {
        val okHttpResponseBody = okHttpResponse.body ?: return null

        okHttpResponseBody.use { body ->
            val binaryContent = body.bytes()
            return HttpResponseBody(
                contentType = body.contentType()?.let { return@let MediaTypeMapper.INSTANCE.toInternalDto(it) },
                contentLength = body.contentLength(),
                stringContent = binaryContent.toString(Charset.forName("UTF-8")),
                binaryContent = binaryContent,
            )
        }
    }
}
