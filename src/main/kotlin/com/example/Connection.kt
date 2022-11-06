package com.example

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.atomic.*

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(1)
    }

    val name = "user${lastId.getAndIncrement()}"
}
