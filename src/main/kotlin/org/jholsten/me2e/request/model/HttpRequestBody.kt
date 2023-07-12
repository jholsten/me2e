package org.jholsten.me2e.request.model

import java.io.File

class HttpRequestBody<T> {
    private val stringContent: String?
    private val fileContent: File?
    private val contentType: MediaType
    
    constructor(content: String, contentType: MediaType) {
        this.stringContent = content
        this.fileContent = null
        this.contentType = contentType
    }
    
    constructor(content: File, contentType: MediaType) {
        this.fileContent = content
        this.stringContent = null
        this.contentType = contentType
    }
}
