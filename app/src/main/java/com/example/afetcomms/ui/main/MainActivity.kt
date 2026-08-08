package com.example.afetcomms.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationListener
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.data.model.AccountRole
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.databinding.ActivityMainBinding
import com.example.afetcomms.service.BleRelayService
import com.example.afetcomms.ui.AfetCommsViewModelFactory
import com.example.afetcomms.ui.auth.RoleSelectionActivity
import com.example.afetcomms.ui.family.FamilyStatusActivity
import com.example.afetcomms.ui.settings.SettingsActivity
import com.example.afetcomms.util.AppPreferences
import com.example.afetcomms.util.AuthNavigator
import com.example.afetcomms.util.BluetoothPermissionHelper
import com.example.afetcomms.util.LocationHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val app get() = application as AfetCommsApp
    private val viewModel: MainViewModel by viewModels {
        AfetCommsViewModelFactory(app)
    }

    private lateinit var binding: ActivityMainBinding
    private var lastCoordinates: LocationHelper.Coordinates? = null
    private var locationListener: LocationListener? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startRelayAndTransports()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            lastCoordinates = LocationHelper.getLastKnown(this)
            startLocationUpdatesIfAllowed()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* optional */ }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.loadProfile()
            if (app.useFakeTransport) startRelayAndTransports()
            else requestBluetoothPermissionsAndStart()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthNavigator.requireSetup(this)) {
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
            return
        }
        if (app.userSessionRepository.getAccountRole() == AccountRole.RESCUER) {
            startActivity(AuthNavigator.homeIntent(this, AccountRole.RESCUER))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarMain)

        viewModel.loadProfile()
        observeViewModel()
        setupButtons()
        requestLocationPermissionAndStartGps()
        requestNotificationPermissionIfNeeded()

        if (app.useFakeTransport) {
            Toast.makeText(this, R.string.sim_mode_toast, Toast.LENGTH_LONG).show()
            startRelayAndTransports()
        } else {
            requestBluetoothPermissionsAndStart()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_family_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_family_status -> {
                startActivity(Intent(this, FamilyStatusActivity::class.java))
                true
            }
            R.id.action_settings -> {
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdatesIfAllowed()
        broadcastPresence()
    }

    override fun onPause() {
        LocationHelper.stopUpdates(this, locationListener)
        locationListener = null
        super.onPause()
    }

    private fun observeViewModel() {
        viewModel.toastMessage.observe(this) { resId ->
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
                viewModel.consumeToast()
            }
        }
    }

    private fun setupButtons() {
        binding.btnSafe.setOnClickListener {
            sendWithLocation(MessageType.CHECKIN, getString(R.string.family_checkin_message))
        }
        binding.btnSos.setOnClickListener {
            sendWithLocation(MessageType.SOS, getString(R.string.family_sos_message))
        }
    }

    private fun sendWithLocation(type: MessageType, baseContent: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_LONG).show()
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            viewModel.sendMessage(type, baseContent, null)
            return
        }
        lifecycleScope.launch {
            binding.btnSos.isEnabled = false
            binding.btnSafe.isEnabled = false
            val coords = LocationHelper.awaitForSend(this@MainActivity)
            lastCoordinates = coords ?: lastCoordinates
            if (coords == null && !LocationHelper.isLocationEnabled(this@MainActivity)) {
                Toast.makeText(
                    this@MainActivity,
                    R.string.location_disabled,
                    Toast.LENGTH_SHORT
                ).show()
            }
            viewModel.sendMessage(type, baseContent, coords ?: lastCoordinates)
            binding.btnSos.isEnabled = true
            binding.btnSafe.isEnabled = true
        }
    }

    private fun requestBluetoothPermissionsAndStart() {
        val missing = BluetoothPermissionHelper.missingPermissions(this)
        if (missing.isEmpty()) startRelayAndTransports()
        else permissionLauncher.launch(missing)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestLocationPermissionAndStartGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            lastCoordinates = LocationHelper.getLastKnown(this)
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
        }
    }

    private fun startRelayAndTransports() {
        val prefs = getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
        val familyId = prefs.getString(AppPreferences.KEY_FAMILY_CODE, null).orEmpty()
        val userId = prefs.getString(AppPreferences.KEY_USER_CODE, null)
            ?: prefs.getString(AppPreferences.KEY_USER_ID, null).orEmpty()
        if (familyId.isBlank() || userId.isBlank()) return

        if (!app.useFakeTransport) {
            BleRelayService.start(this, familyId, userId)
        }
        app.startTransports(familyId, userId)
        app.transportCoordinator.flushOutbox()
        app.transportCoordinator.retryFailed()
        broadcastPresence()
    }

    private fun broadcastPresence() {
        app.burstFamilyPresenceSync()
    }
}
