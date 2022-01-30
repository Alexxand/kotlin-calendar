import db.DefaultMeetingDao
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import org.jetbrains.exposed.sql.Database
import org.flywaydb.core.Flyway
import service.DefaultIntervalService
import java.util.*

fun main(args: Array<String>) {

    //Это нужно для того, чтобы на любой платформе начало и конец встречи считывались одинаково,
    //т. к. org.jetbrains.exposed.sql.javatime.datetime использует ZoneId.systemDefault() при записи и чтении из базы
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

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