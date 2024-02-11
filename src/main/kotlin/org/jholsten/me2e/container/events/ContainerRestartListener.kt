package org.jholsten.me2e.container.events

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.events.model.ContainerEvent

/**
 * Consumer which listens to a Container's `restart` events.
 * The sequence of events for a restart is as follows: (1) `kill`, (2) `die`, (3) `stop`, (4) `start`, (5) `restart`.
 * As certain properties of the container, such as its port mapping, can change during a restart (see
 * [GitHub Issue #31926](https://github.com/moby/moby/issues/31926)), this listener enables to react to such changes accordingly.
 * @constructor Instantiates a new restart listener.
 * @param container Container which should be notified in case the Docker container is restarted.
 */
class ContainerRestartListener(
    /**
     * Container which should be notified in case the Docker container is restarted.
     */
    private val container: Container,
) : ContainerEventConsumer() {

    /**
     * Notifies the [container] whenever the corresponding Docker container is restarted.
     * Transfers the timestamp of when the container was restarted.
     */
    override fun accept(event: ContainerEvent) {
        container.onRestart(event.timestamp)
    }
}
