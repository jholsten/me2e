package org.jholsten.me2e.container.stats

import com.github.dockerjava.api.model.Statistics
import org.jholsten.me2e.container.stats.model.ContainerStatsEntry
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.util.RecursiveComparison
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.*

internal class ContainerStatsConsumerTest {

    private val dockerStatsContent = """
        {
          "read": "2015-01-08T22:57:31.547920715Z",
          "pids_stats": {
            "current": 3
          },
          "networks": {
            "eth0": {
              "rx_bytes": 5338,
              "rx_dropped": 0,
              "rx_errors": 0,
              "rx_packets": 36,
              "tx_bytes": 648,
              "tx_dropped": 0,
              "tx_errors": 0,
              "tx_packets": 8
            },
            "eth5": {
              "rx_bytes": 4641,
              "rx_dropped": 0,
              "rx_errors": 0,
              "rx_packets": 26,
              "tx_bytes": 690,
              "tx_dropped": 0,
              "tx_errors": 0,
              "tx_packets": 9
            }
          },
          "memory_stats": {
            "stats": {
              "total_pgmajfault": 0,
              "cache": 25,
              "mapped_file": 0,
              "total_inactive_file": 0,
              "pgpgout": 414,
              "rss": 6537216,
              "total_mapped_file": 0,
              "writeback": 0,
              "unevictable": 0,
              "pgpgin": 477,
              "total_unevictable": 0,
              "pgmajfault": 0,
              "total_rss": 6537216,
              "total_rss_huge": 6291456,
              "total_writeback": 0,
              "total_inactive_anon": 0,
              "rss_huge": 6291456,
              "hierarchical_memory_limit": 67108864,
              "total_pgfault": 964,
              "total_active_file": 0,
              "active_anon": 6537216,
              "total_active_anon": 6537216,
              "total_pgpgout": 414,
              "total_cache": 0,
              "inactive_anon": 0,
              "active_file": 0,
              "pgfault": 964,
              "inactive_file": 0,
              "total_pgpgin": 477
            },
            "max_usage": 6651904,
            "usage": 6537216,
            "failcnt": 0,
            "limit": 67108864
          },
          "blkio_stats": {},
          "cpu_stats": {
            "cpu_usage": {
              "percpu_usage": [
                8646879,
                24472255,
                36438778,
                30657443
              ],
              "usage_in_usermode": 50000000,
              "total_usage": 100215355,
              "usage_in_kernelmode": 30000000
            },
            "system_cpu_usage": 7393065900,
            "online_cpus": 4,
            "throttling_data": {
              "periods": 0,
              "throttled_periods": 0,
              "throttled_time": 0
            }
          },
          "precpu_stats": {
            "cpu_usage": {
              "percpu_usage": [
                8646879,
                24350896,
                36438778,
                30657443
              ],
              "usage_in_usermode": 50000000,
              "total_usage": 100093996,
              "usage_in_kernelmode": 30000000
            },
            "system_cpu_usage": 94921400,
            "online_cpus": 4,
            "throttling_data": {
              "periods": 0,
              "throttled_periods": 0,
              "throttled_time": 0
            }
          }
        }
    """.trimIndent()

    private val dockerStatsWithoutOptionalsContent = """
        {
          "read": "2015-01-08T22:57:31.547920715Z",
          "pids_stats": {},
          "memory_stats": {
            "failcnt": 0
          },
          "blkio_stats": {},
          "cpu_stats": {},
          "precpu_stats": {}
        }
    """.trimIndent()

    private val consumer = TestConsumer()

    private class TestConsumer : ContainerStatsConsumer() {
        var entry: ContainerStatsEntry? = null
        override fun accept(entry: ContainerStatsEntry) {
            this.entry = entry
        }
    }

    @Test
    fun `Aggregating statistics with all values set should succeed`() {
        val statistics = DeserializerFactory.getObjectMapper().readValue(dockerStatsContent, Statistics::class.java)
        consumer.InternalStatsConsumer().accept(statistics)

        val entry = consumer.entry
        val expected = ContainerStatsEntry(
            timestamp = Instant.from(OffsetDateTime.of(2015, 1, 8, 22, 57, 31, 0, ZoneOffset.UTC)),
            memoryUsage = ContainerStatsEntry.MemoryUsage(
                usedMemory = 6537191,
                availableMemory = 67108864,
                percentage = BigDecimal("9.741200"),
            ),
            cpuUsage = ContainerStatsEntry.CpuUsage(
                percentage = BigDecimal("0.006651600"),
            ),
            networkUsage = ContainerStatsEntry.NetworkUsage(
                received = 9979,
                sent = 1338,
            ),
            pids = 3,
        )
        assertNotNull(entry)
        RecursiveComparison.assertEquals(expected, entry)
    }

    @Test
    fun `Aggregating statistics with no values set should succeed`() {
        val statistics = DeserializerFactory.getObjectMapper().readValue(dockerStatsWithoutOptionalsContent, Statistics::class.java)
        consumer.InternalStatsConsumer().accept(statistics)

        val entry = consumer.entry
        val expected = ContainerStatsEntry(
            timestamp = Instant.from(OffsetDateTime.of(2015, 1, 8, 22, 57, 31, 0, ZoneOffset.UTC)),
            memoryUsage = ContainerStatsEntry.MemoryUsage(
                usedMemory = null,
                availableMemory = null,
                percentage = null,
            ),
            cpuUsage = ContainerStatsEntry.CpuUsage(
                percentage = null,
            ),
            networkUsage = ContainerStatsEntry.NetworkUsage(
                received = null,
                sent = null,
            ),
            pids = null,
        )
        assertNotNull(entry)
        RecursiveComparison.assertEquals(expected, entry)
    }
}
