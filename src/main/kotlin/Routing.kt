import db.MeetingDao
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import service.IntervalService
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*

fun Application.configureRouting(meetingDao: MeetingDao, intervalService: IntervalService) {

    routing {

        post("/users") {
            val user = call.receive<User>()
            val id = transaction {
                Users.insert {
                    it[name] = user.name
                    it[lastname] = user.lastname
                } get Users.id
            }
            call.respond(User(id, user.name, user.lastname))
        }

        get("/users") {
            val users = transaction {
                Users.selectAll().map {
                    User(it[Users.id], it[Users.name], it[Users.lastname])
                }
            }
            call.respond(users)
        }

        delete("/users/{id}") {
            val id = call.parameters["id"]?.let {
                try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
                } ?: return@delete call.respondText("Id is not given or has wrong format", status = HttpStatusCode.BadRequest)
            transaction {
                Users.deleteWhere { Users.id eq id }
            }
            call.respondText("", status = HttpStatusCode.OK)
        }

        post("/meetings") {
            val meeting = call.receive<Meeting>()
            if (meeting.startTime >= meeting.endTime)
                return@post call.respondText("startTime should be less than endTime", status = HttpStatusCode.BadRequest)
            val id = transaction {
                val id = Meetings.insert {
                    it[meetingOrganizerId] = meeting.meetingOrganizerId
                    it[startTime] = meeting.startTime
                    it[endTime] = meeting.endTime
                    it[timeZoneOffsetId] = meeting.timeZoneOffset.id
                    it[repetitionType] = meeting.repetitionType
                } get Meetings.id
                for (invitation in meeting.invitations.distinctBy { it.invitedUserId }) {
                    MeetingInvitations.insert {
                        it[meetingId] = id
                        it[invitedUserId] = invitation.invitedUserId
                        it[accepted] = invitation.accepted
                    }
                }
                id
            }
            call.respond(
                Meeting(
                    id,
                    meeting.meetingOrganizerId,
                    meeting.invitations,
                    meeting.startTime,
                    meeting.endTime,
                    meeting.timeZoneOffset,
                    meeting.repetitionType
                )
            )
        }

        put("/meetings/{meetingId}/updateInvitations") {
            val meetingId = call.parameters["meetingId"]?.let {
                try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
            } ?: return@put call.respondText("User id is not given or has wrong format", status = HttpStatusCode.BadRequest)

            val meetingInvitations = call.receive<List<MeetingInvitation>>()

            transaction {
                MeetingInvitations.deleteWhere { MeetingInvitations.meetingId eq meetingId }
                meetingInvitations.distinctBy{ it.invitedUserId }.forEach { invitation ->
                    MeetingInvitations.insert {
                        it[MeetingInvitations.meetingId] = meetingId
                        it[invitedUserId] = invitation.invitedUserId
                        it[accepted] = invitation.accepted
                    }
                }
            }
            call.respondText("", status = HttpStatusCode.OK)
        }

        get("/meetings") {
            call.respond(meetingDao.getAllMeetings())
        }

        get("/meetings/{meetingId}") {
            val meetingId = call.parameters["meetingId"]?.let {
                try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
            } ?: return@get call.respondText("Meeting id is not given or has wrong format", status = HttpStatusCode.BadRequest)

            val meeting = meetingDao.getMeeting(meetingId) ?: call.respondText("Meeting with the given id is not found", status = HttpStatusCode.NotFound)

            call.respond(meeting)
        }

        get("/meetings/nearestInterval") {
            val request = call.receive<NearestIntervalRequest>()
            val allBusyIntervals = meetingDao.getSortedBusyIntervals(request.userIds.map {it.id} )
            call.respond(intervalService.findNearestInterval(Instant.now(), allBusyIntervals, request.minDuration).let{ Interval(it.first, it.second) })
        }

        get("/users/{userId}/meetings") {
            val userId = call.parameters["userId"]?.let {
                try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
            } ?: return@get call.respondText("User id is not given or has wrong format", status = HttpStatusCode.BadRequest)

            val startTime = call.request.queryParameters["startTime"]?.let { Instant.parse(it) }

            val endTime = call.request.queryParameters["endTime"]?.let { Instant.parse(it) }

            val includeNotAccepted = call.request.queryParameters["includeNotAccepted"]?.let { it.toBoolean() } ?: false

            call.respond(meetingDao.getMeetings(userId, startTime, endTime, includeNotAccepted))
        }

        post("/users/{userId}/meetings/{meetingId}/makeActionOnInvitation") {
            val userId = call.parameters["userId"]?.let {
                try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
            } ?: return@post call.respondText("User id is not given or has wrong format", status = HttpStatusCode.BadRequest)

            val meetingId = call.parameters["meetingId"]?.let {
                try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
            } ?: return@post call.respondText("User id is not given or has wrong format", status = HttpStatusCode.BadRequest)

            val invitationAction = call.request.queryParameters["invitationAction"]?.let {
                try { InvitationAction.valueOf(it) } catch (e: IllegalArgumentException) { null }
            } ?: return@post call.respondText("invitationAction is not given or not equal to ACCEPT or REJECT", status = HttpStatusCode.BadRequest)

            when (invitationAction) {
                InvitationAction.ACCEPT -> meetingDao.acceptInvitation(userId, meetingId)
                InvitationAction.REJECT -> meetingDao.rejectInvitation(userId, meetingId)
            }

            call.respondText("", status = HttpStatusCode.OK)
        }

        delete("/meetings/{id}") {
            val id = call.parameters["id"]?.let {
                try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
            } ?: return@delete call.respondText("Id is not given or has wrong format", status = HttpStatusCode.BadRequest)
            transaction {
                MeetingInvitations.deleteWhere { MeetingInvitations.meetingId eq id}
                Meetings.deleteWhere { Meetings.id eq id }
            }
            call.respondText("", status = HttpStatusCode.OK)
        }
    }
}