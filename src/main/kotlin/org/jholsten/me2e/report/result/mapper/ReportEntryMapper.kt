package org.jholsten.me2e.report.result.mapper

import org.jholsten.me2e.report.result.model.ReportEntry
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.factory.Mappers
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.TimeZone

@Mapper
internal abstract class ReportEntryMapper {
    companion object {
        @JvmSynthetic
        val INSTANCE: ReportEntryMapper = Mappers.getMapper(ReportEntryMapper::class.java)
    }

    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = ["mapTimestamp"])
    internal abstract fun toInternalDto(reportEntry: org.junit.platform.engine.reporting.ReportEntry): ReportEntry

    @Named("mapTimestamp")
    protected fun mapTimestamp(timestamp: LocalDateTime): Instant {
        return ZonedDateTime.of(timestamp, TimeZone.getDefault().toZoneId()).toInstant()
    }
}
