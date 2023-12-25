package org.jholsten.me2e.utils

import java.net.ServerSocket

internal class PortUtils {
    companion object {
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
    }
}
