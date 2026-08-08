package com.example.afetcomms.ui.messages

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.afetcomms.AfetCommsApp
import com.example.afetcomms.R
import com.example.afetcomms.ui.AfetCommsViewModelFactory
import com.example.afetcomms.util.AppPreferences

class MessagesActivity : AppCompatActivity() {

    private val app get() = application as AfetCommsApp
    private val viewModel: MessagesViewModel by viewModels {
        AfetCommsViewModelFactory(app)
    }

    private lateinit var recycler: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var txtSummary: TextView
    private lateinit var btnRetryFailed: Button
    private lateinit var btnRefresh: Button
    private val adapter = MessageListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        recycler = findViewById(R.id.recyclerMessages)
        txtEmpty = findViewById(R.id.txtEmpty)
        txtSummary = findViewById(R.id.txtSummary)
        btnRetryFailed = findViewById(R.id.btnRetryFailed)
        btnRefresh = findViewById(R.id.btnRefreshMessages)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val familyId = getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
            .getString(AppPreferences.KEY_FAMILY_CODE, null)

        btnRetryFailed.setOnClickListener { viewModel.retryFailed() }
        btnRefresh.setOnClickListener { viewModel.refreshAndPurge() }

        viewModel.purgeOnOpen()

        val source = if (familyId.isNullOrBlank()) {
            app.messageRepository.getAllMessages()
        } else {
            app.messageRepository.getMessagesByFamily(familyId)
        }
        viewModel.bindMessages(source)

        viewModel.uiState.observe(this) { state ->
            adapter.submitList(state.messages)
            txtSummary.text = getString(
                R.string.messages_summary,
                state.messages.size,
                state.receivedCount,
                state.outboxCount,
                state.failedCount
            )
            txtEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
            btnRetryFailed.isEnabled = state.canRetryFailed
        }

        viewModel.toastMessage.observe(this) { resId ->
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
                viewModel.consumeToast()
            }
        }
    }
}
