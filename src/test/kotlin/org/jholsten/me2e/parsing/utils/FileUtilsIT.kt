package org.jholsten.me2e.parsing.utils

import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.Test
import kotlin.test.assertTrue

internal class FileUtilsIT {

    @Test
    fun `Contents should be read from existing file`() {
        val contents = FileUtils.readFileContentsFromResources("test-file.txt")
        assertEquals("Test", contents)
    }

    @Test
    fun `Reading contents from non-existing file should fail`() {
        assertFailsWith<FileNotFoundException> { FileUtils.readFileContentsFromResources("non-existing") }
    }

    @Test
    fun `Retrieving resource as file should succeed`() {
        val filename = "test-file.txt"

        val file = FileUtils.getResourceAsFile(filename)

        assertEquals(filename, file.name)
        assertTrue(file.isFile)
    }

    @Test
    fun `Retrieving resource as file in subfolder succeed`() {
        val filename = "database/mongo_script.js"

        val file = FileUtils.getResourceAsFile(filename)

        assertEquals("database", file.parentFile.name)
        assertEquals("mongo_script.js", file.name)
        assertTrue(file.isFile)
    }

    @Test
    fun `Getting file instance for non-existing file should fail`() {
        assertFailsWith<FileNotFoundException> { FileUtils.getResourceAsFile("non-existing") }
    }
}
