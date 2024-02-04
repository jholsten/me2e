package org.jholsten.me2e.container.network.mapper

import org.jholsten.me2e.container.network.ContainerNetwork
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class ContainerNetworkMapper {
    companion object {
        val INSTANCE: ContainerNetworkMapper = Mappers.getMapper(ContainerNetworkMapper::class.java)
    }

    @Mapping(target = "networkId", source = "networkID")
    @Mapping(target = "globalIPV6Address", source = "globalIPv6Address")
    @Mapping(target = "globalIPV6PrefixLen", source = "globalIPv6PrefixLen")
    abstract fun toInternalDto(network: com.github.dockerjava.api.model.ContainerNetwork): ContainerNetwork

    abstract fun toInternalDto(networks: Map<String, com.github.dockerjava.api.model.ContainerNetwork>): Map<String, ContainerNetwork>
}
