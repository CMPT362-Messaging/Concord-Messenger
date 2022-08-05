package com.group2.concord_messenger.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.group2.concord_messenger.R

class ContactListAdapter(private val context: Context,
                         var contacts: MutableList<UserProfile>,
                         val showAddButton: Boolean) : BaseAdapter()
{
    override fun getCount(): Int
    {
        return contacts.size
    }

    override fun getItem(position: Int): Any
    {
        return contacts[position]
    }

    override fun getItemId(position: Int): Long
    {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
    {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.contact, parent, false)

        val usernameView: TextView = view.findViewById(R.id.contact_textview_username)
        val profileImageView: ImageView = view.findViewById(R.id.contact_imageview_profile_image)
        val addButtonView: ImageView = view.findViewById(R.id.contact_imageview_plus)

        usernameView.text = contacts[position].userName
        //TODO - Load the users profile picture and display it

        if(showAddButton)
        {
            addButtonView.visibility = View.VISIBLE
        }
        else
        {
            addButtonView.visibility = View.INVISIBLE
        }

        return view
    }
}