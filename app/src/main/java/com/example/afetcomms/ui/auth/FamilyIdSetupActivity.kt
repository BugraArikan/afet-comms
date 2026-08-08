package com.example.afetcomms.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.data.model.MemberRelation
import com.example.afetcomms.databinding.ActivityFamilyIdSetupBinding
import com.example.afetcomms.ui.AfetCommsViewModelFactory
import com.example.afetcomms.util.AuthNavigator

class FamilyIdSetupActivity : AppCompatActivity() {

    private val app get() = application as AfetCommsApp
    private val viewModel: FamilyIdSetupViewModel by viewModels {
        AfetCommsViewModelFactory(app)
    }

    private lateinit var binding: ActivityFamilyIdSetupBinding
    private var firstName: String = ""
    private var lastName: String = ""
    private lateinit var relationLabels: List<String>
    private lateinit var relations: List<MemberRelation>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyIdSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firstName = intent.getStringExtra(EXTRA_FIRST_NAME).orEmpty()
        lastName = intent.getStringExtra(EXTRA_LAST_NAME).orEmpty()
        if (firstName.isEmpty() || lastName.isEmpty()) {
            finish()
            return
        }

        binding.txtFamilyMemberName.text = getString(
            R.string.family_member_label,
            "$firstName $lastName"
        )

        setupRelationSpinner()
        setupModePanels()

        binding.btnCompleteFamilySetup.setOnClickListener {
            val relation = relations[binding.spinnerMemberRelation.selectedItemPosition]
            if (binding.rbCreateFamily.isChecked) {
                if (viewModel.generatedCodes.value == null) {
                    viewModel.createFamily(firstName, lastName, relation)
                } else {
                    viewModel.finalizeCreate(firstName, lastName, relation)
                }
            } else {
                viewModel.joinFamily(
                    firstName = firstName,
                    lastName = lastName,
                    relation = relation,
                    familyCodeInput = binding.edtJoinFamilyCode.text.toString(),
                    inviteTokenInput = binding.edtJoinInviteToken.text.toString()
                )
            }
        }

        viewModel.generatedCodes.observe(this) { codes ->
            if (codes == null) return@observe
            binding.txtGeneratedFamilyCode.text = codes.familyCodeDisplay
            binding.txtGeneratedInviteToken.text = codes.inviteToken
            binding.txtGeneratedUserCode.text = codes.userCode
            binding.btnCompleteFamilySetup.text = getString(R.string.btn_complete_registration)
        }

        viewModel.registrationComplete.observe(this) { complete ->
            if (complete) {
                AuthNavigator.launchEntry(this)
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { resId ->
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
                viewModel.consumeError()
            }
        }
    }

    private fun setupRelationSpinner() {
        relations = MemberRelation.entries.toList()
        relationLabels = relations.map { getString(it.labelResId) }
        binding.spinnerMemberRelation.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            relationLabels
        )
    }

    private fun setupModePanels() {
        binding.radioFamilySetupMode.setOnCheckedChangeListener { _, checkedId ->
            val isCreate = checkedId == R.id.rbCreateFamily
            binding.panelCreateFamily.visibility = if (isCreate) View.VISIBLE else View.GONE
            binding.panelJoinFamily.visibility = if (isCreate) View.GONE else View.VISIBLE
            binding.btnCompleteFamilySetup.text = getString(
                if (isCreate) R.string.btn_create_family else R.string.btn_join_family
            )
        }
        binding.btnCompleteFamilySetup.text = getString(R.string.btn_create_family)
    }

    companion object {
        const val EXTRA_FIRST_NAME = "extra_first_name"
        const val EXTRA_LAST_NAME = "extra_last_name"
    }
}
