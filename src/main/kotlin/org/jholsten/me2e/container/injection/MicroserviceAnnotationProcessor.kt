package org.jholsten.me2e.container.injection

import com.google.auto.service.AutoService
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.TypeElement

@SupportedAnnotationTypes("org.jholsten.me2e.container.injection.Microservice")
@AutoService(Processor::class)
class MicroserviceAnnotationProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        print("HELLO")
        return true
    }
}
