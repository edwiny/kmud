package com.example.service.commands

import com.example.application.config.AppContext
import com.example.commands.CommandFailReasonEnum
import com.example.commands.CommandResult
import com.example.commands.CommandResultEnum
import com.example.model.Session

open class Command {

    /* Since these classes are called dynamically by class name reference, we cannot (or at least I haven't found
       a way to) call the constructor explicitly with arguments. In stead we define a invoke() operator function.
     */
    lateinit var appCtx: AppContext
    lateinit var session: Session
    lateinit var suppliedCommand: String
    lateinit var args: Map<String, String>

    open val key = ""
    open val spec = ""
    open val description = ""
    open val help = ""

    private var prompts: MutableMap<String, (e: Command, prompt: String) -> CommandResult> =
        mutableMapOf<String, (e: Command, prompt: String) -> CommandResult>()

    fun initialise(appContext: AppContext, session: Session, suppliedCommand: String, args: Map<String, String>): Command {
        this.appCtx = appContext
        this.session = session
        this.suppliedCommand = suppliedCommand
        this.args = args
        return this
    }

    open fun help(detailed: Boolean) : String {
        return "${this.key} - ${this.description}\n\n${this.help}"
    }

    open fun helpListing() : String {
        return "${this.key} - ${this.description}"
    }

    fun parseAndExecute(): CommandResult {
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

    fun failInternalError(msg: String): CommandResult {
        return CommandResult(
            CommandResultEnum.FAIL, msg,
            CommandFailReasonEnum.INTERNAL_ERROR
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

    fun successWithChain(msg: String, command: String): CommandResult {
        return CommandResult(
            CommandResultEnum.CHAIN, msg, null, chainCommand = command
        )
    }

    open fun matchPrompt(args: List<String>): Boolean {
        if (args[0] in prompts.keys) {
            return true
        }
        return false
    }
    /*
    operator fun invoke(appCtx: AppContext, session: Session, cmdKey: String): Command {
        return initialise(appCtx, session, cmdKey)
    }

     */
}