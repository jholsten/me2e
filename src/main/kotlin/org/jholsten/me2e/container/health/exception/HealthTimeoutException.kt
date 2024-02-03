package org.jholsten.me2e.container.health.exception

/**
 * Exception which occurs when a service does not become healthy within the specified timeout.
 */
class HealthTimeoutException(message: String) : RuntimeException(message)
