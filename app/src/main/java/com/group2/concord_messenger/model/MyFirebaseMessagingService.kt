package com.group2.concord_messenger.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.group2.concord_messenger.ChatActivity
import com.group2.concord_messenger.R
import java.lang.Exception

/**
 * Firebase messaging service to handle all message exchanges.
 * Using the firebase/quickstart-android repository as a template:
 * https://github.com/firebase/quickstart-android/blob/916586ef511409327f94d38c077b950af45b2159/messaging/app/src/main/java/com/google/firebase/quickstart/fcm/kotlin/MyFirebaseMessagingService.kt
 * and following the tutorial in the Firebase documentation:
 * https://firebase.google.com/docs/cloud-messaging/android/client
 */
class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // super.onMessageReceived(remoteMessage)
        // There are two types of messages, i.e., data messages and notification messages.
        // The data messages are handled here in onMessageReceived, either the app is in the
        // foreground or background.
        // The data messages are traditionally used with GCM.
        // The notification messages are only received here in onMessageReceived when the app is
        // in the foreground, and if the app is in the background, the automatically generated
        // notification is displayed.
        // If the user taps on the notification, they are returned to the app.
        // Messages containing both notifications and data payloads are treated as
        // notification messages.
        // The Firebase console always sends a notification message.
        // For more see: https://firebase.google.com/docs/cloud-messaging/concept-options

        // TODO(developer): Handle FCM messages here.
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.body.toString())
        }

        // Also, if we intend on generating our own notifications as a result of a received FCM
        // message, here is where that should be initiated. See the send notification method below.
    }


    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun scheduleJob() {

    }

    private fun handleNow() {

    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send a token to our app server.
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val penIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT)

        val channelId = "1"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_light)
            .setContentTitle("test message")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(penIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(channelId, 1,  notificationBuilder.build())
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