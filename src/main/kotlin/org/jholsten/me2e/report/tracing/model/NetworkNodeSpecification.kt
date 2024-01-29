package org.jholsten.me2e.report.tracing.model

import org.jholsten.me2e.report.logs.model.ServiceSpecification
import java.util.UUID

/**
 * Specification of one node in a network.
 * This may either be a service, network gateway or mock server.
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
         * A network node representing a service, i.e. a container or the test runner.
         */
        SERVICE,

        /**
         * A network node representing a mock server.
         */
        MOCK_SERVER,

        /**
         * A network node representing a network gateway.
         */
        NETWORK_GATEWAY,
    }
}
