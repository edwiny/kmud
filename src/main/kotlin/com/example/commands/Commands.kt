package com.example.commands

import com.example.config.AppContext

class CommandManager {
    val commands = mutableMapOf<String, () -> Any>()
    fun add(keys: List<String>, commandClass: () -> Any) {
        for (key in keys) {
            commands[key] = commandClass
        }
    }

    fun create(cmdKey: String): Command {
        if (cmdKey in commands) {
            return commands[cmdKey]?.invoke() as Command
        }
        //invalid command
        return object : Command {
            override fun match(args: List<String>): Boolean {
                TODO("Not yet implemented")
            }

            override fun execute(appContext: AppContext, cmd: String, args: List<String>): String {
                return "No such command"
            }
        }
    }
}

interface Command {
    fun match(args: List<String>): Boolean

    fun execute(ppContext: AppContext, cmd: String, args: List<String>): String
}

class LoginCommand : Command {
    override fun match(args: List<String>): Boolean {
        if (args.first().contentEquals("login")) {
            return true
        }
        return false
    }

    override fun execute(appContext: AppContext, cmd: String, args: List<String>): String {

        val account = appContext.dao.findAccountByLogin(args[0])
            ?: return "Looks like you're new here!\n" +
                    "First create a new login like this: register <name> <password>.\n\n" +
                    "For example: \n\tregister fred fred123"


        return "Logging you in ${account.name} id ${account.id}"
    }
}

class Interpreter(private val appContext: AppContext) {
    val mgr = CommandManager()

    var cmdInvoked = ""
    var args: List<String> = emptyList()


    init {
        mgr.add(listOf("login"), ::LoginCommand)
    }

    fun parse(input: String): Command? {

        var parts = input.split(" ").toMutableList()
        cmdInvoked = parts.removeAt(0)
        args = parts.toList()

        return mgr.create(cmdInvoked)
    }

    fun process(input: String): String {
        val cmd = parse(input) ?: return "Huh?"
        return cmd.execute(appContext, cmdInvoked, args)
    }
}

