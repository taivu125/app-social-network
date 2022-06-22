package com.tqc.tuvisocial.model

data class UserModel(
    var uid: String? = "",
    var userName: String? = "",
    var fullName: String? = "",
    var relationship: String? = "",
    var cityName: String? = "",
    var education: String? = "",
    var privateType: Int? = 0,
    var friends: HashMap<String, String>?,
    var requestFriends: HashMap<String, String>?,
    var follow: MutableList<String>?,
    var follower: MutableList<String>?,
    var avtUrl: String? = "",
    var bgUrl: String? = "",
    var birthDay: String? = "",
    var privateTypePost: Int? = 0,
    var privateTypeFriends: Int? = 0,
    var privateTypeInfo: Int? = 0,
    var online: Boolean? = false,
    var postHide: HashMap<String, String>?,
    var fcmToken: String = ""
)