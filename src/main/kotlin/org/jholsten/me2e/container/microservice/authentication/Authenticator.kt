package org.jholsten.me2e.container.microservice.authentication

import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.request.interceptor.RequestInterceptor

/**
 * Base class for authenticating HTTP requests towards a [org.jholsten.me2e.container.microservice.MicroserviceContainer].
 * For authentication, the request interceptor returned by [getRequestInterceptor] is used that sets the required
 * authentication information in the HTTP request before each request. This could be a Bearer token, for example, which
 * is requested from an authentication server and set in the `Authorization` header of the request.
 * While the [RequestInterceptor.intercept] method is executed for every request, the [initialize] method is only executed
 * once when [org.jholsten.me2e.container.microservice.MicroserviceContainer.authenticate] is called.
 * @sample org.jholsten.samples.container.ApiKeyAuthenticator
 * @see UsernamePasswordAuthentication
 * @constructor Instantiates a new authenticator.
 */
abstract class Authenticator {

    /**
     * Method that can be used to initialize the authenticator. Is executed once when
     * [org.jholsten.me2e.container.microservice.MicroserviceContainer.authenticate] is called.
     * @param microservice Microservice instance to which the authentication should be applied.
     */
    open fun initialize(microservice: MicroserviceContainer) {}

    /**
     * Request interceptor to set for all requests towards the [org.jholsten.me2e.container.microservice.MicroserviceContainer].
     * Should set the required authentication information in the corresponding request header.
     */
    abstract fun getRequestInterceptor(): RequestInterceptor
}
