package com.group2.concord_messenger.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class ConcordMessage(
    val fromUid: String = "",
    val fromName: String = "",
    var text: String = "Default message",
    var audio: Boolean = false, // Audio files are stored in Firebase Storage under /audio/{generatedUUID}.3gp
    var audioId: String = "",
    var image: Boolean = false,
    var imageName: String = "",
    @ServerTimestamp
    var createdAt: Date? = null) : Serializable