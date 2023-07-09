package org.jholsten.me2e.manager

import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.microservice.MicroserviceContainer

/**
 * Manager for parsing the configuration and starting the containers.
 */
class Me2eTestManager private constructor(builder: Builder) {
    /**
     * Test configuration to use for the end-to-end tests.
     */
    private val config: TestConfig
    
    /**
     * List of container names defined in the test configuration.
     */
    val containerNames: List<String>
    
    /**
     * Microservices to start as map of `(containerName, container)`.
     */
    val microservices: Map<String, MicroserviceContainer>
    
    init {
        config = builder.config ?: throw IllegalArgumentException("Test configuration needs to be provided")
        containerNames = config.containers.map { it.key }
        microservices = config.containers.filterValues { it is MicroserviceContainer } as Map<String, MicroserviceContainer>
    }
    
    class Builder {
        var config: TestConfig? = null
        var format: ConfigFormat = ConfigFormat.YAML
        
        /**
         * Specify the file which contains the test configuration.
         * File needs to be located in `resources` folder.
         */
        fun withFile(filename: String, format: ConfigFormat = ConfigFormat.YAML): Builder {
            config = format.parser.parseFile(filename)
            return this
        }
        
        fun build(): Me2eTestManager {
            return Me2eTestManager(this)
        }
    }
    
    /**
     * Starts containers with the given names.
     * @return Started container instances
     */
    fun start(containerNames: List<String> = this.containerNames): List<Container> {
        val containers = containerNames.map { config.containers[it] ?: throw IllegalArgumentException("Container $it is unknown") }
        for (container in containers) {
            container.start()
        }
        
        return containers
    }
}
