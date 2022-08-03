//package com.group2.concord_messenger.ContactsActivityFolder
//
//import android.R
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.group2.concord_messenger.model.ChatMessageListAdapter
//import com.group2.concord_messenger.model.ConcordMessage
//import com.group2.concord_messenger.model.ReceivedMessageHolder
//import com.group2.concord_messenger.model.SentMessageHolder
//
//
//class ContactsActivityAdapter(usernames: List<String>): RecyclerView.Adapter<ContactsActivityAdapter.ViewHolder>() {
//
////    private var usernames: List<String>? = null
////    private lateinit var checkmark: ImageView
////    private var mInflater: LayoutInflater? = null
////    private var mClickListener: ItemClickListener? = null
////
////    // data is passed into the constructor
////
////
////    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
////        val view = LayoutInflater.from(parent.context).inflate(
////            com.group2.concord_messenger.R.layout.activity_contacts_list_item,
////            parent, false
////        )
////        return ViewHolder(view);
////    }
////
////    override fun getItemViewType(position: Int): Int {
////
////    }
////
////    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
////
////
////    }
////
////
////    override fun getItemCount(): Int {
////        TODO("Not yet implemented")
////    }
////
////    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
////        View.OnClickListener {
////        var myTextView: TextView
////        override fun onClick(view: View) {
////            if (mClickListener != null) mClickListener.onItemClick(view, adapterPosition)
////        }
////
////        init {
////            itemView.setOnClickListener(this)
////        }
////    }
////
////    fun setClickListener(itemClickListener: ItemClickListener) {
////        this.mClickListener = itemClickListener
////    }
////
////    // parent activity will implement this method to respond to click events
////    interface ItemClickListener {
////        fun onItemClick(view: View?, position: Int)
////    }
////}
////
////class ReceivedMessageHolder(itemView: View) :
////    RecyclerView.ViewHolder(itemView) {
////    var name: TextView
////    var profilePic: ImageView
////
////
////    init {
////        name = itemView.findViewById<View>(com.group2.concord_messenger.R.id.username) as TextView
////        profilePic = itemView.findViewById<View>(com.group2.concord_messenger.R.id.profilePic) as ImageView
////
////    }
//
//}