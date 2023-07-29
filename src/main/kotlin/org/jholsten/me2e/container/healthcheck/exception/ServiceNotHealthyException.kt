package org.jholsten.me2e.container.healthcheck.exception

/**
 * Exception that occurs when a service is not healthy within the specified timespan.
 */
class ServiceNotHealthyException(message: String) : RuntimeException(message)
