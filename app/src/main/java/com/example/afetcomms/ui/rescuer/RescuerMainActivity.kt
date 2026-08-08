package com.example.afetcomms.ui.rescuer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationListener
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.data.model.AccountRole
import com.example.afetcomms.databinding.ActivityRescuerMainBinding
import com.example.afetcomms.service.BleRelayService
import com.example.afetcomms.transport.TransportState
import com.example.afetcomms.transport.TransportStateHolder
import com.example.afetcomms.ui.AfetCommsViewModelFactory
import com.example.afetcomms.ui.auth.RoleSelectionActivity
import com.example.afetcomms.ui.settings.SettingsActivity
import com.example.afetcomms.util.AuthNavigator
import com.example.afetcomms.util.BluetoothPermissionHelper
import com.example.afetcomms.util.LocationHelper

class RescuerMainActivity : AppCompatActivity() {

    private val app get() = application as AfetCommsApp
    private val viewModel: RescuerMainViewModel by viewModels {
        AfetCommsViewModelFactory(app)
    }

    private lateinit var binding: ActivityRescuerMainBinding
    private val helpCallAdapter = HelpCallListAdapter { message ->
        viewModel.selectHelpCall(message)
    }
    private var locationListener: LocationListener? = null
    private var lastCoordinates: LocationHelper.Coordinates? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) startRelayAndTransports()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            refreshLocation()
            startLocationUpdatesIfAllowed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthNavigator.requireSetup(this)) {
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
            return
        }
        if (app.userSessionRepository.getAccountRole() != AccountRole.RESCUER) {
            startActivity(AuthNavigator.homeIntent(this, AccountRole.FAMILY))
            finish()
            return
        }

        binding = ActivityRescuerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerHelpCalls.layoutManager = LinearLayoutManager(this)
        binding.recyclerHelpCalls.adapter = helpCallAdapter

        viewModel.loadProfile()
        observeViewModel()
        setupListeners()
        setupTransportStateListener()
        requestLocationPermissionAndRefresh()

        if (app.useFakeTransport) {
            Toast.makeText(this, R.string.sim_mode_toast, Toast.LENGTH_LONG).show()
            startRelayAndTransports()
        } else {
            requestBluetoothPermissionsAndStart()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLocation()
    }

    override fun onPause() {
        LocationHelper.stopUpdates(this, locationListener)
        locationListener = null
        super.onPause()
    }

    override fun onDestroy() {
        TransportStateHolder.listener = null
        super.onDestroy()
    }

    private fun observeViewModel() {
        viewModel.profile.observe(this) { profile ->
            binding.txtRescuerProfile.text = profile.summary()
            binding.txtRescuerSubtitle.text = if (app.useFakeTransport) {
                getString(R.string.rescuer_mode_sim_subtitle)
            } else {
                getString(R.string.rescuer_main_subtitle)
            }
        }

        viewModel.nearbyHelpCalls.observe(this) { calls ->
            helpCallAdapter.submitList(calls)
            val empty = calls.isNullOrEmpty()
            binding.txtEmptyHelpCalls.visibility = if (empty) View.VISIBLE else View.GONE
            binding.recyclerHelpCalls.visibility = if (empty) View.GONE else View.VISIBLE
        }

        viewModel.selectedHelpCall.observe(this) { selected ->
            helpCallAdapter.selectedMsgId = selected?.msgId
            binding.btnRescuerSafe.isEnabled = selected != null
        }

        viewModel.toastMessage.observe(this) { resId ->
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
                viewModel.consumeToast()
            }
        }
    }

    private fun setupListeners() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnRescuerSafe.setOnClickListener {
            val selected = viewModel.selectedHelpCall.value ?: return@setOnClickListener
            val citizenName = selected.senderDisplayName.ifBlank { selected.senderId }
            viewModel.sendSafeCheckin(
                getString(R.string.rescuer_checkin_message_for, citizenName),
                lastCoordinates
            )
        }
        binding.btnRescuerSos.setOnClickListener {
            viewModel.sendRescuerSos(
                getString(R.string.rescuer_sos_message),
                lastCoordinates
            )
        }
    }

    private fun setupTransportStateListener() {
        TransportStateHolder.listener = { transportName, state, detail ->
            runOnUiThread { updateTransportStatus(transportName, state, detail) }
        }
    }

    private fun requestBluetoothPermissionsAndStart() {
        val missing = BluetoothPermissionHelper.missingPermissions(this)
        if (missing.isEmpty()) startRelayAndTransports()
        else permissionLauncher.launch(missing)
    }

    private fun requestLocationPermissionAndRefresh() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            refreshLocation()
            startLocationUpdatesIfAllowed()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startLocationUpdatesIfAllowed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return
        locationListener = LocationHelper.startUpdates(this) { coords ->
            lastCoordinates = coords
            runOnUiThread { updateLocationText(coords) }
        }
    }

    private fun refreshLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            binding.txtLocation.text = getString(R.string.location_permission_needed)
            return
        }
        lastCoordinates = LocationHelper.getLastKnown(this)
        updateLocationText(lastCoordinates)
    }

    private fun updateLocationText(coords: LocationHelper.Coordinates?) {
        binding.txtLocation.text = if (coords != null) {
            getString(R.string.location_format, coords.latitude, coords.longitude)
        } else {
            getString(R.string.location_waiting)
        }
    }

    private fun startRelayAndTransports() {
        val profile = viewModel.profile.value ?: return
        if (!app.useFakeTransport) {
            BleRelayService.start(this, profile.organization, profile.rescuerId)
        } else {
            binding.txtWifiStatus.text = getString(R.string.wifi_status_skipped_sim)
        }
        app.restartTransports(profile.organization, profile.rescuerId)
        app.transportCoordinator.flushOutbox()
        app.transportCoordinator.retryFailed()
    }

    private fun updateTransportStatus(transportName: String, state: TransportState, detail: String) {
        val label = when (state) {
            TransportState.RUNNING -> detail
            TransportState.STARTING -> "Başlatılıyor…"
            TransportState.ERROR -> detail
            TransportState.IDLE -> "Bekleniyor"
        }
        when (transportName) {
            "BLE", "Simülasyon" -> binding.txtBleStatus.text = getString(R.string.ble_status_format, label)
            "WiFiDirect" -> if (!app.useFakeTransport) {
                binding.txtWifiStatus.text = getString(R.string.wifi_status_format, label)
            }
        }
    }
}
