package com.example

import com.example.service.commands.RegexCommandParser
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testCommandParser() {
        val parser = RegexCommandParser()
        val state = parser.build("CMD:login {NAME:STR} with {PASSWORD:STR} [as] [SOMEONE:INT]")

        assertNotNull(parser.parseToArgs("login edwin with edwinpass as 2", state!!))
    }
}
