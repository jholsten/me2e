package org.jholsten.me2e.container.microservice

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.request.model.HttpResponse

class MicroserviceContainer(
    name: String,
    image: String,
    environment: Map<String, String>? = null,
): Container(
    name = name,
    image = image,
    type = ContainerType.MICROSERVICE,
    environment = environment,
) {
    
    /**
     * Sends a GET request over HTTP to this microservice.
     * TODO
     */
    fun get(relativePath: String): HttpResponse {
        // TODO
        throw NotImplementedError()
    }
}
