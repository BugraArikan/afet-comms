package com.example.afetcomms.data.local

import androidx.room.TypeConverter
import com.example.afetcomms.data.model.MessageType

class Converters {

    @TypeConverter
    fun fromMessageType(value: MessageType): String {
        return value.name
    }

    @TypeConverter
    fun toMessageType(value: String): MessageType {
        return MessageType.valueOf(value)
    }
}