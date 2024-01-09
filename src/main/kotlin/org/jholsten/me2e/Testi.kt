package org.jholsten.me2e

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Secured(val isLocked: Boolean = false)

@Aspect
class SecuredMethodAspect {
    @Suppress("unused")
    @Pointcut("@annotation(secured)")
    fun callAt(secured: Secured?) {
        print(secured)
        print("HOHOHO")
    }

    @Suppress("unused")
    @Before("execution(* org.jholsten.me2e.container.injection.SecuredMethod.*(..))")
    fun beforeMyMethod() {
        // Dein Aspect-Code vor myMethod
        print("HEYYY")
    }
}


class SecuredMethod {
    @Secured(isLocked = true)
    fun lockedMethod() {
    }

    @Secured(isLocked = false)
    fun unlockedMethod() {
    }
}
