package org.jholsten.me2e.container.microservice.authentication

import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.model.HttpResponse
import java.util.Base64

/**
 * Basic authenticator, which sets username and password Base64 encoded in the `Authorization` header
 * of each HTTP request.
 */
class UsernamePasswordAuthentication(
    /**
     * Username to set in the `Authorization` header.
     */
    username: String,

    /**
     * Password to set in the `Authorization` header.
     */
    password: String,
) : Authenticator() {

    /**
     * Value of the `Authorization` header which is composed of the username and password,
     * encoded as Base64.
     */
    private val authorizationHeader: String = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    /**
     * Returns request interceptor which sets the basic authentication in the `Authorization`
     * header of each request.
     */
    override fun getRequestInterceptor(): RequestInterceptor {
        return object : RequestInterceptor {
            override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                val request = chain.getRequest().newBuilder()
                    .addHeader("Authorization", "Basic $authorizationHeader")
                    .build()
                return chain.proceed(request)
            }
        }
    }
}
