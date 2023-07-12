package org.jholsten.me2e.request.model

class MediaType(
    val value: String,
) {
    companion object {
        val JSON_UTF8 = MediaType("application/json; charset=utf-8")
    }
}
