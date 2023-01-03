package com.example.infra.network

import io.netty.channel.socket.SocketChannel

interface NetworkServerInterface {
    fun newConnectionHandler(handler: (channel: SocketChannel) -> String)
    fun closeConnectionHandler(handler: (channel: SocketChannel) -> Unit)
    fun incomingDataHandler(handler: (channel: SocketChannel, message: String) -> String)
    fun intervalHandler(handler: () -> Boolean)
}