package org.jholsten.me2e.container.logging

/**
 * List of log entries that were collected so far.
 */
class LogEntryList : ArrayList<LogEntry>() {
    override fun toString(): String {
        return this.joinToString("")
    }
}
