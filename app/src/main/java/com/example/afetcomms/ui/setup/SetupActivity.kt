package com.example.afetcomms.ui.setup

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.data.local.MemberEntity
import com.example.afetcomms.ui.main.MainActivity
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etFamilyCode: EditText
    private lateinit var rgRole: RadioGroup
    private lateinit var btnSaveProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        etName = findViewById(R.id.etName)
        etFamilyCode = findViewById(R.id.etFamilyCode)
        rgRole = findViewById(R.id.rgRole)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        btnSaveProfile.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun saveUserProfile() {
        val name = etName.text.toString().trim()
        val familyCode = etFamilyCode.text.toString().trim()

        val selectedRoleId = rgRole.checkedRadioButtonId

        if (name.isEmpty() || familyCode.isEmpty() || selectedRoleId == -1) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRoleButton: RadioButton = findViewById(selectedRoleId)
        val role = selectedRoleButton.text.toString()

        val sharedPreferences: SharedPreferences =
            getSharedPreferences("user_prefs", MODE_PRIVATE)

        val userId = "U_${System.currentTimeMillis() % 100000}"

        sharedPreferences.edit()
            .putString("user_name", name)
            .putString("user_id", userId)
            .putString("family_code", familyCode)
            .putString("user_role", role)
            .apply()

        val app = application as AfetCommsApp
        lifecycleScope.launch {
            app.memberRepository.insertMember(
                MemberEntity(
                    userId = userId,
                    userCode = userId,
                    displayName = name,
                    familyId = familyCode,
                    relationRole = "DIGER"
                )
            )
        }

        Toast.makeText(this, "Bilgiler kaydedildi", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}