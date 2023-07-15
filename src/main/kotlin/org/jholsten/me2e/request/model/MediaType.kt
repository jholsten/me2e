package org.jholsten.me2e.request.model

import java.util.regex.Pattern

class MediaType(
    val value: String,
) {
    companion object {
        private const val TOKEN = "[0-9A-Za-z!#\$%&'*+.^_`|~-]+"
        private val TYPE_REGEX = Pattern.compile("($TOKEN)/($TOKEN)")

        val JSON_UTF8 = MediaType("application/json; charset=utf-8")
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
        val typeMatcher = TYPE_REGEX.matcher(value)
        require(typeMatcher.find()) { "Invalid media type \"$value\"" }
        this.type = typeMatcher.group(1)
        this.subtype = typeMatcher.group(2)
    }

    /**
     * Returns type and subtype without additional parameters (e.g. `"application/json"`).
     */
    fun withoutParameters(): String {
        return "$type/$subtype"
    }

    /**
     * Returns whether this media type should be interpreted as a string.
     * This includes all media types of type `"text"`, as well as `application/json`
     * and `application/xml`. In any other case, the value is represented as a byte
     * string or file.
     */
    fun isStringType(): Boolean {
        return type.lowercase() == "text" || listOf("application/json", "application/xml").contains(withoutParameters().lowercase())
    }
}
