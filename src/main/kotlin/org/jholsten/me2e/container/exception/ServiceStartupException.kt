package org.jholsten.me2e.container.exception

/**
 * Exception that occurs when a service could not be started.
 */
class ServiceStartupException internal constructor(message: String) : RuntimeException(message)
