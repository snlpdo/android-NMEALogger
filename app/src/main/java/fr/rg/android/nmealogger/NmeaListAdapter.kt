package fr.rg.android.nmealogger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.rg.android.nmealogger.databinding.NmeaFrameViewBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dtf =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())

class NmeaFrameListAdapter(val clickListener: NmeaClickListener) : ListAdapter<NmeaFrame,
        NmeaFrameListAdapter.ViewHolder>(NmeaFrameDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder.from(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)!!, clickListener)
    }

    class ViewHolder private constructor(val binding: NmeaFrameViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dtf =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())

        fun bind(item: NmeaFrame, clickListener: NmeaClickListener) {
            binding.nmea = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
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

class NmeaClickListener(val clickListener: (nmeaFrame: NmeaFrame) -> Unit) {
    fun onClick(nmeaFrame: NmeaFrame) = clickListener(nmeaFrame)
}

class NmeaFrameDiffCallback : DiffUtil.ItemCallback<NmeaFrame>() {
    override fun areItemsTheSame(oldItem: NmeaFrame, newItem: NmeaFrame): Boolean {
        return oldItem.timeStamp == newItem.timeStamp
    }

    override fun areContentsTheSame(oldItem: NmeaFrame, newItem: NmeaFrame): Boolean {
        return oldItem.content == newItem.content
    }
}

@BindingAdapter("nmeaTimestamp")
fun TextView.setNmeaTimeStamp(item: NmeaFrame) {
    item?.let {
        val instant = Instant.ofEpochMilli(item.timeStamp)
        text = dtf.format(instant)
    }
}

@BindingAdapter("nmeaFrameContent")
fun TextView.setNmeaFrameContent(item: NmeaFrame) {
    item?.let {
        text = item.content
    }
}