package com.tqc.tuvisocial.helper

import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.model.localModel.ColorModel

object ConstantKey {
    const val usersRefer = "Users"
    const val postRefer = "Pots"
    const val chatsRefer = "Chats"
    const val mediaRefer = "Media"
    const val requestFriends = "requestFriends"
    const val voiceKeyRefer = "VoiceKey"

    const val conversion = "conversation"
    const val notify = "Notify"
    const val callRefer = "Call"
    const val videoRefer = "Video"
    const val imgRef = "Image"

    const val isPublic = 0
    const val isFriend = 1
    const val isOnlyMe = 2

    const val isRequestFriend = 0
    const val isLike = 1
    const val isCmt = 2
    const val isShare = 3

    val listColor = arrayListOf(
        ColorModel(false, R.color.grey),
        ColorModel(false, R.color.orange),
        ColorModel(false, R.color.purple),
        ColorModel(false, R.color.pink),
        ColorModel(false, R.color.green),
        ColorModel(false, R.color.red),
        ColorModel(false, R.color.yellow),
        ColorModel(false, R.color.blue),
        ColorModel(false, android.R.color.holo_blue_light),
        ColorModel(false, android.R.color.holo_green_light),
    )
}