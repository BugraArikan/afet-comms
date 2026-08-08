package com.example.afetcomms.data.model

enum class AccountRole(val storageValue: String) {
    RESCUER("RESCUER"),
    FAMILY("FAMILY");

    companion object {
        fun fromStorage(value: String?): AccountRole? {
            return entries.find { it.storageValue == value }
        }
    }
}
