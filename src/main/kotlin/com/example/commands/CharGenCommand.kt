package com.example.commands

import com.example.model.Character

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