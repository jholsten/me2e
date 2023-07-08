package org.jholsten.me2e.config.parser

import org.jholsten.me2e.config.exception.InvalidFormatException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class YamlConfigParserTest {
    
    @Test
    fun testParseYamlConfig() {
        val config = YamlConfigParser().parseFile("me2e-config-test.yaml")
        
        assertEquals(1, config.containers.size)
        // TODO: Add additional assertions
    }
    
    @Test
    fun testParseInvalidYamlFile() {
        assertThrowsExactly(InvalidFormatException::class.java) { YamlConfigParser().parseFile("test-file.txt") }
    }
}
