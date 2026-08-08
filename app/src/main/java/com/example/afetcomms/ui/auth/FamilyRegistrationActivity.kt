package com.example.afetcomms.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.afetcomms.R
import com.example.afetcomms.databinding.ActivityFamilyRegistrationBinding

class FamilyRegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFamilyRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFamilyContinue.setOnClickListener {
            val firstName = binding.edtFamilyFirstName.text.toString().trim()
            val lastName = binding.edtFamilyLastName.text.toString().trim()
            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, R.string.registration_validation, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(
                Intent(this, FamilyIdSetupActivity::class.java).apply {
                    putExtra(FamilyIdSetupActivity.EXTRA_FIRST_NAME, firstName)
                    putExtra(FamilyIdSetupActivity.EXTRA_LAST_NAME, lastName)
                }
            )
        }
    }
}
