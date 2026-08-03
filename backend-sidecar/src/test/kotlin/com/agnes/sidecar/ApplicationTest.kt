package com.agnes.sidecar

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicationTest {

    @Test
    fun testHealthEndpoint() = runTest {
        withTestApplication({
            module()
        }) {
            handleRequest(HttpMethod.Get, "/healthz").apply {
                assertEquals(200, response.status().value)
                assertEquals("ok", response.content!!.decodeToString().let { it.contains("ok") })
            }
        }
    }
}