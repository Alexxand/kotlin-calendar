import java.util.*
import kotlinx.serialization.Serializable
import util.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Serializable
data class User(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String,
    val lastname: String
    )

@Serializable
data class MeetingInvitation(
    @Serializable(with = UUIDSerializer::class)
    val invitedUserId: UUID,
    val accepted: Boolean = false,
    )

@Serializable
data class Meeting(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val meetingOrganizerId: UUID,
    val invitations: List<MeetingInvitation>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startTime: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endTime: LocalDateTime,
    //Зона не может отличаться у начала и конца встречи,
    //поэтому вместо OffsetDateTime использую ZoneOffset
    //отдельно от временных полей
    @Serializable(with = ZoneOffsetSerializer::class)
    val timeZoneOffset: ZoneOffset
)

// Нужно, чтобы можно было использовать List<UserId> вместо List<UUID>, т. к. последний вариант
// не компилируется, даже если использовать метод ListSerializer() или создать ListUUIDSerializer
@Serializable
data class UserId(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID
)


@Serializable
data class NearestIntervalRequest(
    @Serializable(with = DurationSerializer::class)
    val minDuration: Duration,
    val userIds: List<UserId>
)

@Serializable
data class Interval(
    @Serializable(with = InstantSerializer::class)
    val startTime: Instant,
    @Serializable(with = InstantSerializer::class)
    val endTime: Instant
)

data class MeetingInfo(
    val id: UUID? = null,
    val meetingOrganizerId: UUID,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val timeZoneOffsetId: String
) {
    val timeZoneOffset = ZoneOffset.of(timeZoneOffsetId)
}

enum class InvitationAction {
    ACCEPT, REJECT
}
