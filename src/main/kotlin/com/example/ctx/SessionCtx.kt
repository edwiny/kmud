package com.example.ctx

import com.example.commands.CommandRuntime
import com.example.config.AppContext
import com.example.model.Session
import com.example.repository.Message
import com.example.service.SessionServiceImpl
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame

class SessionCtx(
    val channel: SocketChannel,
    val interpreter: CommandRuntime,
    var session: Session
)

interface SessionCtxManagerInterface {
    fun newSessionCtxFromChannel(appContext: AppContext, channel: SocketChannel) : SessionCtx
    fun deleteSessionCtxByChannel(channel: SocketChannel) : Boolean
    fun findSessionCtxByChannel(channel: SocketChannel) : SessionCtx?
    fun broadcast(message: Message)
    fun queueMessage(message: Message, target: Session)
    fun queueMessageByChannel(message: Message, channel: SocketChannel)
    fun queueCloseByChannel(channel: SocketChannel)
    fun processQueue()
}

class SessionCtxManager(val sessionService: SessionServiceImpl) : SessionCtxManagerInterface {

    val connections = mutableMapOf<SocketChannel, SessionCtx>()
    val sessions = mutableMapOf<SessionCtx, SocketChannel>()
    val queue = mutableListOf<QueuedMessage>()
    val closeQueue = mutableListOf<SocketChannel>()

    data class QueuedMessage(val channel: SocketChannel, val msg:Message)

    override fun newSessionCtxFromChannel(appContext: AppContext, channel: SocketChannel): SessionCtx {
        val session = sessionService.emptySessionNetty()
        val sessionCtx = SessionCtx(channel, CommandRuntime(appContext, session), session)
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
        connections.keys.forEach {
            if(it.isWritable) {
                it.writeAndFlush(TextWebSocketFrame(message.toString()))
                println("SessionCtxManager.broadcast() sending something to channel $it")
            }
        }
    }

    override fun queueMessage(message: Message, target: Session) {
        TODO("Not yet implemented")
    }

    override fun queueMessageByChannel(message: Message, channel: SocketChannel) {
        queue.add(QueuedMessage(channel = channel, msg = message))
    }

    override fun processQueue() {
        if (queue.isNotEmpty()) {
            val item = queue.removeAt(0)
            if (item.channel.isWritable) {
                item.channel.writeAndFlush(TextWebSocketFrame(item.msg.toString()))
                println("processQueue: sending something to ${item.channel}")
            }
        }
        if(closeQueue.isNotEmpty()) {
            val item = closeQueue.removeAt(0)
            println("processQueue: closing channel $item")
            item.close()
        }
    }

    override fun queueCloseByChannel(channel: SocketChannel) {
        closeQueue.add(channel)
    }

}