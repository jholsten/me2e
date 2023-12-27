package org.jholsten.me2e.utils

import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory


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
