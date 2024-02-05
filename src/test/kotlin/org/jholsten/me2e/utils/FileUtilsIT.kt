package org.jholsten.me2e.utils

import org.jholsten.me2e.parsing.utils.FileUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileUtilsIT {

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
}
