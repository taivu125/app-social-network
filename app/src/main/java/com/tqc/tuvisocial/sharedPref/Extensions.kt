package com.tqc.tuvisocial.sharedPref

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.tqc.tuvisocial.fcm.API
import com.tqc.tuvisocial.model.NotifyModel
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object Extensions {

    //Hàm setOnClick được tạo thêm nhầm để hạn chế việc ng dùng nhấn nhanh khiến bị duplicate
    fun View.setOnClick(onClick: (() -> Unit)? = null) {
        this.setOnClickListener {
            this.isEnabled = false
            onClick?.invoke()
            Handler(Looper.getMainLooper()).postDelayed({
                this.isEnabled = true
            }, 1000)
        }
    }

    //Kiểm tra xem file có phải là ảnh
    fun String.isImage() : Boolean {
        val arr = this.split(".")
        return arr.size > 1 && arr[1] == "jpg" || arr[1] == "jpeg" || arr[1] == "png"
    }

    fun Date.toDateTime(): String {
        //Thoi gian se quy chugn ve UTC
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS a", Locale.ENGLISH)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return simpleDateFormat.format(this)
    }

    fun String.toDateForCMT(): String {
        //Convert ve UTC
        val currentFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS a", Locale.ENGLISH)
        currentFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date: Date = currentFormat.parse(this)
        //Convert tiep ve local time
        val simpleDateFormat = SimpleDateFormat("HH:mm (dd/MM/yyy)", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        return simpleDateFormat.format(date)
    }

    fun String.toDateForChat(): String {
        val currentFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS a", Locale.ENGLISH)
        currentFormat.timeZone = TimeZone.getTimeZone("UTC")
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss", Locale.getDefault())
        simpleDateFormat.timeZone = TimeZone.getDefault()
        return simpleDateFormat.format(currentFormat.parse(this))
    }

    @SuppressLint("SimpleDateFormat")
    fun String.toTimeInMillis() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(this).time

    @SuppressLint("SimpleDateFormat")
    fun String.toTimeDashInMillis() : Long {
        //Convert ve UTC
        val currentFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS a", Locale.ENGLISH)
        currentFormat.timeZone = TimeZone.getTimeZone("UTC")
        return currentFormat.parse(this).time
    }


    fun NotifyModel.sendFCM(token: String?): NotifyModel {
        val httpClient = OkHttpClient.Builder()

        val hasMap = HashMap<String, Any>()
        hasMap["to"] = "$token"
        hasMap["data"] = hashMapOf(
            "group" to this.group,
            "postID" to this.postID,
            "share" to this.share,
            "notification" to true,
            "title" to this.name,
            "body" to this.message,
        )

        httpClient.addInterceptor { chain ->
            val request: Request =
                chain.request().newBuilder()
                    .addHeader("Authorization", "key=AAAA8uyvheg:APA91bEONV0nx6h6DP1xbqVNPpFbqTaiaXzp0GasC4FL-D1K3V1aIEqs5GyuWr4v1iSKYQi1PUPVWHPLDgg7OpQdgQvxr9QcmQX36SL2RxIw1UmCtmpCw8zFc3v9mFiLpS43TYctvyzg")
                    .addHeader("Content-Type", "application/json")
                    .build()
            chain.proceed(request)
        }
        Retrofit.Builder().
        baseUrl("https://fcm.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build().create(API::class.java).sendData(hasMap)
            .enqueue(
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
        return this
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}