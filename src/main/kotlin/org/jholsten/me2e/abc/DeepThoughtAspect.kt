package org.jholsten.me2e.abc

import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut

@Aspect
class DeepThoughtAspect {

    @Suppress("unused")
    @Pointcut("execution(* org.jholsten.me2e.abc.DeepThought.*())")
    fun deepThought() {
        print("HE")
    }

    @Suppress("unused")
    @Around("deepThought()")
    fun answer(): Any {
        print("HO")
        return "42"
    }
}
