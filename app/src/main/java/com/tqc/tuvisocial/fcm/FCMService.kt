package com.tqc.tuvisocial.fcm

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.sharedPref.SharedPref
import com.tqc.tuvisocial.sharedPref.SharedPref.chatOpening
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.HangOut
import com.tqc.tuvisocial.sharedPref.eventBus.HangUp
import com.tqc.tuvisocial.ui.main.MainActivity
import com.tqc.tuvisocial.ui.main.chat.call.VoiceCallActivity
import com.tqc.tuvisocial.ui.main.post.PostDetailActivity
import com.tqc.tuvisocial.ui.splash_screen.SplashScreenActivity
import org.greenrobot.eventbus.EventBus
import kotlin.random.Random

class FCMService : FirebaseMessagingService() {

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        //Xử lý thông báo từ firebase
        //Nếu là notify thì hiển thị notify
        if (p0.data["notification"] != null && p0.data["notification"].toBoolean()) {
            val title = p0.data["title"]
            val body = p0.data["body"]

            createNotificationChannel()
            val intent: Intent
            if (p0.data["chat"] == null) {
                intent = Intent(this, PostDetailActivity::class.java)
                intent.putExtra("PostID", p0.data["postID"])
                intent.putExtra("IsShared", p0.data["share"])
            } else {
                intent = Intent(this, MainActivity::class.java)
                intent.putExtra("chat", p0.data["chat"])
                intent.putExtra("group", p0.data["group"])
                intent.putExtra("user", p0.data["user"])
            }
            if (p0.data["chat"] != null && p0.data["chat"] == chatOpening) return

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            with(NotificationManagerCompat.from(this)) {
                // notificationId is a unique int for each notification that you must define
                notify(
                    (1..1000).random(), NotificationCompat.Builder(this@FCMService, "1")
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.ic_logo_blue)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true).build())
            }
            //Không phải notify xử lý sự kiện cuộc gọi
        } else if (p0.data["notification"] == null) {
            when (p0.data["HangOut"]) {
                "true" -> {
                    EventBus.getDefault().post(HangOut())
                }
                "false" -> {
                    EventBus.getDefault().post(HangUp())
                }
                else -> {
                    val callerID = p0.data["CallerID"]
                    val receiverID = p0.data["ReceiverID"]
                    val name = p0.data["Name"]
                    val messageKey = p0.data["MessageKey"]
                    val intent = Intent(this, VoiceCallActivity::class.java)
                    intent.putExtra("CallerID", callerID)
                    intent.putExtra("ReceiverID", receiverID)
                    intent.putExtra("Status", false)
                    intent.putExtra("MessageKey", messageKey)
                    val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent,
                        PendingIntent.FLAG_IMMUTABLE)

                    if (BaseApplication.instance?.isBackground == false) {
                        with(NotificationManagerCompat.from(this)) {
                            // notificationId is a unique int for each notification that you must define
                            notify(1, NotificationCompat.Builder(this@FCMService, "1")
                                    .setContentTitle("Calling")
                                    .setSmallIcon(R.drawable.ic_logo)
                                    .setContentText("$name called you")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).build())
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                    }
                    BaseApplication.instance?.activity?.startActivity(intent)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("1", "Notify", importance).apply {
                description = "Notify"
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}