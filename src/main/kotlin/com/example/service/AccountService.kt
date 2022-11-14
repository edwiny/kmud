package com.example.service

import com.example.model.Account
import com.example.model.Session
import com.example.repository.DatabaseAccessInterface

interface AccountService {
    fun loadAccount(login: String): Account?
    fun createAccount(login: String, password: String): Account
}

class AccountServiceImp(val dao: DatabaseAccessInterface) : AccountService {
    override fun loadAccount(login: String): Account? {
        return(dao.findAccountByLogin(login))
    }

    override fun createAccount(login: String, password: String): Account {
        return (dao.insertAccount(login, password, false))
    }
}
