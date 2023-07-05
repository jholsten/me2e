package org.jholsten.me2e.request.model

import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Request

/**
 * Model representing an HTTP response.
 */
class HttpResponse(
    /**
     * TODO
     */
    private val request: Request,
    private val protocol: Protocol,
    private val message: String,
    private val code: Int,
    private val handshake: Handshake?,
    
) {
}
