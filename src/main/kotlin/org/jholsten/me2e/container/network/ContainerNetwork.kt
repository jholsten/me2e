package org.jholsten.me2e.container.network

/**
 * Specification of the network that a container is part of.
 * Corresponds to one entry in the `Networks` returned by
 * [`docker inspect $containerId`](https://docs.docker.com/engine/reference/commandline/inspect).
 * @see <a href="https://docs.docker.com/engine/reference/commandline/inspect">Docker Documentation</a>
 */
data class ContainerNetwork internal constructor(
    /**
     * Custom IPAM configuration defined for the network.
     */
    val ipamConfig: IpamConfig?,

    /**
     * List of network links for the container.
     */
    val links: List<Link>?,

    /**
     * List of network aliases for the container.
     */
    val aliases: List<String>?,

    /**
     * Unique identifier of the network.
     */
    val networkId: String,

    /**
     * Unique identifier of the network endpoint.
     */
    val endpointId: String?,

    /**
     * Gateway IPV4 address for the network.
     */
    val gateway: String?,

    /**
     * IPV4 address assigned to the container on the network.
     */
    val ipAddress: String?,

    /**
     * Mask length of the [ipAddress].
     */
    val ipPrefixLen: Int?,

    /**
     * Gateway IPV6 address for the network.
     */
    val ipV6Gateway: String?,

    /**
     * Global IPV6 address assigned to the container.
     */
    val globalIPV6Address: String?,

    /**
     * Mask length of the [globalIPV6Address].
     */
    val globalIPV6PrefixLen: Int?,

    /**
     * MAC address of the container on the network.
     */
    val macAddress: String?,
) {
    /**
     * Custom IPAM (IP Address Management) configuration defined for the network.
     * @see <a href="https://docs.docker.com/compose/compose-file/06-networks/#ipam">Docker Documentation</a>
     */
    data class IpamConfig(
        /**
         * Configured IPv4 address.
         */
        val ipV4Address: String?,

        /**
         * Configured IPv6 address.
         */
        val ipV6Address: String?,
    )

    /**
     * Representation of a network link between two Docker containers.
     * The container with the name [name] is made available in the target container with the aliased name [alias].
     * This involves creating an entry in `/etc/hosts` and some environment variables in the target container as
     * well as creating a network bridge between both containers.
     * @see com.github.dockerjava.api.model.Link
     */
    data class Link(
        /**
         * Name of the container that is linked into the target container.
         * @see com.github.dockerjava.api.model.Link.getName
         */
        val name: String,

        /**
         * Aliased name under which the linked container will be available in the target container.
         * @see com.github.dockerjava.api.model.Link.getAlias
         */
        val alias: String,
    )
}
