package org.jholsten.me2e.container.injection

import org.apache.commons.lang3.reflect.FieldUtils
import org.jholsten.me2e.Me2eTest
import org.jholsten.me2e.utils.logger
import java.lang.reflect.Field

/**
 * Utility class for injecting container instances into the fields in [testClassInstance]
 * which are annotated with [InjectContainer].
 */
internal class InjectionUtils<T : Me2eTest>(
    private val testClassInstance: T,
) {
    private val logger = logger(this)

    /**
     * Injects container instances into the fields in [testClassInstance] which are annotated
     * with [InjectContainer].
     */
    internal fun injectContainers() {
        val annotatedFields = FieldUtils.getFieldsWithAnnotation(testClassInstance.javaClass, InjectContainer::class.java)
        for (field in annotatedFields) {
            injectContainer(field)
        }
    }

    /**
     * Injects container instance into the given field.
     * Retrieves container instance with the derived container name from container
     * manager and sets it as a value of the given field.
     */
    private fun injectContainer(field: Field) {
        val containerName = getContainerName(field)
        val container = Me2eTest.containerManager.containers[containerName]
            ?: throw RuntimeException("Unable to find container with name '$containerName'.")
        if (!field.type.isAssignableFrom(container.javaClass)) {
            throw RuntimeException("Container instance '$containerName' of type ${container.javaClass.simpleName} cannot be assigned to field '${field.name}' of type ${field.type.simpleName}.")
        }
        field.isAccessible = true
        field.set(testClassInstance, container)
        logger.debug("Injected container instance {} into field '{}' in class '{}'.", container, field.name, field.javaClass)
    }

    /**
     * Returns container name to use for the given field.
     * If [InjectContainer.name] is set, this value is returned. Otherwise, the field name
     * is converted to kebab case and returned.
     */
    private fun getContainerName(field: Field): String {
        val annotationName = field.getAnnotation(InjectContainer::class.java).name
        if (annotationName.isNotBlank()) {
            return annotationName
        }
        return camelToKebabCase(field.name)
    }

    /**
     * Converts the given String in camel case to the equivalent in kebab case.
     *
     * Examples:
     * ```
     * assertEquals("backend-api", camelToKebabCase("backendApi"))
     * assertEquals("Backend-api", camelToKebabCase("BackendApi"))
     * assertEquals("backendapi", camelToKebabCase("backendapi"))
     * ```
     */
    private fun camelToKebabCase(value: String): String {
        val stringBuilder = StringBuilder()
        for (c in value.toCharArray()) {
            if (c.isUpperCase()) {
                stringBuilder.append("-").append(c.lowercase())
            } else {
                stringBuilder.append(c)
            }
        }
        return stringBuilder.toString()
    }
}
