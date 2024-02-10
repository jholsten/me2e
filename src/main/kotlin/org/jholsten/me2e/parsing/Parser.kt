package org.jholsten.me2e.parsing

import org.jholsten.me2e.parsing.exception.ParseException
import java.io.FileNotFoundException

/**
 * Generic interface for services that parse and validate values to a model.
 * @param T Model to which values should be parsed.
 */
internal interface Parser<T> {

    /**
     * Parses and validates the given value to the given model.
     * @param value Value to be parsed.
     * @return Value parsed to the given model.
     * @throws ParseException if value could not be parsed or the validation failed.
     */
    fun parse(value: String): T

    /**
     * Parses and validates the given file contents to the given model.
     * @param filename Name of the file. Needs to be located in `resources` folder.
     * @return File contents parsed to the given model.
     * @throws ParseException if value could not be parsed or the validation failed.
     * @throws FileNotFoundException if file with the given name does not exist in `resources` folder.
     */
    fun parseFile(filename: String): T
}
