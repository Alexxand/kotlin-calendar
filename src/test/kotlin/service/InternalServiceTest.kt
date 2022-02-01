package service

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class IntervalServiceTest {
    private val intervalService: IntervalService = DefaultIntervalService()

    private val interval = Pair(
        LocalDateTime.of(2022, 1, 28, 15, 0, 0).toInstant(ZoneOffset.UTC),
        LocalDateTime.of(2022, 1, 28, 16, 0, 0).toInstant(ZoneOffset.UTC)
    )

    private val now = LocalDateTime.of(2022, 1, 28, 15, 0, 0).toInstant(ZoneOffset.UTC)

    private val minDuration = Duration.parse("PT10M")

    @Test
    fun intersectWithEmpty() {
        assertFalse(intervalService.intersects(interval, emptyList()))
    }

    @Test
    fun intersectWithNotEmpty1() {
        val intervals = listOf(
            Pair(
                LocalDateTime.of(2022, 1, 28, 13, 0, 0).toInstant(ZoneOffset.UTC),
                LocalDateTime.of(2022, 1, 28, 15, 0, 0).toInstant(ZoneOffset.UTC)
            ),
            Pair(
                LocalDateTime.of(2022, 1, 28, 17, 0, 0).toInstant(ZoneOffset.UTC),
                LocalDateTime.of(2022, 1, 28, 18, 0, 0).toInstant(ZoneOffset.UTC)
            )
        )
        assertFalse(intervalService.intersects(interval, intervals))
    }

    @Test
    fun intersectWithNotEmpty2() {
        val intervals = listOf(
            Pair(
                LocalDateTime.of(2022, 1, 28, 14, 0, 0).toInstant(ZoneOffset.UTC),
                LocalDateTime.of(2022, 1, 28, 15, 30, 0).toInstant(ZoneOffset.UTC)
            ),
            Pair(
                LocalDateTime.of(2022, 1, 28, 15, 40, 0).toInstant(ZoneOffset.UTC),
                LocalDateTime.of(2022, 1, 28, 16, 30, 0).toInstant(ZoneOffset.UTC)
            )
        )
        assertTrue(intervalService.intersects(interval, intervals))
    }

    @Test
    fun findNearestWithEmpty() {
        assertEquals(
            Pair(now, now.plus(minDuration)),
            intervalService.findNearestInterval(now, emptyList(), minDuration)
        )
    }

    @Test
    fun findNearestBefore() {
        assertEquals(
            Pair(now, now.plus(minDuration.multipliedBy(2))),
            intervalService.findNearestInterval(
                now,
                listOf(
                    Pair(
                        now.plus(minDuration.multipliedBy(2)),
                        now.plus(minDuration.multipliedBy(3)))
                ),
                minDuration
            )
        )
    }

    @Test
    fun findNearestAfter() {
        assertEquals(
            Pair(now.plus(minDuration.multipliedBy(3)), now.plus(minDuration.multipliedBy(4))),
            intervalService.findNearestInterval(
                now,
                listOf(
                    Pair(
                        now.plus(minDuration.dividedBy(2)),
                        now.plus(minDuration.multipliedBy(3)))
                ),
                minDuration
            )
        )
    }

    @Test
    fun findNearestInside() {
        val intervals = listOf(
            Pair(
                now.plus(minDuration.dividedBy(2)),
                now.plus(minDuration.multipliedBy(2))
            ),
            Pair(
                now.plus(minDuration),
                now.plus(minDuration.multipliedBy(4))
            ),
            Pair(
                now.plus(minDuration.multipliedBy(3)),
                now.plus(minDuration.multipliedBy(5))
            ),
            Pair(
                now.plus(minDuration.multipliedBy(10).dividedBy(2)),
                now.plus(minDuration.multipliedBy(7))
            ),
            Pair(
                now.plus(minDuration.multipliedBy(10).dividedBy(2)),
                now.plus(minDuration.multipliedBy(9))
            ),
            Pair(
                now.plus(minDuration.multipliedBy(11)),
                now.plus(minDuration.multipliedBy(13))
            )
        )
        assertEquals(
            Pair(now.plus(minDuration.multipliedBy(9)), now.plus(minDuration.multipliedBy(11))),
            intervalService.findNearestInterval(now, intervals, minDuration)
        )
    }
}