package com.example.commands

import com.example.config.AppContext
import com.example.model.Session

open class Command {

    lateinit var appCtx: AppContext
    lateinit var session: Session
    lateinit var parser: CommandParser
    lateinit var suppliedCommand: String

    open val key = ""
    open val spec = ""
    open val description = ""

    private var prompts: MutableMap<String, (e: Command, prompt: String) -> CommandResult> =
        mutableMapOf<String, (e: Command, prompt: String) -> CommandResult>()

    fun initialise(appContext: AppContext, session: Session, suppliedCommand: String): Command {
        this.appCtx = appContext
        this.session = session
        this.parser = RegexCommandParser(this.spec)
        this.suppliedCommand = suppliedCommand
        if (!parser.build()) throw Exception("Failed to parse command spec ${this.spec}")
        return this
    }

    open fun help(detailed: Boolean) : String {
        return "${this.key} - ${this.description}\n\n${this.parser.simpleHelpSyntax()}"
    }

    open fun helpListing() : String {
        return "${this.key} - ${this.description}"
    }

    fun parseAndExecute(input: String): CommandResult {
        val args = this.parser.parse(input) ?: return failSyntax()
        if("help" in args) {
            return(success(help(false)))
        }
        return execute(this.suppliedCommand, args)
    }

    open fun execute(cmd: String, args: Map<String, String>): CommandResult {
        TODO("base class method should never be called")
    }

    open fun executePrompt(cmdName: String): CommandResult {
        if (cmdName in prompts.keys) {
            return prompts.getValue(cmdName).invoke(this, cmdName)
        }
        return failInvalid("$cmdName is not one of the choices. \n${promptChoices()}")
    }

    fun promptChoices(): String {
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

        // make sure the user can cancel the prompts
        addPrompt("cancel") { p, c ->
            success("Ok, lets go back.")
        }
        return CommandResult(
            CommandResultEnum.PROMPT, "$msg\n${promptChoices()}"
        )
    }

    fun addPrompt(prompt: String, handler: (e: Command, prompt: String) -> CommandResult) {
        this.prompts.set(prompt, handler)
    }

    open fun matchPrompt(args: List<String>): Boolean {
        if (args[0] in prompts.keys) {
            return true
        }
        return false
    }
}