package db

import Id
import User
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface UserDao {
    fun addUser(user: User): Id

    fun deleteUser(userId: UUID)

    fun getAllUsers(): List<User>
}

class DefaultUserDao: UserDao {
    override fun addUser(user: User): Id =
        transaction {
            val id = Users.insert {
                it[name] = user.name
                it[lastname] = user.lastname
            } get Users.id
            Id(id)
        }

    override fun deleteUser(userId: UUID) {
        transaction {
            Users.deleteWhere { Users.id eq userId }
        }
    }


    override fun getAllUsers(): List<User> =
        transaction {
            Users.selectAll().map {
                User(it[Users.id], it[Users.name], it[Users.lastname])
            }
        }

}