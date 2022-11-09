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
    private var prompts: MutableMap<String,(e:Command, prompt: String) -> CommandResult> =
        mutableMapOf<String,(e:Command, prompt: String) -> CommandResult>()

    

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

    open fun executePrompt(cmd: String, args: List<String>): CommandResult {
        if (cmd in prompts.keys) {
            return prompts.getValue(cmd).invoke(this, cmd)
        }
        return failInvalid("${cmd} is not one of the choices. \n${promptChoices()}")
    }

    fun promptChoices() : String {
        val prompts = prompts.keys.joinToString(separator = "\n * ")
        return "Choose:\n * $prompts"
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
    override fun match(args: List<String>): Boolean {
        if (args.first().contentEquals("chargen")) {
            return true
        }
        return false
    }

    override fun execute(cmd: String, args: List<String>): CommandResult {

        val p1 = fun(c: Command, p: String) : CommandResult {
            println("Doing wizard")
            return c.success("You're a wizard, Harry!")
        }

        addPrompt("wizard", p1)
        addPrompt("fighter") { c, p ->
            println("doing fighter")
            c.success("Rarr! you're a fighter!")
        }

        return(successWithPrompts("What sort of character do you want?"))
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

        if (!appCtx.sessionService.loginAccount(session, account.name, args[1])) {
            return failInvalid("Password incorrect, try again!")
        }
        var resultStr = "Welcome back, ${account.name}.\n"


        val chars = appCtx.sessionService.characters(account)
        if (chars.isNotEmpty()) {
            if (args.indexOf("with") <= 0) {
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

    var cmdInvoked = ""
    var args: List<String> = emptyList()

    init {
        mgr.add(listOf("login"), ::LoginCommand)
        mgr.add(listOf("chargen"), ::CharGenCommand)
    }

    fun parse(input: String): Command? {

        var parts = input.split(" ").toMutableList()
        cmdInvoked = parts.removeAt(0)
        args = parts.toList()
        if (promptCmd != null) return promptCmd

        return mgr.create(cmdInvoked).setContext(appContext, session)
    }

    fun process(input: String): CommandResult {
        try {
            val cmd = parse(input) ?: return CommandResult(
                CommandResultEnum.FAIL, "Huh?",
                CommandFailReasonEnum.INVALID
            )

            var result: CommandResult? = null

            if (promptCmd != null && cmd == promptCmd) {
                println("Rerouting to propmt command")
                result = cmd.executePrompt(cmdInvoked, args)

            } else {
                result = cmd.execute(cmdInvoked, args)
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
