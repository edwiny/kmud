package com.example.repository

import com.example.application.config.AppContext
import com.example.service.commands.Command
import com.example.service.commands.CommandParser
import com.example.service.commands.ParserState
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