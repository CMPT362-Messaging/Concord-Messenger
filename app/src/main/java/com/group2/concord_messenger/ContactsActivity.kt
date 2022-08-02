package com.group2.concord_messenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.ListView
import com.firebase.ui.auth.data.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.group2.concord_messenger.model.UserProfile

class ContactsActivity : AppCompatActivity() {
    private lateinit var fsDb: FirebaseFirestore
    private lateinit var listView: ListView
    private var fromUser: UserProfile? = null
    private lateinit var contactsList: ArrayList<UserProfile>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        listView = findViewById(R.id.contacts_listView)

        ConcordDatabase.getCurrentUser {
            fromUser = it
            populateListView()
        }
    }

    private fun populateListView() {
        if (fromUser != null) {
            val fromUId = fromUser?.uId
            fsDb = FirebaseFirestore.getInstance()
            println("Firebase id: $fromUId")
            val uIRef = fsDb.collection("users").document(fromUId!!)
            uIRef.get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val doc = it.result
                    if (doc.exists()) {
                        fromUser = doc.toObject(UserProfile::class.java)!!
                        println("populateListView: fromUser: ${fromUser!!.userName}")
                        if (fromUser!!.groups != null) {
                            println("fromUser groups is not null, size: ${fromUser!!.groups!!.size}")
                        } else {
                            println("fromUser's group is null")
                        }
                        // Get all users so the current user can start a chat with
                        // any user registered with the app
                        val userContactsRef = fsDb.collection("users")
                        userContactsRef.get().addOnCompleteListener { p ->
                            if (p.isSuccessful) {
                                val contactsNames = ArrayList<String>()
                                contactsList = ArrayList()
                                val groupsList = ArrayList<String>()
                                for (i in p.result) {
                                    val contact = i.toObject(UserProfile::class.java)
                                    println("Discovered user: ${contact.userName}")
                                    if (contact.uId != fromUser!!.uId) {
                                        contactsNames.add(contact.userName)
                                        contactsList.add(contact)
                                        groupsList.add(contact.uId)
                                    }
                                }
                                val adapter = ArrayAdapter(this,
                                    android.R.layout.simple_list_item_1, android.R.id.text1, contactsNames)
                                listView.adapter = adapter
                                listView.setOnItemClickListener { parent, view, position, id ->
                                    val intent = Intent(this, ChatActivity::class.java)
                                    intent.putExtra("fromUser", fromUser)
                                    intent.putExtra("toUser", contactsList[position])
                                    intent.putExtra("roomId", "none")
                                    startActivity(intent)
                                    finish()
                                }
                            } else {
                                println("Getting list of users was unsuccessful")
                            }
                        }
                    }
                }
            }
        }
    }
}