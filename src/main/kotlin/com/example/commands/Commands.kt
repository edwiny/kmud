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
        return object : Command {
            override fun match(args: List<String>): Boolean {
                TODO("Not yet implemented")
            }

            override fun execute(): String {
                return "No such command"
            }
        }
    }
}

interface Command {
    fun match(args: List<String>): Boolean

    fun execute(): String
}

class LoginCommand : Command {
    override fun match(args: List<String>): Boolean {
        if (args.first().contentEquals("login")) {
            return true
        }
        return false
    }

    override fun execute(): String {
        return "Logging you in"
    }
}

class Interpreter(val appContext: AppContext) {
    val mgr = CommandManager()
    init {
        mgr.add(listOf("login"), ::LoginCommand)
    }

    fun parse(input: String): Command? {
        val parts = input.split(" ")
        return mgr.create(parts.first())
    }

    fun process(input: String): String {
        val cmd = parse(input) ?: return "Huh?"
        return cmd.execute()
    }
}

