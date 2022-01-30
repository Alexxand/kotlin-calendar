package util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun instantOf(localDateTime: LocalDateTime, timeZoneOffset: ZoneOffset):Instant =
    localDateTime.atOffset(timeZoneOffset).toInstant()

fun instantOf(localDateTime: LocalDateTime, timeZoneOffset: String):Instant =
    localDateTime.atOffset(ZoneOffset.of(timeZoneOffset)).toInstant()