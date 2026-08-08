package com.example.afetcomms.data.model

enum class MessageType {
    CHECKIN,
    SOS,
    /** Aile üyesi tanıtımı — mesaj geçmişine yazılmaz, yalnızca üye tablosunu günceller. */
    PRESENCE
}