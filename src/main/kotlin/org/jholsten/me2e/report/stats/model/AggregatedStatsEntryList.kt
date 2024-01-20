package org.jholsten.me2e.report.stats.model

/**
 * List of stats entries that were collected from all containers.
 */
class AggregatedStatsEntryList(entries: Collection<AggregatedStatsEntry> = listOf()) : ArrayList<AggregatedStatsEntry>(entries.toList())
