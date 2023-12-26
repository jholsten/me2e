package org.jholsten.me2e.config.utils

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.parsing.utils.Validator
import org.jholsten.me2e.utils.logger
import java.util.*

/**
 * Additional validator which ensures that a parsed test configuration is valid.
 */
internal class ConfigValidator : Validator<TestConfig> {
    private val logger = logger(this)

    override fun validate(value: TestConfig) {
        validateMockServers(value)
    }

    private fun validateMockServers(testConfig: TestConfig) {
        val hostnames = testConfig.environment.mockServers.values.map { it.hostname }
            .groupingBy { it }.eachCount()
        if (hostnames.any { it.value > 1 }) {
            val duplicateHostnames = hostnames.filter { it.value > 1 }
                .map { (hostname, count) -> "- $hostname ($count times)" }
            logger.warn(
                "Detected different mock servers with the same hostnames:\n" +
                    "${duplicateHostnames.joinToString("\n")}\n" +
                    "This means that requests to this host names are assigned to multiple mock servers."
            )
        }
    }
}
