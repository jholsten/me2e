package org.jholsten.me2e.config.utils

import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import kotlin.test.assertEquals

class FileUtilsTest {
    
    @Test
    fun testReadFileFromResources() {
        val contents = FileUtils.readFileContentsFromResources("test-file.txt")
        assertEquals("Test", contents)
    }
    
    @Test
    fun testReadFileFromResourcesWithNonExistingFile() {
        assertThrowsExactly(FileNotFoundException::class.java) { FileUtils.readFileContentsFromResources("non-existing") }
    }
}
