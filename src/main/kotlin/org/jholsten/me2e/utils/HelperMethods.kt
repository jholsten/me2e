@file:JvmSynthetic

package org.jholsten.me2e.utils

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color


/**
 * Returns logger for the given class.
 * @param T Class for which logger is to be returned.
 * @return SLF4J logger for class [T].
 */
internal inline fun <reified T> logger(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}

/**
 * Serializes the given [obj] to JSON string.
 * @param obj Object to serialize.
 */
internal inline fun <reified T> toJson(obj: T): String {
    val writer = DeserializerFactory.getObjectMapper().writer(customPrettyPrinter)
    return writer.writeValueAsString(obj)
}

/**
 * Custom printer for serializing objects to string.
 * Adds indentation to entries in arrays.
 */
private val customPrettyPrinter: DefaultPrettyPrinter by lazy {
    val printer = DefaultPrettyPrinter()
    printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
    printer
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
