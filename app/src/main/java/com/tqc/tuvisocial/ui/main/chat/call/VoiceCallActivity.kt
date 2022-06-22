package com.tqc.tuvisocial.ui.main.chat.call

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import android.media.RingtoneManager

import android.media.Ringtone
import android.net.Uri
import com.tqc.tuvisocial.model.MessageModel
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import java.util.*
import android.media.MediaPlayer
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.tqc.tuvisocial.sharedPref.SharedPref
import io.agora.rtc.models.ChannelMediaOptions


class VoiceCallActivity : AppCompatActivity() {
    //https://docs.agora.io/en/Video/landing-page
    //SDK bên thứ 3 hỗ trợ call thông qua giao thức WebRTC
    // Kotlin
    // Fill the App ID of your project generated on Agora Console.
    private val mAppID = "164ab7dd0e7a44fca26da6daeeab472f"
    // Fill the channel name.
    private val mChannel = "VoiceKey"
    // Fill the temp token generated on Agora Console.
    private var mToken = ""

    private var mRtcEngine: RtcEngine? = null
    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Log.d("Cuong", "Join Success")
        }
    }

    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    private val PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1

    private var answerBtn: ImageView? = null
    private var callTimer: TextView? = null

    private var isCalling: Boolean = false
    private var callerID = ""
    private var receiverCallID = ""
    private var messageKey = ""

    private var hour = 0L
    private var minute = 0L
    private var seconds = 0L
    private var userModel: UserModel? = null
    private var ringTone: Ringtone? = null
    private var countTime: CountDownTimer ? = null
    private var mediaPlayer: MediaPlayer ? = null
    private var countDownCalling: CountDownTimer? = null
    private val changeMeOption = ChannelMediaOptions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)
        mToken = SharedPref.tokenVoiceCall
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.voiceKeyRefer)?.get()?.addOnSuccessListener {
            if (it.value.toString() != SharedPref.tokenVoiceCall) {
                mToken = it.value.toString()
                SharedPref.tokenVoiceCall = it.value.toString()
            } else {
                mToken = SharedPref.tokenVoiceCall
            }
        }

        isCalling = intent.extras?.getBoolean("Status") ?: true
        callerID = intent.extras?.getString("CallerID") ?: ""
        receiverCallID = intent.extras?.getString("ReceiverID") ?: ""
        messageKey = intent.extras?.getString("MessageKey") ?: ""

        changeMeOption.autoSubscribeAudio = true
        changeMeOption.publishLocalAudio = true
        listenStatusVoiceCall()

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO,
                PERMISSION_REQ_ID_RECORD_AUDIO
            ) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
        ) {
            initializeAndJoinChannel()
        } else {
            Toast.makeText(this, getString(R.string.message_call), Toast.LENGTH_LONG).show()
            hangOut()
        }

        answerBtn = findViewById(R.id.anserImg)
        callTimer = findViewById(R.id.timerTV)

        //Lấy tt user
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(receiverCallID)?.addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                try {
                                    if (snapshot.value != null) {
                                        userModel = Gson().fromJson(
                                            Gson().toJson(snapshot.value),
                                            object : TypeToken<UserModel>() {}.type
                                        )
                                        if (userModel?.avtUrl?.isNotEmpty() == true) {
                                            Glide.with(this@VoiceCallActivity).load(userModel?.avtUrl)
                                                .into(findViewById<ImageView>(R.id.avtImg))
                                        } else {
                                            Glide.with(this@VoiceCallActivity).load(R.drawable.avatar)
                                                .into(findViewById<ImageView>(R.id.avtImg))
                                        }
                                        findViewById<TextView>(R.id.nameTV).text = userModel?.fullName

                                        if (isCalling) {
                                            if (userModel?.online == false) {
                                                audioPlayer(R.raw.khong_lien_lac_duoc)
                                                Handler(mainLooper).postDelayed({
                                                    mediaPlayer?.stop()
                                                    finish()
                                                }, 6000)
                                            } else {
                                                audioPlayer(R.raw.nhac_cho_dt)
                                                countDownCalling = object : CountDownTimer(30000, 1000) {
                                                    override fun onTick(p0: Long) {

                                                    }

                                                    override fun onFinish() {
                                                        mediaPlayer?.stop()
                                                        finish()
                                                    }

                                                }
                                            }
                                        }
                                    }
                                }catch (ex: java.lang.Exception) {}
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        }
                )

        if (isCalling) {
            // Join the channel with a token.
            mRtcEngine!!.joinChannel(mToken, mChannel, "",0, changeMeOption)
            answerBtn?.visibility = View.GONE
            findViewById<ImageView>(R.id.hangoutImg).setOnClickListener {
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.callRefer)?.child(callerID)?.updateChildren(
                        mapOf(
                                "HangOut" to true
                        )
                )
            }
        } else {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringTone = RingtoneManager.getRingtone(applicationContext, notification)
            ringTone?.play()
            answerBtn?.visibility = View.VISIBLE
            //Lấy tt user
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(callerID)?.addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value != null) {
                            val user: UserModel = Gson().fromJson(
                                Gson().toJson(snapshot.value),
                                object : TypeToken<UserModel>() {}.type
                            )
                            if (user.avtUrl?.isNotEmpty() == true) {
                                Glide.with(this@VoiceCallActivity).load(user.avtUrl)
                                    .into(findViewById<ImageView>(R.id.avtImg))
                            } else {
                                Glide.with(this@VoiceCallActivity).load(R.drawable.avatar)
                                    .into(findViewById<ImageView>(R.id.avtImg))
                            }
                            findViewById<TextView>(R.id.nameTV).text = user.fullName
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
            )
            answerBtn?.setOnClickListener {
                // Join the channel with a token.
                ringTone?.stop()
                answerBtn?.visibility = View.GONE
                mRtcEngine!!.joinChannel(mToken, mChannel, "",0, changeMeOption)
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.callRefer)?.child(callerID)?.updateChildren(
                        mapOf(
                                "HangOut" to false
                        )
                )
            }
            findViewById<ImageView>(R.id.backImg).setOnClick {
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.callRefer)?.child(callerID)?.updateChildren(
                        mapOf(
                                "HangOut" to true
                        )
                )
            }
        }

        findViewById<ImageView>(R.id.hangoutImg).setOnClick {
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.callRefer)?.child(callerID)?.updateChildren(
                    mapOf(
                            "HangOut" to true
                    )
            )
        }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                requestCode
            )
            return false
        }
        return true
    }

    private fun initializeAndJoinChannel() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, mAppID, mRtcEventHandler)
            mRtcEngine?.enableAudio()
        } catch (e: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        if (ringTone?.isPlaying == true) {
            ringTone?.stop()
        }
        if (SharedPref.myInfo?.uid?.isNotEmpty() == true) {
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(SharedPref.myInfo?.uid ?: "")?.child("online")?.setValue(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine?.leaveChannel()
        countTime?.cancel()
        mediaPlayer?.stop()
        RtcEngine.destroy()
    }

    private fun countCallTimer() {
        countTime = object: CountDownTimer(3600000 * 99L, 1000) {
            override fun onFinish() {
            }

            @SuppressLint("SetTextI18n")
            override fun onTick(p0: Long) {
                seconds += 1
                if (seconds == 60L) {
                    seconds = 0
                    minute+= 1
                }
                if (minute == 60L) {
                    minute = 0
                    hour += 1
                }
                runOnUiThread {
                    callTimer?.text = "${if (hour < 10) "0$hour" else hour}:${if (minute < 10) "0$minute" else minute}:${if (seconds < 10) "0$seconds" else seconds}"
                }
            }

        }.start()
    }

    private fun listenStatusVoiceCall() {
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.callRefer)?.child(callerID)?.addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value != null) {
                            val value = snapshot.value as HashMap<*, *>
                            if (value["HangOut"] == false) {
                                hangUp()
                            }
                            if (value["HangOut"] == true) {
                                hangOut()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
        )
    }

    private fun hangOut() {
        //Gửi tin nhắn đi khi kết thúc cuộc gọi, người gọi gửi là đủ
        if (callerID == userID) {
            BaseApplication.instance?.dataBase?.run {
                getReference(ConstantKey.chatsRefer).child(messageKey).child(ConstantKey.conversion)
                    .push().run {
                        setValue(
                            MessageModel(
                                if (countTime == null) "Missed call" else "Voice called ${callTimer?.text.toString()}",
                                this.key ?: "",
                                "",
                                Calendar.getInstance().time.toDateTime(),
                                read = false,
                                callerID,
                                call = true
                            )
                        )
                    }
            }
        }
        mRtcEngine?.leaveChannel()
        finish()
    }

    private fun hangUp() {
        if (isCalling) {
            countDownCalling?.cancel()
        }
        countCallTimer()
        mediaPlayer?.stop()
    }

    fun audioPlayer(resource: Int) {
        //set up MediaPlayer
        mediaPlayer = MediaPlayer.create(this, resource)
        try {
            mediaPlayer?.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (SharedPref.myInfo?.uid?.isNotEmpty() == true) {
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(SharedPref.myInfo?.uid ?: "")?.child("online")?.setValue(true)
        }
    }
}