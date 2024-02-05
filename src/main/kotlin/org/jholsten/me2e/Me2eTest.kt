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
 */
@ExtendWith(Me2eAssertHealthyExtension::class, Me2eRequestInterceptorResetExtension::class)
open class Me2eTest {
    companion object {
        private val logger = logger(this)

        /**
         * Configuration annotation that is used to configure the tests.
         */
        @get:JvmStatic
        val configAnnotation: Me2eTestConfig by lazy {
            Me2eTestConfigStorage.configAnnotation ?: throw RuntimeException("Unable to find Me2eTestConfig annotation.")
        }

        /**
         * Parsed test configuration.
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
         * Mock server manager instance responsible for managing the mock servers.
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
