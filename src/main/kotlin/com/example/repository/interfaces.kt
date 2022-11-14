package com.example.repository

import com.example.model.Account
import com.example.model.Character
import com.example.model.Session

interface DatabaseInterface {
    fun connect(url: String)
}

interface DatabaseAccessInterface {
    fun createSession(acct: Account, char: Character): Session
    fun saveNewSessionRetire(session: Session): Int
    fun updateSession(session: Session)
    fun searchSessions(acct: Account): List<Session>
    fun removeSession(session: Session)
    fun insertCharacter(acct: Account, char: Character): Int
    fun deleteCharacter(char: Character) : Boolean
    fun findCharactersByAccount(acct: Account): List<Character>
    fun findCharacterByName(charName: String) : Character?
    fun findAccountById(id: Int): Account
    fun findAccountByLogin(login: String): Account?
    fun insertAccount(login: String, password: String, isAdmin: Boolean): Account
}
