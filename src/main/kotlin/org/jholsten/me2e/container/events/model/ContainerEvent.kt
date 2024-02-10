package org.jholsten.me2e.container.events.model

import java.time.Instant

/**
 * Model representing one event of a container returned by `docker events`.
 * @see com.github.dockerjava.api.model.Event
 */
data class ContainerEvent internal constructor(
    /**
     * Timestamp of when this event occurred.
     */
    val timestamp: Instant,

    /**
     * Type of the event.
     */
    val type: Type,
) {
    /**
     * Representation of the possible container event types.
     * @see <a href="https://docs.docker.com/engine/reference/commandline/system_events/#containers">Docker Documentation</a>
     */
    enum class Type(
        /**
         * Name of the event as specified by Docker.
         */
        val value: String,
    ) {
        ATTACH("attach"),
        COMMIT("commit"),
        COPY("copy"),
        CREATE("create"),
        DESTROY("destroy"),
        DETACH("detach"),
        DIE("die"),
        EXEC_CREATE("exec_create"),
        EXEC_DETACH("exec_detach"),
        EXEC_DIE("exec_die"),
        EXEC_START("exec_start"),
        EXPORT("export"),
        HEALTH_STATUS("health_status"),
        KILL("kill"),
        OOM("oom"),
        PAUSE("pause"),
        RENAME("rename"),
        RESIZE("resize"),
        RESTART("restart"),
        START("start"),
        STOP("stop"),
        TOP("top"),
        UNPAUSE("unpause"),
        UPDATE("update");

        companion object {
            @JvmSynthetic
            internal fun fromString(value: String): Type? {
                return Type.values().firstOrNull { it.value == value }
            }
        }
    }
}
