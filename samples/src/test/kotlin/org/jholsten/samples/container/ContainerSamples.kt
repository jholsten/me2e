package org.jholsten.samples.container

import org.jholsten.me2e.container.events.ContainerEventConsumer
import org.jholsten.me2e.container.events.model.ContainerEvent
import org.jholsten.me2e.container.logging.ContainerLogConsumer
import org.jholsten.me2e.container.logging.model.ContainerLogEntry
import org.jholsten.me2e.container.microservice.authentication.Authenticator
import org.jholsten.me2e.container.stats.ContainerStatsConsumer
import org.jholsten.me2e.container.stats.model.ContainerStatsEntry
import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.model.HttpResponse
import org.slf4j.LoggerFactory

/**
 * Sample of a [ContainerEventConsumer] which simply logs all incoming events.
 */
class LoggingContainerEventConsumer : ContainerEventConsumer() {
    private val logger = LoggerFactory.getLogger(LoggingContainerEventConsumer::class.java)

    override fun accept(event: ContainerEvent) {
        logger.info("Received new event: $event")
    }
}

/**
 * Sample of a [ContainerLogConsumer] which simply logs all logs received by the container.
 */
class LoggingContainerLogConsumer : ContainerLogConsumer() {
    private val logger = LoggerFactory.getLogger(LoggingContainerLogConsumer::class.java)

    override fun accept(entry: ContainerLogEntry) {
        logger.info("Received new log entry: $entry")
    }
}

/**
 * Sample of a [ContainerStatsConsumer] which simply logs all stats received by the container.
 */
class LoggingContainerStatsConsumer : ContainerStatsConsumer() {
    private val logger = LoggerFactory.getLogger(LoggingContainerStatsConsumer::class.java)

    override fun accept(entry: ContainerStatsEntry) {
        logger.info("Received new stats entry: $entry")
    }
}

/**
 * Sample of an [Authenticator] which uses an API key for authentication.
 */
class ApiKeyAuthenticator : Authenticator() {
    override fun getRequestInterceptor(): RequestInterceptor {
        return object : RequestInterceptor {
            override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                val request = chain.getRequest().newBuilder()
                    .addHeader("X-API-KEY", "secret")
                    .build()
                return chain.proceed(request)
            }
        }
    }
}
