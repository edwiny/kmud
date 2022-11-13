package com.example.commands

class CharListCommand : Command() {

    override val key = "charlist"
    override val description = "List all your characters."
    override val spec = "CMD:charlist"

    override fun execute(cmd: String, args: Map<String, String>): CommandResult {
        val chars = appCtx.sessionService.characters(session.account)
        if (chars.isEmpty()) {
            return success("You currently have no characters to play with." +
                    "\n" +
                    "\nHINT: you can create a new character using the 'chargen' command.")
        }
        val resultstr = chars.joinToString("\n * ") { it.name }
        return success("You own the following characters:\n * $resultstr")
    }
}