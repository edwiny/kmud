package com.example.commands

import java.util.regex.Pattern

open class ParserState(val spec: String) {

}

/* The abstract class only has static methods, however kotlin does not
   have static methods, and the substitution companion object cannot be
   overridden.
 */
open class CommandParser {

    open fun build(spec: String): ParserState? {
        TODO("CommandParser.parse() base class method called")
    }

    open fun parseToArgs(input: String, state: ParserState): Map<String, String>? {
        TODO("CommandParser.parse() base class method called")
    }

    open fun simpleHelpSyntax(state: ParserState) : String {
        TODO("CommandParser().simpleHelpSyntax() base class method called")
    }

}

class RegexParserState(spec: String): ParserState(spec) {
    lateinit var regex: Regex
    lateinit var regexStr: String
    val components = mutableListOf<RegexCommandParser.Component>()

}

class RegexCommandParser : CommandParser() {
    val regexCommand = Regex("""CMD:(\w+)""")
    val regexLiteral = Regex("""\w+""")
    val regexOptionalLiteral = Regex("""\[(\w+)\]""")
    val regexRequiredStringArg = Regex("""\{(\w+):STR\}""")
    val regexRequiredIntArg = Regex("""\{(\w+):INT\}""")
    val regexOptionalStringArg = Regex("""\[(\w+):STR\]""")
    val regexOptionalIntArg = Regex("""\[(\w+):INT\]""")


    enum class TokenTypeEnum {
        COMMAND,
        LITERAL,
        OPTIONAL_LITERAL,
        STRING_ARG,
        INT_ARG,
        OPTIONAL_STRING_ARG,
        OPTIONAL_INT_ARG
    }

    class Component(val type: TokenTypeEnum, val name: String)

    override fun simpleHelpSyntax(state: ParserState): String {
        var resultStr = ""
        var first = 1
        val components = (state as RegexParserState).components
        for (component in components) {
            if(component.name == "help") continue
            var space = " "
            val token = when (component.type) {
                TokenTypeEnum.LITERAL, TokenTypeEnum.COMMAND -> "${component.name}"
                TokenTypeEnum.STRING_ARG -> "SOMETHING"
                TokenTypeEnum.INT_ARG -> "NUMBER"
                else -> {""}
            }
            if (first == 1) {
                resultStr = resultStr.plus(token)
                first = 0
            } else {
                resultStr = resultStr.plus("${space}$token")
            }
        }
        return resultStr
    }

    override fun build(spec: String): ParserState? {

        val state = RegexParserState(spec)

        println("RegexCommandParser building spec [$spec]")
        val parts = spec.split(Regex("""\s"""), limit = 256)

        parts.forEach {
            var result: MatchResult
            if (regexLiteral matches it) {
                state.components.add(Component(TokenTypeEnum.LITERAL, it))
            } else if (regexCommand matches it) {
                val result = regexCommand.find(it)
                state.components.add(Component(TokenTypeEnum.COMMAND, result!!.groups[1]!!.value))
            } else if (regexOptionalLiteral matches it) {
                val result = regexOptionalLiteral.find(it)
                state.components.add(Component(TokenTypeEnum.OPTIONAL_LITERAL, result!!.groups[1]!!.value))
            } else if (regexOptionalStringArg matches it) {
                val result = regexOptionalStringArg.find(it)
                state.components.add(Component(TokenTypeEnum.OPTIONAL_STRING_ARG, result!!.groups[1]!!.value))
            } else if (regexRequiredStringArg matches it) {
                val result = regexRequiredStringArg.find(it)
                state.components.add(Component(TokenTypeEnum.STRING_ARG, result!!.groups[1]!!.value))
            } else if (regexOptionalIntArg matches it) {
                val result = regexOptionalIntArg.find(it)
                state.components.add(Component(TokenTypeEnum.OPTIONAL_INT_ARG, result!!.groups[1]!!.value))
            } else if (regexRequiredIntArg matches it) {
                val result = regexRequiredIntArg.find(it)
                state.components.add(Component(TokenTypeEnum.INT_ARG, result!!.groups[1]!!.value))
            } else {
                return null
            }
        }

        var regexStr = ""
        var first = 1
        for (component in state.components) {
            var space = "\\s+"
            val token = when (component.type) {
                TokenTypeEnum.LITERAL, TokenTypeEnum.COMMAND -> "(${component.name})((\\s+(?<help>help))|("
                TokenTypeEnum.LITERAL -> "(${component.name})"
                TokenTypeEnum.OPTIONAL_LITERAL -> { space = "\\s*"; "(${component.name})?" }
                TokenTypeEnum.OPTIONAL_STRING_ARG -> { space = "\\s*"; "(?<${component.name}>\\w+)?" }
                TokenTypeEnum.STRING_ARG -> "(?<${component.name}>\\w+)"
                TokenTypeEnum.OPTIONAL_INT_ARG -> { space = "\\s*"; "(?<${component.name}>\\d+)?" }
                TokenTypeEnum.INT_ARG -> "(?<${component.name}>\\d+)"
            }
            if (first == 1) {
                regexStr = regexStr.plus(token)
                first = 0
            } else {
                regexStr = regexStr.plus("${space}$token")
            }
        }

        //add implicit help arg
        state.components.add(Component(TokenTypeEnum.LITERAL, "help"))

        regexStr = "${regexStr}))"
        println("regex: $regexStr")


        state.regexStr = regexStr
        state.regex = Regex(regexStr)
        return state
    }

    override fun parseToArgs(input: String, state: ParserState): Map<String, String>? {
        val state = (state as RegexParserState)

        println("Trying to match [$input] with [$state.regexStr]")
        // val match = regex.find(input) ?: return null
        val pattern = Pattern.compile(state.regexStr)
        val matcher = pattern.matcher(input)
        if (!matcher.find()) return null

        val args = mutableMapOf<String, String>()

        for (i in 0 until state.components.size) {
            println("[$i] attempting to match [${state.components[i].name}]")
            when (state.components[i].type) {
                TokenTypeEnum.STRING_ARG,
                TokenTypeEnum.INT_ARG -> {
                    println("looking to see if named group ${state.components[i].name} has been matched...")
                    val value: String? = matcher.group(state.components[i].name)
                    println("the value for ${state.components[i].name}: $value ")
                    if (value != null) {
                        args[state.components[i].name] = value
                    }
                }
                TokenTypeEnum.OPTIONAL_STRING_ARG,
                TokenTypeEnum.OPTIONAL_INT_ARG -> {
                    println("looking to see if optional named group ${state.components[i].name} has been matched...")
                    val value = matcher.group(state.components[i].name)
                    println("got value for ${state.components[i].name}: $value ")

                    if (value != null) {
                        args[state.components[i].name] = value
                    }
                }
                TokenTypeEnum.COMMAND -> {
                    // awful :(
                    if (i == 0) {
                        args["cmd"] = state.components[i].name
                    }
                }

                TokenTypeEnum.LITERAL -> {
                    println("looking to see if literal ${state.components[i].name} has been matched...")
                    val value = matcher.group(state.components[i].name)
                    println("got value for ${state.components[i].name}: $value ")

                    if (value != null) {
                        args[state.components[i].name] = value
                    }

                }

                else -> {
                }
            }
        }
        return args
    }
}
