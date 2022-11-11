package com.example.commands

import java.lang.Exception
import java.util.regex.Pattern

open class CommandParser(val spec: String) {

    open fun build(): Boolean {
        TODO("CommandParser.parse() base class method called")

    }

    open fun parse(input: String) : Map<String, String>? {
        TODO("CommandParser.parse() base class method called")
    }
}


class RegexCommandParser(spec: String): CommandParser(spec) {
    lateinit var regex: Regex
    lateinit var regexStr: String
    val regexCommand = Regex("""CMD:(\w+)""")
    val regexLiteral =  Regex("""\w+""")
    val regexOptionalLiteral =  Regex("""\[(\w+)\]""")
    val regexRequiredStringArg = Regex("""\{(\w+):STR\}""")
    val regexRequiredIntArg = Regex("""\{(\w+):INT\}""")
    val regexOptionalStringArg = Regex("""\[(\w+):STR\]""")
    val regexOptionalIntArg = Regex("""\[(\w+):INT\]""")

    val components = mutableListOf<Component>()

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


    override fun build(): Boolean {

        println("RegexCommandParser building spec [${spec}]")
        val parts = spec.split(Regex("""\s"""), limit = 256)

        parts.forEach {
            var result: MatchResult
            if (regexLiteral matches it) {
                components.add(Component(TokenTypeEnum.LITERAL, it))
            } else if (regexCommand matches it) {
                val result = regexCommand.find(it)
                components.add(Component(TokenTypeEnum.COMMAND, result!!.groups[1]!!.value))
            } else if (regexOptionalLiteral matches it) {
                val result = regexOptionalLiteral.find(it)
                components.add(Component(TokenTypeEnum.OPTIONAL_LITERAL, result!!.groups[1]!!.value))
            } else if (regexOptionalStringArg matches it) {
                val result = regexOptionalStringArg.find(it)
                components.add(Component(TokenTypeEnum.OPTIONAL_STRING_ARG, result!!.groups[1]!!.value))
            } else if (regexRequiredStringArg matches it) {
                val result = regexRequiredStringArg.find(it)
                components.add(Component(TokenTypeEnum.STRING_ARG, result!!.groups[1]!!.value))
            } else if (regexOptionalIntArg matches it) {
                val result = regexOptionalIntArg.find(it)
                components.add(Component(TokenTypeEnum.OPTIONAL_INT_ARG, result!!.groups[1]!!.value))
            } else if (regexRequiredIntArg matches it) {
                val result = regexRequiredIntArg.find(it)
                components.add(Component(TokenTypeEnum.INT_ARG, result!!.groups[1]!!.value))
            } else {
                return false
            }
        }

        var regexStr = ""
        var first = 1
        for (component in components) {
            var space = "\\s+"
            val token = when (component.type){
                TokenTypeEnum.LITERAL, TokenTypeEnum.COMMAND -> "(${component.name})"
                TokenTypeEnum.OPTIONAL_LITERAL -> { space = "\\s*"; "(${component.name})?" }
                TokenTypeEnum.OPTIONAL_STRING_ARG -> { space = "\\s*";  "(?<${component.name}>\\w+)?" }
                TokenTypeEnum.STRING_ARG -> "(?<${component.name}>\\w+)"
                TokenTypeEnum.OPTIONAL_INT_ARG ->  { space = "\\s*"; "(?<${component.name}>\\d+)?" }
                TokenTypeEnum.INT_ARG -> "(?<${component.name}>\\d+)"
            }
            if(first == 1) {
                regexStr = regexStr.plus(token)
                first = 0
            } else {
                regexStr = regexStr.plus("${space}${token}")
            }

        }
        println("regex: $regexStr")

        this.regexStr = regexStr
        this.regex = Regex(regexStr)
        return true
    }

    override fun parse(input: String): Map<String, String>? {
        println("Trying to match [$input] with [$regexStr]")
        //val match = regex.find(input) ?: return null
        val pattern = Pattern.compile(this.regexStr)
        val matcher = pattern.matcher(input)
        if(!matcher.find()) return null

        val args = mutableMapOf<String, String>()

        for(i in 0 until components.size) {
            when(components[i].type) {
                TokenTypeEnum.STRING_ARG,
                TokenTypeEnum.INT_ARG -> {
                    //val value: String = matcher.group(components[i].name)
                    //println("got value for ${components[i].name}: $value ")
                    args[components[i].name] = matcher.group(components[i].name)
                }
                TokenTypeEnum.OPTIONAL_STRING_ARG,
                TokenTypeEnum.OPTIONAL_INT_ARG -> {
                    //val value: String = matcher.group(components[i].name)
                    //println("got value for ${components[i].name}: $value ")
                    val value = matcher.group(components[i].name)
                    if(value != null) {
                        args[components[i].name] = value
                    }
                }
                TokenTypeEnum.COMMAND, TokenTypeEnum.LITERAL -> {
                    // awful :(
                    if (i == 0) {
                        args["cmd"] = components[i].name
                    }
                }
                else -> {

                }
            }
        }
        return args
    }
}