package com.example

import com.example.commands.CommandFailReasonEnum
import com.example.commands.CommandResult
import com.example.commands.CommandResultEnum
import com.example.commands.Interpreter
import com.example.config.AppContextFactory
import com.example.config.AppProfilesEnum
import com.example.config.ConfigurationFactory
import com.example.config.EnvironmentEnum
import io.ktor.server.application.*

import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>) {

    // Database.connect("jdbc:sqlite:kmud.sqlite")

    // val acct_repo = DatabaseAccess()

    // val id = acct_repo.insert(Account(0, "ron", true))
    // println("Created account with id $id")

    /*
    transaction {
        val needle = acct_repo.findAccountByLogin("edwin")
        println("result of search: ${needle?.id} ${needle?.name} ${needle?.admin}")
    }

     */

    // old method
    // io.ktor.server.netty.EngineMain.main(args)

    val appContext = AppContextFactory.getAppContext(AppProfilesEnum.RUNTIME, ConfigurationFactory.getConfigForEnvironment(EnvironmentEnum.DEV))

    embeddedServer(Netty, 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

            webSocket("/chat") {
                println("Adding user!")
                val thisConnection = Connection(this)
                val session = appContext.sessionService.emptySessionFromSocket(this)
                connections += thisConnection
                val interpreter = Interpreter(appContext, session)

                try {
                    send("You are connected! There are ${connections.count()} users here.")

                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val receivedText = frame.readText()
                        println("message received! $receivedText")
                        /*
                        val textWithUsername = "[${thisConnection.name}]: $receivedText"
                        connections.forEach {
                            it.session.send(textWithUsername)
                        }

                         */
                        val result = interpreter.process(receivedText)
                        handleResponse(result)
                        if (result.status == CommandResultEnum.CHAIN && result.chainCommand != null) {
                            println("Doing chain command: ${result.chainCommand}")


                            val result2 = interpreter.process(result.chainCommand)
                            handleResponse(result2)
                        }
                        if (result.status == CommandResultEnum.EXIT) break
                    }
                } catch (e: Exception) {
                    println(e.localizedMessage)
                    println(e.stackTraceToString())
                } finally {
                    println("Removing $thisConnection!")
                    connections -= thisConnection
                    appContext.sessionService.removeSession(session)
                }
            }
        }
    }.start(wait = true)
}

private suspend fun DefaultWebSocketServerSession.handleResponse(result: CommandResult) {
    var presentation: String = ""
    presentation = when (result.status) {
        CommandResultEnum.COMPLETE,
        CommandResultEnum.CHAIN,
        CommandResultEnum.EXIT
            ,
        CommandResultEnum.PROMPT -> result.presentation ?: ""
        CommandResultEnum.FAIL -> {
            when (result.failReason) {
                CommandFailReasonEnum.INVALID -> "Whoops! ${result.presentation ?: ""}"
                CommandFailReasonEnum.SYNTAX -> "That is not quite right, something is wrong with the syntax."
                CommandFailReasonEnum.INTERNAL_ERROR -> throw Exception(result.presentation)
                else -> {
                    "Error! ${result.presentation ?: ""}"
                }
            }
        }
        else -> {
            "Command Result Not implemented yet"
        }
    }
    send(presentation ?: "")
}

/*
@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {

    configureSockets()
    configureRouting()
}

 */
