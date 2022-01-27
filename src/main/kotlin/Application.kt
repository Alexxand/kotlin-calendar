import io.ktor.application.*
import io.ktor.server.engine.*
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>) {

    Database.connect(hikari(commandLineEnvironment(args)))

    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureRouting()
}