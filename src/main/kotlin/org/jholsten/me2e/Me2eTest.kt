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
 * When initializing this class, i.e. when starting a test in a class that inherits from [Me2eTest],
 * the test environment including all containers and Mock Servers are started and the services are
 * injected into the fields annotated with [org.jholsten.me2e.container.injection.InjectService].
 * For this purpose, the [Me2eTestConfig] annotation defined in this project is first searched for
 * and the configuration file specified in [Me2eTestConfig.config] is parsed. The containers and
 * Mock Servers are then started using the [ContainerManager] and [MockServerManager].
 *
 * The environment is started once when the first test class, which inherits from [Me2eTest], is
 * initialized. The instantiated environment is then reused for all subsequent test classes.
 * After all tests have been executed, the containers and Mock Servers are automatically shut down.
 *
 * To invoke functions on the containers and Mock Servers, use [org.jholsten.me2e.container.injection.InjectService].
 * You may also use the [containerManager] and [mockServerManager] referenced in this class to
 * execute functions for managing the environment.
 * @see org.jholsten.me2e.container.injection.InjectService
 * @see Me2eTestConfig
 * @see ContainerManager
 * @see MockServerManager
 * @sample org.jholsten.samples.Me2eTestSample
 * @constructor Instantiates a new [Me2eTest] instance. Injects services into the fields annotated with
 * [org.jholsten.me2e.container.injection.InjectService].
 */
@ExtendWith(Me2eAssertHealthyExtension::class, Me2eStateResetExtension::class)
open class Me2eTest {
    companion object {
        private val logger = logger<Me2eTest>()

        /**
         * Configuration annotation that is used to configure the tests.
         * @throws RuntimeException if [Me2eTestConfig] annotation is not defined in the current project.
         */
        @get:JvmStatic
        val configAnnotation: Me2eTestConfig by lazy {
            Me2eTestConfigStorage.configAnnotation ?: throw RuntimeException("Unable to find Me2eTestConfig annotation.")
        }

        /**
         * Parsed test configuration.
         * @throws RuntimeException if [Me2eTestConfig] annotation is not defined in the current project.
         */
        @get:JvmStatic
        val config: TestConfig by lazy {
            Me2eTestConfigStorage.config ?: throw RuntimeException("Unable to find Me2eTestConfig annotation.")
        }

        /**
         * Container manager instance responsible for managing the containers.
         */
        @get:JvmStatic
        val containerManager: ContainerManager by lazy {
            ContainerManager(
                dockerComposeFile = FileUtils.getResourceAsFile(config.environment.dockerCompose),
                dockerConfig = config.settings.docker,
                containers = config.environment.containers,
            )
        }

        /**
         * Mock Server manager instance responsible for managing the Mock Servers.
         */
        @get:JvmStatic
        val mockServerManager: MockServerManager by lazy {
            MockServerManager(
                mockServers = config.environment.mockServers,
                mockServerConfig = config.settings.mockServers,
            )
        }

        init {
            try {
                mockServerManager.start()
                containerManager.start()
            } catch (e: Exception) {
                logger.error("Unable to start environment.", e)
            }
        }
    }

    init {
        @Suppress("LeakingThis")
        InjectionUtils(this).injectServices()
    }
}
