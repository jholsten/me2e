package org.jholsten.me2e

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.mock.MockServerManager
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.utils.logger

/**
 * Service for managing the test environment.
 *
 * Stores references to all containers and Mock Servers. For this purpose, the [Me2eTestConfig] annotation defined
 * in this project is first searched for and the configuration file specified in [Me2eTestConfig.config] is parsed.
 */
internal class Me2eTestEnvironmentManager {
    companion object {
        private val logger = logger<Me2eTestEnvironmentManager>()

        /**
         * Configuration annotation that is used to configure the tests.
         */
        @get:JvmSynthetic
        val configAnnotation: Me2eTestConfig by lazy {
            Me2eTestConfigScanner.findFirstTestConfigAnnotation()
        }

        /**
         * Parsed test configuration.
         */
        @get:JvmSynthetic
        val config: TestConfig by lazy {
            configAnnotation.format.parser.parseFile(configAnnotation.config)
        }

        /**
         * Container manager instance responsible for managing the containers.
         */
        @get:JvmSynthetic
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
        @get:JvmSynthetic
        val mockServerManager: MockServerManager by lazy {
            MockServerManager(
                mockServers = config.environment.mockServers,
                mockServerConfig = config.settings.mockServers,
            )
        }

        /**
         * Current status of the test environment.
         * Will be set to [EnvironmentStatus.RUNNING] if Docker-Compose was started successfully and all containers are healthy.
         * In case the Docker-Compose did not start successfully, the status is set to [EnvironmentStatus.FAILED].
         */
        @JvmSynthetic
        var status: EnvironmentStatus = EnvironmentStatus.NOT_STARTED

        /**
         * Starts the test environment including all containers and Mock Servers.
         * In case of errors, all subsequent test will be aborted since the environment is a prerequisite for all tests.
         * @see Me2eExtension.beforeAll
         */
        @JvmSynthetic
        fun startTestEnvironment() {
            logger.info("Starting test environment...")
            status = try {
                mockServerManager.start()
                containerManager.start()
                EnvironmentStatus.RUNNING
            } catch (e: Exception) {
                logger.error("Unable to start the test environment and therefore cannot execute any End-to-End tests. Reason:", e)
                EnvironmentStatus.FAILED
            }
        }

        /**
         * Stops the test environment including all containers and Mock Servers.
         */
        @JvmSynthetic
        fun stopTestEnvironment() {
            if (status != EnvironmentStatus.RUNNING) return
            logger.info("Stopping test environment...")
            try {
                containerManager.stop()
            } catch (e: Exception) {
                logger.warn("Unable to stop Docker-Compose:", e)
            }
            try {
                if (mockServerManager.isRunning) {
                    mockServerManager.stop()
                }
            } catch (e: Exception) {
                logger.warn("Unable to stop Mock Server:", e)
            }
        }
    }

    /**
     * Status of the test environment.
     */
    enum class EnvironmentStatus {
        /**
         * Status indicating that the environment has not been started yet.
         */
        NOT_STARTED,

        /**
         * Status indicating that the environment has successfully been started.
         */
        RUNNING,

        /**
         * Status indicating that starting the environment has failed.
         */
        FAILED,
    }
}
