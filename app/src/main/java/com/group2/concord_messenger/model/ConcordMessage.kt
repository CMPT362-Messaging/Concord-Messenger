package com.group2.concord_messenger.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class ConcordMessage(
    val fromUid: String = "",
    var text: String = "Default message",
    @ServerTimestamp
    var createdAt: Date? = Date()) : Serializable