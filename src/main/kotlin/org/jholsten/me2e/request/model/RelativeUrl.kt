package org.jholsten.me2e.request.model

import org.apache.commons.lang3.StringUtils

/**
 * Model representing the relative part of a URL.
 * Includes path, query parameters and fragment.
 */
class RelativeUrl(
    /**
     * Relative URL that this instance represents.
     * If the relative URL contains a path (i.e. it does not start with `?` or `#`),
     * a leading slash is automatically prepended on initialization.
     */
    value: String,
) {
    val value: String

    init {
        if (value.isBlank()) {
            this.value = ""
        } else if (!value.startsWith("?") && !value.startsWith("#")) {
            this.value = "/${StringUtils.stripStart(value, "/")}"
        } else {
            this.value = value
        }
    }

    companion object {
        /**
         * Returns empty relative URL.
         * This means that the base URL is used for the request.
         */
        @JvmStatic
        fun empty(): RelativeUrl {
            return RelativeUrl("")
        }
    }

    class Builder {
        private var path: String? = null
        private var queryParameters: MutableMap<String, MutableList<String>> = mutableMapOf()
        private var fragment: String? = null

        /**
         * Sets the given [path] for this URL.
         * Examples:
         * - `/search`
         * - `/account/groups`
         */
        fun withPath(path: String) = apply {
            this.path = path
        }

        /**
         * Sets the given list of values as query parameters for the given [key].
         * Overwrites all values which were previously set.
         */
        fun withQueryParameter(key: String, values: List<String>) = apply {
            this.queryParameters[key] = values.toMutableList()
        }

        /**
         * Adds the given query parameter value to the list of values for the given [key].
         */
        fun withQueryParameter(key: String, value: String) = apply {
            if (queryParameters.containsKey(key)) {
                this.queryParameters[key]!!.add(value)
            } else {
                this.queryParameters[key] = mutableListOf(value)
            }
        }

        /**
         * Sets the given [fragment] for this URL.
         */
        fun withFragment(fragment: String?) = apply {
            this.fragment = fragment
        }

        /**
         * Builds an instance of the [RelativeUrl] by constructing a relative URL from the set values.
         * Examples:
         *
         * | Path           | Query Params          | Fragment  | Output                            |
         * | :------------- | :-------------------- | :-------- | :-------------------------------- |
         * | `"/search"`    | `"q": [1, 2]`         | `"p=42"`  | `"/search?q=1&q=2#p=42"`          |
         * | `"account"`    | `"id": [A]`           | `null`    | `"/account?id=A"`                 |
         * | `null`         | `null`                | `null`    | `""`                              |
         */
        fun build(): RelativeUrl {
            val stringBuilder = StringBuilder()
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
            return RelativeUrl(stringBuilder.toString())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is RelativeUrl) {
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
}
