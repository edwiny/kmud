package com.example.service.dto


enum class MessageType {
    TEXT,
}


class Message(val text: String, val type: MessageType = MessageType.TEXT) {
    override fun toString(): String {
        return text
    }
}


