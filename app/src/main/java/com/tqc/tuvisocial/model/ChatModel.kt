package com.tqc.tuvisocial.model

data class ChatModel(
    var keyIdentity: String,
    var name: String,
    var createDate: String,
    var color: Int,
    var nickName1: String = "",
    var nickName2: String = "",
    var conversation: HashMap<String, Any>?,
    var group: Boolean ? = false,
    var groupName: String ? = "",
    var memberGroup: HashMap<String, Any>?,
    var avtGroup: String? = ""
)
