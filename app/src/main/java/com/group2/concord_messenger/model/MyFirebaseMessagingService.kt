package com.group2.concord_messenger.model

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.group2.concord_messenger.ChatActivity
import com.group2.concord_messenger.ConcordDatabase
import com.group2.concord_messenger.R
import java.lang.Exception

/**
 * Firebase messaging service to handle all message exchanges.
 * Using the firebase/quickstart-android repository as a template:
 * https://github.com/firebase/quickstart-android/blob/916586ef511409327f94d38c077b950af45b2159/messaging/app/src/main/java/com/google/firebase/quickstart/fcm/kotlin/MyFirebaseMessagingService.kt
 * and following the tutorial in the Firebase documentation:
 * https://firebase.google.com/docs/cloud-messaging/android/client
 */
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService: FirebaseMessagingService() {

    // Receives the remote message sent by our Google Cloud function.
    // Currently the message will always be processed here as it is sent with the "data" tag,
    // if sent with the "notification" tag it will be processed automatically, outside of our control
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // The current user, as a UserProfile, is needed as the ChatActivity will be launched
        // if the user clicks on the notification.
        ConcordDatabase.getCurrentUser { usr ->
            if (usr != null) {
                // Intent for launching ChatActivity
                val intent = Intent(this, ChatActivity::class.java)
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channelName = "New notification"
                    val description = "FCM notification"
                    val adminChannel = NotificationChannel("admin_channel", channelName,
                        NotificationManager.IMPORTANCE_HIGH)
                    adminChannel.description = description
                    adminChannel.enableLights(true)
                    adminChannel.lightColor = Color.BLUE
                    adminChannel.enableVibration(true)
                    notifManager.createNotificationChannel(adminChannel)
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val senderId = remoteMessage.data["senderId"]
                val fsDb = FirebaseFirestore.getInstance()
                // Find the sender in the database and build a UserProfile for them
                val senderRef = fsDb.collection("users").document(senderId!!)
                senderRef.get().addOnCompleteListener {
                    if(it.isSuccessful && it.result.exists()) {
                        val sender = it.result.toObject(UserProfile::class.java)
                        // Add all data to the intent that ChatActivity needs
                        // roomId of "none" will cause ChatActivity to automatically search for it
                        intent.putExtra("fromUser", usr)
                        intent.putExtra("toUser", sender)
                        intent.putExtra("roomId", "none")

                        val stackBuilder = TaskStackBuilder.create(this)
                        stackBuilder.addNextIntentWithParentStack(intent)
                        val pIntent = stackBuilder.getPendingIntent(0,
                            PendingIntent.FLAG_UPDATE_CURRENT)
                        // Set the notification settings
                        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val builder = NotificationCompat.Builder(this, "admin_channel")
                            .setContentTitle(remoteMessage.data["title"])
                            .setContentText(remoteMessage.data["body"])
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setAutoCancel(true)
                            .setSound(soundUri)
                            .setContentIntent(pIntent)
                        notifManager.notify(0, builder.build())
                    }
                    else {
                        Log.println(Log.DEBUG, "MyFirebaseMessagingService",
                            "Query for sender user was not successful")
                    }
                }
            }
        }
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.println(Log.DEBUG, "FirebaseMessaging", "$msgId: Message sent")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.println(Log.ERROR, "FirebaseMessaging", "$msgId: ERROR: Message could not be sent")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}