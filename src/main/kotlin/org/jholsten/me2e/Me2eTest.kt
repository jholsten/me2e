package org.jholsten.me2e

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.container.injection.InjectionUtils
import org.jholsten.me2e.mock.MockServerManager
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.report.summary.ReportDataAggregator


/**
 * Base class for the definition of ME2E-Tests.
 */
open class Me2eTest {
    companion object {
        /**
         * Configuration annotation that is used to configure the tests.
         */
        @get:JvmStatic
        val configAnnotation: Me2eTestConfig by lazy {
            Me2eTestConfigScanner.findFirstTestConfigAnnotation()
        }

        /**
         * Parsed test configuration.
         */
        @get:JvmStatic
        val config: TestConfig by lazy {
            configAnnotation.format.parser.parseFile(configAnnotation.config)
        }

        /**
         * Container manager instance responsible for managing the containers.
         */
        @get:JvmStatic
        val containerManager: ContainerManager by lazy {
            ContainerManager(
                dockerComposeFile = FileUtils.getResourceAsFile(config.environment.dockerCompose),
                dockerConfig = config.docker,
                containers = config.environment.containers,
            )
        }

        /**
         * Mock server manager instance responsible for managing the mock servers.
         */
        @get:JvmStatic
        val mockServerManager: MockServerManager by lazy {
            MockServerManager(mockServers = config.environment.mockServers)
        }

        init {
            mockServerManager.start()
            containerManager.start()
            ReportDataAggregator.initializeOnContainersStarted(config.environment.containers.values)
        }
    }

    init {
        @Suppress("LeakingThis")
        InjectionUtils(this).injectServices()
    }
}
