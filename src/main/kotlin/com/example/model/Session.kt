package com.example.model

import io.ktor.websocket.*
import java.time.LocalDateTime

class Session(
    var id: Int,
    var account: Account,
    var character: Character,
    val startTime: LocalDateTime,
)
