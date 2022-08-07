package com.group2.concord_messenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.group2.concord_messenger.model.ContactListAdapter
import com.group2.concord_messenger.model.UserProfile

class ContactsActivity : AppCompatActivity() {
    private lateinit var fsDb: FirebaseFirestore
    private lateinit var fromUser: UserProfile
    private lateinit var contactsList: ArrayList<UserProfile>

    private lateinit var listView: ListView

    private var deleteMode = false
    private lateinit var menuView: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        listView = findViewById(R.id.contacts_listView)

        fsDb = FirebaseFirestore.getInstance()

        ConcordDatabase.getCurrentUser {
            val firebaseAuth = FirebaseAuth.getInstance()
            if(firebaseAuth.currentUser != null)
            {
                val userRef = fsDb.collection("users").document(firebaseAuth.currentUser!!.uid)
                userRef.addSnapshotListener()
                {
                    snapshot, _ ->
                    if(snapshot != null && snapshot.exists())
                    {
                        fromUser = snapshot.toObject(UserProfile::class.java)!!
                        populateListView()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuView = menu
        menuInflater.inflate(R.menu.contacts_delete, menu)
        menuInflater.inflate(R.menu.contacts_search, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when(item.itemId)
        {
            // Back button
            android.R.id.home -> finish()
            // Search button
            R.id.contacts_toolbar_search ->
            {
                val intent = Intent(this, AddContactsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            }
            // Delete button
            R.id.contacts_toolbar_delete ->
            {
                deleteMode = !deleteMode
                if(deleteMode)
                {
                    menuView.getItem(0).setIcon(R.drawable.ic_baseline_check_24)
                    (listView.adapter as ContactListAdapter).type = ContactListAdapter.TYPE_REMOVABLE
                    (listView.adapter as ContactListAdapter).notifyDataSetChanged()
                }
                else
                {
                    menuView.getItem(0).setIcon(R.drawable.ic_baseline_delete_outline_24)
                    (listView.adapter as ContactListAdapter).type = ContactListAdapter.TYPE_NORMAL
                    (listView.adapter as ContactListAdapter).notifyDataSetChanged()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun populateListView() {
        val fromUId = fromUser.uId
        println("Firebase id: $fromUId")
        val uIRef = fsDb.collection("users").document(fromUId)
        uIRef.get().addOnCompleteListener {
            if (it.isSuccessful) {
                val doc = it.result
                if (doc.exists()) {
                    fromUser = doc.toObject(UserProfile::class.java)!!
                    println("populateListView: fromUser: ${fromUser.userName}")
                    if (fromUser.groups != null) {
                        println("fromUser groups is not null, size: ${fromUser.groups!!.size}")
                    } else {
                        println("fromUser's group is null")
                    }

                    // Get a list of users from the current user's contact list
                    val userContactsRef = fsDb.collection("users")
                    userContactsRef.get().addOnCompleteListener { p ->
                        if (p.isSuccessful) {
                            contactsList = ArrayList()
                            for (i in p.result) {
                                val contact = i.toObject(UserProfile::class.java)
                                println("Discovered user: ${contact.userName}")
                                if (fromUser.contacts.contains(contact.uId)) {
                                    contactsList.add(contact)
                                }
                            }

                            val adapter = ContactListAdapter(this,
                                contactsList,
                                ContactListAdapter.TYPE_NORMAL)
                            listView.adapter = adapter
                            listView.setOnItemClickListener()
                            {
                                _, _, position, _ ->
                                if(deleteMode)
                                {
                                    val dialog = AlertDialog.Builder(this)
                                        .setMessage(String.format(resources.getString(R.string.contacts_confirm_remove_contact),
                                            adapter.contacts[position].userName))
                                        .setPositiveButton("Yes")
                                        {
                                            _, _ -> removeFromContacts(adapter.contacts[position].uId)
                                        }
                                        .setNegativeButton("No")
                                        {
                                            _, _ ->
                                        }
                                        .create()
                                    dialog.show()
                                }
                                else
                                {
                                    val intent = Intent(this, ChatActivity::class.java)
                                    intent.putExtra("fromUser", fromUser)
                                    intent.putExtra("toUser", contactsList[position])
                                    intent.putExtra("roomId", "none")
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        } else {
                            println("Getting list of users was unsuccessful")
                        }
                    }
                }
            }
        }
    }

    // Remove uid to the current user's contact list
    private fun removeFromContacts(uid: String)
    {
        // Make sure the uid is in the contact list
        if(fromUser.contacts.contains(uid))
        {
            fsDb.collection("users").document(fromUser.uId)
                .update("contacts", FieldValue.arrayRemove(uid))
            Toast.makeText(this,
                resources.getString(R.string.contacts_contact_removed),
                Toast.LENGTH_SHORT).show()
        }
    }
}