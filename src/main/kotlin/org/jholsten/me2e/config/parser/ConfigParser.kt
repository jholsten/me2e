package org.jholsten.me2e.config.parser

import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.parsing.Parser
import java.io.FileNotFoundException
import kotlin.jvm.Throws

/**
 * Interface for service class parsing the test configuration.
 */
interface ConfigParser : Parser<TestConfig> {

    /**
     * Parses the test configuration from the given file.
     * @param filename Name of the file. Needs to be located in resources folder.
     * @return Parsed test configuration.
     */
    @Throws(FileNotFoundException::class, ParseException::class)
    override fun parseFile(filename: String): TestConfig
}
