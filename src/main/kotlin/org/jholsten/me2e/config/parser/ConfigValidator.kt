package org.jholsten.me2e.config.parser

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.parsing.utils.Validator
import org.jholsten.me2e.utils.logger

/**
 * Additional validator which ensures that a parsed test configuration is valid.
 */
internal class ConfigValidator : Validator<TestConfig> {
    private val logger = logger<ConfigValidator>()

    @JvmSynthetic
    override fun validate(value: TestConfig) {
        validateMockServers(value)
        validateMockServerStubs(value)
    }

    private fun validateMockServers(testConfig: TestConfig) {
        val duplicateHostnames = testConfig.environment.mockServers.values.map { it.hostname }.getDuplicates()
        if (duplicateHostnames.isNotEmpty()) {
            logger.warn(
                "Detected different Mock Servers with the same hostnames:\n" +
                    "${duplicateHostnames.toStringList().joinToString("\n")}\n" +
                    "This means that requests to these host names are assigned to multiple Mock Servers."
            )
        }
    }

    private fun validateMockServerStubs(testConfig: TestConfig) {
        val duplicateStubNames = testConfig.environment.mockServers.values
            .map { it.name to it.stubs.mapNotNull { stub -> stub.name }.getDuplicates() }
            .filter { it.second.isNotEmpty() }
        if (duplicateStubNames.isNotEmpty()) {
            throwVerificationExceptionForDuplicateStubNames(duplicateStubNames)
        }
    }

    private fun throwVerificationExceptionForDuplicateStubNames(duplicateStubNames: List<Pair<String, Map<String, Int>>>) {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("Detected request stubs with duplicate names for at least one Mock Server:")
        for ((mockServerName, duplicateNames) in duplicateStubNames) {
            stringBuilder.appendLine("Mock Server \"$mockServerName\":")
            val duplicateNameLines = duplicateNames.toStringList(indent = 2)
            for (line in duplicateNameLines) {
                stringBuilder.appendLine(line)
            }
        }
        throw ValidationException(stringBuilder.toString())
    }

    private fun List<String>.getDuplicates(): Map<String, Int> {
        return this.groupingBy { it }.eachCount().filter { it.value > 1 }
    }

    private fun Map<String, Int>.toStringList(indent: Int = 0): List<String> {
        return this.map { (value, count) -> "${" ".repeat(indent)}- $value ($count times)" }
    }
}
