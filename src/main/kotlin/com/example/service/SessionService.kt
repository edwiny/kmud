package com.example.service

import com.example.model.Account
import com.example.model.Character
import com.example.model.Session
import com.example.repository.DatabaseAccessInterface
import io.ktor.websocket.*
import java.time.LocalDateTime

interface SessionService {
    fun characters(acct: Account): List<Character>
    fun emptySessionFromSocket(socket: DefaultWebSocketSession): Session
    fun createSessionOld(acct: Account, charName: String): Session
    fun removeSession(session: Session)
    fun numSessions(): Int
    fun loginAccount(session: Session, login: String, password: String) : Boolean
    fun puppetCharacter(session: Session, char: Character) : Boolean
}

class SessionServiceImpl(val dao: DatabaseAccessInterface) : SessionService {
    var sessions =  mutableListOf<Session>()

    private val anonAccount = Account(0, "anon", "", false)
    private val anonChar = Character(0, "noface" , anonAccount)

    override fun characters(acct: Account): List<Character> {
        return dao.findCharactersByAccount(acct)
    }

    override fun emptySessionFromSocket(socket: DefaultWebSocketSession): Session {
        val session = Session(id = 0, account = anonAccount, character = anonChar,
        startTime = LocalDateTime.now(), socket = socket)
        sessions.add(session)
        return session
    }

    override fun loginAccount(session: Session, login: String, password: String): Boolean {
        val acct = dao.findAccountByLogin(login) ?: return false
        // the session passed in to this function is a dummy session that was created
        // when the web socket connection was established.
        // Here we must update it with the id that the db allocated for it.
        if (password == acct.pwHash) {
            val fromDbSession = dao.createSession(acct, anonChar)
            session.id = fromDbSession.id
            session.account = acct
            return true

        }
        return false
    }


    override fun createSessionOld(acct: Account, charName: String): Session {
        val character = dao.findCharactersByAccount(acct).first {
            it.name == charName
        }
        return dao.createSession(acct, character,)
    }

    override fun removeSession(session: Session) {
        println("Removing session for ${session.account.name}.")
        dao.removeSession(session)
        sessions.remove(session)
    }

    override fun numSessions(): Int {
        return sessions.count()
    }


    override fun puppetCharacter(session: Session, char: Character): Boolean {
        if (session.account.id == 0) throw Exception("SessionService.puppterCharacter(): unexpected anon account")
        session.character = char
        dao.updateSession(session)
        return true
    }
}