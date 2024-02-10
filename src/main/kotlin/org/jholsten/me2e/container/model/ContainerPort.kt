package org.jholsten.me2e.container.model

/**
 * Representation of a port mapping from container-internal port
 * to port that is accessible from localhost.
 */
data class ContainerPort internal constructor(
    /**
     * Container-internal port to be exposed.
     */
    val internal: Int,

    /**
     * Port from which container is accessible from localhost.
     * This value is assigned automatically as soon as the container is started.
     */
    var external: Int? = null,
)
