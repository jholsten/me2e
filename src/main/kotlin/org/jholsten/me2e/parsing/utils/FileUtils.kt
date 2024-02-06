package org.jholsten.me2e.parsing.utils

import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL


/**
 * Utility class for reading files from `resource` folder and their contents.
 */
internal class FileUtils private constructor() {
    companion object {
        /**
         * Reads contents of file with the given name located on the classpath, i.e. in the `resources` folder.
         * @param filename Relative path of the file to read, starting from `resources` folder.
         * @throws FileNotFoundException if file does not exist.
         */
        @JvmSynthetic
        fun readFileContentsFromResources(filename: String): String {
            val file = getResource(filename)
            return file.readText()
        }

        /**
         * Returns input stream of file with the given name located on the classpath, i.e. in the `resources` folder.
         * @param filename Relative path of the file to read, starting from `resources` folder.
         * @throws FileNotFoundException if file does not exist.
         */
        @JvmSynthetic
        fun getResourceAsStream(filename: String): InputStream {
            return FileUtils::class.java.classLoader.getResourceAsStream(filename)
                ?: throw FileNotFoundException("File $filename could not be found in resources folder.")
        }

        /**
         * Returns [File] instance for the file with the given name located on the classpath, i.e. in the `resources` folder.
         * @param filename Relative path of the file to read, starting from `resources` folder.
         * @throws FileNotFoundException if file does not exist.
         */
        @JvmSynthetic
        fun getResourceAsFile(filename: String): File {
            val resource = getResource(filename)
            return File(resource.path)
        }

        /**
         * Returns resource with the given name.
         * @throws FileNotFoundException if resource with the given name does not exist.
         */
        private fun getResource(filename: String): URL {
            return FileUtils::class.java.classLoader.getResource(filename)
                ?: throw FileNotFoundException("File $filename could not be found in resources folder.")
        }
    }
}
