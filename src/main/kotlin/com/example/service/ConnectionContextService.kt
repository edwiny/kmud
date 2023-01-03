package com.example.application.ctx

import com.example.commands.CommandService
import com.example.model.Session
import com.example.service.dto.Message
import com.example.service.SessionServiceImpl
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame

class ConnectionContext(
    val channel: SocketChannel,
    val interpreter: CommandService,
    var session: Session
)

interface ConnectionContextServiceInterface {
    fun registerConnectionCtx(connectionContext: ConnectionContext, session: Session, channel: SocketChannel)
    fun deleteConnectionCtxByChannel(channel: SocketChannel) : Boolean
    fun findConnectionCtxByChannel(channel: SocketChannel) : ConnectionContext?
    fun broadcast(message: Message)
    fun queueMessage(message: Message, target: Session)
    fun queueMessageByChannel(message: Message, channel: SocketChannel)
    fun queueCloseByChannel(channel: SocketChannel)
    fun processQueue()
}

class ConnectionContextService(val sessionService: SessionServiceImpl) : ConnectionContextServiceInterface {

    val connections = mutableMapOf<SocketChannel, ConnectionContext>()
    val sessions = mutableMapOf<ConnectionContext, SocketChannel>()
    val queue = mutableListOf<QueuedMessage>()
    val closeQueue = mutableListOf<SocketChannel>()

    data class QueuedMessage(val channel: SocketChannel, val msg: Message)

    override fun registerConnectionCtx(connectionContext: ConnectionContext, session: Session, channel: SocketChannel) {
        sessions[connectionContext] = channel
        connections[channel] = connectionContext
    }

    override fun deleteConnectionCtxByChannel(channel: SocketChannel): Boolean {
        val sessionCtx = findConnectionCtxByChannel(channel) ?: throw Exception("Failed to locate channel in Session Ctx manager")

        val session = sessionService.removeSessionNetty(sessionCtx.session)
        println("[Netty] Removing session for ${sessionCtx.session.account.name}.")
        sessions.remove(sessionCtx)
        connections.remove(channel)
        return true

    }

    override fun findConnectionCtxByChannel(channel: SocketChannel): ConnectionContext? {
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