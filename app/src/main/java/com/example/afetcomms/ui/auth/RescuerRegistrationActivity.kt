package com.example.afetcomms.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.databinding.ActivityRescuerRegistrationBinding
import com.example.afetcomms.ui.AfetCommsViewModelFactory
import com.example.afetcomms.util.AuthNavigator

class RescuerRegistrationActivity : AppCompatActivity() {

    private val app get() = application as AfetCommsApp
    private val viewModel: RescuerRegistrationViewModel by viewModels {
        AfetCommsViewModelFactory(app)
    }

    private lateinit var binding: ActivityRescuerRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRescuerRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRescuerRegister.setOnClickListener {
            viewModel.register(
                firstName = binding.edtRescuerFirstName.text.toString(),
                lastName = binding.edtRescuerLastName.text.toString(),
                organization = binding.edtRescuerOrganization.text.toString(),
                rescuerId = binding.edtRescuerId.text.toString()
            )
        }

        viewModel.registrationComplete.observe(this) { complete ->
            if (complete) {
                val intent = AuthNavigator.homeIntent(this, com.example.afetcomms.data.model.AccountRole.RESCUER)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { resId ->
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
                viewModel.consumeError()
            }
        }
    }
}
