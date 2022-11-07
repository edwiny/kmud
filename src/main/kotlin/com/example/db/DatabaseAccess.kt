package com.example.db

import com.example.model.Account
import com.example.model.Character
import com.example.model.Session
import com.example.repository.DatabaseAccessInterface
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Exception
import java.time.LocalDateTime

class DatabaseAccess(val jdbcLocation: String) : DatabaseAccessInterface {

    object Accounts : IntIdTable() {
        val name: Column<String> = varchar("name", 50).uniqueIndex()
        val admin: Column<Boolean> = bool("admin")
    }

    object Characters : IntIdTable() {
        val name: Column<String> = varchar("name", 50).uniqueIndex()
        val owner: Column<EntityID<Int>> = reference(name = "owner_id", foreign = Accounts)
    }

    object Sessions : IntIdTable() {
        val accountId: Column<EntityID<Int>> = reference(name = "account_id", foreign = Accounts)
        val characterId: Column<EntityID<Int>> = reference(name = "char_id", foreign = Characters)
        var startTime: Column<LocalDateTime> = datetime("start_time")
    }

    init {
        Database.connect(jdbcLocation)

        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Accounts)
            SchemaUtils.create(Characters)
            SchemaUtils.create(Sessions)
        }
    }

    override fun createSession(acct: Account, char: Character): Session {
        var id = 0
        transaction {
            id = Sessions.insertAndGetId {
                it[accountId] = acct.id
                it[characterId] = char.id
                it[startTime] = LocalDateTime.now()
            }.value
        }
        return Session(
            id = id,
            account = acct,
            character = char,
            startTime = LocalDateTime.now()
        )
    }

    override fun saveNewSession(session: Session): Int {
        if(session.account != null && session.account.id > 0 &&
                session.character != null && session.character.id > 0) {
            val tmp = createSession(session.account, session.character)
            return tmp.id
        }
        throw Exception("Unexpected state when saving new session")
    }

    override fun updateSession(session: Session) {
        transaction {
            Sessions.update({ Sessions.id eq session.id }) {
                it[accountId] = session.account.id
                it[characterId] = session.character.id
            }
        }
    }

    override fun searchSessions(acct: Account): List<Session> {
        val resultList = mutableListOf<Session>()

        val rows = Sessions.select { Sessions.accountId eq acct.id }
        if (rows.empty()) {
            return resultList
        }
        val acctChars = findCharactersByAccount(acct)
        for (row in rows) {
            val char = acctChars.first {
                it.owner.id == row[Sessions.characterId].value
            }
            resultList.add(
                Session(
                    id = row[Sessions.id].value,
                    account = acct,
                    character = char,
                    startTime = row[Sessions.startTime]
                )
            )
            return resultList
        }
        return resultList
    }

    override fun removeSession(session: Session) {
        transaction {
            Sessions.deleteWhere { Sessions.id eq session.id }
        }
    }

    override fun insertCharacter(char: Character): Int {
        var id = 0
        transaction {
            id = Characters.insertAndGetId {
                it[name] = char.name
                it[owner] = char.owner.id
            }.value
        }
        return id
    }

    override fun findCharactersByAccount(acct: Account): List<Character> {
        val resultList = mutableListOf<Character>()
        transaction {
            Characters.select { Characters.owner eq acct.id }.forEach {
                resultList.add(
                    Character(
                        id = it[Characters.id].value,
                        name = it[Characters.name],
                        owner = acct
                    )
                )
            }
        }

        return resultList
    }

    override fun findAccountById(id: Int): Account {
        val row = Accounts.select { Accounts.id eq id }.first()
        return Account(id = id, name = row[Accounts.name], admin = row[Accounts.admin])
    }

    override fun findAccountByLogin(login: String): Account? {

        var result: Account? = null

        transaction {
            val rows = Accounts.select {
                Accounts.name eq login
            }

            if (rows.count() > 0) {
                val first = rows.first()
                result = Account(
                    id = first[Accounts.id].value,
                    name = first[Accounts.name],
                    admin = first[Accounts.admin]
                )
            }
        }
        return result
    }

    override fun insertAccount(login: String, password: String, isAdmin: Boolean): Account {
        var id = 0
        transaction {
            id = Accounts.insertAndGetId {
                it[name] = login
                it[admin] = isAdmin
            }.value
        }
        return Account(
            id = id,
            name = login,
            admin = isAdmin
        )
    }
}
