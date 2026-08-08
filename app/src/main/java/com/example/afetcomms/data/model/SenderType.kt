package com.example.afetcomms.data.model

enum class SenderType(val storageValue: String) {
    RESCUER("RESCUER"),
    CITIZEN("CITIZEN");

    companion object {
        fun fromStorage(value: String?): SenderType {
            return entries.find { it.storageValue == value } ?: CITIZEN
        }
    }
}
