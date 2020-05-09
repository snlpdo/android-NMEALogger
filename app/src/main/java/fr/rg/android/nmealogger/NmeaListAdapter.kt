package fr.rg.android.nmealogger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.rg.android.nmealogger.databinding.NmeaFrameViewBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NmeaFrameListAdapter : ListAdapter<NmeaFrame,
        NmeaFrameListAdapter.ViewHolder>(NmeaFrameDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder.from(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder private constructor(val binding: NmeaFrameViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())

        fun bind(nmeaFrame: NmeaFrame) {
            val instant = Instant.ofEpochMilli(nmeaFrame.timeStamp)
            binding.frameTimestamp.text = dtf.format(instant)

            binding.frameContent.text = nmeaFrame.content
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = NmeaFrameViewBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}


class NmeaFrameDiffCallback : DiffUtil.ItemCallback<NmeaFrame>() {
    override fun areItemsTheSame(oldItem: NmeaFrame, newItem: NmeaFrame): Boolean {
        return oldItem.timeStamp === newItem.timeStamp
    }

    override fun areContentsTheSame(oldItem: NmeaFrame, newItem: NmeaFrame): Boolean {
        return oldItem.content == newItem.content
    }
}