package org.jholsten.me2e

import org.jholsten.me2e.config.model.TestConfig

/**
 * Utility class which stores the reference to the [TestConfig] to use
 * for the ME2E tests.
 */
internal class Me2eTestConfigStorage {

    companion object {
        /**
         * Configuration annotation that is used to configure the tests.
         */
        @get:JvmSynthetic
        val configAnnotation: Me2eTestConfig? by lazy {
            Me2eTestConfigScanner.findFirstTestConfigAnnotation()
        }

        /**
         * Parsed test configuration.
         */
        @get:JvmSynthetic
        val config: TestConfig? by lazy {
            configAnnotation?.let { it.format.parser.parseFile(it.config) }
        }
    }
}
