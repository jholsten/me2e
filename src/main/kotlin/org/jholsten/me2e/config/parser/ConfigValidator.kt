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

    /**
     * Validates that the given deserialized test configuration is valid.
     * @throws ValidationException if validation was not successful.
     */
    @JvmSynthetic
    override fun validate(value: TestConfig) {
        validateMockServers(value)
        validateMockServerStubs(value)
    }

    /**
     * Validates the Mock Servers defined in the given test configuration.
     * Logs warning if multiple Mock Servers share the same Hostname.
     */
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

    /**
     * Validates the stubs defined for the Mock Servers in the given test configuration.
     * Ensures that each [org.jholsten.me2e.mock.stubbing.MockServerStub.name] is unique.
     * Also logs a warning if there are Mock Server without any registered stubs.
     */
    private fun validateMockServerStubs(testConfig: TestConfig) {
        val duplicateStubNames = testConfig.environment.mockServers.values
            .map { it.name to it.stubs.mapNotNull { stub -> stub.name }.getDuplicates() }
            .filter { it.second.isNotEmpty() }
        if (duplicateStubNames.isNotEmpty()) {
            throwValidationExceptionForDuplicateStubNames(duplicateStubNames)
        }
        val mockServersWithoutStubs = testConfig.environment.mockServers.values.filter { it.stubs.isEmpty() }
        if (mockServersWithoutStubs.isNotEmpty()) {
            logger.warn(
                "No stubs registered for Mock Servers ${mockServersWithoutStubs.map { it.name }}. " +
                    "In order for these Mock Servers to respond to requests with predefined responses, you need to specify at least " +
                    "one stub in the me2e-config file. Otherwise all requests will be answered with 404."
            )
        }
    }

    /**
     * Throws validation exception for the given duplicate stub names.
     */
    private fun throwValidationExceptionForDuplicateStubNames(duplicateStubNames: List<Pair<String, Map<String, Int>>>) {
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

    /**
     * Returns duplicate string values contained in the given List.
     * @return Map of duplicate value and the number of occurrences.
     */
    private fun List<String>.getDuplicates(): Map<String, Int> {
        return this.groupingBy { it }.eachCount().filter { it.value > 1 }
    }

    /**
     * Builds string representation of the given map.
     */
    private fun Map<String, Int>.toStringList(indent: Int = 0): List<String> {
        return this.map { (value, count) -> "${" ".repeat(indent)}- $value ($count times)" }
    }
}
