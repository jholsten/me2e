package org.jholsten.me2e.utils

import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color


/**
 * Returns logger for the class of the given object.
 */
inline fun <reified T> logger(from: T): Logger {
    return LoggerFactory.getLogger(T::class.java)
}

/**
 * Serializes the given [obj] to JSON string.
 * @param obj Object to serialize.
 */
internal inline fun <reified T> toJson(obj: T): String {
    val writer = DeserializerFactory.getObjectMapper().writerWithDefaultPrettyPrinter()
    return writer.writeValueAsString(obj)
}

/**
 * Returns Map containing all values that are instances of the specified type [T].
 */
internal inline fun <K, reified T> Map<K, *>.filterValuesIsInstance(): Map<K, T> {
    @Suppress("UNCHECKED_CAST")
    return this.filterValues { it is T } as Map<K, T>
}

/**
 * Returns the Hex value of the given color.
 */
@JvmSynthetic
internal fun Color.toHex(): String {
    return "#" + Integer.toHexString(this.rgb).substring(2)
}
