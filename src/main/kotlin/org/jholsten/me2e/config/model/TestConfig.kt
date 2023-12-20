package org.jholsten.me2e.config.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.TestEnvironmentConfigDeserializer

/**
 * Model containing the test configuration to use.
 */
class TestConfig(
    /**
     * Configuration of the test environment.
     */
    @JsonDeserialize(using = TestEnvironmentConfigDeserializer::class)
    val environment: TestEnvironmentConfig,
)
