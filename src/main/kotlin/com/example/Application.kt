package com.example

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
                connections += thisConnection
                val interpreter = Interpreter(appContext)

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
                        val resp = interpreter.process(receivedText)
                        send(resp)
                    }
                } catch (e: Exception) {
                    println(e.localizedMessage)
                } finally {
                    println("Removing $thisConnection!")
                    connections -= thisConnection
                }
            }
        }
    }.start(wait = true)
}

/*
@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {

    configureSockets()
    configureRouting()
}

 */