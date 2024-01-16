package org.jholsten.me2e.request.model

class MediaType(
    val value: String,
) {
    companion object {
        private const val TOKEN = "[0-9A-Za-z!#\$%&'*+.^_`|~-]+"
        private val TYPE_REGEX = Regex("($TOKEN)/($TOKEN)")

        @JvmStatic
        val JSON_UTF8 = MediaType("application/json; charset=utf-8")

        @JvmStatic
        val TEXT_PLAIN_UTF8 = MediaType("text/plain; charset=utf-8")
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
        val typeMatcher = TYPE_REGEX.find(value)
        requireNotNull(typeMatcher) { "Invalid media type \"$value\"" }
        this.type = typeMatcher.groupValues[1]
        this.subtype = typeMatcher.groupValues[2]
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
