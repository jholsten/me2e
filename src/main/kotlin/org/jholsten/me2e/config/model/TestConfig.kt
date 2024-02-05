package org.jholsten.me2e.config.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.TestConfigDeserializer

/**
 * Model containing the test configuration to use.
 */
@JsonDeserialize(using = TestConfigDeserializer::class)
class TestConfig(
    /**
     * Settings for running the End-to-End tests.
     */
    val settings: TestSettings,

    /**
     * Configuration of the test environment.
     */
    val environment: TestEnvironmentConfig,
)
