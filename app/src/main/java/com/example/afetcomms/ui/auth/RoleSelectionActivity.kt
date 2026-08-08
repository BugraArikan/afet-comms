package com.example.afetcomms.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.afetcomms.R
import com.example.afetcomms.databinding.ActivityRoleSelectionBinding
import com.example.afetcomms.util.AuthNavigator

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthNavigator.requireSetup(this)) {
            AuthNavigator.launchEntry(this)
            finish()
            return
        }

        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectRescuer.setOnClickListener {
            startActivity(Intent(this, RescuerRegistrationActivity::class.java))
        }
        binding.btnSelectFamily.setOnClickListener {
            startActivity(Intent(this, FamilyRegistrationActivity::class.java))
        }
    }
}
