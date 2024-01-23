package org.jholsten.me2e.report.logs.model

import org.jholsten.me2e.utils.ColorIterator
import java.util.UUID

/**
 * Specification of one container instance, used for representation.
 */
data class ServiceSpecification(
    /**
     * Unique identifier of the container instance.
     */
    val id: UUID = UUID.randomUUID(),

    /**
     * Name of the container.
     * @see org.jholsten.me2e.container.Container.name
     */
    val name: String,
) {
    /**
     * Color representing this container as Hex value.
     */
    val color: String = "#" + Integer.toHexString(colorIterator.next().rgb).substring(2)

    companion object {
        private val colorIterator: ColorIterator = ColorIterator()
    }
}
