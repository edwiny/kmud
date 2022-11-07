package com.example.repository

import com.example.model.Account
import com.example.model.Character
import com.example.model.Session

interface DatabaseInterface {
    fun connect(url: String)
}

interface DatabaseAccessInterface {
    fun createSession(acct: Account, char: Character): Session
    fun saveNewSession(session: Session) : Int
    fun updateSession(session: Session)
    fun searchSessions(acct: Account): List<Session>
    fun removeSession(session: Session)
    fun insertCharacter(char: Character): Int
    fun findCharactersByAccount(acct: Account): List<Character>
    fun findAccountById(id: Int): Account
    fun findAccountByLogin(login: String): Account?
    fun insertAccount(login: String, password: String, isAdmin: Boolean): Account
}
