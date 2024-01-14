package org.jholsten.me2e

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.container.injection.InjectionUtils
import org.jholsten.me2e.mock.MockServerManager
import org.jholsten.me2e.parsing.utils.FileUtils

/**
 * Base class for the definition of ME2E-Tests.
 */
open class Me2eTest {
    companion object {
        /**
         * Configuration annotation that is used to configure the tests.
         */
        val configAnnotation = Me2eTestConfigScanner.findFirstTestConfigAnnotation()

        /**
         * Parsed test configuration.
         */
        val config: TestConfig = configAnnotation.format.parser.parseFile(configAnnotation.config)

        /**
         * Container manager instance responsible for managing the containers.
         */
        val containerManager = ContainerManager(
            dockerComposeFile = FileUtils.getResourceAsFile(config.environment.dockerCompose),
            dockerConfig = config.docker,
            containers = config.environment.containers,
        )

        /**
         * Mock server manager instance responsible for managing the mock servers.
         */
        val mockServerManager = MockServerManager(mockServers = config.environment.mockServers)

        init {
            containerManager.start()
            mockServerManager.start()
        }
    }

    init {
        @Suppress("LeakingThis")
        InjectionUtils(this).injectServices()
    }
}
