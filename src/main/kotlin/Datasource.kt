import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*

fun hikari(environment: ApplicationEnvironment): HikariDataSource {
    val config = HikariConfig().apply {
        driverClassName = environment.config.property("hikari.driver_classname").getString()
        jdbcUrl = environment.config.property("hikari.jdbc_url").getString()
        username = environment.config.property("hikari.username").getString()
        password = environment.config.property("hikari.password").getString()
        maximumPoolSize = environment.config.propertyOrNull("hikari.maximum_pool_size")?.getString()?.toIntOrNull() ?: 10
    }
    config.validate()
    return HikariDataSource(config)
}