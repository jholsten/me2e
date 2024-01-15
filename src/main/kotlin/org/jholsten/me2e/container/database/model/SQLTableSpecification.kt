package org.jholsten.me2e.container.database.model

import org.jholsten.me2e.utils.toJson

class SQLTableSpecification(
    name: String,
    val schema: String
) : TableSpecification(name = name) {
    override val representation: String
        get() = "${schema}.${name}"

    override fun toString(): String = toJson(this)

    override fun equals(other: Any?): Boolean {
        if (other is SQLTableSpecification) {
            return other.name.lowercase() == this.name.lowercase() && other.schema.lowercase() == this.schema.lowercase()
        } else if (other is TableSpecification) {
            return super.equals(other)
        }
        return false
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + schema.hashCode()
        return result
    }
}
