package com.tqc.tuvisocial.model

import com.chad.library.adapter.base.entity.MultiItemEntity
import com.google.gson.internal.LinkedTreeMap

data class PostModel(
    var id: String = "",
    var description: String? = "",
    var type: Int? = 0,
    var like: LinkedTreeMap<String, Any>? = null,
    var comment: LinkedTreeMap<String, Any>? = null,
    var postShare: PostModel?,
    var galleryUUID: String? = "",
    var createDate: String? = "",
    var avtUrl: String? = "",
    var fullName: String? = "",
    var userID: String? = "",
    var updateAvt: Boolean? = false,
    var updateBG: Boolean? = false,
    var bgUrl: String = "",
    var hideList: LinkedTreeMap<String, *>? = null,
    var isHaveCmt: Boolean = false,
    var typeDescription: String = ""

): MultiItemEntity {
    override val itemType: Int
        get() = if (postShare == null) POST else SHARE

    companion object {
        const val POST = 1
        const val SHARE = 2
    }
}
