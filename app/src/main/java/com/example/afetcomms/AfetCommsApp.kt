package com.example.afetcomms

import android.app.Application
import com.example.afetcomms.alert.RescuerCheckinAlertHelper
import com.example.afetcomms.alert.SosAlertHelper
import com.example.afetcomms.data.local.AppDatabase
import com.example.afetcomms.data.model.AccountRole
import com.example.afetcomms.data.model.MemberRelation
import com.example.afetcomms.data.repo.FamilyRepository
import com.example.afetcomms.transport.TransportMode
import com.example.afetcomms.data.repo.MemberRepository
import com.example.afetcomms.data.repo.MessageRepository
import com.example.afetcomms.data.repo.UserSessionRepository
import com.example.afetcomms.service.BleRelayService
import com.example.afetcomms.transport.TransportCoordinator
import com.example.afetcomms.transport.ble.BleMessageTransport
import com.example.afetcomms.transport.debug.FakeMessageTransport
import com.example.afetcomms.transport.wifi.WifiDirectMessageTransport
import com.example.afetcomms.util.AppPreferences
import com.example.afetcomms.util.FamilyPresenceScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AfetCommsApp : Application() {

    lateinit var messageRepository: MessageRepository
        private set
    lateinit var memberRepository: MemberRepository
        private set
    lateinit var familyRepository: FamilyRepository
        private set
    lateinit var userSessionRepository: UserSessionRepository
        private set
    lateinit var transportCoordinator: TransportCoordinator
        private set

    val useFakeTransport: Boolean
        get() = AppPreferences.useFakeTransport(this)

    private var transportsStarted = false
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val presenceScheduler = FamilyPresenceScheduler { announceFamilyPresence() }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        messageRepository = MessageRepository(db.messageDao())
        memberRepository = MemberRepository(db.memberDao())
        familyRepository = FamilyRepository(db.familyDao())
        userSessionRepository = UserSessionRepository(
            this,
            db.userProfileDao(),
            familyRepository
        )
        transportCoordinator = buildTransportCoordinator()
        wireSosAlerts()

        appScope.launch {
            messageRepository.deleteExpiredMessages()
        }
    }

    fun rebuildTransportCoordinator() {
        stopTransports()
        transportCoordinator = buildTransportCoordinator()
        wireSosAlerts()
    }

    fun restartTransports(familyId: String, senderId: String) {
        stopTransports()
        transportsStarted = false
        if (!useFakeTransport) {
            BleRelayService.start(this, familyId, senderId)
        } else {
            BleRelayService.stop(this)
        }
        startTransports(familyId, senderId)
    }

    fun startTransports(familyId: String, senderId: String) {
        if (transportsStarted) return
        transportsStarted = true
        val mode = when (userSessionRepository.getAccountRole()) {
            AccountRole.RESCUER -> TransportMode.RESCUER
            else -> TransportMode.FAMILY
        }
        transportCoordinator.start(familyId, senderId, mode)
        if (mode == TransportMode.FAMILY) {
            presenceScheduler.start()
        }
    }

    fun stopTransports() {
        if (!transportsStarted) return
        transportsStarted = false
        presenceScheduler.stop()
        transportCoordinator.stop()
    }

    fun burstFamilyPresenceSync() {
        if (userSessionRepository.getAccountRole() != AccountRole.FAMILY) return
        presenceScheduler.burst()
    }

    fun announceFamilyPresence(latitude: Double? = null, longitude: Double? = null) {
        if (userSessionRepository.getAccountRole() != AccountRole.FAMILY) return
        val prefs = getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
        val displayName = prefs.getString(AppPreferences.KEY_USER_NAME, null)?.takeIf { it.isNotBlank() }
            ?: return
        val relation = prefs.getString(AppPreferences.KEY_MEMBER_RELATION, null)
            ?: MemberRelation.DIGER.storageValue
        transportCoordinator.announceLocalPresence(
            displayName = displayName,
            relationRole = relation,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun buildTransportCoordinator(): TransportCoordinator {
        val transports = if (useFakeTransport) {
            listOf(FakeMessageTransport(this))
        } else {
            listOf(
                BleMessageTransport(this),
                WifiDirectMessageTransport(this)
            )
        }
        return TransportCoordinator(
            messageRepository = messageRepository,
            memberRepository = memberRepository,
            transports = transports,
            meshRelayEnabled = !useFakeTransport
        )
    }

    private fun wireSosAlerts() {
        transportCoordinator.onSosReceived = { entity ->
            SosAlertHelper.showIncoming(applicationContext, entity)
        }
        transportCoordinator.onSosSent = { entity ->
            SosAlertHelper.showSent(applicationContext, entity)
        }
        transportCoordinator.onRescuerCheckinReceived = { entity ->
            if (userSessionRepository.getAccountRole() == AccountRole.FAMILY) {
                RescuerCheckinAlertHelper.showIncoming(applicationContext, entity)
            }
        }
    }
}
