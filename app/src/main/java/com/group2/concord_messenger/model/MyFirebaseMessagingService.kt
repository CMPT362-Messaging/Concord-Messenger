package com.group2.concord_messenger.model

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase messaging service to handle all message exchanges.
 * Using the firebase/quickstart-android repository as a template:
 * https://github.com/firebase/quickstart-android/blob/916586ef511409327f94d38c077b950af45b2159/messaging/app/src/main/java/com/google/firebase/quickstart/fcm/kotlin/MyFirebaseMessagingService.kt
 * and following the tutorial in the Firebase documentation:
 * https://firebase.google.com/docs/cloud-messaging/android/client
 */
class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    private fun scheduleJob() {

    }

    private fun handleNow() {

    }

    private fun sendRegistrationToServer(token: String?) {

    }

    private fun sendNotification(messageBody: String) {

    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }


}