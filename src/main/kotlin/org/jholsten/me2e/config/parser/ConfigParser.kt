package org.jholsten.me2e.config.parser

import org.jholsten.me2e.config.exception.ConfigParseException
import org.jholsten.me2e.config.model.TestConfig
import java.io.FileNotFoundException
import kotlin.jvm.Throws

/**
 * Interface for service class parsing the test configuration.
 */
interface ConfigParser {
    
    /**
     * Parses the test configuration from the given file.
     * @param filename Name of the file. Needs to be located in resources folder.
     * @return Parsed test configuration.
     */
    @Throws(FileNotFoundException::class, ConfigParseException::class)
    fun parseFile(filename: String): TestConfig
}
