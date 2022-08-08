package com.group2.concord_messenger.ContactsActivityFolder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.data.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.group2.concord_messenger.R
import com.group2.concord_messenger.model.ConcordMessage
import com.group2.concord_messenger.model.UserProfile
import kotlinx.coroutines.CompletableDeferred
import java.text.SimpleDateFormat
import java.util.*


class ContactsActivityAdapter(private var usernames: List<ContactsData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val VIEW_TYPE_PRIVATE = 0
        const val VIEW_TYPE_GROUP = 1
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> PrivateViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    com.group2.concord_messenger.R.layout.activity_contacts_list_item,
                    parent, false
                )
            )
            1 -> GroupViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    com.group2.concord_messenger.R.layout.activity_contacts_list_item,
                    parent, false
                )
            )
            else -> PrivateViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    com.group2.concord_messenger.R.layout.activity_contacts_list_item,
                    parent, false
                )
            )

        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (usernames[position].type == VIEW_TYPE_PRIVATE) {
            (holder as PrivateViewHolder).name.text = usernames[position].name
            holder.profilePic.setImageResource(R.drawable.ic_baseline_person_24)
            if (usernames[position].isChecked) {
                holder.isSelected.setImageResource(R.drawable.ic_baseline_check_circle_24)
            } else if (!usernames[position].isChecked) {
                holder.isSelected.setImageResource(com.group2.concord_messenger.R.drawable.ic_baseline_radio_button_unchecked_24)
            }
            //unselect row if its currently selected and clicked. Select row if its unselected and clicked.
            holder.itemView.setOnClickListener {
                if (usernames[position].isChecked) {
                    usernames[position].isChecked = false

                    holder.isSelected.setImageResource(com.group2.concord_messenger.R.drawable.ic_baseline_radio_button_unchecked_24)
                } else {
                    usernames[position].isChecked = true
                    holder.isSelected.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    //if a user is selected, no groups should be selected
                    for (user in 0 until usernames.size - 1) {
                        if (usernames[user].type == VIEW_TYPE_GROUP) {
                            usernames[user].isChecked = false
                        }
                    }
                    notifyDataSetChanged()
                }
            }
        }
        if (usernames[position].type == VIEW_TYPE_GROUP) {
            (holder as GroupViewHolder).groupName.text = usernames[position].groupName
            holder.profilePic.setImageResource(R.drawable.group_image)
            if (usernames[position].isChecked) {
                holder.isSelected.setImageResource(R.drawable.ic_baseline_check_circle_24)
            } else if (!usernames[position].isChecked) {
                holder.isSelected.setImageResource(com.group2.concord_messenger.R.drawable.ic_baseline_radio_button_unchecked_24)
            }
            holder.itemView.setOnClickListener {
                if (usernames[position].isChecked) {
                    usernames[position].isChecked = false
                    holder.isSelected.setImageResource(com.group2.concord_messenger.R.drawable.ic_baseline_radio_button_unchecked_24)
                } else {
                    //when the user selected a group, nothing else should be selected
                    for (user in 0 until usernames.size - 1) {
                        usernames[user].isChecked = false
                    }
                }
                usernames[position].isChecked = true
                holder.isSelected.setImageResource(R.drawable.ic_baseline_check_circle_24)
                notifyDataSetChanged()
            }
        }
    }


    override fun getItemCount(): Int {
        return usernames.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (usernames[position].type) {
            0 -> VIEW_TYPE_PRIVATE
            1 -> VIEW_TYPE_GROUP
            else -> -1
        }
    }


    inner class PrivateViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val name = itemView.findViewById<TextView>(R.id.username)
        val profilePic =
            itemView.findViewById<ImageView>(R.id.profilePic)
        val isSelected =
            itemView.findViewById<ImageView>(R.id.checkbox)
    }

    inner class GroupViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val groupName = itemView.findViewById<TextView>(R.id.username)
        val profilePic =
            itemView.findViewById<ImageView>(R.id.profilePic)
        val isSelected =
            itemView.findViewById<ImageView>(R.id.checkbox)
    }


}




