package org.jholsten.me2e.container.injection

import org.apache.commons.lang3.reflect.FieldUtils
import org.jholsten.me2e.Me2eTest
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.injection.exception.NoSuchServiceException
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.utils.logger
import java.lang.reflect.Field

/**
 * Utility class for injecting container and Mock Server instances into the fields of the
 * [testClassInstance] which are annotated with [InjectService].
 *
 * For each field that is annotated with [InjectService], the corresponding instance is retrieved from the [Me2eTest.containerManager]
 * or the [Me2eTest.mockServerManager]. If the [InjectService.name] is set, the corresponding instance is retrieved based on this
 * given name. Otherwise, the name of the field is converted to kebab case and the corresponding instance with this converted
 * name is retrieved. If the instance with the corresponding name does not exist, a [NoSuchServiceException] is thrown.
 */
internal class InjectionUtils<T : Me2eTest>(
    /**
     * Instance of a Test class which inherits from [Me2eTest] and for which container and Mock Server instances
     * are to be injected into the fields annotated with [InjectService].
     */
    private val testClassInstance: T,
) {
    private val logger = logger<InjectionUtils<*>>()

    /**
     * Injects container and Mock Server instances into the fields in [testClassInstance] which are annotated
     * with [InjectService]. Retrieves instances to inject from the [Me2eTest.containerManager] and [Me2eTest.mockServerManager].
     * @throws NoSuchServiceException if service could not be injected.
     */
    @JvmSynthetic
    fun injectServices() {
        val annotatedFields = FieldUtils.getFieldsWithAnnotation(testClassInstance.javaClass, InjectService::class.java)
        logger.debug(
            "Injecting container and Mock Server instances into {} fields of class {}.",
            annotatedFields.size,
            testClassInstance::class,
        )
        for (field in annotatedFields) {
            injectService(field)
        }
    }

    /**
     * Injects the corresponding service into the given field.
     * If the field's type is a subtype of [Container], the corresponding container instance is injected.
     * If the field's type is a subtype of [MockServer], the corresponding Mock Server instance is injected.
     */
    private fun injectService(field: Field) {
        if (Container::class.java.isAssignableFrom(field.type)) {
            injectService(field, InjectableServiceType.CONTAINER)
        } else if (MockServer::class.java.isAssignableFrom(field.type)) {
            injectService(field, InjectableServiceType.MOCK_SERVER)
        } else {
            throw NoSuchServiceException("Unable to inject service for field ${field.name}. Only container and Mock Server instances can be injected.")
        }
    }

    /**
     * Injects service instance into the given field.
     * Retrieves container/Mock Server instance with the derived name from container/Mock Server manager and sets it as a
     * value of the given field.
     */
    private fun injectService(field: Field, serviceType: InjectableServiceType) {
        val serviceName = getServiceName(field)
        val service: Any = when (serviceType) {
            InjectableServiceType.CONTAINER -> Me2eTest.containerManager.containers[serviceName]
            InjectableServiceType.MOCK_SERVER -> Me2eTest.mockServerManager.mockServers[serviceName]
        } ?: throw exceptionForNonExistingService(serviceType, serviceName)

        if (!field.type.isAssignableFrom(service.javaClass)) {
            throw exceptionForWrongFieldType(serviceType, serviceName, service, field)
        }
        field.isAccessible = true
        field.set(testClassInstance, service)
        logger.debug("Injected {} instance {} into field '{}' in class '{}'.", serviceType, service, field.name, field.javaClass)
    }

    /**
     * Returns service name to use for the given field.
     * If [InjectService.name] is set, this value is returned. Otherwise, the field name is converted to kebab case and returned.
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

    private fun exceptionForNonExistingService(serviceType: InjectableServiceType, serviceName: String): NoSuchServiceException {
        val messageHint = when (serviceType) {
            InjectableServiceType.CONTAINER -> if (serviceName in Me2eTest.mockServerManager.mockServers) " Did you mean to use type 'MockServer'?" else ""
            InjectableServiceType.MOCK_SERVER -> if (serviceName in Me2eTest.containerManager.containers) " Did you mean to use type 'Container'?" else ""
        }
        return NoSuchServiceException("Unable to inject services: Unable to find $serviceType with name '$serviceName'.$messageHint")
    }

    private fun exceptionForWrongFieldType(
        serviceType: InjectableServiceType,
        serviceName: String,
        service: Any,
        field: Field
    ): NoSuchServiceException {
        return NoSuchServiceException(
            "Unable to inject services: $serviceType instance '$serviceName' of type ${service.javaClass.simpleName} cannot be assigned to " +
                "field '${field.name}' of type ${field.type.simpleName}. Please check the datatype of the field."
        )
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
