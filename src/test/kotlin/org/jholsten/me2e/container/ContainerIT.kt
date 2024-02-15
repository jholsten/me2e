package org.jholsten.me2e.container

import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerPort
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.parsing.utils.FileUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.opentest4j.TestAbortedException
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ContainerIT {

    companion object {
        private val tempDirectory = "${System.getProperty("user.dir").replace("\\", "/")}/build/tmp"

        private val backendApi = MicroserviceContainer(
            name = "backend-api",
            image = "gitlab.informatik.uni-bremen.de:5005/master-thesis1/test-system/backend-api:latest",
            environment = mapOf("DB_PASSWORD" to "123", "DB_USER" to "user"),
            requestConfig = RequestConfig(),
            ports = ContainerPortList(
                ports = listOf(ContainerPort(internal = 8000))
            ),
        )

        private val manager = ContainerManager(
            dockerComposeFile = FileUtils.getResourceAsFile("docker-compose.yml"),
            dockerConfig = DockerConfig(),
            containers = mapOf("backend-api" to backendApi)
        )

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            manager.start()
            generateDirectoryInContainer()
            generateFile("folder/file-1")
            generateFile("folder/file-2")
            generateFile("folder/subfolder/file-3")
            generateFile("folder/subfolder/file-4")
            generateFile("file-5")
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            manager.stop()
        }

        private fun generateFile(filename: String) {
            generateFileOnHost(filename)
            generateFileInContainer(filename)
        }

        private fun generateFileOnHost(filename: String) {
            val file = File("$tempDirectory/$filename")
            file.parentFile.mkdirs()
            file.createNewFile()
        }

        private fun generateDirectoryInContainer() {
            val result = backendApi.execute("mkdir", "-p", "folder/subfolder")
            if (result.exitCode != 0) {
                throw TestAbortedException("Unable to generate directory folder/subfolder.")
            }
        }

        private fun generateFileInContainer(filename: String) {
            val result = backendApi.execute("touch", filename)
            if (result.exitCode != 0) {
                throw TestAbortedException("Unable to generate file $filename.")
            }
        }
    }

    @ParameterizedTest
    @CsvSource(
        "database/mongo_script.js, /app/database/mongo_script.js",
        "test-file.txt, /test-file.txt",
    )
    fun `Copying resource file to container should succeed`(sourcePath: String, containerPath: String) {
        backendApi.copyResourceToContainer(sourcePath, containerPath)

        assertFileExistsInContainer(containerPath)
        deleteFileInContainer(containerPath)
    }

    @Test
    fun `Copying resource directory to container should succeed`() {
        val sourcePath = "database"
        val containerPath = "/app/database"
        val expectedFilenames = listOf("mongo_script.js", "mongo_script_authenticated.js", "mysql_script.sql", "postgres_script.sql")
        backendApi.copyResourceToContainer(sourcePath, containerPath)

        for (filename in expectedFilenames) {
            assertFileExistsInContainer("$containerPath/$filename")
        }
        deleteFileInContainer(containerPath)
    }

    @Test
    fun `Copying non-existing resource to container should fail`() {
        assertFailsWith<FileNotFoundException> { backendApi.copyResourceToContainer("non-existing", "any") }
    }

    @ParameterizedTest
    @CsvSource(
        "folder/file-1, /tmp/folder/file-1",
        "file-5, /file-5",
    )
    fun `Copying file to container should succeed`(sourcePath: String, containerPath: String) {
        backendApi.copyFileToContainer("$tempDirectory/$sourcePath", containerPath)

        assertFileExistsInContainer(containerPath)
        deleteFileInContainer(containerPath)
    }

    @Test
    fun `Copying directory to container should succeed`() {
        val sourcePath = "$tempDirectory/folder/subfolder"
        val containerPath = "/tmp/folder/subfolder"
        val expectedFilenames = listOf("file-3", "file-4")
        backendApi.copyFileToContainer(sourcePath, containerPath)

        for (filename in expectedFilenames) {
            assertFileExistsInContainer("$containerPath/$filename")
        }
        deleteFileInContainer(containerPath)
    }

    @Test
    fun `Copying non-existing file to container should fail`() {
        assertFailsWith<FileNotFoundException> { backendApi.copyFileToContainer("non-existing", "any") }
    }

    @ParameterizedTest
    @CsvSource(
        "/app/folder/file-1, folder-1/file-1",
        "/app/folder/subfolder/file-3, folder-1//subfolder/file-3",
        "/app/file-5, folder-1/file-5",
    )
    fun `Copying file from container should succeed`(containerPath: String, destinationPath: String) {
        val destination = "$tempDirectory/$destinationPath"
        backendApi.copyFileFromContainer(containerPath, destination)

        assertFileExistsOnHost(destination)
        deleteFileOnHost(destinationPath)
    }

    @Test
    fun `Copying non-existing file from container should fail`() {
        assertFailsWith<FileNotFoundException> { backendApi.copyFileFromContainer("/app/non-existing", "any") }
    }

    private fun deleteFileOnHost(path: String) {
        File(path).delete()
    }

    private fun deleteFileInContainer(containerPath: String) {
        backendApi.execute("rm", containerPath)
    }

    private fun assertFileExistsOnHost(path: String) {
        assertTrue(File(path).exists(), "File $path does not exist on host.")
    }

    private fun assertFileExistsInContainer(containerPath: String) {
        val result = backendApi.execute("test", "-e", containerPath)
        assertEquals(0, result.exitCode, "File $containerPath does not exist in container.")
    }
}
