package com.example.afetcomms.ui.family

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.databinding.ActivityFamilyStatusBinding
import com.example.afetcomms.transport.TransportStateHolder
import com.example.afetcomms.ui.AfetCommsViewModelFactory
import com.example.afetcomms.util.AppPreferences

class FamilyStatusActivity : AppCompatActivity() {

    private val app get() = application as AfetCommsApp
    private val viewModel: FamilyStatusViewModel by viewModels {
        AfetCommsViewModelFactory(app)
    }

    private lateinit var binding: ActivityFamilyStatusBinding
    private val adapter = FamilyMemberStatusAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarFamilyStatus)
        binding.toolbarFamilyStatus.setNavigationOnClickListener { finish() }

        binding.recyclerFamilyBoard.layoutManager = LinearLayoutManager(this)
        binding.recyclerFamilyBoard.adapter = adapter

        val familyId = getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
            .getString(AppPreferences.KEY_FAMILY_CODE, null).orEmpty()
        viewModel.load(familyId)

        viewModel.connectionPanel.observe(this) { panel ->
            binding.txtFamilyCodeStatus.text = getString(
                com.example.afetcomms.R.string.family_status_code_label,
                panel.familyCodeDisplay
            )
            binding.txtBleStatusPanel.text = panel.bleStatus
            binding.txtWifiStatusPanel.text = panel.wifiStatus
            binding.chipSimMode.visibility = if (panel.simMode) View.VISIBLE else View.GONE
        }

        viewModel.memberRows.observe(this) { rows ->
            adapter.submitList(rows)
            val empty = rows.isEmpty()
            binding.txtEmptyFamilyBoard.visibility = if (empty) View.VISIBLE else View.GONE
            binding.recyclerFamilyBoard.visibility = if (empty) View.GONE else View.VISIBLE
        }

        binding.fabRefreshFamily.setOnClickListener {
            app.burstFamilyPresenceSync()
            viewModel.refresh()
        }

    }

    override fun onResume() {
        super.onResume()
        TransportStateHolder.listener = { _, _, _ ->
            runOnUiThread { viewModel.refreshConnectionPanel() }
        }
        app.burstFamilyPresenceSync()
        viewModel.refresh()
    }

    override fun onPause() {
        TransportStateHolder.listener = null
        super.onPause()
    }
}
