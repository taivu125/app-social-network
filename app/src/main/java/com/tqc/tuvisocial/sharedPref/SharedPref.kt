package com.tqc.tuvisocial.sharedPref

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.model.UserModel

object SharedPref {
    private const val PREF_NAME = "ActivitySPref"
    private const val PREF_USER_ID = "UserID"
    private const val PREF_MY_INFO = "MyInfo"
    private const val PREF_CHAT_KEY = "ChatKey"
    private const val PREF_VOICE_KEY = "VoiceKey"
    private const val PREF_FCM_KEY = "FCMToken"


    var userID: String?
        get() = BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.getString(PREF_USER_ID, "") ?: ""
        @SuppressLint("CommitPrefEdits")
        set(value) {
            BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()
                ?.putString(
                    PREF_USER_ID, value
                )?.apply()
        }


    //Thông tin user sẽ được lưu dưới dạng json nên khi lấy lên cần phải convert lại
    var myInfo: UserModel?
        get() = if (BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                ?.getString(PREF_MY_INFO, "")?.isNotEmpty() == true) Gson().fromJson<UserModel>(BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.getString(PREF_MY_INFO, ""), object : TypeToken<UserModel>() {}.type) else null
        @SuppressLint("CommitPrefEdits")
        set(value) {
            BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()
                ?.putString(
                    PREF_MY_INFO, Gson().toJson(value)
                )?.apply()
        }

    //Lưu thông tin chat dang mở
    var chatOpening: String?
        get() = BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.getString(PREF_CHAT_KEY, "") ?: ""
        @SuppressLint("CommitPrefEdits")
        set(value) {
            BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()
                ?.putString(
                    PREF_CHAT_KEY, value
                )?.apply()
        }

    //Lưu key voice call
    var tokenVoiceCall: String
        get() = BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.getString(PREF_VOICE_KEY, "") ?: ""
        @SuppressLint("CommitPrefEdits")
        set(value) {
            BaseApplication.instance?.applicationContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()
                ?.putString(
                    PREF_VOICE_KEY, value
                )?.apply()
        }
}