package org.jholsten.me2e.container.injection

import com.google.testing.compile.Compiler.javac
import org.jholsten.me2e.SecuredMethod
import org.jholsten.me2e.abc.DeepThought
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContainerInjectionIT {

    @Microservice
    var microservice: MicroserviceContainer? = null

    @Test
    fun blub() {
        print("H")
    }

    @Test
    fun testMethod() {
        val service = SecuredMethod()
        service.unlockedMethod()
        service.lockedMethod()
    }

    @Test
    fun testTheUltimateQuestion() {
        assertEquals("42", DeepThought().theAnswerToTheUltimateQuestionOfLifeUniverseAndEverything())
    }

    @Test
    fun testo() {
        print(microservice)
        val a = org.jholsten.me2e.container.injection.Test().test()
        print(a)
    }
}
