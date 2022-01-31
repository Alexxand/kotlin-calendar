package model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class RepeatableMeetingInterval(
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    offset: ZoneOffset,
    repetitionType: RepetitionType
): Iterable<Interval> {

    /**
     * Проверяет, находится ли какой-нибудь повтор встречи внутри заданного интервала [startTime, endTime]
     * Если startTime==null, то считается, что начало интервала равно -inf
     * Если endTime==null, то считается, что конец интервала равен +inf
     */
    fun checkInside(startTime: Instant? = null, endTime: Instant? = null): Boolean {
        
    }
}