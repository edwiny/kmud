package com.example

import com.example.application.config.*
import com.example.application.ctx.ConnectionContext
import com.example.commands.CommandFailReasonEnum
import com.example.commands.CommandResult
import com.example.commands.CommandResultEnum
import com.example.commands.CommandService
import com.example.infra.network.NettyWebsocketServer
import com.example.service.dto.Message

import io.netty.channel.socket.SocketChannel

fun main(args: Array<String>) {

    val appContext = AppContextFactory.getAppContext(
        AppProfilesEnum.RUNTIME, ConfigurationFactory.getConfigForEnvironment(
            EnvironmentEnum.DEV))

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


fun newConnectionHandler(appContext: AppContext, channel: SocketChannel): String {
    println("New Connection Handler called...")
    val session = appContext.sessionService.emptySessionNetty()

    val connectionContext = ConnectionContext(channel, CommandService(appContext, session), session)

    appContext.sessionCtxManager.registerConnectionCtx(connectionContext, session, channel)
    appContext.sessionCtxManager.queueMessageByChannel(
        Message("Welcome to Hogwarts, young student of magic!"),
        channel
    )

    return "refactor me"
}

fun closeConnectionHandler(appContext: AppContext, channel: SocketChannel) {
    println("closeConnectionHandler() called")
    appContext.sessionCtxManager.deleteConnectionCtxByChannel(channel)
}

fun incomingDataHandler(appContext: AppContext, channel: SocketChannel, textInput: String) : String {

    println("incomingDataHandler() called")

    val sessionCtx = appContext.sessionCtxManager.findConnectionCtxByChannel(channel)
    val result = sessionCtx!!.interpreter.process(textInput)
    var response = handleResponse(result)
    if (result.status == CommandResultEnum.CHAIN && result.chainCommand != null) {
        println("Doing chain command: ${result.chainCommand}")
        val result2 = sessionCtx.interpreter.process(result.chainCommand)
        response = handleResponse(result2)
        //TODO rather use recursion with safeguard
    }
    if (result.status == CommandResultEnum.EXIT) {
        appContext.sessionCtxManager.queueCloseByChannel(channel)
    }

    return response
}


fun intervalHandler(appContext: AppContext) : Boolean {
    appContext.sessionCtxManager.processQueue()
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
}

