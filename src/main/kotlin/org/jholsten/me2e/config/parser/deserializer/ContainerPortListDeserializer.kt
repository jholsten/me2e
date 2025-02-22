package org.jholsten.me2e.config.parser.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import org.jholsten.me2e.container.model.ContainerPort
import org.jholsten.me2e.container.model.ContainerPortList

/**
 * Custom deserializer for extracting internal container ports from list of ports specified in a Docker-Compose file.
 * @see <a href="https://docs.docker.com/compose/compose-file/compose-file-v3/#ports>Docker Documentation</a>
 */
internal class ContainerPortListDeserializer : JsonDeserializer<ContainerPortList>() {

    /**
     * Deserializes the array node containing entries for the ports of a container to an instance
     * of [ContainerPortList]. Resolves port ranges to the individual values.
     *
     * As the external ports of a container do not have to be specified in the Docker-Compose file
     * and may be set randomly, the [ContainerPort.external] ports are only set after the container
     * has been started (see [org.jholsten.me2e.container.Container.initializeOnContainerStarted]).
     * @return Deserialized list of container ports.
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ContainerPortList {
        val containerPorts = mutableListOf<Int>()
        val node = p.readValueAsTree<ArrayNode>()
        for (entry in node.elements()) {
            containerPorts.addAll(extractContainerPorts(entry.asText()))
        }
        return ContainerPortList(containerPorts.map { ContainerPort(internal = it) })
    }

    /**
     * Extracts container port from the given value which could be given in one of 3 ways:
     * - `HOST_PORT:CONTAINER_PORT_SPEC`
     * - `CONTAINER_PORT_SPEC`
     * - `IP_ADDRESS:HOST_PORT:CONTAINER_PORT_SPEC`
     * where `CONTAINER_PORT_SPEC` includes the container port and optionally the port protocol.
     * Additionally, `CONTAINER_PORT_SPEC` can be defined as a range of values.
     * Examples:
     * ```
     *   - "3000"
     *   - "3000-3005"
     *   - "8000:8000"
     *   - "127.0.0.1:5000-5010:5000-5010"
     *   - "6060:6060/udp"
     *   - "12400-12500:1240"
     * ```
     */
    private fun extractContainerPorts(value: String): List<Int> {
        val containerPortValue = extractContainerPortValue(value)
        if (containerPortValue.contains("-")) {
            return extractRangeOfContainerPorts(containerPortValue)
        }

        return listOf(containerPortValue.toInt())
    }

    /**
     * Extracts value of the container port from the given ports entry.
     * Removes protocol if specified.
     */
    private fun extractContainerPortValue(value: String): String {
        val containerPortValue = value.split(":").last()
        // Remove port protocol
        return containerPortValue.split("/").first()
    }

    /**
     * Extracts list of port numbers from a given range.
     *
     * Example:
     * ```kotlin
     * extractRangeOfContainerPorts("3002-3004")
     * // [3002, 3003, 3004]
     * ```
     */
    private fun extractRangeOfContainerPorts(containerPortValue: String): List<Int> {
        val range = containerPortValue.split("-")
        val fromValue = range[0].toInt()
        val toValue = range[1].toInt()
        return (fromValue..toValue).toList()
    }
}
