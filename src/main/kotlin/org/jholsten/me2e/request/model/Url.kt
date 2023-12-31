package org.jholsten.me2e.request.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.apache.commons.lang3.StringUtils

/**
 * Model representing an absolute URL.
 */
class Url(
    /**
     * URL that this instance represents.
     */
    val value: String,
) {
    /**
     * Returns a new [Builder] instance initialized with the URL in [value].
     */
    fun newBuilder(): Builder {
        return Builder(this)
    }

    /**
     * Appends the given [relativeUrl] to this base URL.
     * @return New instance with the absolute URL
     */
    fun withRelativeUrl(relativeUrl: RelativeUrl): Url {
        val url = StringUtils.stripEnd(this.toString(), "/") + relativeUrl.toString()

        return Url(url)
    }

    class Builder() {
        private var scheme: Scheme? = null
        private var host: String? = null
        private var port: Int? = null
        private val relativeUrlBuilder = RelativeUrl.Builder()

        internal constructor(httpUrl: Url) : this() {
            val url = httpUrl.value.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid url format")
            this.scheme = Scheme.parse(url.scheme)
            this.host = url.host
            this.port = when {
                url.port != -1 && httpUrl.value.contains(":${url.port}") -> url.port
                else -> null
            }
            relativeUrlBuilder.withPath(url.encodedPath)
            relativeUrlBuilder.withFragment(url.fragment)
            for (key in url.queryParameterNames) {
                relativeUrlBuilder.withQueryParameter(key, url.queryParameterValues(key).filterNotNull())
            }
        }

        /**
         * Sets the given URL [scheme] for this URL.
         */
        fun withScheme(scheme: Scheme) = apply {
            this.scheme = scheme
        }

        /**
         * Sets the given [host] for this URL.
         */
        fun withHost(host: String) = apply {
            this.host = host
        }

        /**
         * Sets the given [port] to use to connect to the web server.
         * This only needs to be set if it differs from the default HTTP (80) or HTTPS (443) port.
         */
        fun withPort(port: Int) = apply {
            require(port in 1..65535) { "Port number needs to be between 1 and 65535" }
            this.port = port
        }

        /**
         * Sets the given [path] for this URL.
         * Examples:
         * - `/search`
         * - `/account/groups`
         */
        fun withPath(path: String) = apply {
            this.relativeUrlBuilder.withPath(path)
        }

        /**
         * Sets the given list of values as query parameters for the given [key].
         * Overwrites all values which were previously set.
         */
        fun withQueryParameter(key: String, values: List<String>) = apply {
            this.relativeUrlBuilder.withQueryParameter(key, values)
        }

        /**
         * Adds the given query parameter value to the list of values for the given [key].
         */
        fun withQueryParameter(key: String, value: String) = apply {
            this.relativeUrlBuilder.withQueryParameter(key, value)
        }

        /**
         * Sets the given [fragment] for this URL.
         */
        fun withFragment(fragment: String) = apply {
            this.relativeUrlBuilder.withFragment(fragment)
        }

        /**
         * Builds an instance of the [Url] by constructing an absolute URL from the set values.
         */
        fun build(): Url {
            requireNotNull(scheme) { "Scheme cannot be null" }
            val stringBuilder = StringBuilder()
            stringBuilder.append("$scheme://")
            val strippedHost = host?.let { StringUtils.stripEnd(host, "/") }
            require(!strippedHost.isNullOrBlank()) { "Host cannot be null or blank" }
            stringBuilder.append(strippedHost)
            if (port != null) {
                stringBuilder.append(":$port")
            }
            return Url(stringBuilder.toString()).withRelativeUrl(relativeUrlBuilder.build())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Url) {
            return this.value == other.value
        }
        return false
    }

    override fun toString(): String {
        return this.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    enum class Scheme {
        HTTP, HTTPS;

        companion object {
            @JvmStatic
            fun parse(value: String): Scheme {
                return when (value.lowercase()) {
                    "http" -> HTTP
                    "https" -> HTTPS
                    else -> throw IllegalArgumentException("Unknown scheme $value")
                }
            }
        }

        override fun toString(): String {
            return when (this) {
                HTTP -> "http"
                HTTPS -> "https"
            }
        }
    }
}
