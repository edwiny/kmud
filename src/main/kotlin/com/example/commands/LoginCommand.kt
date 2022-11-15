package com.example.commands

class LoginCommand : Command() {

    override val key = "login"
    override val description = "Logs a player into their account."

    override val spec = "CMD:login {login:STR} {password:STR}"

    override fun execute(cmd: String, args: Map<String, String>): CommandResult {

        val account = appCtx.accountService.loadAccount(args["login"]!!)
            ?: return failInvalid(
                "Looks like you're new here!\n" +
                    "First create a new login like this: register <name> <password>.\n\n" +
                    "For example: \n\tregister fred ilovepranks"
            )

        if (!appCtx.sessionService.loginAccount(session, account.name, args["password"]!!)) {
            return failInvalid("Password incorrect, try again!")
        }
        var resultStr = ""


        val chars = appCtx.sessionService.characters(account)
        if (chars.isNotEmpty()) {
            resultStr = "Welcome back, ${account.name}.\n"
            if (!args.containsKey("character")) {
                chars.forEach {
                    addPrompt(it.name) {
                        c, p -> successWithChain("Switching to $p", "puppet $p")
                    }
                }
                return successWithPrompts("Welcome back, ${account.name}. Which character do you want to play with?")
            } else {
                resultStr = resultStr.plus("with argument not implemented yet")
            }
        } else {
            resultStr = resultStr.plus("You have no characters to play with. Create one with the 'chargen' command.")
        }

        return success(resultStr)

        /* skip auto login for now
        } else if (chars.count() == 1) {

            appCtx.sessionService.puppetCharacter(session, chars[0])
            return success("Welcome back, ${session.account.name}. You are playing as ${session.character.name}")
        }
        */
    }
}

class LogoutCommand : Command() {

    override val key = "logout"
    override val description = "Save and logout."
    override val spec = "CMD:$key"

    fun createAndPuppet(name: String, playerClass: String) : CommandResult {
        val character = appCtx.sessionService.createCharacter(session, name, playerClass)
        appCtx.sessionService.puppetCharacter(session, character)
        return success("You are $name the ${playerClass}.")
    }

    override fun execute(cmd: String, args: Map<String, String>): CommandResult {
        appCtx.sessionService.logout(session)
        return CommandResult(status = CommandResultEnum.EXIT, "Bye!")
    }

}