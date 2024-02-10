package org.jholsten.me2e.container.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.parser.deserializer.ContainerPortListDeserializer

/**
 * Representation of the port mappings of a container.
 */
@JsonDeserialize(using = ContainerPortListDeserializer::class)
class ContainerPortList internal constructor(ports: List<ContainerPort> = listOf()) : ArrayList<ContainerPort>(ports) {
    /**
     * Returns the first [ContainerPort] instance for which the internal port is equal to the given [port]
     * or `null`, if no such instance exists in this list.
     */
    fun findByInternalPort(port: Int): ContainerPort? {
        return this.firstOrNull { it.internal == port }
    }

    /**
     * Returns the first [ContainerPort] instance for which an external port is set or `null`, if no such
     * instance exists in this list.
     */
    fun findFirstExposed(): ContainerPort? {
        return this.firstOrNull { it.external != null }
    }
}
