package org.jholsten.me2e.container.injection

/**
 * Annotation for attributes for which the corresponding container or mock server instance is to be automatically
 * injected.
 * When initializing a test class that inherits from [org.jholsten.me2e.Me2eTest], the corresponding container or
 * mock server instance from the test environment is set as the value for each attribute that is marked with this
 * annotation. The name of the container or mock server to be injected is determined either from the [name]
 * attribute of this annotation or from the name of the annotated attribute. If [name] is set (i.e. not blank),
 * this value is used as the container/mock server name. However, if [name] is blank, the name of the annotated
 * attribute is converted to Kebab-Case and this value is used as the container/mock server name.
 * @see org.jholsten.me2e.container.injection.InjectionUtils
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class InjectService(
    /**
     * Name of the container/mock server to inject.
     * If this value is not set (i.e. blank), the name of the annotated attribute is converted to Kebab-Case
     * and this converted value is used as the container/mock server name.
     */
    val name: String = "",
)
