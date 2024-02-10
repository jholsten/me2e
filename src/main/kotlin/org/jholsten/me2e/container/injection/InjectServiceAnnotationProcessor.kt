package org.jholsten.me2e.container.injection

import com.google.auto.service.AutoService
import org.jholsten.me2e.Me2eTest
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.mock.MockServer
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlin.reflect.KClass

/**
 * Annotation processor for fields annotated with [InjectService] which ensures that all prerequisites are fulfilled.
 * Requires field to be of type [Container] or [MockServer] and the enclosing class to extend [Me2eTest].
 * If any of these requirements is not met, the compiler will show an error.
 */
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_16)
@SupportedAnnotationTypes("org.jholsten.me2e.container.injection.InjectService")
internal class InjectServiceAnnotationProcessor : AbstractProcessor() {

    /**
     * Ensures that the prerequisites are fulfilled for all fields annotated with [InjectService].
     * This method is invoked upon compilation.
     */
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

    /**
     * Ensures that the datatype of the annotated field is either of type [Container] or [MockServer].
     * In case this prerequisite is not fulfilled, an error message is transferred to the compiler.
     * @param element Field annotated with [InjectService].
     */
    private fun assertThatFieldTypeIsValid(element: Element) {
        val elementType = element.asType()
        if (elementType == null || !(elementType.isOfType(Container::class) || elementType.isOfType(MockServer::class))) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@InjectService annotation can only be applied to fields of type ${Container::class.qualifiedName} and ${MockServer::class.qualifiedName}",
                element
            )
        }
    }

    /**
     * Ensures that the class in which the annotated field is defined inherits from [Me2eTest].
     * In case this prerequisite is not fulfilled, an error message is transferred to the compiler.
     * @param element Field annotated with [InjectService].
     */
    private fun assertThatEnclosingClassIsValid(element: Element) {
        val enclosingClass = element.enclosingElement.asType()
        if (!enclosingClass.isOfType(Me2eTest::class)) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@InjectService annotation can only be applied to fields of classes which extend ${Me2eTest::class.qualifiedName}",
                element
            )
        }
    }

    /**
     * Returns whether this type mirror is of type [clazz], i.e. if they are equal of it this type
     * is a direct or indirect subtype of [clazz].
     */
    private fun TypeMirror.isOfType(clazz: KClass<*>): Boolean {
        val type = processingEnv.elementUtils.getTypeElement(clazz.java.name).asType()
        return processingEnv.typeUtils.isAssignable(this, type)
    }
}
