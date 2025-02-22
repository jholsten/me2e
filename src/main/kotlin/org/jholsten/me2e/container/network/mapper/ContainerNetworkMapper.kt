package org.jholsten.me2e.container.network.mapper

import com.github.dockerjava.api.model.ContainerNetwork as DockerContainerNetwork
import org.jholsten.me2e.container.network.ContainerNetwork
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class ContainerNetworkMapper {
    companion object {
        @JvmSynthetic
        val INSTANCE: ContainerNetworkMapper = Mappers.getMapper(ContainerNetworkMapper::class.java)
    }

    @Mapping(target = "networkId", source = "networkID")
    @Mapping(target = "globalIPV6Address", source = "globalIPv6Address")
    @Mapping(target = "globalIPV6PrefixLen", source = "globalIPv6PrefixLen")
    @Mapping(target = "ipamConfig.ipV4Address", source = "ipamConfig.ipv4Address")
    @Mapping(target = "ipamConfig.ipV6Address", source = "ipamConfig.ipv6Address")
    internal abstract fun toInternalDto(network: DockerContainerNetwork): ContainerNetwork

    internal abstract fun toInternalDto(networks: Map<String, DockerContainerNetwork>): Map<String, ContainerNetwork>
}
