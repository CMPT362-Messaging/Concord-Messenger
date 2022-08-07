package com.group2.concord_messenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.group2.concord_messenger.model.ContactListAdapter
import com.group2.concord_messenger.model.UserProfile

class AddContactsActivity : AppCompatActivity(), SearchView.OnQueryTextListener
{
    private lateinit var interactableViews: List<View>

    private lateinit var linearLayout: LinearLayout
    private lateinit var searchView: SearchView
    private lateinit var contactsListView: ListView
    private lateinit var progressBarView: ProgressBar

    private lateinit var db: FirebaseFirestore
    private var userList: List<UserProfile>? = null
    private var contactList: MutableList<UserProfile> = mutableListOf()
    private lateinit var currentUserProfile: UserProfile
    private var searchText = ""

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contacts)

        // Get views
        linearLayout = findViewById(R.id.add_contacts_linear_layout)
        searchView = findViewById(R.id.add_contacts_searchview)
        contactsListView = findViewById(R.id.add_contacts_listview)
        progressBarView = findViewById(R.id.add_contacts_progress_bar)
        // Listener when the search button is pressed
        searchView.setOnQueryTextListener(this)
        // Don't allow the list to be focused
        linearLayout.isFocusable = false
        contactsListView.isFocusable = false

        interactableViews = listOf(
            searchView,
            contactsListView,
        )

        // Attach ContactListAdapter
        val addContactListAdapter = ContactListAdapter(this, contactList, ContactListAdapter.TYPE_ADDABLE)
        contactsListView.adapter = addContactListAdapter
        contactsListView.setOnItemClickListener()
        {
            // Item in list view is pressed
            _, _, position, _ ->
            // Show an alert dialog asking if the person should be added to contacts
            val dialog = AlertDialog.Builder(this)
                .setMessage(String.format(resources.getString(R.string.contacts_confirm_add_contact),
                    contactList[position].userName))
                .setPositiveButton("Yes")
                {
                    _, _ -> addToContacts(contactList[position].uId)
                }
                .setNegativeButton("No")
                {
                    _, _ ->
                }
                .create()
            dialog.show()
        }

        // Load users from db
        setLoading(this, progressBarView, interactableViews)
        ConcordDatabase.getAllUsers()
        {
            userList = it
            updateContactsList()
            clearLoading(this, progressBarView, interactableViews)
        }

        /*
         * Update currentUserProfile whenever a change is made to the current user in the db
         * Now we can use currentUserProfile to change the list view
         */
        val firebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser != null)
        {
            db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(firebaseAuth.currentUser!!.uid)
            userRef.addSnapshotListener()
            {
                snapshot, _ ->
                if(snapshot != null && snapshot.exists())
                {
                    currentUserProfile = snapshot.toObject(UserProfile::class.java)!!
                    updateContactsList()
                }
            }
        }
    }

    override fun onBackPressed()
    {
        finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    // Add uid to the current user's contact list
    private fun addToContacts(uid: String)
    {
        // Make sure the uid isn't already in the contact list
        if(!currentUserProfile.contacts.contains(uid))
        {
            db.collection("users").document(currentUserProfile.uId)
                .update("contacts", FieldValue.arrayUnion(uid))
            Toast.makeText(this,
                resources.getString(R.string.contacts_contact_added),
                Toast.LENGTH_SHORT).show()
        }
    }

    // Update the list that the list view adapter displays
    private fun updateContactsList()
    {
        if(userList != null)
        {
            // First clear the list
            contactList.clear()
            if(searchText.isNotBlank())
            {
                // For every user in the db...
                for(user in userList!!)
                {
                    // Don't display users that are already in the contact list
                    if(!currentUserProfile.contacts.contains(user.uId)
                        // Don't display the current user themself
                        && currentUserProfile.uId != user.uId
                        // Don't display users whose username didn't match the search query
                        && user.userName.contains(searchText, true))
                    {
                        contactList.add(user)
                    }
                }
                // Sort alphabetically
                contactList.sortWith(compareBy{it.userName})
                // Update the list view
                (contactsListView.adapter as ContactListAdapter).notifyDataSetChanged()
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean
    {
        if(searchView.query.isNotBlank())
        {
            searchText = searchView.query.toString()
        }
        linearLayout.clearFocus()
        updateContactsList()
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean
    {
        return true
    }
}