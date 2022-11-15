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

class CharDeleteCommand : Command() {

    override val key = "chardelete"
    override val description = "Deletes a character you created previously."
    override val spec = "CMD:$key {name:STR}"

    override fun execute(cmd: String, args: Map<String, String>): CommandResult {
        val name = args["name"]!!

        val chars = appCtx.sessionService.characters(session.account)
        var charToDelete = chars.find { it.name == name && session.account.id == it.owner.id }

        if (charToDelete != null) {
            addPrompt("yes") {
                c, p ->
                appCtx.sessionService.deleteCharacter(session, charToDelete)
                success("Deleted $name.")
            }
            addPrompt("no") {
                    c, p ->
                success("Okay, lets not delete $name.")
            }
            return(successWithPrompts("Are you sure you want to delete $name?"))
        }
        return failInvalid("There is no such character $name.")
    }
}

class CharGenCommand : Command() {

    override val key = "chargen"
    override val description = "Creates a new character."
    override val spec = "CMD:$key {name:STR} [class:STR]"

    fun createAndPuppet(name: String, playerClass: String) : CommandResult {
        val character = appCtx.sessionService.createCharacter(session, name, playerClass)
        appCtx.sessionService.puppetCharacter(session, character)
        return success("You are $name the ${playerClass}.")
    }

    override fun execute(cmd: String, args: Map<String, String>): CommandResult {
        val name = args["name"]!!
        return if ("class" in args) {
            val playerClass = args["class"]!!
            createAndPuppet(name, playerClass)
        } else {
            addPrompt("wizard") { c, p ->
                createAndPuppet(name, p)
            }
            addPrompt("fighter") { c, p ->
                createAndPuppet(name, p)
            }
           successWithPrompts("What sort of character do you want?")
        }
    }
}

class CharPuppetCommand : Command() {

    override val key = "puppet"
    override val description = "Switch to this character."
    override val spec = "CMD:$key {character:STR}"

    fun createAndPuppet(name: String, playerClass: String) : CommandResult {
        val character = appCtx.sessionService.createCharacter(session, name, playerClass)
        appCtx.sessionService.puppetCharacter(session, character)
        return success("You are $name the ${playerClass}.")
    }

    override fun execute(cmd: String, args: Map<String, String>): CommandResult {
        val name = args["character"]!!
        val characters = appCtx.sessionService.characters(session.account)
        val char = characters.filter { it.name == name }.firstOrNull()
        return if (char != null) {
            appCtx.sessionService.puppetCharacter(session, char)
            success("You are now $name.")
        } else {
            failInvalid("No such character $name!")
        }
    }

}