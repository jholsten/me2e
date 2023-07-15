package org.jholsten.me2e.request.mapper

import okhttp3.Response
import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.model.HttpResponseBody
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class HttpResponseMapper {
    companion object {
        val INSTANCE: HttpResponseMapper = Mappers.getMapper(HttpResponseMapper::class.java)
    }

    @Mapping(target = "body", source = "okHttpResponse", qualifiedByName = ["mapOkHttpResponseBody"])
    abstract fun toInternalDto(okHttpResponse: Response): HttpResponse

    @Named("mapOkHttpResponseBody")
    fun mapOkHttpResponseBody(okHttpResponse: Response): HttpResponseBody? {
        val okHttpResponseBody = okHttpResponse.body ?: return null

        okHttpResponseBody.use { body ->
            return HttpResponseBody(
                contentType = body.contentType()?.let { return@let MediaTypeMapper.INSTANCE.toInternalDto(it) },
                contentLength = body.contentLength(),
                stringContent = body.string(),
            )
        }
    }
}
