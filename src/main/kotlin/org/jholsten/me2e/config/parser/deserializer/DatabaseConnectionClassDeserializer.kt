package org.jholsten.me2e.config.parser.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.jholsten.me2e.container.database.connection.DatabaseConnection
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.parsing.exception.ValidationException

/**
 * Deserializer for the definition of the custom database connection implementation class.
 * Validates that the following requirements are met:
 * - Class name exists on the classpath
 * - Class extends [DatabaseConnection]
 * - Class contains an implementation of the [DatabaseConnection.Builder]
 * - Builder class has a public, no-args constructor
 * @see DatabaseContainer.databaseConnectionClass
 */
internal class DatabaseConnectionClassDeserializer : JsonDeserializer<Class<*>?>() {

    /**
     * Deserializes a class name to a [Class] that extends [DatabaseConnection].
     * Ensures that all requirements are met.
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Class<*>? {
        val className = p.readValueAs(String::class.java) ?: return null
        val clazz = getClass(className)
        assertThatClassHasValidBuilder(clazz)
        return clazz
    }

    /**
     * Tries to load the class with the given name. Validates that the class extends [DatabaseConnection].
     * @param className Name of the class to load.
     * @throws ValidationException if class does not exist or class does not extend [DatabaseConnection].
     */
    private fun getClass(className: String): Class<out DatabaseConnection> {
        val clazz = try {
            Class.forName(className)
        } catch (e: Exception) {
            throw ValidationException(
                "Class $className does not exist on the classpath of this project. " +
                    "Make sure that this is a public, not nested class."
            )
        }
        if (!DatabaseConnection::class.java.isAssignableFrom(clazz)) {
            throw ValidationException("Class $className needs to extend ${DatabaseConnection::class.java}.")
        }
        @Suppress("UNCHECKED_CAST")
        return clazz as Class<out DatabaseConnection>
    }

    private fun assertThatClassHasValidBuilder(clazz: Class<out DatabaseConnection>) {
        val builderClass = clazz.declaredClasses
            .filterIsInstance<Class<out DatabaseConnection.Builder<*>>>()
            .firstOrNull()
            ?: throw ValidationException("Class ${clazz.simpleName} needs to include builder extending the DatabaseConnection.Builder class.")
        if (!builderClass.constructors.any { it.parameterCount == 0 }) {
            throw ValidationException("Builder class ${builderClass.simpleName} needs to have a public no-args constructor.")
        }
    }
}
