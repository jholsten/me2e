package org.jholsten.util

import kotlin.test.fail

fun assertDoesNotThrow(message: String? = null, block: () -> Unit) {
    try {
        block()
    } catch (e : Exception){
        fail(message, e)
    }
}
