package org.jholsten.me2e.report.generator.html

import org.apache.commons.io.FileUtils
import org.jholsten.me2e.report.generator.html.data.TemplateData
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.io.File
import java.nio.charset.Charset

/**
 * Engine which fills HTML templates using Thymeleaf.
 * @constructor Instantiates a new Thymeleaf engine.
 * @param template Path to the Thymeleaf template to be filled.
 * @param data Data to be inserted into the Thymeleaf template.
 * @param outputPath Path where generated HTML file is to be stored.
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
        FileUtils.writeStringToFile(File(outputPath), htmlContent, Charset.forName("UTF-8"))
    }
}
