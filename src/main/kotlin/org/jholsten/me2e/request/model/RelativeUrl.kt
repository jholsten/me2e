package org.jholsten.me2e.request.model

import org.apache.commons.lang3.StringUtils

/**
 * Model representing the relative part of a URL. Includes path, query parameters and fragment.
 * @sample org.jholsten.samples.request.relativeUrl
 * @sample org.jholsten.samples.request.relativeUrlWithBuilder
 * @constructor Instantiates a new relative url instance. Appends leading slash automatically.
 * To set the properties of the URL individually, use the [Builder] instead.
 * @param value Relative URL that he instance should represent.
 */
class RelativeUrl(
    /**
     * Relative URL that this instance represents.
     * If the relative URL contains a path (i.e. it does not start with `?` or `#`),
     * a leading slash is automatically prepended on initialization.
     */
    value: String,
) {
    /**
     * Relative URL that this instance represents.
     */
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

    /**
     * Builder for instantiating instances of [RelativeUrl].
     * @constructor Instantiates a new builder instance for constructing a [RelativeUrl].
     */
    class Builder {
        private var path: String? = null
        private var queryParameters: MutableMap<String, MutableList<String>> = mutableMapOf()
        private var fragment: String? = null

        /**
         * Sets the given [path] for this URL.
         * Examples:
         * - `/search`
         * - `/account/groups`
         * The leading slash of the path is optional, as it is automatically prepended upon initialization.
         * @param path Path to set for the URL.
         * @return This builder instance, to use for chaining.
         */
        fun withPath(path: String): Builder = apply {
            this.path = path
        }

        /**
         * Sets the given list of values as query parameters for the given [key].
         * Overwrites all values which were previously set for this [key].
         * @param key Key of the query parameter to add or update.
         * @param values Values of the query parameter to set.
         * @return This builder instance, to use for chaining.
         */
        fun withQueryParameter(key: String, values: List<String>): Builder = apply {
            this.queryParameters[key] = values.toMutableList()
        }

        /**
         * Adds the given query parameter value to the list of values for the given [key].
         * @param key Key of the query parameter to add or update.
         * @param value Value to add for the query parameter with the given key.
         * @return This builder instance, to use for chaining.
         */
        fun withQueryParameter(key: String, value: String): Builder = apply {
            if (queryParameters.containsKey(key)) {
                this.queryParameters[key]!!.add(value)
            } else {
                this.queryParameters[key] = mutableListOf(value)
            }
        }

        /**
         * Sets the given [fragment] for this URL.
         * @param fragment Fragment to set for this URL.
         * @return This builder instance, to use for chaining.
         */
        fun withFragment(fragment: String?): Builder = apply {
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
