package org.jholsten.me2e.config.parser.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.mockk
import org.jholsten.me2e.container.database.CustomDatabaseConnection
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.parsing.exception.ValidationException
import kotlin.test.*

class DatabaseConnectionClassDeserializerTest {

    private val yamlMapper = YAMLMapper()
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val mockedDeserializationContext = mockk<DeserializationContext>()

    @Test
    fun `Deserializing valid class should succeed`() {
        val parser = prepareParser("org.jholsten.me2e.container.database.CustomDatabaseConnection")

        val clazz = DatabaseConnectionClassDeserializer().deserialize(parser, mockedDeserializationContext)

        assertEquals(CustomDatabaseConnection::class.java, clazz)
    }

    @Test
    fun `Deserializing non-existing class should fail`() {
        val parser = prepareParser("non_existing")

        val e = assertFailsWith<ValidationException> {
            DatabaseConnectionClassDeserializer().deserialize(parser, mockedDeserializationContext)
        }
        assertNotNull(e.message)
        assertContains(e.message!!, "does not exist")
    }

    @Test
    fun `Deserializing class not extending DatabaseConnection should fail`() {
        val parser = prepareParser("org.jholsten.me2e.config.parser.deserializer.DatabaseConnectionClassDeserializerTest")

        val e = assertFailsWith<ValidationException> {
            DatabaseConnectionClassDeserializer().deserialize(parser, mockedDeserializationContext)
        }
        assertNotNull(e.message)
        assertContains(e.message!!, "needs to extend")
    }

    @Test
    fun `Deserializing class without builder should fail`() {
        val parser = prepareParser("org.jholsten.me2e.config.parser.deserializer.CustomDatabaseConnectionWithoutBuilder")

        val e = assertFailsWith<ValidationException> {
            DatabaseConnectionClassDeserializer().deserialize(parser, mockedDeserializationContext)
        }
        assertNotNull(e.message)
        assertContains(e.message!!, "needs to include builder")
    }

    @Test
    fun `Deserializing class with builder with private constructor should fail`() {
        val parser = prepareParser("org.jholsten.me2e.config.parser.deserializer.CustomDatabaseConnectionWithBuilderWithPrivateConstructor")

        val e = assertFailsWith<ValidationException> {
            DatabaseConnectionClassDeserializer().deserialize(parser, mockedDeserializationContext)
        }
        assertNotNull(e.message)
        assertContains(e.message!!, "needs to have a public no-args constructor")
    }

    @Test
    fun `Deserializing class with builder without no-args constructor should fail`() {
        val parser =
            prepareParser("org.jholsten.me2e.config.parser.deserializer.CustomDatabaseConnectionWithBuilderWithoutNoArgsConstructor")

        val e = assertFailsWith<ValidationException> {
            DatabaseConnectionClassDeserializer().deserialize(parser, mockedDeserializationContext)
        }
        assertNotNull(e.message)
        assertContains(e.message!!, "needs to have a public no-args constructor")
    }

    /**
     * Prepares JSON parser by reading the given [value] as tokens.
     */
    private fun prepareParser(value: String): JsonParser {
        return yamlMapper.factory.createParser(value)
    }
}

class CustomDatabaseConnectionWithoutBuilder(
    host: String,
    port: Int,
    database: String,
    username: String?,
    password: String?,
    container: DatabaseContainer?,
) :
    CustomDatabaseConnection(
        host = host,
        port = port,
        database = database,
        username = username,
        password = password,
        container = container,
    )

class CustomDatabaseConnectionWithBuilderWithPrivateConstructor(
    host: String,
    port: Int,
    database: String,
    username: String?,
    password: String?,
    container: DatabaseContainer?,
) :
    CustomDatabaseConnection(
        host = host,
        port = port,
        database = database,
        username = username,
        password = password,
        container = container,
    ) {
    class Builder private constructor() : CustomDatabaseConnection.Builder()
}

class CustomDatabaseConnectionWithBuilderWithoutNoArgsConstructor(
    host: String,
    port: Int,
    database: String,
    username: String?,
    password: String?,
    container: DatabaseContainer?,
) :
    CustomDatabaseConnection(
        host = host,
        port = port,
        database = database,
        username = username,
        password = password,
        container = container,
    ) {
    class Builder(value: String) : CustomDatabaseConnection.Builder()
}
