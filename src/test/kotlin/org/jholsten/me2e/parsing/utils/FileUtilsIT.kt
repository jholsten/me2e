package org.jholsten.me2e.parsing.utils

import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.Test

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
}
