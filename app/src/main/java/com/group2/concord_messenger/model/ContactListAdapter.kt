package com.group2.concord_messenger.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.media.Image
import android.provider.MediaStore
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.group2.concord_messenger.R

class ContactListAdapter(private val context: Context,
                         var contacts: MutableList<UserProfile>,
                         var type: Int = TYPE_NORMAL) : BaseAdapter()
{
    private val DEFAULT_PROFILE_IMAGE = "gs://concord-messenger.appspot.com/images/default-profile-image.png"
    private val MAX_CACHED_IMAGES = 32

    private lateinit var addButtonView: ImageView
    private lateinit var removeButtonView: ImageView

    private var imgCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(4096 * MAX_CACHED_IMAGES)
    {
        override fun sizeOf(key: String, bitmap: Bitmap): Int
        {
            return bitmap.byteCount
        }
    }

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

        val usernameView: TextView = view.findViewById(R.id.contact_textview_username)
        val profileImageView: ImageView = view.findViewById(R.id.contact_imageview_profile_image)
        addButtonView = view.findViewById(R.id.contact_imageview_plus)
        removeButtonView = view.findViewById(R.id.contact_imageview_minus)

        usernameView.text = contacts[position].userName

        // Get the user's profile image
        val path = contacts[position].profileImg
        // Don't bother loading if it's the default image
        if(path != DEFAULT_PROFILE_IMAGE)
        {
            // Check if there's a cached version of the image using the url as the key
            val bitmapCached = imgCache.get(path)
            if(bitmapCached != null)
            {
                // Cached version exists
                profileImageView.setImageBitmap(bitmapCached)
            }
            else
            {
                // No cached version
                val imgRef = Firebase.storage.getReferenceFromUrl(path)
                // Get the image from storage
                imgRef.getBytes(Long.MAX_VALUE).addOnSuccessListener()
                {
                    // Compress is to 32 x 32 (4096 bytes total)
                    val bitmapOptions = BitmapFactory.Options()
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size, bitmapOptions)
                    val bitmapScaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
                    // Bitmaps need to be recycled!
                    bitmap.recycle()
                    profileImageView.setImageBitmap(bitmapScaled)
                    // Cache the compressed bitmap
                    imgCache.put(path, bitmapScaled)
                }
            }
        }

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