package org.jholsten.me2e.container.exception

/**
 * Exception to inform about any failures which occur when executing Docker or Docker-Compose commands.
 */
class DockerException(message: String, throwable: Throwable? = null) : RuntimeException(message, throwable)
