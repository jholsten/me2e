package org.jholsten.me2e.container.injection

import org.apache.commons.lang3.reflect.FieldUtils
import org.jholsten.me2e.Me2eTest
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.utils.logger
import java.lang.reflect.Field

/**
 * Utility class for injecting container and mock server instances into the fields in
 * [testClassInstance] which are annotated with [InjectService].
 */
internal class InjectionUtils<T : Me2eTest>(
    private val testClassInstance: T,
) {
    private val logger = logger(this)

    /**
     * Injects container and mock server instances into the fields in [testClassInstance] which are annotated
     * with [InjectService].
     */
    internal fun injectServices() {
        val annotatedFields = FieldUtils.getFieldsWithAnnotation(testClassInstance.javaClass, InjectService::class.java)
        for (field in annotatedFields) {
            injectService(field)
        }
    }

    /**
     * Injects the corresponding service into the given field.
     * If the field's type is a subtype of [Container], the corresponding container instance is injected.
     * If the field's type is a subtype of [MockServer], the corresponding mock server instance is injected.
     */
    private fun injectService(field: Field) {
        if (Container::class.java.isAssignableFrom(field.type)) {
            injectService(field, InjectableServiceType.CONTAINER)
        } else if (MockServer::class.java.isAssignableFrom(field.type)) {
            injectService(field, InjectableServiceType.MOCK_SERVER)
        } else {
            logger.warn("Unable to inject service for field ${field.name}. Only container and mock server instances can be injected.")
        }
    }

    /**
     * Injects service instance into the given field.
     * Retrieves container/mock server instance with the derived name from container/mock server
     * manager and sets it as a value of the given field.
     */
    private fun injectService(field: Field, serviceType: InjectableServiceType) {
        val serviceName = getServiceName(field)
        val service = when (serviceType) {
            InjectableServiceType.CONTAINER -> Me2eTest.containerManager.containers[serviceName]
            InjectableServiceType.MOCK_SERVER -> Me2eTest.mockServerManager.mockServers[serviceName]
        } ?: throw RuntimeException("Unable to find $serviceType with name '$serviceName'.")
        if (!field.type.isAssignableFrom(service.javaClass)) {
            throw RuntimeException("$serviceType instance '$serviceName' of type ${service.javaClass.simpleName} cannot be assigned to field '${field.name}' of type ${field.type.simpleName}.")
        }
        field.isAccessible = true
        field.set(testClassInstance, service)
        logger.debug("Injected {} instance {} into field '{}' in class '{}'.", serviceType, service, field.name, field.javaClass)
    }

    /**
     * Returns service name to use for the given field.
     * If [InjectService.name] is set, this value is returned. Otherwise, the field name
     * is converted to kebab case and returned.
     */
    private fun getServiceName(field: Field): String {
        val annotationName = field.getAnnotation(InjectService::class.java).name
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

    private enum class InjectableServiceType {
        CONTAINER,
        MOCK_SERVER;

        override fun toString(): String {
            return when (this) {
                CONTAINER -> "Container"
                MOCK_SERVER -> "Mock Server"
            }
        }
    }
}
