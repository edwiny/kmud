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
    fun startSessionWithChar(session: Session, acct: Account, char: Character) : Boolean
}

class SessionServiceImpl(val dao: DatabaseAccessInterface) : SessionService {
    var sessions =  mutableListOf<Session>()

    private val anonAccount = Account(0, "anon", false)
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

    override fun startSessionWithChar(session: Session, acct: Account, char: Character): Boolean {
        session.account = acct
        session.character = char
        session.id = dao.saveNewSession(session)
        return true
    }
}
