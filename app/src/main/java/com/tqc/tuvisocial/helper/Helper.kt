package com.tqc.tuvisocial.helper

import OnSwipeTouchListener
import android.app.Dialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.listener.OnItemSwipeListener
import com.github.chrisbanes.photoview.PhotoView
import com.google.gson.Gson
import com.ortiz.touchview.TouchImageView
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.fcm.API
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.sendFCM
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.ui.splash_screen.SplashScreenActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.math.abs

object Helper {

    private const val SWIPE_THRESHOLD = 100
    private const val SWIPE_VELOCITY_THRESHOLD = 100

    fun checkSession() {
        if (BaseApplication.instance?.user == null) {
            userID = ""
            myInfo = null
            val intent = Intent(
                BaseApplication.instance?.applicationContext,
                SplashScreenActivity::class.java
            )
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            BaseApplication.instance?.startActivity(intent)
        }
    }

    fun saveLanguage(local: String) {
        BaseApplication.instance?.applicationContext?.getSharedPreferences("Language", MODE_PRIVATE)
            ?.edit()?.apply {
                putString("Locale", local)
                apply()
            }
    }

    fun getLanguage() =
        BaseApplication.instance?.applicationContext?.getSharedPreferences("Language", MODE_PRIVATE)
            ?.getString("Locale", "en")

    fun showImageFullScreen(
        context: Context,
        media: String,
        name: String,
        senderID: String? = null,
        listMedia: List<String>? = null
    ) {
        Dialog(context, android.R.style.Theme_Translucent_NoTitleBar).apply {
            var currentPosition = 0
            listMedia?.forEachIndexed { index, s ->
                if (s == media) {
                    currentPosition = index
                    return@forEachIndexed
                }
            }
            val view = View.inflate(context, R.layout.show_image_full_screen_layout, null)

            view?.findViewById<ImageView>(R.id.backImg)?.apply {
                setColorFilter(ContextCompat.getColor(context, R.color.white))
                setOnClick {
                    dismiss()
                }
            }
            view?.findViewById<TextView>(R.id.nameTV)?.text = name
            view?.findViewById<PhotoView>(R.id.imageMessageImg)?.let {
                Glide.with(context).load(media).placeholder(R.drawable.gif_loading).into(it)
                it.setOnSingleFlingListener { e1, e2, velocityX, _ ->
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x
                    if (abs(diffX) > abs(diffY)) {
                        if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                if (currentPosition > 0) {
                                    currentPosition--
                                    Glide.with(context).load(listMedia?.get(currentPosition))
                                        .placeholder(R.drawable.gif_loading).into(it)
                                }
                            } else {
                                if (currentPosition < listMedia!!.size - 1) {
                                        currentPosition++
                                        Glide.with(context).load(listMedia[currentPosition])
                                            .placeholder(R.drawable.gif_loading).into(it)
                                    }
                            }
                        }
                    }
                    true
                }
//                it.setOnTouchListener(object : OnSwipeTouchListener(context) {
//
//                    override fun onSwipeRight() {
//                        if (currentPosition >= 0) {
//                            currentPosition--
//                            Glide.with(context).load(listMedia?.get(currentPosition)).placeholder(R.drawable.gif_loading).into(it)
//                        }
//                    }
//
//                    override fun onSwipeLeft() {
//                        if (currentPosition < (listMedia?.size ?: 0)) {
//                            currentPosition++
//                            Glide.with(context).load(listMedia?.get(currentPosition)).placeholder(R.drawable.gif_loading).into(it)
//                        }
//                    }
//                })
            }
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setContentView(view)
            show()
        }
    }

    fun changeLanguage(context: Context) {
        val locale = if (getLanguage() == "en") Locale.ENGLISH else Locale("ru", "RU")
        val config = context.resources.configuration
        Locale.setDefault(locale)
        config.locale = locale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun sendNotifyFCM(title: String, message: String, token: String?, chatID: String, isGroup: Boolean, userModel: UserModel?) {
        val httpClient = OkHttpClient.Builder()

        val hasMap = HashMap<String, Any>()
        hasMap["to"] = "$token"
        hasMap["data"] = hashMapOf(
            "notification" to true,
            "title" to title,
            "body" to message,
            "chat" to chatID,
            "group" to isGroup,
            "user" to Gson().toJson(userModel)
            )

        httpClient.addInterceptor { chain ->
            val request: Request =
                chain.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        "key=AAAA8uyvheg:APA91bEONV0nx6h6DP1xbqVNPpFbqTaiaXzp0GasC4FL-D1K3V1aIEqs5GyuWr4v1iSKYQi1PUPVWHPLDgg7OpQdgQvxr9QcmQX36SL2RxIw1UmCtmpCw8zFc3v9mFiLpS43TYctvyzg"
                    )
                    .addHeader("Content-Type", "application/json")
                    .build()
            chain.proceed(request)
        }
        Retrofit.Builder().baseUrl("https://fcm.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build().create(API::class.java).sendData(hasMap).enqueue(
                object : Callback<Response<Any>> {
                    override fun onResponse(
                        call: retrofit2.Call<Response<Any>>,
                        response: Response<Response<Any>>
                    ) {
                        Log.d("Cuong", "SendFCM ${response.isSuccessful}")
                    }

                    override fun onFailure(call: retrofit2.Call<Response<Any>>, t: Throwable) {
                        Log.d("Cuong", "SendFCM ${t.message}")

                    }
                }
            )
    }
}