import db.DefaultMeetingDao
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import org.jetbrains.exposed.sql.Database
import org.flywaydb.core.Flyway
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
    val meetingDao = DefaultMeetingDao()
    val periodsServiceDao = DefaultIntervalService()
    configureRouting(meetingDao, periodsServiceDao)
}