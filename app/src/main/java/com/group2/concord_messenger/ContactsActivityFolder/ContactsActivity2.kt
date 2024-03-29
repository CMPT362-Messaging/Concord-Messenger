package com.group2.concord_messenger.ContactsActivityFolder

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.group2.concord_messenger.ChatActivity
import com.group2.concord_messenger.ConcordDatabase
import com.group2.concord_messenger.GroupChatActivity
import com.group2.concord_messenger.R
import com.group2.concord_messenger.model.UserProfile
import java.lang.Thread.sleep

class ContactsActivity2 : AppCompatActivity() {
    private lateinit var fsDb: FirebaseFirestore
    private var fromUser: UserProfile? = null

    //store user profiles
    private lateinit var contactsList: ArrayList<UserProfile>


    //will take relevent user profile data that will be read by adapter for recylcerview
    private lateinit var contactsDataList: ArrayList<ContactsData>

    private lateinit var recyclerView: RecyclerView

    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts2)
        recyclerView = findViewById(R.id.contacts_recyclerView)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        ConcordDatabase.getCurrentUser {
            fromUser = it
            populateListView()
        }

        /*
        when the user clicks the submit button, there are 4 things that could happen. the
        user selects 1 other user, the user selected multiple other users,  the user selects a group,
        or the user selects nothing. Each of these cases will be dealt with separately.
         */
        submitButton = findViewById(R.id.submitButton)
        submitButton.setOnClickListener {

            val selectedUsers = getSelectedUsers()
            //user selected nobody
            if (selectedUsers.size == 0) {
                finish()
            }
            //if the user selected multiple users
            else if (selectedUsers.size > 1) {
                val arrayOfSelectedUsers = ArrayList<UserProfile>()
                //build array of user profiles
                for (ind in selectedUsers) {
                    arrayOfSelectedUsers.add(contactsList[ind])
                }
                val groupID = createGroup(arrayOfSelectedUsers)
                //wait a little bit so that the database has enough time to add the group
                sleep(150)
                val intent = Intent(this, GroupChatActivity::class.java)
                intent.putExtra("fromUser", fromUser)
                //intent.putExtra("toUser", selectedUsers)
                intent.putExtra("roomId", groupID)
                startActivity(intent)
                finish()
            }
            //if the user selected 1 person
            else if (selectedUsers.size == 1) {
                //check if its a user or a group

                //if user selected a group
                if (contactsDataList[selectedUsers[0]].type == 1) {
                    val intent = Intent(this, GroupChatActivity::class.java)
                    intent.putExtra("fromUser", fromUser)
                    //intent.putExtra("toUser", selectedUsers)
                    intent.putExtra("roomId", contactsDataList[selectedUsers[0]].name)
                    startActivity(intent)
                }
                //if user selected 1 user
                else if (contactsDataList[selectedUsers[0]].type == 0) {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("fromUser", fromUser)
                    intent.putExtra("toUser", contactsList[selectedUsers[0]])
                    intent.putExtra("roomId", "none")
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun createGroup(selectedUsers: ArrayList<UserProfile>): String {
        fsDb = FirebaseFirestore.getInstance()
        //create group id
        val groupId = fsDb.collection("messages").document().id
        //create array for holding user profiles
        val updateduserProfiles = ArrayList<UserProfile>()
        //add group for current user
        if (fromUser!!.groups != null) {
            fromUser!!.groups!![groupId] = true
        } else {
            fromUser!!.groups = mutableMapOf()
            fromUser!!.groups!![groupId] = true
        }
        updateduserProfiles.add(fromUser!!)
        //update the groups field in each selected user object to include the new group
        // and add each selected user to the updateduserProfiles list
        for (user in selectedUsers) {
            val uIRef = fsDb.collection("users").document(user.uId)
            uIRef.get().addOnCompleteListener {
                if (it.isSuccessful && it.result.exists()) {
                    val doc = it.result
                    val userSnapshot = doc.toObject(UserProfile::class.java)
                    if (userSnapshot != null) {
                        //update user snapshot to include new group
                        if (userSnapshot.groups == null) {
                            userSnapshot.groups = mutableMapOf()
                            userSnapshot.groups!![groupId] = true

                        } else {
                            userSnapshot.groups!![groupId] = true
                        }
                        //add user to userProfiles variables
                        updateduserProfiles.add(userSnapshot)

                    }

                } else {
                    println("No historical data to retrieve")
                }
                //for each updatedUser, update necessary fields in the database.
                //these fields will be
                //groups->[userid]->usergroups->[groupid]
                for (updatedUser in updateduserProfiles) {
                    //update the users profile in the users collection
                    fsDb.collection("users").document(updatedUser.uId)
                        .set(updatedUser, SetOptions.merge())
                }
                val map: MutableMap<String, ArrayList<UserProfile>> = mutableMapOf()
                map["Users"] = updateduserProfiles
                fsDb.collection("MultipleUserGroup").document(groupId).set(map)
            }
        }

        return groupId
    }

    private fun getSelectedUsers(): ArrayList<Int> {
        //if a user is marked as "checked", add the index of that UserProfile to the contactsListReturn
        //using the contactsList array.
        val contactsListReturn = ArrayList<Int>()
        for (userind in 0 until contactsDataList.size - 1) {
            if (contactsDataList[userind].isChecked) {
                contactsListReturn.add(userind)
            }
        }
        return contactsListReturn
    }

    private fun populateListView() {
        if (fromUser != null) {
            contactsDataList = ArrayList()
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
                        // Get all users so the current user can start a chat with
                        // any user registered with the app
                        val userContactsRef = fsDb.collection("users")
                        userContactsRef.get().addOnCompleteListener { p ->
                            if (p.isSuccessful) {
                                contactsList = ArrayList()
                                val groupsList = ArrayList<String>()
                                for (i in p.result) {
                                    val contact = i.toObject(UserProfile::class.java)
                                    println("Discovered user: ${contact.userName}")
                                    if (contact.uId != fromUser!!.uId) {
                                        groupsList.add(contact.uId)
                                        val contactsData = ContactsData(
                                            contact.userName,
                                            false,
                                            Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
                                            ContactsActivityAdapter.VIEW_TYPE_PRIVATE,
                                            ""
                                        )
                                        contactsList.add(contact)
                                        contactsDataList.add(contactsData)
                                    }

                                    val adapter = ContactsActivityAdapter(contactsDataList)
                                    recyclerView.adapter = adapter
                                    recyclerView.layoutManager = LinearLayoutManager(this)
                                }

                                //add all groups to contact data if there are any groups
                                if (fromUser!!.groups != null) {
                                    for ((key, value) in fromUser!!.groups!!) {
                                        if (fromUser!!.groups!![key] == true) {
                                            contactsDataList.add(
                                                ContactsData(
                                                    key,
                                                    false,
                                                    Bitmap.createBitmap(
                                                        100,
                                                        100,
                                                        Bitmap.Config.ARGB_8888
                                                    ),
                                                    ContactsActivityAdapter.VIEW_TYPE_GROUP, ""
                                                )
                                            )
                                        }
                                    }
                                    println("fromUser groups is not null, size: ${fromUser!!.groups!!.size}")
                                } else {
                                    println("fromUser's group is null")
                                }

                                setTitleFromGroup()

                            } else {
                                println("Getting list of users was unsuccessful")
                            }


                        }


                    }
                }
            }


        }
    }

    private fun setTitleFromGroup() {
        val fsDb = FirebaseFirestore.getInstance()
        for (contact in contactsDataList) {
            if (contact.type == ContactsActivityAdapter.VIEW_TYPE_GROUP) {
                val uIRef = fsDb.collection("MultipleUserGroup").document(contact.name)
                var groupName = ""
                uIRef.get().addOnCompleteListener {
                    if (it.isSuccessful && it.result.exists()) {
                        val doc = it.result
                        val users: java.util.ArrayList<MutableMap<String, String>> =
                            doc.get("Users") as java.util.ArrayList<MutableMap<String, String>>

                        for (userInd in 0 until users.size - 1) {
                            val curUserMap = users[userInd]
                            val mName = curUserMap["userName"]
                            groupName = "$groupName $mName"
                        }
                        contact.groupName = groupName
                        recyclerView.adapter?.notifyDataSetChanged()

                    } else {

                    }
                }
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}