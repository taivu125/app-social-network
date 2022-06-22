package com.tqc.tuvisocial.fcm

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface API {
    @POST("fcm/send")
    fun sendData(@Body params : HashMap<String ,Any>) : Call<Response<Any>>
}