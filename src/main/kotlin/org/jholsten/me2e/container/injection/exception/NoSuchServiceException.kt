package org.jholsten.me2e.container.injection.exception

/**
 * Exception which indicates that a service for an attribute annotated with [org.jholsten.me2e.container.injection.InjectService]
 * could not be injected.
 */
class NoSuchServiceException(message: String) : RuntimeException(message)
