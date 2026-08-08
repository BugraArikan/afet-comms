package com.example.afetcomms.transport

import com.example.afetcomms.data.model.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MessagePayloadTest {

    @Test
    fun jsonRoundTrip_preservesFields() {
        val original = MessagePayload(
            msgId = "id-1",
            senderId = "U001",
            familyId = "FAM001",
            type = MessageType.SOS,
            content = "Yardım",
            createdAt = 1_700_000_000_000L,
            ttlSeconds = 300,
            priority = 1
        )
        val parsed = MessagePayload.fromJsonString(original.toJsonString())
        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun fromJsonString_invalid_returnsNull() {
        assertNull(MessagePayload.fromJsonString("{broken"))
    }

    @Test
    fun fromJsonBytes_matchesString() {
        val payload = MessagePayload(
            msgId = "a",
            senderId = "b",
            familyId = "c",
            type = MessageType.CHECKIN,
            content = "ok",
            createdAt = 100L,
            ttlSeconds = 60,
            priority = 0
        )
        val fromBytes = MessagePayload.fromJsonBytes(payload.toJsonBytes())
        assertEquals(payload, fromBytes)
    }
}
