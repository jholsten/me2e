package org.jholsten.me2e.request.model

import okhttp3.internal.toImmutableList
import okhttp3.internal.toImmutableMap

/**
 * Representation of HTTP headers of an HTTP request or an HTTP response.
 */
class HttpHeaders internal constructor(
    private val values: Map<String, List<String>>
) : Iterable<Pair<String, List<String>>> {

    companion object {
        /**
         * Returns empty HTTP headers.
         */
        @JvmStatic
        fun empty(): HttpHeaders {
            return HttpHeaders(mapOf())
        }
    }

    /**
     * Returns the number of HTTP headers.
     */
    val size: Int
        get() = values.flatMap { it.value }.size

    /**
     * Returns values of the HTTP headers with the given [key] or `null`, if
     * header with the given key does not exist.
     */
    operator fun get(key: String): List<String>? {
        return values[key]
    }

    /**
     * Returns whether an HTTP header with the given [key] exists.
     */
    operator fun contains(key: String): Boolean {
        return key in values
    }

    /**
     * Returns all HTTP headers set in this instance.
     */
    val entries: Map<String, List<String>>
        get() = values

    override fun iterator(): Iterator<Pair<String, List<String>>> {
        return values.map { it.key to it.value }.iterator()
    }

    /**
     * Returns new [Builder] instance initialized with the HTTP headers of this instance.
     * This allows to create new instances with partly modified properties.
     */
    fun newBuilder(): Builder {
        return Builder(this.values)
    }

    class Builder {
        private val values: MutableMap<String, MutableList<String>>

        constructor() : this(mutableMapOf())

        internal constructor(values: Map<String, List<String>>) {
            this.values = values.map { (k, v) -> k to v.toMutableList() }.toMap().toMutableMap()
        }

        /**
         * Adds an HTTP header with the given [key] and the given [value].
         * @param key Key of the header to add.
         * @param value Value of the header to add.
         * @return Builder instance, to use for chaining.
         */
        fun add(key: String, value: String) = apply {
            add(key, listOf(value))
        }

        /**
         * Adds HTTP headers for the given [values] with the given [key].
         * @param key Key of the headers to add.
         * @param values Values of the headers to add to the existing values.
         * @return Builder instance, to use for chaining.
         * @throws IllegalArgumentException if [key] is blank or [values] are empty.
         */
        fun add(key: String, values: List<String>) = apply {
            require(key.isNotBlank()) { "Key cannot be blank" }
            require(values.isNotEmpty()) { "List of values need to contain at least one entry" }
            if (this.values.containsKey(key)) {
                this.values[key]!!.addAll(values)
            } else {
                this.values[key] = values.toMutableList()
            }
        }

        /**
         * Removes the HTTP header for the given [key] and the given [value].
         * Does not modify the builder instance if combination of key and value does not exist.
         * @param key Key of the HTTP header to remove.
         * @param value Value of the HTTP header to remove.
         * @return Builder instance, to use for chaining.
         */
        fun remove(key: String, value: String) = apply {
            if (this.values.containsKey(key)) {
                this.values[key]!!.remove(value)
                if (this.values[key]!!.isEmpty()) {
                    remove(key)
                }
            }
        }

        /**
         * Removes all HTTP headers for the given [key].
         * Does not modify the builder instance if key does not exist.
         * @param key Key of the HTTP headers to remove.
         * @return Builder instance, to use for chaining.
         */
        fun remove(key: String) = apply {
            this.values.remove(key)
        }

        /**
         * Sets the value of the HTTP header with the given key.
         * If the key is not found, it is added. If the key is found, the existing values are replaced.
         * @param key Key of the HTTP header to set.
         * @param value Value of the HTTP header to set.
         * @return Builder instance, to use for chaining.
         */
        operator fun set(key: String, value: String) = apply {
            this.values[key] = mutableListOf(value)
        }

        /**
         * Builds an instance of the [HttpHeaders] using the properties set in this builder.
         */
        fun build(): HttpHeaders {
            val immutableValues = values.map { (key, values) -> key to values.toImmutableList() }.toMap()
            return HttpHeaders(immutableValues.toImmutableMap())
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is HttpHeaders && this.values == other.values
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }
}
