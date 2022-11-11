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

    fun create(cmdKey: String, appContext: AppContext, session: Session): Command {
        if (cmdKey in commands) {
            val cmd = commands[cmdKey]?.invoke() as Command
            cmd.initialise(appContext, session, cmdKey)
            return cmd
        }
        // invalid command
        return object : Command() {
            override fun execute(cmd: String, args: Map<String, String>): CommandResult {
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
    lateinit var parser: CommandParser
    lateinit var suppliedCommand: String

    open val spec = ""


    private var prompts: MutableMap<String,(e:Command, prompt: String) -> CommandResult> =
        mutableMapOf<String,(e:Command, prompt: String) -> CommandResult>()


    fun initialise(appContext: AppContext, session: Session, suppliedCommand: String): Command {
        this.appCtx = appContext
        this.session = session
        this.parser = RegexCommandParser(this.spec)
        this.suppliedCommand = suppliedCommand
        if(!parser.build()) throw Exception("Failed to parse command spec ${this.spec}")
        return this
    }

    fun parseAndExecute(input: String): CommandResult {
        val args = this.parser.parse(input) ?: return failSyntax()
        return execute(this.suppliedCommand, args)
    }

    open fun execute(cmd: String, args: Map<String, String>): CommandResult {
        TODO("base class method should never be called")
    }

    open fun executePrompt(cmdName: String): CommandResult {
        if (cmdName in prompts.keys) {
            return prompts.getValue(cmdName).invoke(this, cmdName)
        }
        return failInvalid("${cmdName} is not one of the choices. \n${promptChoices()}")
    }

    fun promptChoices() : String {
        val prompts = prompts.keys.joinToString(separator = "\n * ")
        return "Choose:\n * $prompts"
    }

    fun failSyntax(): CommandResult {
        return CommandResult(
            CommandResultEnum.FAIL, "Excuse me?",
            CommandFailReasonEnum.SYNTAX
        )
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

    fun successWithPrompts(msg: String): CommandResult {

        //make sure the user can cancel the prompts
        addPrompt("cancel") { p, c ->
            success("Ok, lets go back.")
        }
        return CommandResult(
            CommandResultEnum.PROMPT, "$msg\n${promptChoices()}"
        )
    }


    fun addPrompt(prompt: String, handler: (e:Command, prompt: String) -> CommandResult) {
        this.prompts.set(prompt, handler)
    }

    open fun matchPrompt(args: List<String>): Boolean {
        if (args[0] in prompts.keys) {
            return true
        }
        return false
    }


}

class CharGenCommand : Command() {

    override val spec = "CMD:chargen {name:STR} [class:STR]"

    override fun execute(cmd: String, args: Map<String, String>): CommandResult {

        val name = args["name"]!!
        if("class" in args) {
            val playerClass = args["class"]
            return success("Creating character ${name} class ${playerClass}")
        } else {

            addPrompt("wizard") { c, p ->
                println("Doing wizard")
                c.success("You're a wizard, ${name}!")
            }
            addPrompt("fighter") { c, p ->
                println("doing fighter")
                c.success("Rarr! you're a fighter!")
            }
            return (successWithPrompts("What sort of character do you want?"))
        }
    }
}


class LoginCommand : Command() {

    override val spec = "CMD:login {login:STR} [password:STR]"


    override fun execute(cmd: String, args: Map<String, String>): CommandResult {

        val account = appCtx.accountService.loadAccount(args["login"]!!)
            ?: return failInvalid(
                "Looks like you're new here!\n" +
                    "First create a new login like this: register <name> <password>.\n\n" +
                    "For example: \n\tregister fred freds_password"
            )

        if (!appCtx.sessionService.loginAccount(session, account.name, args["password"]!!)) {
            return failInvalid("Password incorrect, try again!")
        }
        var resultStr = "Welcome back, ${account.name}.\n"


        val chars = appCtx.sessionService.characters(account)
        if (chars.isNotEmpty()) {
            if (!args.containsKey("character")) {
                val names = chars.joinToString("\n * ") { it.name }
                resultStr = resultStr.plus("Which character do you want to log in with?\n * " +
                        names +
                        "\n\nChoose like this: login ${account.name} your_password with ${chars.first().name}"
                )
            } else {
                resultStr = resultStr.plus("with argument not implemented yet")

            }
        } else {
            resultStr  = resultStr.plus("You have no characters to play with. Create one with the 'chargen' command.")
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

/* a instance will be created for each connection */
class Interpreter(private val appContext: AppContext, private val session: Session) {
    val mgr = CommandManager()
    var promptCmd: Command? = null

    var args: List<String> = emptyList()

    init {
        mgr.add(listOf("login"), ::LoginCommand)
        mgr.add(listOf("chargen"), ::CharGenCommand)
    }


    fun commandNameFrom(input: String): String {
        val parts = input.split(" ").toMutableList()
        return(parts.removeAt(0))
    }

    fun process(input: String): CommandResult {
        try {
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
                result = cmd.executePrompt(cmdName)

            } else {
                cmd = mgr.create(cmdName, appContext, session)
                result = cmd.parseAndExecute(input)
            }

            when (result.status) {
                CommandResultEnum.PROMPT -> {
                    this.promptCmd = cmd
                }
                CommandResultEnum.COMPLETE -> {
                    println("Resetting prompt")
                    this.promptCmd = null
                }

                else -> {}
            }
            return result
        } catch (e: Exception) {
            return CommandResult(CommandResultEnum.FAIL, "Uh-oh: There was an error in the program! ${e.message}",
                CommandFailReasonEnum.INTERNAL_ERROR)
        }
    }
}
