package com.example.commands

import com.example.config.AppContext
import com.example.model.Session

class CommandManager {
    val commands = mutableMapOf<String, () -> Any>()
    fun add(keys: List<String>, commandClass: () -> Any) {
        for (key in keys) {
            commands[key] = commandClass
        }
    }

    fun create(cmdKey: String, appContext: AppContext, session: Session): Command? {
        if (cmdKey in commands) {
            val cmd = commands[cmdKey]?.invoke() as Command
            cmd.initialise(appContext, session, cmdKey)
            return cmd
        }
        // invalid command
        return null

    }
}
enum class CommandResultEnum {
    FAIL,
    PROMPT,
    COMPLETE,
    CHAIN,
    EXIT
}

enum class CommandFailReasonEnum {
    INTERNAL_ERROR,
    SYNTAX,
    TARGET_NOT_FOUND,
    COMMAND_NOT_FOUND,
    INVALID
}

class CommandResult(
    val status: CommandResultEnum,
    val presentation: String? = null,
    val failReason: CommandFailReasonEnum? = null,
    val chainCommand: String? = null,
    val meta: Map<String, String>? = null
)

/* a instance will be created for each connection */
class Interpreter(private val appContext: AppContext, private val session: Session) {
    val mgr = CommandManager()
    var promptCmd: Command? = null


    var args: List<String> = emptyList()

    init {
        mgr.add(listOf("login"), ::LoginCommand)
        mgr.add(listOf("chargen"), ::CharGenCommand)
        mgr.add(listOf("charlist"), ::CharListCommand)
        mgr.add(listOf("chardelete"), ::CharDeleteCommand)
        mgr.add(listOf("register"), ::AccountCreateCommand)
        mgr.add(listOf("puppet"), ::CharPuppetCommand)
        mgr.add(listOf("logout", "quit"), ::LogoutCommand)
    }

    fun resetPrompt() {
        this.promptCmd = null
    }

    fun commandNameFrom(input: String): String {
        val parts = input.split(" ").toMutableList()
        return(parts.removeAt(0))
    }

    fun process(input: String): CommandResult {
//        try {
            val cmdName = commandNameFrom(input)
            if (cmdName.isBlank()) {
                return CommandResult(
                    CommandResultEnum.FAIL, "Huh?",
                    CommandFailReasonEnum.INVALID
                )
            }

            var result: CommandResult? = null
            val cmd: Command

            if (this.promptCmd != null) {
                println("Rerouting to prompt command")
                cmd = this.promptCmd!!
                resetPrompt()
                result = cmd.executePrompt(cmdName)
            } else {
                cmd = mgr.create(cmdName, appContext, session) ?: return CommandResult(
                    CommandResultEnum.FAIL, "Command $cmdName not found.", CommandFailReasonEnum.COMMAND_NOT_FOUND
                )
                result = cmd.parseAndExecute(input)
            }

            when (result.status) {
                CommandResultEnum.PROMPT -> {
                    this.promptCmd = cmd
                }
                CommandResultEnum.COMPLETE -> {

                }
                else -> {}
            }
            return result
  /*      } catch (e: Exception) {
            return CommandResult(
                CommandResultEnum.FAIL, "Uh-oh: There was an error in the program! ${e.message} ${e.cause?:"unknown cause"}",
                CommandFailReasonEnum.INTERNAL_ERROR
            )
        }

   */
    }
}
