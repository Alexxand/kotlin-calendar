import java.util.*
import kotlinx.serialization.Serializable
import util.DurationSerializer
import util.InstantSerializer
import util.UUIDSerializer
import java.time.Duration
import java.time.Instant

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
    @Serializable(with = InstantSerializer::class)
    val startTime: Instant,
    @Serializable(with = InstantSerializer::class)
    val endTime: Instant
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
    val startTime: Instant,
    val endTime: Instant
)

enum class InvitationAction {
    ACCEPT, REJECT
}
