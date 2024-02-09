package org.jholsten.me2e.request.mapper

import com.github.tomakehurst.wiremock.http.ContentTypeHeader
import org.jholsten.me2e.request.model.ContentType
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class ContentTypeMapper {
    companion object {
        @JvmSynthetic
        val INSTANCE: ContentTypeMapper = Mappers.getMapper(ContentTypeMapper::class.java)
    }

    @Mapping(target = "value", source = "okHttpMediaType", qualifiedByName = ["getOkHttpMediaType"])
    internal abstract fun toInternalDto(okHttpMediaType: okhttp3.MediaType): ContentType

    @Mapping(target = "value", expression = "java(contentTypeHeader.mimeTypePart())")
    internal abstract fun toInternalDto(contentTypeHeader: ContentTypeHeader): ContentType

    @Named("getOkHttpMediaType")
    protected fun getOkHttpMediaType(okHttpMediaType: okhttp3.MediaType): String {
        return okHttpMediaType.type + "/" + okHttpMediaType.subtype
    }
}
