package org.jholsten.me2e.request.mapper

import org.jholsten.me2e.request.model.MediaType
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class MediaTypeMapper {
    companion object {
        val INSTANCE: MediaTypeMapper = Mappers.getMapper(MediaTypeMapper::class.java)
    }

    @Mapping(target = "value", source = "okHttpMediaType", qualifiedByName = ["getOkHttpMediaType"])
    abstract fun toInternalDto(okHttpMediaType: okhttp3.MediaType): MediaType

    @Named("getOkHttpMediaType")
    fun getOkHttpMediaType(okHttpMediaType: okhttp3.MediaType): String {
        return okHttpMediaType.type + "/" + okHttpMediaType.subtype
    }
}
