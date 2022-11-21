package com.example.network

import com.example.commands.CommandResultEnum
import com.example.commands.Interpreter
import com.example.config.AppContext
import com.example.handleResponse
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet

lateinit var  server: NettyApplicationEngine

fun runKtorServer(port: Int, uri: String, appContext: AppContext) {
    server = embeddedServer(Netty, port) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            val connections = Collections.synchronizedSet<KtorConnection?>(LinkedHashSet())

            webSocket(uri) {
                println("Adding user!")
                val thisConnection = KtorConnection(this)
                val session = appContext.sessionService.emptySession()
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
suspend fun DefaultWebSocketServerSession.sendNetworkKtor(presentation: String) {
    send(presentation ?: "")
}

class KtorConnection(val socket: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(1)
    }

    val name = "user${lastId.getAndIncrement()}"
}