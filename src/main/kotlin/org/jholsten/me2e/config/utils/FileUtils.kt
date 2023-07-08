package org.jholsten.me2e.config.utils

import java.io.FileNotFoundException
import kotlin.jvm.Throws

/**
 * Utility class for reading file contents.
 */
class FileUtils {
    companion object {
        /**
         * Reads contents of file with the given name located in the `resources` folder.
         * @param filename Relative path of the file to read, starting from `resources` folder.
         */
        @Throws(FileNotFoundException::class)
        fun readFileFromResources(filename: String): String {
            val file = FileUtils::class.java.classLoader.getResource(filename)
                ?: throw FileNotFoundException("File $filename could not be found in resources folder.")
            return file.readText()
        }
    }
}
