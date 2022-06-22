package com.tqc.tuvisocial.ui.splash_screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.helper.Helper.changeLanguage
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.ui.account.login.LoginActivity
import com.tqc.tuvisocial.ui.main.MainActivity

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changeLanguage(this)
        window.statusBarColor = ContextCompat.getColor(this, R.color.red)
        setContentView(R.layout.activity_splash_screen)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            Handler(Looper.getMainLooper()).postDelayed({
                if (userID != "" && myInfo != null) {
                    //Cập nhật FCM token, để nhận thông báo
                    FirebaseMessaging.getInstance().token.addOnSuccessListener {
                        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)
                                ?.child("fcmToken")?.setValue(it)
                    }
                    //Cập nhật thông tin mới nhất mỗi khi vào ứngdujng
                    BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(myInfo?.uid!!)?.addListenerForSingleValueEvent(object:
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.value != null) {
                                val userModel: UserModel = Gson().fromJson(
                                    Gson().toJson(snapshot.value),
                                    object : TypeToken<UserModel>() {}.type
                                )
                                //Luu lại uid - định danh và thông tin của user
                                userID = userModel.uid
                                myInfo = userModel
                                //Cập nhật fcm token
                                startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
                                finish()
                            } else {
                                startActivity(Intent(this@SplashScreenActivity, LoginActivity::class.java))
                                finish()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        when {
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO), 105)
            }
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA), 105)
            }
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 105)
            }
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 105)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            Handler(Looper.getMainLooper()).postDelayed({
                if (userID != "" && myInfo != null) {
                    //Cập nhật thông tin mới nhất mỗi khi vào ứngdujng
                    BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(myInfo?.uid!!)?.addListenerForSingleValueEvent(object:
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.value != null) {
                                val userModel: UserModel = Gson().fromJson(
                                    Gson().toJson(snapshot.value),
                                    object : TypeToken<UserModel>() {}.type
                                )
                                //Luu lại uid - định danh và thông tin của user
                                userID = userModel.uid
                                myInfo = userModel
                                startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
                                finish()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }, 1000)
        }
    }
}