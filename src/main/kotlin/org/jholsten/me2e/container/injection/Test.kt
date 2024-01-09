package org.jholsten.me2e.container.injection

import org.jholsten.me2e.container.microservice.MicroserviceContainer

class Test {
    @Microservice
    var microservice: MicroserviceContainer? = null

    fun test() {
        println("YO")
        println(microservice)
    }
}
