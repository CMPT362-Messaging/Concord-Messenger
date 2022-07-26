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
    private var firebaseUser: GoogleSignInAccount? = null
    private lateinit var listView: ListView
    private lateinit var fromUser: UserProfile
    private lateinit var contactsList: ArrayList<UserProfile>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        firebaseUser = GoogleSignIn.getLastSignedInAccount(this)

        listView = findViewById(R.id.contacts_listView)
        populateListView()

        listView.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("fromUser", fromUser)
            intent.putExtra("toUser", contactsList[position])
            intent.putExtra("roomId", "none")
            startActivity(intent)
            finish()
        }
    }

    private fun populateListView() {
        if (firebaseUser != null) {
            val fromUId = firebaseUser?.id
            fsDb = FirebaseFirestore.getInstance()
            println("Firebase id: $fromUId")
            val uIRef = fsDb.collection("users").document(fromUId!!)
            uIRef.get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val doc = it.result
                    if (doc.exists()) {
                        fromUser = doc.toObject(UserProfile::class.java)!!
                        /*val userContactsRef = fsDb.collection("contacts")
                            .document(fromUId).collection("userContacts")*/
                        val userContactsRef = fsDb.collection("users")
                        userContactsRef.get().addOnCompleteListener { p ->
                            if (p.isSuccessful) {
                                val contactsNames = ArrayList<String>()
                                contactsList = ArrayList()
                                val groupsList = ArrayList<String>()
                                for (i in p.result) {
                                    val contact = i.toObject(UserProfile::class.java)
                                    println("Discovered user: ${contact.userName}")
                                    if (contact.userName != firebaseUser!!.displayName) {
                                        contactsNames.add(contact.userName)
                                        contactsList.add(contact)
                                        groupsList.add(contact.uId)
                                    }
                                }
                                val adapter = ArrayAdapter(this,
                                    android.R.layout.activity_list_item, android.R.id.text1, contactsNames)
                                listView.adapter = adapter
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