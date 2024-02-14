package org.jholsten.me2e

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.container.injection.InjectionUtils
import org.jholsten.me2e.mock.MockServerManager
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.utils.logger
import org.junit.jupiter.api.extension.ExtendWith


/**
 * Base class for the definition of ME2E-Tests.
 *
 * To invoke functions on the containers and Mock Servers, use [org.jholsten.me2e.container.injection.InjectService].
 * When initializing this class, i.e. when starting a test in a class that inherits from [Me2eTest], the
 * services are injected into the fields annotated with [org.jholsten.me2e.container.injection.InjectService].
 * You may also use the [containerManager] and [mockServerManager] referenced in this class to execute functions
 * for managing the environment.
 *
 * The test environment is started by the [Me2eTestExecutionListener] when the test execution starts, i.e. before
 * any test or test class is initialized or executed. The instantiated environment is then reused for all test classes.
 * After all tests have been executed, the containers and Mock Servers are automatically shut down.
 * @see org.jholsten.me2e.container.injection.InjectService
 * @see Me2eTestConfig
 * @see ContainerManager
 * @see MockServerManager
 * @sample org.jholsten.samples.Me2eTestSample
 * @constructor Instantiates a new [Me2eTest] instance. Injects services into the fields annotated with
 * [org.jholsten.me2e.container.injection.InjectService].
 */
@ExtendWith(Me2eExtension::class)
open class Me2eTest {
    companion object {
        /**
         * Configuration annotation that is used to configure the tests.
         */
        @get:JvmStatic
        val configAnnotation: Me2eTestConfig by lazy {
            Me2eTestEnvironmentManager.configAnnotation
        }

        /**
         * Parsed test configuration.
         */
        @get:JvmStatic
        val config: TestConfig by lazy {
            Me2eTestEnvironmentManager.config
        }

        /**
         * Container manager instance responsible for managing the containers.
         */
        @get:JvmStatic
        val containerManager: ContainerManager by lazy {
            Me2eTestEnvironmentManager.containerManager
        }

        /**
         * Mock Server manager instance responsible for managing the Mock Servers.
         */
        @get:JvmStatic
        val mockServerManager: MockServerManager by lazy {
            Me2eTestEnvironmentManager.mockServerManager
        }
    }

    init {
        @Suppress("LeakingThis")
        InjectionUtils(this).injectServices()
    }
}
