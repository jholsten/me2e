package org.jholsten.me2e.request.mapper

import com.github.tomakehurst.wiremock.http.ContentTypeHeader
import org.jholsten.me2e.request.model.MediaType
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class MediaTypeMapper {
    companion object {
        @JvmSynthetic
        val INSTANCE: MediaTypeMapper = Mappers.getMapper(MediaTypeMapper::class.java)
    }

    @Mapping(target = "value", source = "okHttpMediaType", qualifiedByName = ["getOkHttpMediaType"])
    internal abstract fun toInternalDto(okHttpMediaType: okhttp3.MediaType): MediaType

    @Mapping(target = "value", expression = "java(contentTypeHeader.mimeTypePart())")
    internal abstract fun toInternalDto(contentTypeHeader: ContentTypeHeader): MediaType

    @Named("getOkHttpMediaType")
    protected fun getOkHttpMediaType(okHttpMediaType: okhttp3.MediaType): String {
        return okHttpMediaType.type + "/" + okHttpMediaType.subtype
    }
}
