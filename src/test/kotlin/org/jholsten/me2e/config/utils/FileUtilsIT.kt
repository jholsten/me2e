package org.jholsten.me2e.config.utils

import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import kotlin.test.assertEquals

internal class FileUtilsIT {
    
    @Test
    fun `Contents should be read from existing file`() {
        val contents = FileUtils.readFileContentsFromResources("test-file.txt")
        assertEquals("Test", contents)
    }
    
    @Test
    fun `Reading contents from non-existing file should fail`() {
        assertThrowsExactly(FileNotFoundException::class.java) { FileUtils.readFileContentsFromResources("non-existing") }
    }
}
