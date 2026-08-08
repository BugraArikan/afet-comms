package com.example.afetcomms.transport

enum class TransportState {
    IDLE,
    STARTING,
    RUNNING,
    ERROR
}

interface TransportListener {
    fun onMessageReceived(payload: MessagePayload, transportName: String)
    fun onStateChanged(state: TransportState, detail: String, transportName: String)
    fun onSendResult(msgId: String, success: Boolean, transportName: String)
}

interface MessageTransport {
    val name: String
    fun start(familyId: String, mode: TransportMode = TransportMode.FAMILY)
    fun stop()
    fun send(payload: MessagePayload)
    fun setListener(listener: TransportListener?)
    fun currentState(): TransportState
}
