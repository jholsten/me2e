package org.jholsten.me2e.container.microservice

import com.fasterxml.jackson.annotation.JacksonInject
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Model representing one Microservice container.
 * In this context, a Microservice is expected to offer a REST API.
 */
class MicroserviceContainer(
    name: String,
    image: String,
    environment: Map<String, String>? = null,
    public: Boolean = false,
    ports: ContainerPortList = ContainerPortList(),
    @JacksonInject("requestConfig")
    requestConfig: RequestConfig,
) : Container(
    name = name,
    image = image,
    type = ContainerType.MICROSERVICE,
    environment = environment,
    public = public,
    ports = ports,
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
