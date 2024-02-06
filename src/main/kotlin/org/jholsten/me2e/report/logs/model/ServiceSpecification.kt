package org.jholsten.me2e.report.logs.model

import org.jholsten.me2e.utils.ColorIterator
import org.jholsten.me2e.utils.toHex
import java.util.UUID

/**
 * Specification of one service instance, used for representation.
 * In this context, a service may be a [org.jholsten.me2e.container.Container] or the Test Runner for example.
 */
data class ServiceSpecification(
    /**
     * Unique identifier of the service instance.
     */
    val id: UUID = UUID.randomUUID(),

    /**
     * Name of the service.
     */
    val name: String,
) {
    /**
     * Color representing this container as Hex value. The color is set automatically using the [colorIterator].
     * This assigns a unique color to each instance, which is selected deterministically according to the order
     * of their instantiation.
     */
    val color: String = colorIterator.next().toHex()

    companion object {
        /**
         * Color iterator, which is shared by all service instances. As a result, each instance is
         * deterministically assigned a unique color.
         */
        private val colorIterator: ColorIterator = ColorIterator()
    }
}
