package org.jholsten.me2e.report.tracing.model

import org.jholsten.me2e.report.logs.model.ServiceSpecification
import java.util.UUID

/**
 * Specification of one node in a network.
 * This may either be a service, network gateway or a Mock Server.
 */
class NetworkNodeSpecification internal constructor(
    val nodeType: NodeType,
    val ipAddress: String,
    specification: ServiceSpecification,
) {
    val id: UUID = specification.id
    val color: String = specification.color
    val name: String = specification.name

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
