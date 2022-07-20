package com.group2.concord_messenger.model

import android.text.format.DateUtils.formatDateTime
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.installations.Utils
import com.group2.concord_messenger.R
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageListAdapter(private val dataset: List<MessageContent>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        val VIEW_TYPE_MESSAGE_SENT = 0
        val VIEW_TYPE_MESSAGE_RECEIVED = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Currently only support demo of sending a message
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_me,
                parent, false)
        return SentMessageHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message: MessageContent = dataset[position]
        // Currently only support demo of sending a message
        (holder as SentMessageHolder).bind(message)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}

class MessageContent(
    val message: String,
    val sender: String?,
    val sentAt: Long = 0
) {}

class SentMessageHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var messageText: TextView
    var dateText: TextView
    var timeText: TextView

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_me) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_me) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_me) as TextView
    }

    fun bind(message: MessageContent) {
        messageText.text = message.message
        // Format time
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM dd")
        val timeString = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
        val dateString = sdf.format(calendar.time)
        timeText.text = timeString
        dateText.text = dateString
    }
}