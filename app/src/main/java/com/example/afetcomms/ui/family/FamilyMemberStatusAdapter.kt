package com.example.afetcomms.ui.family

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.afetcomms.R
import com.google.android.material.chip.Chip

class FamilyMemberStatusAdapter :
    ListAdapter<FamilyMemberRowUi, FamilyMemberStatusAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_family_member_status, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgSosAlert: ImageView = itemView.findViewById(R.id.imgSosAlert)
        private val txtName: TextView = itemView.findViewById(R.id.txtMemberName)
        private val txtRelation: TextView = itemView.findViewById(R.id.txtMemberRelation)
        private val chipConnection: Chip = itemView.findViewById(R.id.chipConnection)
        private val txtLocation: TextView = itemView.findViewById(R.id.txtMemberLocation)

        fun bind(row: FamilyMemberRowUi) {
            txtName.text = row.displayName
            txtRelation.text = row.relationLabel
            chipConnection.text = row.connectionLabel
            chipConnection.setChipBackgroundColorResource(
                if (row.isConnected) R.color.chip_connected_bg else R.color.chip_away_bg
            )
            chipConnection.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (row.isConnected) R.color.chip_connected_text else R.color.chip_away_text
                )
            )

            if (row.showSosAlert) {
                imgSosAlert.visibility = View.VISIBLE
                txtLocation.visibility = View.VISIBLE
                txtLocation.text = row.locationText
                txtLocation.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.sos_location_text)
                )
            } else {
                imgSosAlert.visibility = View.GONE
                txtLocation.visibility = if (row.locationText.isNotBlank()) View.VISIBLE else View.GONE
                txtLocation.text = row.locationText
                txtLocation.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.on_surface_variant)
                )
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FamilyMemberRowUi>() {
        override fun areItemsTheSame(oldItem: FamilyMemberRowUi, newItem: FamilyMemberRowUi) =
            oldItem.userId == newItem.userId

        override fun areContentsTheSame(oldItem: FamilyMemberRowUi, newItem: FamilyMemberRowUi) =
            oldItem == newItem
    }
}
