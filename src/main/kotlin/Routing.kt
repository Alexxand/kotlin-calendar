import db.MeetingDao
import db.UserDao
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import service.IntervalService
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*

fun Application.configureRouting(userDao: UserDao, meetingDao: MeetingDao, intervalService: IntervalService) {

    routing {

        post("/users") {
            val user = call.receive<User>()
            val id = userDao.addUser(user)
            call.respond(id)
        }

        get("/users") {
            val users = userDao.getAllUsers()
            call.respond(users)
        }

        delete("/users/{id}") {
            val id = call.parameters["id"]?.let {
                try { UUID.fromString(it) } catch (e: IllegalArgumentException) { null }
                } ?: return@delete call.respondText("Id is not given or has wrong format", status = HttpStatusCode.BadRequest)
            userDao.deleteUser(id)
            call.respondText("", status = HttpStatusCode.OK)
        }

        post("/meetings") {
            val meeting = call.receive<RequestMeeting>()
            val busyPeriods = meetingDao.getSortedBusyIntervals(meeting.meetingOrganizerId)
            if (meeting.startTime >= meeting.endTime)
                return@post call.respondText("startTime should be less than endTime", status = HttpStatusCode.BadRequest)
            if (intervalService.intersects(Pair(meeting.startTime, meeting.endTime), busyPeriods))
                return@post call.respondText("Given meeting time period intersect with some already planned meeting period", status = HttpStatusCode.BadRequest)
            val id = meetingDao.addMeeting(meeting)
            call.respond(id)
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

            if (invitationAction == InvitationAction.ACCEPT) {
                val meeting = meetingDao.getMeeting(meetingId) ?: return@post call.respondText("Meeting with such id is not found", status = HttpStatusCode.NotFound)
                val busyPeriods = meetingDao.getSortedBusyIntervals(userId)
                if (intervalService.intersects(Pair(meeting.startTime, meeting.endTime), busyPeriods))
                    return@post call.respondText("This meeting time period intersect with some already planned meeting period", status = HttpStatusCode.BadRequest)
            }

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
            meetingDao.deleteMeeting(id)
            call.respondText("", status = HttpStatusCode.OK)
        }
    }
}