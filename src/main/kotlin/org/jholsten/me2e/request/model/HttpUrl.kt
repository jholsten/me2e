package org.jholsten.me2e.request.model

import org.apache.commons.lang3.StringUtils

/**
 *
 */
class HttpUrl(
    val value: String,
) {
    class Builder {
        private var scheme: Scheme? = null
        private var host: String? = null
        private var port: Int? = null
        private var path: String? = null
        private var queryParameters: MutableMap<String, MutableList<String>> = mutableMapOf()
        private var fragment: String? = null

        fun withScheme(scheme: Scheme) = apply {
            this.scheme = scheme
        }

        fun withHost(host: String) = apply {
            this.host = host
        }

        fun withPort(port: Int) = apply {
            require(port in 1..65535) { "Port number needs to be between 1 and 65535" }
            this.port = port
        }

        fun withPath(path: String) = apply {
            this.path = path
        }

        fun withQueryParameter(key: String, values: List<String>) = apply {
            this.queryParameters[key] = values.toMutableList()
        }

        fun withQueryParameter(key: String, value: String) = apply {
            if (queryParameters.containsKey(key)) {
                this.queryParameters[key]!!.add(value)
            } else {
                this.queryParameters[key] = mutableListOf(value)
            }
        }

        fun withFragment(fragment: String) = apply {
            this.fragment = fragment
        }

        fun build(): HttpUrl {
            requireNotNull(scheme) { "Scheme cannot be null" }
            requireNotNull(host) { "Host cannot be null" }
            val stringBuilder = StringBuilder()
            stringBuilder.append("$scheme://")
            stringBuilder.append(host)
            if (port != null) {
                stringBuilder.append(":$port")
            }
            val strippedPath = path?.let { StringUtils.stripStart(path, "/") }
            if (!strippedPath.isNullOrBlank()) {
                stringBuilder.append("/$strippedPath")
            }
            if (queryParameters.isNotEmpty()) {
                stringBuilder.append("?")
                val queryParameterList = queryParameters.flatMap { (key, values) ->
                    MutableList(values.size) { key }.zip(values)
                }
                for ((index, queryParamPair) in queryParameterList.withIndex()) {
                    if (index > 0) {
                        stringBuilder.append("&")
                    }
                    val (key, value) = queryParamPair
                    stringBuilder.append("$key=$value")
                }
            }
            if (fragment != null) {
                stringBuilder.append("#$fragment")
            }
            return HttpUrl(stringBuilder.toString())
        }
    }

    enum class Scheme {
        HTTP, HTTPS;

        override fun toString(): String {
            return when (this) {
                HTTP -> "http"
                HTTPS -> "https"
            }
        }
    }
}
