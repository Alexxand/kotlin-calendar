package db

import model.Meeting
import model.MeetingInfo
import model.MeetingInvitation
import MeetingInvitations
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.asLiteral
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.javatime.hour
import org.jetbrains.exposed.sql.transactions.transaction
import util.instantOf
import java.time.Instant
import java.util.*

interface MeetingDao {

    /**
     * Возвращает список интервалов для всех встреч, которые заданные пользователи создали,
     * либо на которые были приглашены, при этом приняв приглашение
     * Возвращаемый список отсортирован по startTime
     */
    fun getSortedBusyIntervals(userIds: List<UUID>): List<Pair<Instant, Instant>>

    /**
     * Возвращает все встречи для всех пользователей
     */
    fun getAllMeetings(): List<Meeting>

    /**
     * Возвращает встречу по её id или null, если встречи с таким id не существует
     */
    fun getMeeting(meetingId: UUID): Meeting?

    /**
     * Возвращает все встречи, которые пользователь создал, или на которые был приглашён, которые должны состоятся в указанном временном интервале
     * Если includeNotAccepted==true, включает в результат также встречи, на которые пользователь был приглашён, но пока не принял приглашение
     * Если startTime==null, считается, что заданный временной интервал начинается с -inf
     * Если endTime==null, считается, что заданный временной интервал оканчивается на +inf
     */
    fun getMeetings(userId: UUID, startTime: Instant? = null, endTime: Instant? = null, includeNotAccepted: Boolean = false): List<Meeting>

    /**
     * Принять приглашение пользователя на встречу
     */
    fun acceptInvitation(userId: UUID, meetingId: UUID)

    /**
     * Отклонить приглашение пользователя на встречу
     */
    fun rejectInvitation(userId: UUID, meetingId: UUID)
}

class DefaultMeetingDao: MeetingDao {

    override fun getSortedBusyIntervals(userIds: List<UUID>): List<Pair<Instant, Instant>> =
        transaction {
            Meetings
                .leftJoin(MeetingInvitations, { id }, { meetingId })
                .slice(Meetings.startTime, Meetings.endTime, Meetings.timeZoneOffsetId)
                .select(
                    (Meetings.meetingOrganizerId inList userIds) or
                            ((MeetingInvitations.invitedUserId inList userIds)
                                    and (MeetingInvitations.accepted eq true))
                )
                .orderBy(Meetings.startTime to SortOrder.ASC)
                .withDistinct(true)
                .map {
                    val startTime = it[Meetings.startTime]
                    val endTime = it[Meetings.endTime]
                    val timeZoneOffset = it[Meetings.timeZoneOffsetId]
                    Pair(instantOf(startTime, timeZoneOffset), instantOf(endTime, timeZoneOffset))
                }
        }

    override fun getAllMeetings(): List<Meeting> =
        transaction {
            Meetings.leftJoin(MeetingInvitations, { id }, { meetingId })
                .selectAll()
                .groupBy {
                    MeetingInfo(
                        it[Meetings.id],
                        it[Meetings.meetingOrganizerId],
                        it[Meetings.startTime],
                        it[Meetings.endTime],
                        it[Meetings.timeZoneOffsetId],
                        it[Meetings.repetitionType]
                    )
                }
                .map {
                    Meeting(
                        it.key.id,
                        it.key.meetingOrganizerId,
                        it.value
                            .filter { row -> row.getOrNull(MeetingInvitations.invitedUserId) != null }
                            .map { row ->
                                MeetingInvitation (
                                    row[MeetingInvitations.invitedUserId],
                                    row[MeetingInvitations.accepted]
                                ) },
                        it.key.startTime,
                        it.key.endTime,
                        it.key.timeZoneOffset,
                        it.key.repetitionType
                    )
                }
        }

    override fun getMeeting(meetingId: UUID): Meeting? =
        transaction {
            Meetings
                .leftJoin(MeetingInvitations, { id }, { MeetingInvitations.meetingId })
                .select({ Meetings.id eq meetingId })
                .groupBy {
                    MeetingInfo(
                        it[Meetings.id],
                        it[Meetings.meetingOrganizerId],
                        it[Meetings.startTime],
                        it[Meetings.endTime],
                        it[Meetings.timeZoneOffsetId],
                        it[Meetings.repetitionType]
                    )
                }
                .map {
                Meeting(
                    it.key.id,
                    it.key.meetingOrganizerId,
                    it.value
                        .filter { row -> row.getOrNull(MeetingInvitations.invitedUserId) != null }
                        .map { row ->
                            MeetingInvitation (
                                row[MeetingInvitations.invitedUserId],
                                row[MeetingInvitations.accepted]
                            ) },
                    it.key.startTime,
                    it.key.endTime,
                    it.key.timeZoneOffset,
                    it.key.repetitionType
                )
            }.singleOrNull()
        }

    override fun getMeetings(
        userId: UUID,
        startTime: Instant?,
        endTime: Instant?,
        includeNotAccepted: Boolean
    ): List<Meeting> {
        val meetings = transaction {
            Meetings
                .leftJoin(MeetingInvitations, { id }, { meetingId })
                .select(
                    (startTime?.let { instantOf(Meetings.startTime., Meetings.timeZoneOffsetId) greaterEq it } ?: Op.TRUE)
                            and (endTime?.let { Meetings.endTime lessEq it } ?: Op.TRUE)
                )
                .groupBy {
                    MeetingInfo(
                        it[Meetings.id],
                        it[Meetings.meetingOrganizerId],
                        it[Meetings.startTime],
                        it[Meetings.endTime],
                        it[Meetings.timeZoneOffsetId],
                        it[Meetings.repetitionType]
                    )
                }
                .filter {
                    val meetingStartTimeInstant = instantOf(it.key.startTime, it.key.timeZoneOffset)
                    val meetingEndTimeInstant = instantOf(it.key.endTime, it.key.timeZoneOffset)
                    (startTime?.let { requestedStartTime -> meetingStartTimeInstant >= requestedStartTime } ?: true)
                            && (endTime?.let { requestedEndTime -> meetingEndTimeInstant <= requestedEndTime } ?: true)
                            &&
                            && (it.key.meetingOrganizerId == userId || (it.value.find { row ->
                                row[MeetingInvitations.invitedUserId] == userId &&
                                        (row[MeetingInvitations.accepted] || includeNotAccepted)
                            }?.let { true } ?: false))
                }
                .map {
                    Meeting(
                        it.key.id,
                        it.key.meetingOrganizerId,
                        it.value
                            .filter { row -> row.getOrNull(MeetingInvitations.invitedUserId) != null }
                            .map { row ->
                                MeetingInvitation(
                                    row[MeetingInvitations.invitedUserId],
                                    row[MeetingInvitations.accepted]
                                )
                            },
                        it.key.startTime,
                        it.key.endTime,
                        it.key.timeZoneOffset,
                        it.key.repetitionType
                    )
                }

        }
        return meetings
    }

    override fun acceptInvitation(userId: UUID, meetingId: UUID) {
        transaction {
            MeetingInvitations.update({
                (MeetingInvitations.invitedUserId eq userId) and (MeetingInvitations.meetingId eq meetingId)
            }) {
                it[accepted] = true
            }
        }
    }

    override fun rejectInvitation(userId: UUID, meetingId: UUID) {
        transaction {
            MeetingInvitations.deleteWhere{
                (MeetingInvitations.invitedUserId eq userId) and (MeetingInvitations.meetingId eq meetingId)
            }
        }
    }
}