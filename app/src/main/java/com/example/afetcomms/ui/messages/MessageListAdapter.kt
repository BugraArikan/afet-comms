package com.example.afetcomms.ui.messages

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.afetcomms.R
import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.data.model.MessageStatus
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.util.HelpCallFormatter
import com.example.afetcomms.util.MessageTtl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageListAdapter :
    ListAdapter<MessageEntity, MessageListAdapter.MessageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtType: TextView = itemView.findViewById(R.id.txtType)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        private val txtContent: TextView = itemView.findViewById(R.id.txtContent)
        private val txtMeta: TextView = itemView.findViewById(R.id.txtMeta)

        fun bind(message: MessageEntity) {
            txtType.text = when (message.type) {
                MessageType.SOS -> "SOS"
                MessageType.CHECKIN -> "Güvendeyim"
                MessageType.PRESENCE -> "Varlık"
            }
            txtStatus.text = message.status
            txtStatus.setTextColor(statusColor(message.status))
            txtContent.text = message.content
            val time = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date(message.createdAt))
            val ttlLeft = MessageTtl.remainingSeconds(message)
            val senderLabel = HelpCallFormatter.title(message)
            txtMeta.text = "$senderLabel · $time · TTL: ${ttlLeft}s · Öncelik: ${message.priority}"
        }

        private fun statusColor(status: String): Int = when (status) {
            MessageStatus.FAILED -> Color.parseColor("#DC2626")
            MessageStatus.SENT -> Color.parseColor("#16A34A")
            MessageStatus.RECEIVED -> Color.parseColor("#2563EB")
            MessageStatus.OUTBOX -> Color.parseColor("#CA8A04")
            else -> Color.parseColor("#64748B")
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MessageEntity>() {
        override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
            oldItem.msgId == newItem.msgId

        override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
            oldItem == newItem
    }
}
