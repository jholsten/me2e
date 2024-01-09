package org.jholsten.me2e.abc;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;

public class DeepThoughtAspect2 {
    @Pointcut("execution(* org.jholsten.me2e.abc.DeepThought2.*())")
    public void deepThought() {
        System.out.println("HE");
    }

    @Around("deepThought()")
    public String answer() {
        System.out.println("HO");
        return "42";
    }
}
