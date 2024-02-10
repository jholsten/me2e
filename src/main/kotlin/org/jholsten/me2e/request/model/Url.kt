package org.jholsten.me2e.request.model

import okhttp3.HttpUrl
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
     * Host address part of this URL, e.g. `example.com` for a URL with [value] `https://example.com/search?q=xyz#p=42`.
     */
    val host: String

    /**
     * Path part of this URL, e.g. `/search` for a URL with [value] `https://example.com/search?q=xyz#p=42`.
     */
    val path: String

    /**
     * Query parameters of this URL as map of key and list of parameter values,
     * e.g. `{"q": ["xyz"]}` for a URL with [value] `https://example.com/search?q=xyz#p=42`.
     */
    val queryParameters: Map<String, List<String?>>

    /**
     * Fragment of this URL, e.g. `p=42` for a URL with [value] `https://example.com/search?q=xyz#p=42`.
     */
    val fragment: String?

    init {
        val url = assertUrlIsValid()
        host = url.host
        path = url.encodedPath
        queryParameters = url.queryParameterNames.associateWith {
            url.queryParameterValues(it)
        }
        fragment = url.fragment
    }

    /**
     * Returns a new [Builder] instance initialized with the URL in [value].
     * This allows to create new instances with partly modified properties.
     */
    fun newBuilder(): Builder {
        return Builder(this)
    }

    /**
     * Appends the given [relativeUrl] to this base URL.
     * @return New instance with the absolute URL.
     * @throws IllegalArgumentException if format of generated absolute URL is invalid
     */
    fun withRelativeUrl(relativeUrl: RelativeUrl): Url {
        return Url(StringUtils.stripEnd(this.toString(), "/") + relativeUrl.toString())
    }

    class Builder() {
        private var scheme: Scheme? = null
        private var host: String? = null
        private var port: Int? = null
        private val relativeUrlBuilder = RelativeUrl.Builder()

        internal constructor(httpUrl: Url) : this() {
            val url = requireNotNull(httpUrl.value.toHttpUrlOrNull()) { "Invalid URL format: ${httpUrl.value}" }
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
         * @param scheme of the URL to set.
         * @return This builder instance, to use for chaining.
         */
        fun withScheme(scheme: Scheme): Builder = apply {
            this.scheme = scheme
        }

        /**
         * Sets the given [host] for this URL.
         * @param host Host of the URL to set.
         * @return This builder instance, to use for chaining.
         */
        fun withHost(host: String): Builder = apply {
            this.host = host
        }

        /**
         * Sets the given [port] to use to connect to the web server.
         * This only needs to be set if it differs from the default HTTP (80) or HTTPS (443) port.
         * @param port Port of the URL to set.
         * @return This builder instance, to use for chaining.
         */
        fun withPort(port: Int): Builder = apply {
            require(port in 1..65535) { "Port number needs to be between 1 and 65535" }
            this.port = port
        }

        /**
         * Sets the given [path] for this URL.
         * Examples:
         * - `/search`
         * - `/account/groups`
         * @param path Path of the URL to set.
         * @return This builder instance, to use for chaining.
         */
        fun withPath(path: String): Builder = apply {
            this.relativeUrlBuilder.withPath(path)
        }

        /**
         * Sets the given list of values as query parameters for the given [key].
         * Overwrites all values which were previously set for this [key].
         * @param key Key of the query parameter to add or update.
         * @param values Values of the query parameter to set.
         * @return This builder instance, to use for chaining.
         */
        fun withQueryParameter(key: String, values: List<String>): Builder = apply {
            this.relativeUrlBuilder.withQueryParameter(key, values)
        }

        /**
         * Adds the given query parameter value to the list of values for the given [key].
         * @param key Key of the query parameter to add or update.
         * @param value Value to add for the query parameter with the given key.
         * @return This builder instance, to use for chaining.
         */
        fun withQueryParameter(key: String, value: String): Builder = apply {
            this.relativeUrlBuilder.withQueryParameter(key, value)
        }

        /**
         * Sets the given [fragment] for this URL.
         * @return This builder instance, to use for chaining.
         */
        fun withFragment(fragment: String): Builder = apply {
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

    private fun assertUrlIsValid(): HttpUrl {
        val url = value.toHttpUrlOrNull()
        requireNotNull(url) { "Invalid URL format: $value" }
        return url
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

    /**
     * Scheme of a URL. Only `http` and `https` are supported in this context.
     */
    enum class Scheme {
        HTTP, HTTPS;

        companion object {
            /**
             * Parses the given value to an instance of this enum.
             * @throws IllegalArgumentException if value is unknown.
             */
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
