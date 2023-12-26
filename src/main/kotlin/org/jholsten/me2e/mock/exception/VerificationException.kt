package org.jholsten.me2e.mock.exception

/**
 * Exception that occurs when a verification was not successful.
 */
class VerificationException(message: String) : AssertionError(message)
