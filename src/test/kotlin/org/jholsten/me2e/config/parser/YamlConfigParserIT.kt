package org.jholsten.me2e.config.parser

import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class YamlConfigParserIT {

    @Test
    fun `Parsing valid YAML config should succeed`() {
        val config = YamlConfigParser().parseFile("me2e-config-test.yaml")

        assertEquals(2, config.containers.size)
        // TODO: Add additional assertions
    }

    @Test
    fun `Parsing YAML config with invalid format should fail`() {
        assertThrowsExactly(InvalidFormatException::class.java) { YamlConfigParser().parseFile("test-file.txt") }
    }
}
