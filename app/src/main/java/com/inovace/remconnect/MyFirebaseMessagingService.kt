package com.inovace.remconnect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("m", "r")

        if (remoteMessage.data.isNotEmpty()) {
            val notifytitle = remoteMessage.data["title"]
            val notifybody = remoteMessage.data["body"]
            if (notifybody != null) {
                Log.d("body",notifybody)
            }
            if (notifybody != null ) {
                sendNotification(notifybody, notifytitle)
            }
            val packageNamesData = remoteMessage.data["package_names"]
            val proxmox = remoteMessage.data["prox"]?.toBoolean()
            proxmox?.let { MySingleton.setSomeData(it) }
            packageNamesData?.let {
                val intent = Intent(this, MyAccessibilityService::class.java)
                intent.action = "UPDATE_PACKAGE_NAMES"
                intent.putExtra("package_names", it)
                startService(intent)
            }
        }


    }

    private fun sendNotification(messageBody: String?, notifytitle: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val vibratePattern = longArrayOf(0, 400, 200, 400)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.fcm_message) + notifytitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setVibrate(vibratePattern)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "REMCONNECT",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0, notificationBuilder.build())
    }

    /*override fun onNewToken(token: String) {
        FirebaseApp.initializeApp(this)
        val firebaseAuth = FirebaseAuth.getInstance()
        super.onNewToken(token)
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val tokenRef: DatabaseReference = database.getReference("fcm_tokens")
        val userId: String? = firebaseAuth.currentUser?.uid
        if (userId != null) {
            tokenRef.child(userId).setValue(token)
        } else {
            // Handle the failure to get the FCM token, e.g., logging or showing an error message.
        }
    }*/

}
