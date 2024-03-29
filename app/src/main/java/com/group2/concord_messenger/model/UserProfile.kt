package com.group2.concord_messenger.model

import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*

class UserProfile(var uId: String = "0",
                  var userName: String="",
                  var groups: MutableMap<String, Any>? = null,
                  var email: String="",
                  var contacts: MutableList<String> = mutableListOf(),
                  // The firebase token, unique to each instance of the app on every unique
                  // device
                  var tokenId: String="",
                  var bio: String="",
                  var profileImg: String="gs://concord-messenger.appspot.com/images/default-profile-image.png",
                  @ServerTimestamp
                       var createdAt: Date?=null) : Serializable {
                           fun UserProfile() {

                           }
                       }
