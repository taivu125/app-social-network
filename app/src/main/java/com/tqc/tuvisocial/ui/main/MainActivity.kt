package com.tqc.tuvisocial.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.shared.DialogHelper
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.SharedPref
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.OnToHome
import com.tqc.tuvisocial.ui.main.chat.view.MessageFragment
import com.tqc.tuvisocial.ui.main.chat.view.MessageGroupFragment
import com.tqc.tuvisocial.ui.main.home.create.CreatePostFragment
import com.tqc.tuvisocial.ui.main.profile.setting.avt.EditAvtFragment
import com.tqc.tuvisocial.ui.main.profile.setting.bg.EditBackgroundFragment
import com.tqc.tuvisocial.ui.splash_screen.SplashScreenActivity
import org.greenrobot.eventbus.EventBus

class MainActivity : BaseActivity() {

    override fun getContainerId() = R.id.main

    private var isChat = false

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        setContentView(R.layout.activity_main)
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.voiceKeyRefer)?.get()?.addOnSuccessListener {
            SharedPref.tokenVoiceCall = it.value.toString()
        }
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.get()?.addOnSuccessListener {
            val user = Gson().fromJson<UserModel>(
                Gson().toJson(it.value),
                object : TypeToken<UserModel>() {}.type
            )
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (token != user.fcmToken) {
                    it.ref.updateChildren(
                        mapOf(
                            "fcmToken" to token
                        )
                    )
                }
            }
        }
        if (savedInstanceState == null) {
            if (intent.extras?.getString("chat") != null) {
                isChat = true
                if (intent.extras?.getString("group") == "true") {
                    push(MessageGroupFragment.newInstance(intent.extras?.getString("chat")!!))
                } else {
                    val userModel = Gson().fromJson<UserModel>(intent.extras?.getString("user"), object : TypeToken<UserModel>() {}.type)
                    push(MessageFragment.newInstance(userModel, intent.extras?.getString("chat")!!, true))
                }
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main, MainFragment.newInstance(), "MainFragment")
                    .commitNow()
            }
        }
    }

    override fun onBackPressed() {
        //Kiểm tra nếu đang ở trang chính thi hiển thị đăng xuất, không sẽ back về
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (isChat) finish()
            EventBus.getDefault().post(OnToHome())
        } else {
            val createPost = supportFragmentManager.findFragmentByTag(CreatePostFragment::class.java.simpleName) as? CreatePostFragment
            val editAvt = supportFragmentManager.findFragmentByTag(EditAvtFragment::class.java.simpleName) as? EditAvtFragment
            val editBG = supportFragmentManager.findFragmentByTag(EditBackgroundFragment::class.java.simpleName) as? EditBackgroundFragment
            when {
                createPost != null -> {
                    createPost.onBackPress()
                }
                editAvt != null -> {
                    editAvt.onBackPress()
                }
                editBG != null -> {
                    editBG.onBackPress()
                }
                else -> {
                    supportFragmentManager.popBackStack()
                }
            }
        }
    }

    //Khi vào app thì cập nhật trạng thái online
    override fun onResume() {
        super.onResume()
        if (myInfo?.uid?.isNotEmpty() == true) {
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(myInfo?.uid ?: "")?.child("online")?.setValue(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (myInfo?.uid?.isNotEmpty() == true) {
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(myInfo?.uid ?: "")?.child("online")?.setValue(false)
        }
    }
}