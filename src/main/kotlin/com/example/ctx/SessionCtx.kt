package com.example.ctx

import com.example.commands.Interpreter
import com.example.config.AppContext
import com.example.model.Session
import com.example.repository.Message
import com.example.service.SessionServiceImpl
import io.netty.channel.socket.SocketChannel

class SessionCtx(
    val channel: SocketChannel,
    val interpreter: Interpreter,
    var session: Session
)

interface SessionCtxManagerInterface {
    fun newSessionCtxFromChannel(appContext: AppContext, channel: SocketChannel) : SessionCtx
    fun deleteSessionCtxByChannel(channel: SocketChannel) : Boolean
    fun findSessionCtxByChannel(channel: SocketChannel) : SessionCtx?
    fun broadcast(message: Message)
    fun queueMessage(message: Message, target: Session)
}

class SessionCtxManager(val sessionService: SessionServiceImpl) : SessionCtxManagerInterface {

    val connections = mutableMapOf<SocketChannel, SessionCtx>()
    val sessions = mutableMapOf<SessionCtx, SocketChannel>()

    override fun newSessionCtxFromChannel(appContext: AppContext, channel: SocketChannel): SessionCtx {
        val session = sessionService.emptySessionNetty()
        val sessionCtx = SessionCtx(channel, Interpreter(appContext, session), session)
        sessions[sessionCtx] = channel
        connections[channel] = sessionCtx
        return sessionCtx
    }

    override fun deleteSessionCtxByChannel(channel: SocketChannel): Boolean {
        val sessionCtx = findSessionCtxByChannel(channel) ?: throw Exception("Failed to locate channel in Session Ctx manager")

        val session = sessionService.removeSessionNetty(sessionCtx.session)
        println("[Netty] Removing session for ${sessionCtx.session.account.name}.")
        sessions.remove(sessionCtx)
        connections.remove(channel)
        return true

    }

    override fun findSessionCtxByChannel(channel: SocketChannel): SessionCtx? {
        if(channel in connections) return connections[channel]
        return null

    }

    override fun broadcast(message: Message) {
        TODO("Not yet implemented")
    }

    override fun queueMessage(message: Message, target: Session) {
        TODO("Not yet implemented")
    }

}