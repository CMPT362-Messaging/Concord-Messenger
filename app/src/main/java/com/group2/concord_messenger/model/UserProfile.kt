package com.group2.concord_messenger.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*


class UserProfile(var uId: String = "0",
                       var userName: String="",
                       var idToken: String="",
                       val groups: MutableMap<String, Any>? = null,
                       var email: String="",
                       @ServerTimestamp
                       var createdAt: Date?=null) : Serializable {
                           fun UserProfile() {

                           }
                       }
