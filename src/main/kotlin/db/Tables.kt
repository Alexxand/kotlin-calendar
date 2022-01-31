import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import model.RepetitionType

object Users: Table("users") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", length = 50)
    val lastname = varchar("lastname", length = 50)

    override val primaryKey = PrimaryKey(id)
}

object Meetings: Table("meetings") {
    val id = uuid("id").autoGenerate()
    val meetingOrganizerId = uuid("meeting_organizer_id") references Users.id
    val startTime = datetime("start_time").index("meeting_start_idx")
    val endTime = datetime("end_time")
    val timeZoneOffsetId = varchar("timezone_offset", 10)
    val repetitionType = enumeration("repetition_type", RepetitionType::class).nullable()

    override val primaryKey = PrimaryKey(id)
}

object MeetingInvitations: Table("meeting_invitations") {
    val meetingId = uuid("meeting_id") references Meetings.id
    val invitedUserId = uuid("invited_user_id") references Users.id
    val accepted = bool("accepted").default(false)

    override val primaryKey = PrimaryKey(meetingId, invitedUserId)
}