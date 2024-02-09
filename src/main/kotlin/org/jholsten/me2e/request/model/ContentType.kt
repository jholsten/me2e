package org.jholsten.me2e.request.model

import okhttp3.MediaType.Companion.toMediaTypeOrNull

/**
 * Type to describe the content type of an HTTP request or response body.
 */
class ContentType(
    /**
     * Value of the content type, as specified in RFC 2045.
     */
    val value: String,
) {
    companion object {
        private const val TOKEN = "[0-9A-Za-z!#\$%&'*+.^_`|~-]+"
        private val TYPE_REGEX = Regex("($TOKEN)/($TOKEN)")

        /**
         * Content type for JSON data, encoded with charset UTF-8.
         */
        @JvmField
        val JSON_UTF8 = ContentType("application/json; charset=utf-8")

        /**
         * Content type for plain text data, encoded with charset UTF-8.
         */
        @JvmField
        val TEXT_PLAIN_UTF8 = ContentType("text/plain; charset=utf-8")
    }

    /**
     * High-level media type, such as `text` or `application`.
     */
    val type: String

    /**
     * Specific media subtype, such as `plain`, `png` or `xml`.
     */
    val subtype: String

    init {
        assertContentTypeIsValid()
        val match = TYPE_REGEX.find(value)
        requireNotNull(match) { "Invalid media type \"$value\"" }
        this.type = match.groupValues[1]
        this.subtype = match.groupValues[2]
    }

    /**
     * Returns type and subtype without additional parameters (e.g. `"application/json"`).
     */
    fun withoutParameters(): String {
        return "$type/$subtype"
    }

    /**
     * Returns whether this media type should be interpreted as a string.
     * This includes all media types of type `"text"`, as well as `application/json` and `application/xml`.
     * In any other case, the value is represented as a byte string or file.
     */
    fun isStringType(): Boolean {
        return type.lowercase() == "text" || listOf("application/json", "application/xml").contains(withoutParameters().lowercase())
    }

    private fun assertContentTypeIsValid() {
        requireNotNull(value.toMediaTypeOrNull()) { "Invalid Content Type format: $value" }
    }
}
