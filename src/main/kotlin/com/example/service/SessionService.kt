package com.example.service

import com.example.model.Account
import com.example.model.Character
import com.example.model.Session
import com.example.repository.DatabaseAccessInterface
import io.netty.channel.socket.SocketChannel
import java.time.LocalDateTime

interface SessionService {
    fun characters(acct: Account): List<Character>
    fun emptySession(): Session
    fun emptySessionNetty(channel: SocketChannel): Session
    fun findSession(channel: SocketChannel): Session?
    fun createSessionOld(acct: Account, charName: String): Session
    fun removeSession(session: Session)
    fun numSessions(): Int
    fun loginAccount(session: Session, login: String, password: String): Boolean
    fun puppetCharacter(session: Session, char: Character): Boolean
    fun createCharacter(session: Session, charName: String, playerClass: String): Character
    fun deleteCharacter(session: Session, char: Character): Boolean
    fun logout(session: Session)
}

class SessionServiceImpl(val dao: DatabaseAccessInterface) : SessionService {
    var sessionsOld = mutableListOf<Session>()
    val connections = mutableMapOf<SocketChannel, Session>()
    val sessions = mutableMapOf<Session, SocketChannel>()

    private val anonAccount = Account(0, "anon", "", false)
    private val anonChar = Character(0, "noface", anonAccount)

    override fun characters(acct: Account): List<Character> {
        return dao.findCharactersByAccount(acct)
    }

    //side-effect
    override fun emptySession(): Session {
        val session = Session(
            id = 0, account = anonAccount, character = anonChar,
            startTime = LocalDateTime.now()
        )
        sessionsOld.add(session)
        return session
    }

    override fun emptySessionNetty(channel: SocketChannel) : Session {
        val session = Session(
            id = 0, account = anonAccount, character = anonChar,
            startTime = LocalDateTime.now()
        )
        sessionsOld.add(session)
        sessions[session] = channel
        connections[channel] = session
        return session
    }

    override fun findSession(channel: SocketChannel): Session? {
        if(channel in connections) {
            return connections[channel]!!
        }
        return null
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
        sessionsOld.remove(session)
    }

    override fun numSessions(): Int {
        return sessionsOld.count()
    }

    override fun puppetCharacter(session: Session, char: Character): Boolean {
        if (session.account.id == 0) throw Exception("SessionService.puppterCharacter(): unexpected anon account")
        session.character = char
        dao.updateSession(session)
        return true
    }

    override fun createCharacter(session: Session, charName: String, playerClass: String): Character {
        var character = Character(
            id = 0,
            name = charName,
            session.account
        )

        val id = dao.insertCharacter(session.account, character)
        return Character(
            id = id,
            name = character.name,
            owner = character.owner
        )
    }

    override fun deleteCharacter(session: Session, char: Character): Boolean {

        //implement validation that char is not in use
        return dao.deleteCharacter(char)
    }

    override fun logout(session: Session) {
        dao.updateSession(session)
    }
}
