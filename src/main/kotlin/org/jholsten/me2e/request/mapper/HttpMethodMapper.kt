package org.jholsten.me2e.request.mapper

import org.jholsten.me2e.request.model.HttpMethod
import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.ValueMapping
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class HttpMethodMapper {
    companion object {
        val INSTANCE: HttpMethodMapper = Mappers.getMapper(HttpMethodMapper::class.java)
    }

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = "UNKNOWN")
    abstract fun toInternalDto(value: String): HttpMethod
}
