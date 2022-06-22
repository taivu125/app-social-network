package com.tqc.tuvisocial.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.tqc.tuvisocial.ui.main.MainActivity
import com.tqc.tuvisocial.ui.main.chat.call.VoiceCallActivity

class BaseApplication : Application() {

    var firebaseAuth: FirebaseAuth? = null
    var dataBase: FirebaseDatabase? = null
    var storage: FirebaseStorage? = null
    var user: FirebaseUser? = null
    var activity: Activity? = null
    var isBackground = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: BaseApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Firebase.initialize(this)
        firebaseAuth = FirebaseAuth.getInstance()
        dataBase = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        user = firebaseAuth?.currentUser

        //Lăng nghe trạng thái đăng nhập của user - ví dụ tài khoản bị xóa, bị disable
        FirebaseAuth.getInstance().addAuthStateListener {
            user = it.currentUser
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
            }

            override fun onActivityStarted(p0: Activity) {
                activity = p0
                isBackground = false
            }

            override fun onActivityResumed(p0: Activity) {
            }

            override fun onActivityPaused(p0: Activity) {
                if (p0 is MainActivity && p0 !is VoiceCallActivity) {
                    isBackground = true
                }
            }

            override fun onActivityStopped(p0: Activity) {
                if (activity == p0) {
                    activity = null
                }
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
            }

            override fun onActivityDestroyed(p0: Activity) {
            }

        })
    }
}