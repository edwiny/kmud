package com.example.commands

import com.example.config.AppContext
import com.example.model.Session

class CommandRepository(val parser: CommandParser) {

    data class CommandPatternMapping(
        val commandRef: () -> Any,
        val parserState: ParserState
    )

    val commands = mutableListOf<CommandPatternMapping>()

    fun addCommand(commandClass: () -> Any) {
        val parserState = parser.build((commandClass.invoke() as Command).spec) ?:
            throw Exception("Command spec for $commandClass failed to build")
        commands.add(CommandPatternMapping(commandRef = commandClass, parserState = parserState))
    }

    fun createCommand(textInput: String, appContext: AppContext, session: Session): Command? {
        var args: Map<String, String>? = mutableMapOf()
        val pair = commands.firstOrNull {
            print("${textInput}: trying command: ${it.parserState.spec}")
            args = parser.parseToArgs(textInput, it.parserState )
            args != null
        }
        if (pair != null) {
            print("Found: ${pair.commandRef} / ${pair.parserState.spec}")
            val cmd = pair.commandRef.invoke() as Command
            cmd.initialise(appContext, session, textInput, args?: throw Exception())
            return cmd
        }
        print("Command not found")

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
class CommandRuntime(private val appContext: AppContext, private val session: Session) {
    var promptCmd: Command? = null
    var args: List<String> = emptyList()

    fun resetPrompt() {
        this.promptCmd = null
    }

    fun process(input: String): CommandResult {
        var result: CommandResult?
        val cmd: Command

        try {

            if (this.promptCmd != null) {
                cmd = this.promptCmd ?: throw Exception("Stored promptCmd unexpectedly null")
                println("Rerouting to prompt command")
                resetPrompt()
                result = cmd.executePrompt(input)
            } else {
                cmd = appContext.commandRepository.createCommand(input, appContext, session) ?: return CommandResult(
                    CommandResultEnum.FAIL, "Not recognised: $input", CommandFailReasonEnum.COMMAND_NOT_FOUND
                )
                result = cmd.parseAndExecute()
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
        } catch (e: Exception) {
            return CommandResult(
                CommandResultEnum.FAIL, "Uh-oh: There was an error in the program! ${e.message} ${e.cause?:"unknown cause"}",
                CommandFailReasonEnum.INTERNAL_ERROR
            )
        }
    }
}
