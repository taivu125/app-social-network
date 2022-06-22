package com.tqc.tuvisocial.model

import com.google.firebase.database.ServerValue

data class MessageModel(
        var text: String,
        var id: String,
        var imageUrl: String,
        var date: String,
        var read: Boolean ? = false,
        var sendID: String? =  "",
        var listRead: HashMap<String, String> ? = null,
        var call: Boolean? = false,
        var image: Boolean? = false,
        var timeStamp: Any = ServerValue.TIMESTAMP
)
