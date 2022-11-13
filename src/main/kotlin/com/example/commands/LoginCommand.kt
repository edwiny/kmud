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
        var resultStr = "Welcome back, ${account.name}.\n"

        val chars = appCtx.sessionService.characters(account)
        if (chars.isNotEmpty()) {
            if (!args.containsKey("character")) {
                val names = chars.joinToString("\n * ") { it.name }
                resultStr = resultStr.plus(
                    "Which character do you want to log in with?\n * " +
                        names +
                        "\n\nChoose like this: login ${account.name} your_password with ${chars.first().name}"
                )
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