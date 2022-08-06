package com.group2.concord_messenger.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*


class UserProfile(var uId: String = "0",
                  var userName: String="",
                  var groups: MutableMap<String, Any>? = null,
                  var email: String="",
                  // The firebase token, unique to each instance of the app on every unique
                  // device
                  var tokenId: String="",
                  @ServerTimestamp
                       var createdAt: Date?=null) : Serializable {
                           fun UserProfile() {

                           }
                       }
