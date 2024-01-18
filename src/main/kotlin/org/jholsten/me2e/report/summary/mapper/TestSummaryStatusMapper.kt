@file:JvmSynthetic

package org.jholsten.me2e.report.summary.mapper

import org.jholsten.me2e.report.summary.model.TestSummary
import org.junit.platform.engine.TestExecutionResult
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class TestSummaryStatusMapper {
    companion object {
        val INSTANCE: TestSummaryStatusMapper = Mappers.getMapper(TestSummaryStatusMapper::class.java)
    }

    abstract fun toInternalDto(status: TestExecutionResult.Status): TestSummary.Status
}
