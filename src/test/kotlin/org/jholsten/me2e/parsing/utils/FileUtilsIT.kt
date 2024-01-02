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
    fun `File instance should be returned for existing file`() {
        val file = FileUtils.getResourceAsFile("test-file.txt")
        val contents = file.readText()
        assertEquals("Test", contents)
        assertTrue(file.name.startsWith("tmp_"))
    }

    @Test
    fun `Getting file instance for non-existing file should fail`() {
        assertFailsWith<FileNotFoundException> { FileUtils.getResourceAsFile("non-existing") }
    }
}
