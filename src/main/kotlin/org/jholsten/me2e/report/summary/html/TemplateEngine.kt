package org.jholsten.me2e.report.summary.html

import org.jholsten.me2e.report.summary.html.data.TemplateData
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

/**
 * Engine which fills HTML templates using Thymeleaf.
 * TODO: https://stackoverflow.com/questions/75954551/how-to-use-javax-mail-with-thymeleaf
 */
class TemplateEngine(
    /**
     * Path to the Thymeleaf template to be filled.
     * Needs to be located in `resources` folder.
     */
    private val template: String,

    /**
     * Data to be inserted into the Thymeleaf template.
     */
    private val data: TemplateData,

    /**
     * Path where the generated HTML file is to be stored.
     */
    private val outputPath: String,
) {
    companion object {
        /**
         * Template resolver which loads the [template] from the `resources` folder.
         */
        private val resolver: ClassLoaderTemplateResolver = ClassLoaderTemplateResolver()

        /**
         * Thymeleaf engine which processes the template.
         */
        private val engine: TemplateEngine = TemplateEngine()

        init {
            engine.setTemplateResolver(resolver)
        }
    }

    /**
     * Processes the [template] by filling it with the provided [data].
     * Stores the generated HTML at [outputPath].
     */
    fun process() {
        val htmlContent = engine.process(template, data.context)
        // TODO: Store as file
    }
}
