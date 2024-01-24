package org.jholsten.me2e.report.tracing

import org.testcontainers.containers.GenericContainer

class NetworkMonitor(
    val networkId: String,
) {

    private val container = GenericContainer("TODO")

    fun start() {

    }

    fun collectPackets() {

    }
}
