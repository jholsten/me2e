package org.jholsten.me2e.container.database.model

import org.jholsten.me2e.utils.toJson

open class TableSpecification(
    /**
     * Name of the table.
     */
    val name: String
) {
    /**
     * Representation of this table to use for executing commands.
     */
    open val representation: String
        get() = name

    override fun toString(): String = toJson(this)

    override fun equals(other: Any?): Boolean {
        if (other is TableSpecification) {
            return other.name.lowercase() == this.name.lowercase()
        }
        return false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
