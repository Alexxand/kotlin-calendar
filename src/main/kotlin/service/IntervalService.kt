package service

import java.time.Duration
import java.time.Instant

interface IntervalService {

    /**
     * Ищет ближайший к now интервал, длина которого больше, чем minDuration, и который не пересекается ни с одним из заданных интервалов
     * Предполагается, что интервалы во втором аргументе отсортированы по startTime в порядке возрастания, но могут пересекаться друг с другом
     * Также предполагается, что для каждого интервала его конец больше его начала
     * Если интервал с требуемой минимальной длиной можно найти только после всех интервалов из второго аргумента,
     * то длина возвращаемого интервала равна minDuration
     */
    fun findNearestInterval(now: Instant, intervals: List<Pair<Instant, Instant>>, minDuration: Duration): Pair<Instant, Instant>
}

class DefaultIntervalService: IntervalService {

    override fun findNearestInterval(now: Instant, intervals: List<Pair<Instant, Instant>>, minDuration: Duration): Pair<Instant, Instant> {
        val intervalsFromNow = intervals.filter{ it.first >= now }
        if (intervalsFromNow.isEmpty())
            return Pair(now, now.plus(minDuration))

        if (Duration.between(now, intervalsFromNow[0].first) > minDuration)
            return Pair(now, intervalsFromNow[0].first)

        var endContinuousPeriod = intervalsFromNow[0].second

        for (period in intervals) {
            if (
                period.first > endContinuousPeriod
                && Duration.between(endContinuousPeriod, period.first) >= minDuration
            )
                return Pair(endContinuousPeriod, period.first)
            else if (period.second < endContinuousPeriod)
                continue
            else
                endContinuousPeriod = period.second
        }
        return Pair(endContinuousPeriod, endContinuousPeriod.plus(minDuration))
    }
}