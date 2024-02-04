package org.jholsten.me2e.report.result.mapper

import org.jholsten.me2e.report.result.model.TestResult
import org.junit.platform.engine.TestExecutionResult
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Mapper
internal abstract class TestSummaryStatusMapper {
    companion object {
        val INSTANCE: TestSummaryStatusMapper = Mappers.getMapper(TestSummaryStatusMapper::class.java)
    }

    abstract fun toInternalDto(status: TestExecutionResult.Status): TestResult.Status
}
