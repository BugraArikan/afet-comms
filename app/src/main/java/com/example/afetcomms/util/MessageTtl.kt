package com.example.afetcomms.util

import com.example.afetcomms.data.local.MessageEntity

object MessageTtl {

    fun isExpired(message: MessageEntity, now: Long = System.currentTimeMillis()): Boolean {
        val ageMs = now - message.createdAt
        return ageMs > message.ttlSeconds * 1000L
    }

    fun remainingSeconds(message: MessageEntity, now: Long = System.currentTimeMillis()): Int {
        val remainingMs = (message.ttlSeconds * 1000L) - (now - message.createdAt)
        return (remainingMs / 1000L).toInt().coerceAtLeast(0)
    }
}
