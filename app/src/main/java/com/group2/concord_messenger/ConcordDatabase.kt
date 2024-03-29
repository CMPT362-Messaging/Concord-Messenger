package com.group2.concord_messenger

import android.content.ContentValues
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.group2.concord_messenger.model.UserProfile

class ConcordDatabase
{
    companion object
    {
        const val STATUS_SUCCESS = 0
        const val STATUS_FAILED = 1

        /**
         * Inserts the current authorized user into the database.
         */
        fun insertCurrentUser(insertCurrentUserHandler: (result: Int) -> Unit)
        {
            val firebaseAuth = FirebaseAuth.getInstance()

            if(firebaseAuth.currentUser != null)
            {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(firebaseAuth.currentUser!!.uid)
                userRef.get().addOnCompleteListener()
                {
                    if(it.isSuccessful && !it.result.exists())
                    {
                        // Get Firebase Cloud Messaging registration token
                        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Log.w(ContentValues.TAG,
                                    "Fetching FCM registration token failed", task.exception)
                                insertCurrentUserHandler(STATUS_FAILED)
                            }
                            // Token identifies a unique phone, will be removed from account
                            // when user logs out
                            val token = task.result
                            val userProfile = UserProfile(
                                uId = firebaseAuth.currentUser!!.uid,
                                userName = firebaseAuth.currentUser!!.displayName ?: "",
                                email = firebaseAuth.currentUser!!.email ?: "",
                                tokenId = token // Token will be used for receiving notifications
                            )
                            db.collection("users").document(userProfile.uId)
                                .set(userProfile, SetOptions.merge())
                            insertCurrentUserHandler(STATUS_SUCCESS)
                        })
                    }
                    else
                    {
                        insertCurrentUserHandler(STATUS_FAILED)
                    }
                }
            }
            else
            {
                insertCurrentUserHandler(STATUS_FAILED)
            }
        }

        /**
         * Gets the UserProfile for the current authorized user.
         */
        fun getCurrentUser(getCurrentUserHandler: (userProfile: UserProfile?) -> Unit)
        {
            val firebaseAuth = FirebaseAuth.getInstance()

            if(firebaseAuth.currentUser != null)
            {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(firebaseAuth.currentUser!!.uid)
                userRef.get().addOnCompleteListener()
                {
                    if(it.isSuccessful && it.result.exists())
                    {
                        val user = it.result.toObject(UserProfile::class.java)
                        // User has token removed when logging out, add it to the database
                        // now so that the user can receive notifications
                        FirebaseMessaging.getInstance().token.addOnCompleteListener(
                            OnCompleteListener { task ->
                                if (task.isSuccessful && user != null) {
                                    // Token identifies a unique phone, will be removed from account
                                    // when user logs out
                                    val token = task.result
                                    db.collection("users").document(user.uId)
                                        .update(mapOf("tokenId" to token))
                                    Log.println(Log.DEBUG, "getCurrentUser",
                                        "Updated tokenId for ${user.userName}")
                                }
                                getCurrentUserHandler(user)
                            })
                    }
                    else
                    {
                        getCurrentUserHandler(null)
                    }
                }
            }
            else
            {
                getCurrentUserHandler(null)
            }
        }

        /**
         * Gets all UserProfiles.
         */
        fun getAllUsers(getAllUsersHandler: (userProfiles: List<UserProfile>?) -> Unit)
        {
            val db = FirebaseFirestore.getInstance()
            val colRef = db.collection("users")
            colRef.get().addOnCompleteListener()
            {
                if(it.isSuccessful)
                {
                    val users = mutableListOf<UserProfile>()
                    for(user in it.result.documents)
                    {
                        users.add(user.toObject(UserProfile::class.java)!!)
                    }
                    getAllUsersHandler(users)
                }
                else
                {
                    getAllUsersHandler(null)
                }
            }
        }
    }
}

