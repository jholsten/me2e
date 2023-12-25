package org.jholsten.me2e.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ServerSocket


inline fun <reified T> logger(from: T): Logger {
    return LoggerFactory.getLogger(T::class.java)
}

/**
 * Returns whether the given [port] is available on localhost and can be used for binding.
 */
fun isPortAvailable(port: Int): Boolean {
    return try {
        ServerSocket(port).close()
        true
    } catch (e: Exception) {
        false
    }
}
