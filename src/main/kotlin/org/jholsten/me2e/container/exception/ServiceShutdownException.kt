package org.jholsten.me2e.container.exception

/**
 * Exception that occurs when a service could not be stopped.
 */
class ServiceShutdownException(message: String) : RuntimeException(message)
