package org.jholsten.me2e.mock.parser

import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.parsing.Parser
import org.jholsten.me2e.parsing.exception.ParseException
import java.io.FileNotFoundException
import kotlin.jvm.Throws

/**
 * Interface for parsing a stub definition.
 */
internal interface MockServerStubParser : Parser<MockServerStub> {

    /**
     * Parses the stub definition from the given file.
     * @param filename Name of the file. Needs to be located in resources folder.
     * @return Parsed stub definition.
     */
    @Throws(FileNotFoundException::class, ParseException::class)
    override fun parseFile(filename: String): MockServerStub
}
