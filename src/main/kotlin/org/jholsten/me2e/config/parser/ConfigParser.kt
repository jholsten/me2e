package org.jholsten.me2e.config.parser

import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.parsing.Parser
import java.io.FileNotFoundException

/**
 * Interface for parsing the test configuration.
 */
internal interface ConfigParser : Parser<TestConfig> {

    /**
     * Parses the test configuration from the given file.
     * @param filename Name of the file. Needs to be located in resources folder.
     * @return Parsed test configuration.
     * @throws FileNotFoundException if file with the given name does not exist.
     * @throws ParseException if value could not be parsed or the validation failed.
     */
    override fun parseFile(filename: String): TestConfig
}
