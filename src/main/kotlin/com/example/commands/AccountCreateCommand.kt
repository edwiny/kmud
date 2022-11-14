package com.example.commands

class AccountCreateCommand : Command() {

    override val key = "register"
    override val description = "Creates a new login."
    override val spec = "CMD:$key {name:STR} {password:STR}"

    override fun execute(cmd: String, args: Map<String, String>): CommandResult {
        val name = args["name"]!!
        val password = args["password"]!!

        session.account = appCtx.accountService.createAccount(name, password)
        if (!appCtx.sessionService.loginAccount(session, name, password)) {
            return failInternalError("Login failed")
        }

        return successWithChain("Account created.", "login $name $password")
    }
}