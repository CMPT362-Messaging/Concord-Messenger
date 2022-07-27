package com.group2.concord_messenger.model

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.group2.concord_messenger.R
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(private val options: FirestoreRecyclerOptions<UserProfile>
    ) : FirestoreRecyclerAdapter<UserProfile, ChatListAdapter.UserHolder>(options) {
    private var clickListener: ClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListAdapter.UserHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            android.R.layout.simple_list_item_1,
            parent, false
        )
        return UserHolder(view)
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int, model: UserProfile) {
        val user: UserProfile = getItem(position)
        holder.itemView.setOnClickListener {
            clickListener!!.onItemClicked(position)
        }
        holder.bind(user)
    }

    inner class UserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var userNameText: TextView

        init {
            userNameText = itemView.findViewById<View>(android.R.id.text1) as TextView
        }

        fun bind(user: UserProfile) {
            userNameText.text = user.userName
        }
    }

    fun setOnItemClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onItemClicked(position: Int)
    }
}