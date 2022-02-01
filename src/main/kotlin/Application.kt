import db.DefaultMeetingDao
import db.DefaultUserDao
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import kotlinx.serialization.SerializationException
import org.jetbrains.exposed.sql.Database
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.exceptions.ExposedSQLException
import service.DefaultIntervalService

fun main(args: Array<String>) {

    val dataSource = hikari(commandLineEnvironment(args))

    val flyway = Flyway.configure().dataSource(dataSource).load()

    flyway.migrate()

    Database.connect(dataSource)

    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<SerializationException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
            throw cause
        }
        exception<ExposedSQLException> { cause ->
            call.respond(HttpStatusCode.ExpectationFailed, cause.message ?: "")
            throw cause
        }
    }
    val userDao = DefaultUserDao()
    val meetingDao = DefaultMeetingDao()
    val periodsServiceDao = DefaultIntervalService()
    configureRouting(userDao, meetingDao, periodsServiceDao)
}