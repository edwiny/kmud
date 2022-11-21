package com.example

import com.example.commands.CommandFailReasonEnum
import com.example.commands.CommandResult
import com.example.commands.CommandResultEnum
import com.example.commands.Interpreter
import com.example.config.*
import com.example.network.NettyWebsocketServer
import com.example.network.runKtorServer
import com.example.network.sendNetworkKtor

import io.ktor.server.websocket.*
import io.netty.channel.socket.SocketChannel
import io.netty.util.AttributeKey

fun main(args: Array<String>) {

    val appContext = AppContextFactory.getAppContext(AppProfilesEnum.RUNTIME, ConfigurationFactory.getConfigForEnvironment(EnvironmentEnum.DEV))

    val port = 8080
    val uri = "/chat"

    //runKtorServer(port, uri, appContext)
    val server = NettyWebsocketServer()
    server.newConnectionHandler { ch -> newConnectionHandler(appContext, ch) }
    server.closeConnectionHandler { ch -> closeConnectionHandler(appContext, ch) }
    server.incomingDataHandler { channel, message -> incomingDataHandler(appContext, channel, message) }
    server.intervalHandler { intervalHandler(appContext) }
    server.run(args)
}


fun newConnectionHandler(appContext: AppContext, channel: SocketChannel): Boolean {
    println("newConnectionHandler() called")

    appContext.sessionCtxManager.newSessionCtxFromChannel(appContext, channel)
    return true
}

fun closeConnectionHandler(appContext: AppContext, channel: SocketChannel): Boolean {
    println("closeConnectionHandler() called")
    appContext.sessionCtxManager.deleteSessionCtxByChannel(channel)
    return true
}

fun incomingDataHandler(appContext: AppContext, channel: SocketChannel, message: String) : String {

    println("incomingDataHandler() called")

    val sessionCtx = appContext.sessionCtxManager.findSessionCtxByChannel(channel)
    val result = sessionCtx!!.interpreter.process(message)
    var response = handleResponse(result)
    if (result.status == CommandResultEnum.CHAIN && result.chainCommand != null) {
        println("Doing chain command: ${result.chainCommand}")
        val result2 = sessionCtx.interpreter.process(result.chainCommand)
        response = handleResponse(result2)
        //TODO use recursion with safeguard
    }
    if (result.status == CommandResultEnum.EXIT) {
        TODO("implement exit functionality")
    }

    return response
}


fun intervalHandler(appContext: AppContext) : Boolean {
    println("intervalHandler() called")
    return true
}








//suspend fun DefaultWebSocketServerSession.handleResponse(result: CommandResult) {
fun handleResponse(result: CommandResult): String {
    var presentation: String = ""
    presentation = when (result.status) {
        CommandResultEnum.COMPLETE,
        CommandResultEnum.CHAIN,
        CommandResultEnum.EXIT,
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
    return presentation
    //sendNetworkKtor(presentation)
}

