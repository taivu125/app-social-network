package com.tqc.tuvisocial.model

class NotifyModel(
    var userID: String,
    var name: String,
    var message: String,
    var read: Boolean = false,
    var type: Int = -1,
    var createDate: String = "",
    var id: String = "",
    var postID: String  = "",
    var share: Boolean = false,
    var chatKey: String = "null",
    var group: Boolean = false
)