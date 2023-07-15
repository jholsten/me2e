package org.jholsten.me2e.request.mapper

import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class HttpResponseBodyMapper {
    companion object {
        val INSTANCE: HttpResponseBodyMapper = Mappers.getMapper(HttpResponseBodyMapper::class.java)
    }


}