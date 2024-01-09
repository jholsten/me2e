package org.jholsten.me2e.container.injection

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class Microservice {
}

// https://stackoverflow.com/questions/25930192/aspectj-annotation-on-field-that-triggers-before-advice
