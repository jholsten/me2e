package org.jholsten.me2e

import org.jholsten.me2e.config.model.TestConfig

/**
 * Utility class which stores the reference to the [TestConfig] to use for the ME2E tests.
 * Allows to access the test configuration also for simple unit tests that do not inherit
 * from [Me2eTest] without starting the test environment.
 */
internal class Me2eTestConfigStorage {

    companion object {
        /**
         * Configuration annotation that is used to configure the tests.
         * May be `null` if no test configuration is defined in the project.
         */
        @get:JvmSynthetic
        val configAnnotation: Me2eTestConfig? by lazy {
            Me2eTestConfigScanner.findFirstTestConfigAnnotation()
        }

        /**
         * Parsed test configuration.
         * May be `null` if no test configuration is defined in the project.
         */
        @get:JvmSynthetic
        val config: TestConfig? by lazy {
            configAnnotation?.let { it.format.parser.parseFile(it.config) }
        }
    }
}
