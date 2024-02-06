package org.jholsten.me2e.report.tracing.model

import org.jholsten.me2e.report.logs.model.ServiceSpecification
import java.util.UUID

/**
 * Specification of one node in a network.
 * This may either be a Docker container, the Test Runner, a network gateway or a Mock Server.
 */
class NetworkNodeSpecification internal constructor(
    /**
     * Type of this network node.
     */
    val nodeType: NodeType,

    /**
     * IP address of this network node.
     */
    val ipAddress: String,

    /**
     * Representation of this network node.
     */
    specification: ServiceSpecification,
) {
    /**
     * Unique identifier of this network node.
     * @see ServiceSpecification.id
     */
    val id: UUID = specification.id

    /**
     * Unique color of this network node as Hex value.
     * @see ServiceSpecification.color
     */
    val color: String = specification.color

    /**
     * Name of the network node.
     * @see ServiceSpecification.name
     */
    val name: String = specification.name

    /**
     * Type of a network node.
     */
    enum class NodeType {
        /**
         * A network node representing a Docker container.
         */
        CONTAINER,

        /**
         * A network node representing the Test Runner.
         */
        TEST_RUNNER,

        /**
         * A network node representing a Mock Server.
         */
        MOCK_SERVER,

        /**
         * A network node representing a network gateway.
         */
        NETWORK_GATEWAY,
    }
}
