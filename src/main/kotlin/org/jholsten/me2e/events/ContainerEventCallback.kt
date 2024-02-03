@file:JvmSynthetic

package org.jholsten.me2e.events

import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.api.model.Event
import java.util.function.Consumer

/**
 * Callback for the consumption of events of a Docker container.
 */
internal class ContainerEventCallback : ResultCallbackTemplate<ContainerEventCallback, Event>() {

    /**
     * Registered consumers for this result callback.
     */
    private val consumers: MutableList<Consumer<Event>> = mutableListOf()

    /**
     * Registers the given consumer to be notified when a new event is received.
     */
    fun addConsumer(consumer: Consumer<Event>) {
        consumers.add(consumer)
    }

    /**
     * Callback function which is executed when a new event is received.
     * Notifies all registered consumers.
     */
    override fun onNext(obj: Event?) {
        if (obj != null) {
            consumers.forEach { it.accept(obj) }
        }
    }
}
