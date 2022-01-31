package util

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun instantOf(localDateTime: LocalDateTime, timeZoneOffset: ZoneOffset):Instant =
    localDateTime.atOffset(timeZoneOffset).toInstant()

fun instantOf(localDateTimeColumn: Column<LocalDateTime>, timeZoneOffsetIdColumn: Column<String>):Column<Instant> {
    localDateTimeColumn.
}


fun instantOf(localDateTime: LocalDateTime, timeZoneOffsetId: String):Instant =
    localDateTime.atOffset(ZoneOffset.of(timeZoneOffsetId)).toInstant()