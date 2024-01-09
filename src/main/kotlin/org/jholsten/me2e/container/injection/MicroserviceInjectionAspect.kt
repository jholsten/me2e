package org.jholsten.me2e.container.injection

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.microservice.MicroserviceContainer


@Aspect
class MicroserviceInjectionAspect {

    @Suppress("unused")
    @Pointcut("get(* *) && @annotation(microservice)")
    fun callAt(microservice: Microservice) {
        print("HE")
    }

    @Suppress("unused")
    @Around("callAt(microservice)")
    fun around(pjp: ProceedingJoinPoint, microservice: Microservice): Any? {
        print("HO")
        pjp.proceed()
        return MicroserviceContainer(name = "HELLO", image = "blub", requestConfig = RequestConfig())
    }
}
