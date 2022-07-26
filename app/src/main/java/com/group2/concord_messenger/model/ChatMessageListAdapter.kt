package com.group2.concord_messenger.model

import android.text.format.DateUtils.formatDateTime
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.installations.Utils
import com.group2.concord_messenger.R
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageListAdapter(private val fromUid: String, private val recyclerView: RecyclerView,
                             options: FirestoreRecyclerOptions<ConcordMessage>
) : FirestoreRecyclerAdapter<ConcordMessage, RecyclerView.ViewHolder>(options) {
    companion object {
        const val VIEW_TYPE_MESSAGE_SENT = 0
        const val VIEW_TYPE_MESSAGE_RECEIVED = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        print("Layout id: $viewType")
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.message_me,
                parent, false
            )
            println("SentMessageHolder")
            SentMessageHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.message_other,
                parent, false
            )
            println("ReceivedMessageHolder")
            ReceivedMessageHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).fromUid == fromUid) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, model: ConcordMessage) {
        val message: ConcordMessage = getItem(position)
        if (message.fromUid == fromUid) {
            (holder as SentMessageHolder).bind(message)
        } else {
            (holder as ReceivedMessageHolder).bind(message)
        }
    }

    override fun onDataChanged() {
        recyclerView.layoutManager?.scrollToPosition(itemCount - 1)
    }
}

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

    fun bind(message: ConcordMessage) {
        messageText.text = message.text
        // Format time
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM dd")
        val timeString = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
        val dateString = sdf.format(calendar.time)
        timeText.text = timeString
        dateText.text = dateString
    }
}

class ReceivedMessageHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var messageText: TextView
    var nameText: TextView
    var dateText: TextView
    var timeText: TextView

    init {
        messageText = itemView.findViewById<View>(R.id.text_gchat_message_other) as TextView
        nameText = itemView.findViewById<View>(R.id.text_gchat_user_other) as TextView
        dateText = itemView.findViewById<View>(R.id.text_gchat_date_other) as TextView
        timeText = itemView.findViewById<View>(R.id.text_gchat_timestamp_other) as TextView
    }

    fun bind(message: ConcordMessage) {
        messageText.text = message.text
        nameText.text = message.fromName
        // Format time
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM dd")
        val timeString = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
        val dateString = sdf.format(calendar.time)
        timeText.text = timeString
        dateText.text = dateString
    }
}