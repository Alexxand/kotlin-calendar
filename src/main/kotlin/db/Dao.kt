package db

import ResponseMeeting
import MeetingInfo
import MeetingInvitation
import MeetingInvitations
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

interface MeetingDao {
    /**
     * Возвращает список интервалов для всех встреч, которые пользователь создал,
     * либо на которые он был приглашён, при этом приняв приглашение
     * Возвращаемый список отсортирован по startTime
     */
    fun getSortedBusyIntervals(userId: UUID): List<Pair<Instant, Instant>>

    /**
     * Возвращает список интервалов для всех встреч, которые заданные пользователи создали,
     * либо на которые были приглашены, при этом приняв приглашение
     * Возвращаемый список отсортирован по startTime
     */
    fun getSortedBusyIntervals(userIds: List<UUID>): List<Pair<Instant, Instant>>

    /**
     * Возвращает все встречи для всех пользователей
     */
    fun getAllMeetings(): List<ResponseMeeting>

    /**
     * Возвращает встречу по её id или null, если встречи с таким id не существует
     */
    fun getMeeting(meetingId: UUID): ResponseMeeting?

    /**
     * Возвращает все встречи, которые пользователь создал, или на которые был приглашён, которые должны состоятся в указанном временном интервале
     * Если includeNotAccepted==true, включает в результат также встречи, на которые пользователь был приглашён, но пока не принял приглашение
     * Если startTime==null, считается, что заданный временной интервал начинается с -inf
     * Если endTime==null, считается, что заданный временной интервал оканчивается на +inf
     */
    fun getMeetings(userId: UUID, startTime: Instant? = null, endTime: Instant? = null, includeNotAccepted: Boolean = false): List<ResponseMeeting>

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
    override fun getSortedBusyIntervals(userId: UUID): List<Pair<Instant, Instant>> =
        transaction {
            Meetings
                .leftJoin(MeetingInvitations, { id }, { meetingId })
                .slice(Meetings.startTime, Meetings.endTime)
                .select(
                    (Meetings.meetingOrganizerId eq userId) or
                            ((MeetingInvitations.invitedUserId eq userId)
                                    and (MeetingInvitations.accepted eq true))
                )
                .orderBy(Meetings.startTime to SortOrder.ASC)
                .withDistinct(true)
                .map {
                    Pair(it[Meetings.startTime], it[Meetings.endTime])
                }
        }

    override fun getSortedBusyIntervals(userIds: List<UUID>): List<Pair<Instant, Instant>> =
        transaction {
            Meetings
                .leftJoin(MeetingInvitations, { id }, { meetingId })
                .slice(Meetings.startTime, Meetings.endTime)
                .select(
                    (Meetings.meetingOrganizerId inList userIds) or
                            ((MeetingInvitations.invitedUserId inList userIds)
                                    and (MeetingInvitations.accepted eq true))
                )
                .orderBy(Meetings.startTime to SortOrder.ASC)
                .withDistinct(true)
                .map {
                    Pair(it[Meetings.startTime], it[Meetings.endTime])
                }
        }

    override fun getAllMeetings(): List<ResponseMeeting> =
        transaction {
            Meetings.leftJoin(MeetingInvitations, { id }, { meetingId })
                .selectAll()
                .groupBy {
                    MeetingInfo(
                        it[Meetings.id],
                        it[Meetings.meetingOrganizerId],
                        it[Meetings.startTime],
                        it[Meetings.endTime]
                    )
                }
                .map {
                    ResponseMeeting(
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
                        it.key.endTime
                    )
                }
        }

    override fun getMeeting(meetingId: UUID): ResponseMeeting? =
        transaction {
            Meetings
                .leftJoin(MeetingInvitations, { id }, { MeetingInvitations.meetingId })
                .select({ Meetings.id eq meetingId })
                .groupBy {
                    MeetingInfo(
                        it[Meetings.id],
                        it[Meetings.meetingOrganizerId],
                        it[Meetings.startTime],
                        it[Meetings.endTime]
                    )
                }
                .map {
                ResponseMeeting(
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
                    it.key.endTime
                )
            }.singleOrNull()
        }

    override fun getMeetings(
        userId: UUID,
        startTime: Instant?,
        endTime: Instant?,
        includeNotAccepted: Boolean
    ): List<ResponseMeeting> {
        val meetings = transaction {
            Meetings
                .leftJoin(MeetingInvitations, { id }, { meetingId })
                .select(
                    (startTime?.let { Meetings.startTime greaterEq it } ?: Op.TRUE)
                            and (endTime?.let { Meetings.endTime lessEq it } ?: Op.TRUE)
                )
                .groupBy {
                    MeetingInfo(
                        it[Meetings.id],
                        it[Meetings.meetingOrganizerId],
                        it[Meetings.startTime],
                        it[Meetings.endTime]
                    )
                }
                .filter {
                    it.key.meetingOrganizerId == userId ||
                            (it.value.find { row ->
                                row[MeetingInvitations.invitedUserId] == userId &&
                                        (row[MeetingInvitations.accepted] || includeNotAccepted)
                            }?.let{true} ?: false)
                }
                .map {
                    ResponseMeeting(
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
                        it.key.endTime
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