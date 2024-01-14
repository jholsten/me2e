package org.jholsten.me2e.container.injection

import com.google.auto.service.AutoService
import org.jholsten.me2e.Me2eTest
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.mock.MockServer
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * Annotation processor for fields annotated with [InjectService] which ensures
 * that the prerequisites are fulfilled.
 * Requires field to be of type [Container] or [MockServer] and the enclosing class
 * to extend [Me2eTest]. If any of these requirements is not met, the compiler will
 * show an error.
 */
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_16)
@SupportedAnnotationTypes("org.jholsten.me2e.container.injection.InjectService")
class InjectServiceAnnotationProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (annotation in annotations) {
            val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)
            for (element in annotatedElements) {
                assertThatFieldTypeIsValid(element)
                assertThatEnclosingClassIsValid(element)
            }
        }

        return true
    }

    private fun assertThatFieldTypeIsValid(element: Element) {
        val elementType = Class.forName(element.asType().toString())
        if (!Container::class.java.isAssignableFrom(elementType) && !MockServer::class.java.isAssignableFrom(elementType)) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@${InjectService::class.java.name} annotation can only be applied to fields of type ${Container::class.java.name} and ${MockServer::class.java.name}",
                element
            )
        }
    }

    private fun assertThatEnclosingClassIsValid(element: Element) {
        val enclosingClass = element.enclosingElement.asType()
        val superClasses = processingEnv.typeUtils.directSupertypes(enclosingClass).map { it.toString() }
        if (!superClasses.contains(Me2eTest::class.java.name)) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@${InjectService::class.java.name} annotation can only be applied to fields of classes which extend ${Me2eTest::class.java}",
                element
            )
        }
    }
}
