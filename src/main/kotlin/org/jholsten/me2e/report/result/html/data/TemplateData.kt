package org.jholsten.me2e.report.result.html.data

import org.thymeleaf.context.Context

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
         * Sets variable with the given [key] to the given [value].
         * The value of this variable can be referenced in a Thymeleaf template, e.g. by using:
         * ```html
         * <span th:text="${key}"/>
         * <tr th:each="value: ${key}">
         *     <td th:text="${value.id}"/>
         *     <td th:text="${value.name}"/>
         * </tr>
         * ```
         */
        fun withVariable(key: String, value: Any?): SELF {
            context.setVariable(key, value)
            return self()
        }

        open fun build(): TemplateData {
            return TemplateData(context)
        }

        protected open fun self(): SELF {
            @Suppress("UNCHECKED_CAST")
            return this as SELF
        }
    }
}
