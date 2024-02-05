package org.jholsten.me2e.parsing.utils

import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream


/**
 * Utility class for reading file contents.
 */
internal class FileUtils private constructor() {
    companion object {
        /**
         * Reads contents of file with the given name located in the `resources` folder.
         * @param filename Relative path of the file to read, starting from `resources` folder.
         */
        @JvmSynthetic
        fun readFileContentsFromResources(filename: String): String {
            val file = FileUtils::class.java.classLoader.getResource(filename)
                ?: throw FileNotFoundException("File $filename could not be found in resources folder.")
            return file.readText()
        }

        /**
         * Returns input stream of file with the given name located in the `resources` folder.
         * @param filename Relative path of the file to read, starting from `resources` folder.
         */
        @JvmSynthetic
        fun getResourceAsStream(filename: String): InputStream {
            return FileUtils::class.java.classLoader.getResourceAsStream(filename)
                ?: throw FileNotFoundException("File $filename could not be found in resources folder.")
        }

        /**
         * Returns [File] instance for the file with the given name located in the `resources` folder.
         * @param filename Relative path of the file to read, starting from `resources` folder.
         */
        @JvmSynthetic
        fun getResourceAsFile(filename: String): File {
            val resource = FileUtils::class.java.classLoader.getResource(filename)
                ?: throw FileNotFoundException("File $filename could not be found in resources folder.")
            return File(resource.path)
        }
    }
}
