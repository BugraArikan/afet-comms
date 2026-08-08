package com.example.afetcomms.ui.family

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.data.local.MemberEntity
import com.example.afetcomms.data.model.MemberConnectionStatus
import com.example.afetcomms.data.model.MemberRelation
import com.example.afetcomms.transport.TransportState
import com.example.afetcomms.transport.TransportStateHolder
import com.example.afetcomms.util.AppPreferences
import com.example.afetcomms.util.FamilyCodeGenerator
import com.example.afetcomms.util.LocationHelper
import com.example.afetcomms.util.MessageLocationParser
import kotlinx.coroutines.launch

data class FamilyMemberRowUi(
    val userId: String,
    val displayName: String,
    val relationLabel: String,
    val connectionLabel: String,
    val isConnected: Boolean,
    val showSosAlert: Boolean,
    val locationText: String
)

data class ConnectionPanelUi(
    val bleStatus: String,
    val wifiStatus: String,
    val simMode: Boolean,
    val familyCodeDisplay: String
)

class FamilyStatusViewModel(
    private val app: AfetCommsApp
) : ViewModel() {

    private val _connectionPanel = MutableLiveData<ConnectionPanelUi>()
    val connectionPanel: LiveData<ConnectionPanelUi> = _connectionPanel

    private val _memberRows = MediatorLiveData<List<FamilyMemberRowUi>>()
    val memberRows: LiveData<List<FamilyMemberRowUi>> = _memberRows

    private var familyId: String = ""
    private var membersSource: LiveData<List<MemberEntity>>? = null

    fun load(familyId: String) {
        if (this.familyId == familyId && membersSource != null) return
        this.familyId = familyId
        membersSource?.let { _memberRows.removeSource(it) }
        val source = app.memberRepository.observeFamilyStatusBoard(familyId)
        membersSource = source
        _memberRows.addSource(source) { members ->
            _memberRows.value = members.map { mapMemberRow(it) }
        }
        refreshConnectionPanel()
    }

    fun refresh() {
        viewModelScope.launch {
            if (familyId.isNotBlank()) {
                app.memberRepository.refreshPresenceTimeouts(familyId)
            }
            refreshConnectionPanel()
        }
    }

    fun refreshConnectionPanel() {
        val prefs = app.getSharedPreferences(AppPreferences.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val ble = TransportStateHolder.get("BLE") ?: TransportStateHolder.get("Simülasyon")
        val wifi = TransportStateHolder.get("WiFiDirect")
        _connectionPanel.value = ConnectionPanelUi(
            bleStatus = formatTransportLine("BLE / Simülasyon", ble),
            wifiStatus = formatTransportLine("Wi-Fi Direct", wifi),
            simMode = app.useFakeTransport,
            familyCodeDisplay = FamilyCodeGenerator.formatFamilyCodeForDisplay(
                prefs.getString(AppPreferences.KEY_FAMILY_CODE, "") ?: ""
            )
        )
    }

    private fun mapMemberRow(member: MemberEntity): FamilyMemberRowUi {
        val connection = MemberConnectionStatus.fromStorage(member.connectionStatus)
        val relation = MemberRelation.fromStorage(member.relationRole)
        val locationText = when {
            member.activeSos && member.lastLatitude != null && member.lastLongitude != null -> {
                val ref = MessageLocationParser.formatMapReference(
                    LocationHelper.Coordinates(member.lastLatitude, member.lastLongitude)
                )
                app.getString(com.example.afetcomms.R.string.family_sos_location, ref)
            }
            member.lastLatitude != null && member.lastLongitude != null ->
                MessageLocationParser.formatMapReference(
                    LocationHelper.Coordinates(member.lastLatitude, member.lastLongitude)
                )
            else -> ""
        }
        return FamilyMemberRowUi(
            userId = member.userId,
            displayName = member.displayName,
            relationLabel = app.getString(relation.labelResId),
            connectionLabel = app.getString(connection.labelResId),
            isConnected = connection == MemberConnectionStatus.CONNECTED,
            showSosAlert = member.activeSos,
            locationText = locationText
        )
    }

    private fun formatTransportLine(
        label: String,
        state: Pair<TransportState, String>?
    ): String {
        if (state == null) return "$label: Bekleniyor"
        val detail = when (state.first) {
            TransportState.RUNNING -> state.second
            TransportState.STARTING -> "Başlatılıyor…"
            TransportState.ERROR -> state.second
            TransportState.IDLE -> "Bekleniyor"
        }
        return "$label: $detail"
    }
}
