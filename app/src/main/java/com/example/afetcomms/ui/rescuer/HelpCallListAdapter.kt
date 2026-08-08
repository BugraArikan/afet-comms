package com.example.afetcomms.ui.rescuer

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
import com.example.afetcomms.util.HelpCallFormatter
import com.example.afetcomms.util.MessageTtl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HelpCallListAdapter(
    private val onItemClick: (MessageEntity) -> Unit
) : ListAdapter<MessageEntity, HelpCallListAdapter.HelpCallViewHolder>(DiffCallback) {

    var selectedMsgId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpCallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_help_call, parent, false)
        return HelpCallViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpCallViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, item.msgId == selectedMsgId)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    class HelpCallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitle: TextView = itemView.findViewById(R.id.txtHelpCallTitle)
        private val txtContent: TextView = itemView.findViewById(R.id.txtHelpCallContent)
        private val txtMeta: TextView = itemView.findViewById(R.id.txtHelpCallMeta)

        fun bind(message: MessageEntity, selected: Boolean) {
            itemView.alpha = if (selected) 1f else 0.92f
            itemView.setBackgroundResource(
                if (selected) R.drawable.bg_help_call_selected else R.drawable.bg_card
            )
            txtTitle.text = HelpCallFormatter.title(message)
            txtTitle.setTextColor(Color.parseColor("#DC2626"))
            txtContent.text = message.content
            val time = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
                .format(Date(message.createdAt))
            val ttlLeft = MessageTtl.remainingSeconds(message)
            txtMeta.text = "${HelpCallFormatter.subtitle(message)} · $time · TTL: ${ttlLeft}s"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MessageEntity>() {
        override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
            oldItem.msgId == newItem.msgId

        override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity) =
            oldItem == newItem
    }
}
