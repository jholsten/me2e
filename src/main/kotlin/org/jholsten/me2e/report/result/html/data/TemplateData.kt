package org.jholsten.me2e.report.result.html.data

import org.thymeleaf.context.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Data to be inserted into a Thymeleaf template.
 * For representing the data for the `index.html`, use the [IndexTemplateData].
 * For representing the data for a test detail page, use the [TestDetailTemplateData].
 */
open class TemplateData(
    /**
     * Context which contains all variables that can
     * be used in the Thymeleaf template.
     */
    val context: Context,
) {

    open class Builder<SELF : Builder<SELF>> {
        protected val context: Context = Context()

        /**
         * Sets the locale to use for the Thymeleaf template.
         * In the template, the keys in `messages_$locale.properties` can then be referenced with `#{key}`.
         * @param locale Locale to use for the Thymeleaf template.
         * @return This builder instance, to use for chaining.
         * TODO: Verify
         */
        fun withLocale(locale: Locale): SELF {
            context.locale = locale
            return self()
        }

        /**
         * Sets variable with the given [key] to the given [value].
         * The value of this variable can be referenced in a Thymeleaf template, e.g. by using:
         * ```html
         * <span th:text="${key}"/>
         * <tr th:each="value: ${key}">
         *     <td th:text="${value.id}"/>
         *     <td th:text="${value.name}"/>
         * </tr>
         * ```
         * @param key Key of the variable to set.
         * @param value Value of the variable to set.
         * @return This builder instance, to use for chaining.
         */
        fun withVariable(key: String, value: Any?): SELF {
            context.setVariable(key, value)
            return self()
        }

        /**
         * Sets the timestamp of when the report was generated.
         * The formatted timestamp can be used in the template by referencing `${generationTimestamp}`.
         * @param timestamp Timestamp of when the report was generated.
         * @return This builder instance, to use for chaining.
         */
        fun withGenerationTimestamp(timestamp: Instant): SELF {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss").withZone(ZoneId.systemDefault())
            return withVariable("generationTimestamp", formatter.format(timestamp))
        }

        /**
         * Builds an instance of the [TemplateData] using the properties set in this builder.
         */
        open fun build(): TemplateData {
            return TemplateData(context)
        }

        /**
         * Returns the instantiated Builder instance.
         * For subtypes of this class, this method should use the type of the subclass as the return type.
         * Only then will it be possible to chain method invocations of this class and the subclass.
         */
        private fun self(): SELF {
            @Suppress("UNCHECKED_CAST")
            return this as SELF
        }
    }
}
