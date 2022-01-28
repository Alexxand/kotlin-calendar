import io.ktor.application.*
import io.ktor.server.engine.*
import org.jetbrains.exposed.sql.Database
import org.flywaydb.core.Flyway

fun main(args: Array<String>) {

    val dataSource = hikari(commandLineEnvironment(args))

    val flyway = Flyway.configure().dataSource(dataSource).load()

    flyway.migrate()

    Database.connect(dataSource)

    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureRouting()
}