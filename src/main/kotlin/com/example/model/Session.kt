package com.example.model

import java.time.LocalDateTime

class Session(
    var id: Int,
    var account: Account,
    var character: Character,
    val startTime: LocalDateTime,
)
