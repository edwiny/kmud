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

    fun create(cmdKey: String): Command {
        if (cmdKey in commands) {
            return commands[cmdKey]?.invoke() as Command
        }
        // invalid command
        return object : Command() {
            override fun match(args: List<String>): Boolean {
                TODO("Not yet implemented")
            }

            override fun execute(cmd: String, args: List<String>): CommandResult {
                return failInvalid("No such command")
            }
        }
    }
}
enum class CommandResultEnum {
    FAIL,
    PROMPT,
    COMPLETE
}

enum class CommandFailReasonEnum {
    INTERNAL_ERROR,
    SYNTAX,
    TARGET_NOT_FOUND,
    INVALID
}

class CommandResult(
    val status: CommandResultEnum,
    val presentation: String? = null,
    val failReason: CommandFailReasonEnum? = null,
    val meta: Map<String, String>? = null
)

open class Command {

    lateinit var appCtx: AppContext
    lateinit var session: Session

    fun setContext(appContext: AppContext, session: Session): Command {
        this.appCtx = appContext
        this.session = session
        return this
    }
    open fun match(args: List<String>): Boolean {
        return true
    }

    open fun execute(cmd: String, args: List<String>): CommandResult {
        TODO("base class method should never be called")
    }

    fun failInvalid(msg: String): CommandResult {
        return CommandResult(
            CommandResultEnum.FAIL, msg,
            CommandFailReasonEnum.INVALID
        )
    }

    fun success(msg: String): CommandResult {
        return CommandResult(
            CommandResultEnum.COMPLETE, msg
        )
    }
}

class LoginCommand : Command() {
    override fun match(args: List<String>): Boolean {
        if (args.first().contentEquals("login")) {
            return true
        }
        return false
    }

    override fun execute(cmd: String, args: List<String>): CommandResult {

        val account = appCtx.accountService.loadAccount(args[0])
            ?: return failInvalid(
                "Looks like you're new here!\n" +
                    "First create a new login like this: register <name> <password>.\n\n" +
                    "For example: \n\tregister fred freds_password"
            )

        val chars = appCtx.sessionService.characters(account)
        if (chars.count() > 1) {
            if (args.indexOf("with") <= 0) {
                val names = chars.joinToString("\n") { it.name }
                return failInvalid(
                    "Which character do you want to log in with?\n," +
                        names +
                        "\nChoose like this: login fred freds_password with Blinky973"
                )
            } else {
                TODO("Implement multiple char login")
            }
        } else if (chars.count() == 1) {

            appCtx.sessionService.startSessionWithChar(session, acct = account, chars[0])

            return success("Welcome back, ${session.account.name}. You are playing as ${session.character.name}")
        } else {
            return failInvalid("You need to create a character first.")
        }
    }
}

/* a instance will be created for each connection */
class Interpreter(private val appContext: AppContext, private val session: Session) {
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

        return mgr.create(cmdInvoked).setContext(appContext, session)
    }

    fun process(input: String): CommandResult {
        try {
            val cmd = parse(input) ?: return CommandResult(
                CommandResultEnum.FAIL, "Huh?",
                CommandFailReasonEnum.INVALID
            )
            return cmd.execute(cmdInvoked, args)
        } catch (e: Exception) {
            return CommandResult(CommandResultEnum.FAIL, "Uh-oh: There was an error in the program! ${e.message}",
                CommandFailReasonEnum.INTERNAL_ERROR)
        }
    }
}
