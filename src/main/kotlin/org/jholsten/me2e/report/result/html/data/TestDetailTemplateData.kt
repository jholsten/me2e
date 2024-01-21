package org.jholsten.me2e.report.result.html.data

import org.jholsten.me2e.report.result.model.TestResult
import org.thymeleaf.context.Context

class TestDetailTemplateData(context: Context) : TemplateData(context) {

    class Builder : TemplateData.Builder<Builder>() {

        /**
         * Sets variables for the data contained in the given [summary].
         * The following variables are available:
         * - TODO
         */
        fun withSummary(summary: TestSummary) = apply {

        }

        override fun build(): TestDetailTemplateData {
            return TestDetailTemplateData(super.context)
        }
    }
}
