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
                         var type: Int = 0) : BaseAdapter()
{
    private lateinit var usernameView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var addButtonView: ImageView
    private lateinit var removeButtonView: ImageView

    companion object
    {
        const val TYPE_NORMAL = 0
        const val TYPE_ADDABLE = 1
        const val TYPE_REMOVABLE = 2
    }

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

        usernameView = view.findViewById(R.id.contact_textview_username)
        profileImageView = view.findViewById(R.id.contact_imageview_profile_image)
        addButtonView = view.findViewById(R.id.contact_imageview_plus)
        removeButtonView = view.findViewById(R.id.contact_imageview_minus)

        usernameView.text = contacts[position].userName
        //TODO - Load the users profile picture and display it

        setContactType(type)

        return view
    }

    private fun setContactType(type: Int)
    {
        when(type)
        {
            // No buttons
            0 ->
            {
                addButtonView.visibility = View.INVISIBLE
                removeButtonView.visibility = View.INVISIBLE
            }
            // Plus button
            1 ->
            {
                addButtonView.visibility = View.VISIBLE
                removeButtonView.visibility = View.INVISIBLE
            }
            // Minus button
            2 ->
            {
                addButtonView.visibility = View.INVISIBLE
                removeButtonView.visibility = View.VISIBLE
            }
            // Invalid type
            else -> throw IllegalArgumentException()
        }
    }
}