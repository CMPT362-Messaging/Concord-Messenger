package com.group2.concord_messenger

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
                        val userProfile = UserProfile(
                            uId = firebaseAuth.currentUser!!.uid,
                            userName = firebaseAuth.currentUser!!.displayName ?: "",
                            email = firebaseAuth.currentUser!!.email ?: ""
                        )
                        db.collection("users").document(userProfile.uId)
                            .set(userProfile, SetOptions.merge())
                        insertCurrentUserHandler(STATUS_SUCCESS)
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
                        getCurrentUserHandler(it.result.toObject(UserProfile::class.java))
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
    }
}

