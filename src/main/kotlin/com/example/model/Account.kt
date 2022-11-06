package com.example.model

/*
object Accounts : IntIdTable() {
    //val id = integer("id").uniqueIndex()
    val name = varchar("name", 50)
    val admin = bool("is_admin").default(false)
}

class Account(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Account>(Accounts)
//    var id by Accounts.id
    var name by Accounts.name
    var admin by Accounts.admin
}

 */

class Account(
    val id: Int,
    val name: String,
    val admin: Boolean
)
