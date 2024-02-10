package org.jholsten.me2e.config.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.parser.deserializer.TestConfigDeserializer

/**
 * Representation of the parsed config file which is read when the tests are started.
 * Contains the configuration for running the End-to-End-Tests, including the definition
 * of the test environment and additional settings.
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
