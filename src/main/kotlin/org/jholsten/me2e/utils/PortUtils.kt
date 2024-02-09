package org.jholsten.me2e.utils

import java.net.ServerSocket

/**
 * Utility class for checking port availability.
 */
internal class PortUtils {
    companion object {
        /**
         * Returns whether the given [port] is available on localhost and can be used for binding.
         * @param port Port to check.
         * @return True if port is available, false otherwise.
         */
        @JvmSynthetic
        fun isPortAvailable(port: Int): Boolean {
            return try {
                ServerSocket(port).close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
