package com.tqc.tuvisocial.ui.main.profile.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.shared.DialogHelper
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.CommentModel
import com.tqc.tuvisocial.model.NotifyModel
import com.tqc.tuvisocial.model.PostModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.hideKeyboard
import com.tqc.tuvisocial.sharedPref.Extensions.sendFCM
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateForCMT
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeDashInMillis
import com.tqc.tuvisocial.sharedPref.SharedPref
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.OnRefreshProfile
import com.tqc.tuvisocial.ui.main.home.list.adapter.CommentAdapter
import com.tqc.tuvisocial.ui.main.home.list.adapter.PostAdapter
import com.tqc.tuvisocial.ui.main.home.list.adapter.PostImageAdapter
import com.tqc.tuvisocial.ui.main.other.VideoFullScreenActivity
import com.tqc.tuvisocial.ui.main.profile.ProfileFragment
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import java.util.*

class ProfilePostAdapter(private val onClick: ((id: Int, item: PostModel) -> Unit)? = null) :
    BaseMultiItemQuickAdapter<PostModel, BaseViewHolder>() {
    private var userModel: UserModel? = null

    init {
        addItemType(PostModel.POST, R.layout.item_post_layout)
        addItemType(PostModel.SHARE, R.layout.item_post_share_layout)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUser(userModel: UserModel?) {
        this.userModel = userModel
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addNewData(data: MutableList<PostModel>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: PostModel) {
        if (holder.itemViewType == PostModel.POST) {
            setUpPost(holder, item)
        } else {
            setUpPostShare(holder, item)
        }
    }

    private fun setUpPost(holder: BaseViewHolder, item: PostModel) {
        //Truy vấn tới cây User và lấy profile của user
        holder.setText(R.id.typeDescription, item.typeDescription)
        var user: UserModel? = null
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
            ?.child(item.userID!!)
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Convert json model thành UserModel
                    if (snapshot.value != null) {
                        user = Gson().fromJson(
                            Gson().toJson(snapshot.value),
                            object : TypeToken<UserModel>() {}.type
                        )

                        //Xử lý rule private theo bạn
                        if (user?.privateTypePost != ConstantKey.isPublic) {
                            when {
                                user?.privateTypePost == ConstantKey.isOnlyMe && item.userID != myInfo?.uid -> {
                                    remove(item)
                                }
                                user?.friends?.containsValue(myInfo?.uid) == false && item.userID != myInfo?.uid -> {
                                    remove(item)
                                }
                            }
                        }
                        //Xử lý post bị hide
                        if (myInfo?.postHide?.values?.find { it == item.id } != null) {
                            remove(item)
                        }

                        val avtImg = holder.getView<ImageView>(R.id.avtImg)
                        holder.getView<ImageView>(R.id.avtImg).setOnClick {
                            if (user?.uid != SharedPref.userID) {
                                onClick?.invoke(PostAdapter.avtKey, item)
                            }
                        }
                        holder.setText(R.id.nameTV, user?.fullName)
                        holder.setText(R.id.dateTV, item.createDate?.toDateForCMT())
                        if (user?.avtUrl?.isNotEmpty() == true) {
                            Glide.with(holder.itemView.context).load(user?.avtUrl)
                                .placeholder(R.drawable.gif_loading).into(avtImg)
                        } else {
                            Glide.with(holder.itemView.context).load(R.drawable.avatar)
                                .into(avtImg)
                        }
                        holder.setText(R.id.captionTV, item.description)
                        holder.setText(
                            R.id.likeNumberTV,
                            "${item.like?.size ?: 0} ${context.getString(R.string.like)}"
                        )
                        holder.setText(
                            R.id.cmtNumberTV,
                            "${item.comment?.size ?: 0} ${context.getString(R.string.cmt)}"
                        )
                        holder.setTextColor(
                            R.id.likeNumberTV,
                            if ((item.like?.size ?: 0) > 0)
                                ContextCompat.getColor(context, R.color.black)
                            else
                                ContextCompat.getColor(context, R.color.grey)
                        )
                        holder.setTextColor(
                            R.id.cmtNumberTV,
                            if ((item.comment?.size ?: 0) > 0)
                                ContextCompat.getColor(context, R.color.black)
                            else
                                ContextCompat.getColor(context, R.color.grey)
                        )
                        //Kiểm tra nếu đã like thì icon chuyển màu xanh
                        if (item.like != null) {
                            for (value in item.like as LinkedTreeMap<*, *>) {
                                if ((value.value as LinkedTreeMap<*, *>)["userID"] == myInfo?.uid) {
                                    holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.blue
                                        )
                                    )
                                    holder.setTextColor(
                                        R.id.likeTV,
                                        ContextCompat.getColor(context, R.color.blue)
                                    )
                                    holder.setTextColor(
                                        R.id.likeNumberTV,
                                        if ((item.like?.size ?: 0) > 0)
                                            ContextCompat.getColor(context, R.color.black)
                                        else
                                            ContextCompat.getColor(context, R.color.grey)
                                    )
                                    break
                                } else {
                                    holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                        ContextCompat.getColor(context, R.color.black)
                                    )
                                    holder.setTextColor(
                                        R.id.likeTV,
                                        ContextCompat.getColor(context, R.color.black)
                                    )
                                }
                            }
                        } else {
                            holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                ContextCompat.getColor(context, R.color.black)
                            )
                            holder.getView<TextView>(R.id.likeTV).setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.black
                                )
                            )
                        }
                        holder.getView<LinearLayout>(R.id.likeLayout).setOnClick {
                            //Kiểm tra tinh trạng đã like hay chưa
                            var statusLike = false
                            var idLike = ""
                            if (item.like != null) {
                                for (value in item.like as LinkedTreeMap<*, *>) {
                                    val likeValue = try {
                                        value.value as HashMap<*, *>
                                    } catch (ex: Exception) {
                                        value.value as LinkedTreeMap<*, *>
                                    }
                                    if (likeValue["userID"] == myInfo?.uid) {
                                        statusLike = true
                                        idLike = likeValue["id"].toString()
                                        holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                            ContextCompat.getColor(context, R.color.blue)
                                        )
                                        holder.setTextColor(
                                            R.id.likeTV,
                                            ContextCompat.getColor(context, R.color.blue)
                                        )
                                        holder.setTextColor(
                                            R.id.likeNumberTV,
                                            if ((item.like?.size ?: 0) > 0)
                                                ContextCompat.getColor(context, R.color.black)
                                            else
                                                ContextCompat.getColor(context, R.color.grey)
                                        )
                                        break
                                    }
                                }
                            }
                            BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)
                                ?.child(item.userID!!)
                                ?.child(item.id)?.child("like")?.run {
                                    if (!statusLike) {
                                        val key = push().key!!
                                        val value = hashMapOf(
                                            "id" to key,
                                            "userID" to myInfo?.uid
                                        )
                                        child(key).setValue(value)
                                        if (item.userID != SharedPref.userID) {
                                            BaseApplication.instance?.dataBase?.getReference(
                                                ConstantKey.notify
                                            )
                                                ?.child(item.userID!!)?.run {
                                                    val key = push().key!!
                                                    val notify = NotifyModel(
                                                        myInfo?.uid!!,
                                                        myInfo?.fullName!!,
                                                        context.getString(R.string.liked_ur_post),
                                                        false,
                                                        ConstantKey.isLike,
                                                        Calendar.getInstance().time.toDateTime(),
                                                        key,
                                                        item.id,
                                                        item.postShare != null
                                                    )
                                                    child(key).setValue(
                                                        notify
                                                    )
                                                    notify.sendFCM(user?.fcmToken)
                                                }
                                        }

                                        //Kiểm tra null để khởi tạo, khi nhấn like/unlike sẽ tăng giá trị like lên mà ko cần refresh adapter, sẽ làm cho
                                        // app trong có vẻ mượt mà hơn
                                        if (item.like == null) {
                                            item.like = LinkedTreeMap()
                                        }
                                        item.like?.put(key, value)
                                        holder.setText(
                                            R.id.likeNumberTV,
                                            "${item.like?.values?.size ?: 0} ${context.getString(R.string.like)}"
                                        )
                                        holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                            ContextCompat.getColor(context, R.color.blue)
                                        )
                                        holder.setTextColor(
                                            R.id.likeTV,
                                            ContextCompat.getColor(context, R.color.blue)
                                        )
                                        holder.setTextColor(
                                            R.id.likeNumberTV,
                                            if ((item.like?.size ?: 0) > 0)
                                                ContextCompat.getColor(context, R.color.black)
                                            else
                                                ContextCompat.getColor(context, R.color.grey)
                                        )
                                    } else {
                                        child(idLike).removeValue()
                                        if (item.like != null) {
                                            //Chỗ này có lúc sẽ nhận type HashMap lúc type là LinkedTreeMap nên phải bắt ClassCastException để lấy dữ liệu
                                            for (value in item.like as LinkedTreeMap<*, *>) {
                                                val likeValue = try {
                                                    value.value as HashMap<*, *>
                                                } catch (ex: ClassCastException) {
                                                    value.value as LinkedTreeMap<*, *>
                                                }
                                                if (likeValue["userID"] == myInfo?.uid) {
                                                    item.like?.remove(value.key)
                                                    holder.setText(
                                                        R.id.likeNumberTV,
                                                        "${item.like?.values?.size ?: 0} ${
                                                            context.getString(
                                                                R.string.like
                                                            )
                                                        }"
                                                    )
                                                    holder.getView<ImageView>(R.id.likeImg)
                                                        .setColorFilter(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.black
                                                            )
                                                        )
                                                    holder.setTextColor(
                                                        R.id.likeTV,
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.black
                                                        )
                                                    )
                                                    holder.setTextColor(
                                                        R.id.likeNumberTV,
                                                        if ((item.like?.size ?: 0) > 0)
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.black
                                                            )
                                                        else
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.grey
                                                            )
                                                    )
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                        }

                        //Xử lý comment
                        setUpComment(holder, item, user)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        //setUpImage
        //Truy vấn lấy toàn bộ hình ảnh của tài khoản
        holder.getView<View>(R.id.lineStartMedia).visibility = View.GONE
        holder.getView<View>(R.id.lineEndMedia).visibility = View.GONE
        if (item.userID?.isNotEmpty() == true) {
            val videoLayout = holder.getView<FrameLayout>(R.id.videoLayout)
            videoLayout.visibility = View.GONE

            BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.imgRef}")
                ?.listAll()?.addOnSuccessListener {
                    //Tiếp tục truy cập vào các post đã tạo để lấy hình ảnh
                    if (it.items.isNotEmpty()) {
                        holder.getView<View>(R.id.lineStartMedia).visibility = View.VISIBLE
                        holder.getView<View>(R.id.lineEndMedia).visibility = View.VISIBLE
                    } else {
                        holder.getView<View>(R.id.lineStartMedia).visibility = View.GONE
                        holder.getView<View>(R.id.lineEndMedia).visibility = View.GONE
                    }
                    val rev = holder.getView<RecyclerView>(R.id.reView)
                    val adapter = PostImageAdapter()
                    adapter.size = it.items.size
                    if (it.items.size > 2) {
                        rev.layoutManager = GridLayoutManager(context, 2)
                    } else {
                        rev.layoutManager = GridLayoutManager(context, 1)
                    }
                    rev.adapter = adapter

                    it.items.forEach { downLink ->
                        downLink.downloadUrl.addOnSuccessListener { uri ->
                            adapter.addData("$uri")
                        }
                    }
                    when {
                        item.updateAvt == true -> {
                            adapter.addData(item.avtUrl!!)
                        }
                        item.updateBG == true -> {
                            adapter.addData(item.avtUrl!!)
                        }
                    }
                }

            //Load video
            BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.videoRefer}")
                ?.listAll()?.addOnSuccessListener {
                    it.items.forEach { downLink ->
                        downLink.downloadUrl.addOnSuccessListener { uri ->
                            videoLayout.visibility = View.VISIBLE
                            val player = ExoPlayer.Builder(context).build()
                            holder.getView<PlayerView>(R.id.videoView).apply {
                                setOnClick {
                                    val intent =
                                        Intent(context, VideoFullScreenActivity::class.java)
                                    intent.putExtra("Video", uri.toString())
                                    intent.putExtra("Name", "")
                                    context?.startActivity(intent)
                                }
                                setPlayer(player)
                                setShowNextButton(false)
                                setShowPreviousButton(false)
                                setShowFastForwardButton(false)
                                setShowRewindButton(false)
                                setShowMultiWindowTimeBar(false)
                                player.setMediaItem(MediaItem.fromUri(uri))
                                player.prepare()
                                player.play()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    player.pause()
                                }, 1000)
                            }
                        }
                    }
                }
        }
        //Xử lý nút more
        val moreView = holder.getView<ImageView>(R.id.moreImg)
        moreView.setOnClick {
            deletePost(item, holder, moreView)
        }
        //Xử lý nút share
        holder.getView<LinearLayout>(R.id.shareLayout).setOnClick {
            sharePost(item, user?.fcmToken)
        }
    }

    private fun setUpPostShare(holder: BaseViewHolder, post: PostModel) {
        //Hiển thị post
        holder.setText(R.id.typeDescription, post.typeDescription)
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
            ?.child(post.userID!!)?.get()?.addOnSuccessListener {
                val userPost: UserModel? = Gson().fromJson(
                    Gson().toJson(it.value),
                    object : TypeToken<UserModel>() {}.type
                )
                //Xử lý post bị hide
                if (myInfo?.postHide?.values?.find { value -> value == post.id } != null) {
                    removeAt(holder.adapterPosition)
                }

                val avtImg = holder.getView<ImageView>(R.id.avtShareImg)
                avtImg.setOnClick {
                    if (userPost?.uid != SharedPref.userID) {
                        onClick?.invoke(PostAdapter.avtKey, post)
                    }
                }
                holder.setText(R.id.nameShareTV, userPost?.fullName)
                holder.setText(R.id.dateShareTV, post.createDate?.toDateForCMT())
                if (post.avtUrl?.isNotEmpty() == true) {
                    Glide.with(holder.itemView.context).load(userPost?.avtUrl)
                        .placeholder(R.drawable.gif_loading).into(avtImg)
                } else {
                    Glide.with(holder.itemView.context).load(R.drawable.avatar)
                        .into(avtImg)
                }
                holder.setText(
                    R.id.likeNumberTV,
                    "${post.like?.size ?: 0} ${context.getString(R.string.like)}"
                )
                holder.setText(
                    R.id.cmtNumberTV,
                    "${post.comment?.size ?: 0} ${context.getString(R.string.cmt)}"
                )
                holder.setTextColor(
                    R.id.likeNumberTV,
                    if ((post.like?.size ?: 0) > 0)
                        ContextCompat.getColor(context, R.color.black)
                    else
                        ContextCompat.getColor(context, R.color.grey)
                )
                holder.setTextColor(
                    R.id.cmtNumberTV,
                    if ((post.comment?.size ?: 0) > 0)
                        ContextCompat.getColor(context, R.color.black)
                    else
                        ContextCompat.getColor(context, R.color.grey)
                )
                //Kiểm tra nếu đã like thì icon chuyển màu xanh
                if (post.like != null) {
                    for (value in post.like as LinkedTreeMap<*, *>) {
                        if ((value.value as LinkedTreeMap<*, *>)["userID"] == myInfo?.uid) {
                            holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                ContextCompat.getColor(
                                    context,
                                    R.color.blue
                                )
                            )
                            holder.getView<TextView>(R.id.likeTV).setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.blue
                                )
                            )
                            holder.setTextColor(
                                R.id.likeNumberTV,
                                if ((post.like?.size ?: 0) > 0)
                                    ContextCompat.getColor(context, R.color.black)
                                else
                                    ContextCompat.getColor(context, R.color.grey)
                            )
                            break
                        } else {
                            holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                ContextCompat.getColor(context, R.color.black)
                            )
                            holder.setTextColor(
                                R.id.likeTV,
                                ContextCompat.getColor(context, R.color.black)
                            )
                        }
                    }
                } else {
                    holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                        ContextCompat.getColor(context, R.color.black)
                    )
                    holder.setTextColor(
                        R.id.likeTV,
                        ContextCompat.getColor(context, R.color.black)
                    )
                    holder.setTextColor(
                        R.id.likeNumberTV,
                        if ((post.like?.size ?: 0) > 0)
                            ContextCompat.getColor(context, R.color.black)
                        else
                            ContextCompat.getColor(context, R.color.grey)
                    )
                }
                holder.getView<LinearLayout>(R.id.likeLayout).setOnClick {
                    //Kiểm tra tinh trạng đã like hay chưa
                    var statusLike = false
                    var idLike = ""
                    if (post.like != null) {
                        for (value in post.like as LinkedTreeMap<*, *>) {
                            val likeValue = try {
                                value.value as HashMap<*, *>
                            } catch (ex: Exception) {
                                value.value as LinkedTreeMap<*, *>
                            }
                            if (likeValue["userID"] == myInfo?.uid) {
                                statusLike = true
                                idLike = likeValue["id"].toString()
                                holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                    ContextCompat.getColor(context, R.color.blue)
                                )
                                holder.setTextColor(
                                    R.id.likeTV,
                                    ContextCompat.getColor(context, R.color.blue)
                                )
                                holder.setTextColor(
                                    R.id.likeNumberTV,
                                    if ((post.like?.size ?: 0) > 0)
                                        ContextCompat.getColor(context, R.color.black)
                                    else
                                        ContextCompat.getColor(context, R.color.grey)
                                )
                                break
                            }
                        }
                    }
                    BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)
                        ?.child(post.userID!!)
                        ?.child(post.id)?.child("like")?.run {
                            if (!statusLike) {
                                val key = push().key!!
                                val value = hashMapOf(
                                    "id" to key,
                                    "userID" to myInfo?.uid
                                )
                                child(key).setValue(value)
                                if (post.userID != SharedPref.userID) {
                                    BaseApplication.instance?.dataBase?.getReference(
                                        ConstantKey.notify
                                    )
                                        ?.child(post.userID!!)?.run {
                                            val key = push().key!!
                                            val notify = NotifyModel(
                                                myInfo?.uid!!,
                                                myInfo?.fullName!!,
                                                context.getString(R.string.liked_ur_post),
                                                false,
                                                ConstantKey.isLike,
                                                Calendar.getInstance().time.toDateTime(),
                                                key,
                                                post.id,
                                                post.postShare != null
                                            )
                                            child(key).setValue(
                                                notify
                                            )
                                            notify.sendFCM(userPost?.fcmToken)
                                        }
                                }

                                //Kiểm tra null để khởi tạo, khi nhấn like/unlike sẽ tăng giá trị like lên mà ko cần refresh adapter, sẽ làm cho
                                // app trong có vẻ mượt mà hơn
                                if (post.like == null) {
                                    post.like = LinkedTreeMap()
                                }
                                post.like?.put(key, value)
                                holder.setText(
                                    R.id.likeNumberTV,
                                    "${post.like?.values?.size ?: 0} ${
                                        context.getString(R.string.like)
                                    }"
                                )
                                holder.getView<ImageView>(R.id.likeImg).setColorFilter(
                                    ContextCompat.getColor(context, R.color.blue)
                                )
                                holder.setTextColor(
                                    R.id.likeTV,
                                    ContextCompat.getColor(context, R.color.blue)
                                )
                                holder.setTextColor(
                                    R.id.likeNumberTV,
                                    if ((post.like?.size ?: 0) > 0)
                                        ContextCompat.getColor(context, R.color.black)
                                    else
                                        ContextCompat.getColor(context, R.color.grey)
                                )
                            } else {
                                child(idLike).removeValue()
                                if (post.like != null) {
                                    //Chỗ này có lúc sẽ nhận type HashMap lúc type là LinkedTreeMap nên phải bắt ClassCastException để lấy dữ liệu
                                    for (value in post.like as LinkedTreeMap<*, *>) {
                                        val likeValue = try {
                                            value.value as HashMap<*, *>
                                        } catch (ex: ClassCastException) {
                                            value.value as LinkedTreeMap<*, *>
                                        }
                                        if (likeValue["userID"] == myInfo?.uid) {
                                            post.like?.remove(value.key)
                                            holder.setText(
                                                R.id.likeNumberTV,
                                                "${
                                                    (post.like?.values?.size
                                                        ?: 0)
                                                } ${context.getString(R.string.like)}"
                                            )
                                            holder.getView<ImageView>(R.id.likeImg)
                                                .setColorFilter(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.black
                                                    )
                                                )
                                            holder.setTextColor(
                                                R.id.likeTV,
                                                ContextCompat.getColor(context, R.color.black)
                                            )
                                            holder.setTextColor(
                                                R.id.likeNumberTV,
                                                if ((post.like?.size ?: 0) > 0)
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.black
                                                    )
                                                else
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.grey
                                                    )
                                            )
                                            break
                                        }
                                    }
                                }
                            }
                        }
                }

                //Xử lý comment
                setUpComment(holder, post, userPost)
            }
        //Hiển thị post shared
        post.postShare?.let { item ->
            //Truy vấn tới cây User và lấy profile của user
            var user: UserModel? = null
            val videoLayout = holder.getView<FrameLayout>(R.id.videoLayout)
            videoLayout.visibility = View.GONE

            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(item.userID!!)
                ?.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //Convert json model thành UserModel
                        if (snapshot.value != null) {
                            user = Gson().fromJson(
                                Gson().toJson(snapshot.value),
                                object : TypeToken<UserModel>() {}.type
                            )

                            val avtImg = holder.getView<ImageView>(R.id.avtImg)
                            holder.getView<ImageView>(R.id.avtImg).setOnClick {
                                if (user?.uid != SharedPref.userID) {
                                    onClick?.invoke(PostAdapter.avtKey, item)
                                }
                            }
                            holder.setText(R.id.nameTV, user?.fullName)
                            holder.setText(R.id.dateTV, item.createDate?.toDateForCMT())
                            if (user?.avtUrl?.isNotEmpty() == true) {
                                Glide.with(holder.itemView.context).load(user?.avtUrl)
                                    .placeholder(R.drawable.gif_loading).into(avtImg)
                            } else {
                                Glide.with(holder.itemView.context).load(R.drawable.avatar)
                                    .into(avtImg)
                            }
                            holder.setText(R.id.captionTV, item.description)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })
            //setUpImage
            //Truy vấn lấy toàn bộ hình ảnh của post
            holder.getView<View>(R.id.lineStartMedia).visibility = View.GONE
            holder.getView<View>(R.id.lineEndMedia).visibility = View.GONE
            if (item.userID?.isNotEmpty() == true) {
                BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.imgRef}")
                    ?.listAll()?.addOnSuccessListener {
                        //Tiếp tục truy cập vào các post đã tạo để lấy hình ảnh
                        val rev = holder.getView<RecyclerView>(R.id.reView)
                        val adapter = PostImageAdapter()
                        adapter.size = it.items.size
                        if (it.items.isNotEmpty()) {
                            holder.getView<View>(R.id.lineStartMedia).visibility = View.VISIBLE
                            holder.getView<View>(R.id.lineEndMedia).visibility = View.VISIBLE
                        } else {
                            holder.getView<View>(R.id.lineStartMedia).visibility = View.GONE
                            holder.getView<View>(R.id.lineEndMedia).visibility = View.GONE
                        }

                        if (it.items.size > 2) {
                            rev.layoutManager = GridLayoutManager(context, 2)
                        } else {
                            rev.layoutManager = GridLayoutManager(context, 1)
                        }
                        rev.adapter = adapter

                        it.items.forEach { downLink ->
                            downLink.downloadUrl.addOnSuccessListener { uri ->
                                adapter.addData("$uri")
                            }
                        }
                        when {
                            item.updateAvt == true -> {
                                adapter.addData(item.avtUrl!!)
                            }
                            item.updateBG == true -> {
                                adapter.addData(item.avtUrl!!)
                            }
                        }
                    }

                //Load video
                BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.videoRefer}")
                    ?.listAll()?.addOnSuccessListener {
                        it.items.forEach { downLink ->
                            downLink.downloadUrl.addOnSuccessListener { uri ->
                                videoLayout.visibility = View.VISIBLE
                                val player = ExoPlayer.Builder(context).build()
                                holder.getView<PlayerView>(R.id.videoView).apply {
                                    setOnClick {
                                        val intent =
                                            Intent(context, VideoFullScreenActivity::class.java)
                                        intent.putExtra("Video", uri.toString())
                                        intent.putExtra("Name", "")
                                        context?.startActivity(intent)
                                    }
                                    setPlayer(player)
                                    setShowNextButton(false)
                                    setShowPreviousButton(false)
                                    setShowFastForwardButton(false)
                                    setShowRewindButton(false)
                                    setShowMultiWindowTimeBar(false)
                                    player.setMediaItem(MediaItem.fromUri(uri))
                                    player.prepare()
                                    player.play()
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        player.pause()
                                    }, 1000)
                                }
                            }
                        }
                    }
            }
            //Xử lý nút more
            val moreView = holder.getView<ImageView>(R.id.moreImg)
            moreView.setOnClick {
                deletePost(post, holder, moreView)
            }
            //Xử lý nút share
            holder.getView<LinearLayout>(R.id.shareLayout).setOnClick {
                sharePost(item, user?.fcmToken)
            }
        }
    }

    private fun setUpComment(holder: BaseViewHolder, item: PostModel, userModel: UserModel?) {
        val cmtRcv = holder.getView<RecyclerView>(R.id.cmtRcv)
        val layoutItemCmt = holder.getView<LinearLayout>(R.id.layoutItemCmt)
        val cmtEdt = holder.getView<EmojiconEditText>(R.id.cmtEdt)
        val sendImg = holder.getView<ImageView>(R.id.sendCmtImg)
        val lineView = holder.getView<View>(R.id.lineView)

        val adapter = CommentAdapter()

        if (!item.isHaveCmt) {
            cmtRcv.visibility = View.GONE
            layoutItemCmt.visibility = View.GONE
            lineView.visibility = View.GONE
            item.isHaveCmt = false
        } else {
            layoutItemCmt.visibility = View.VISIBLE
            lineView.visibility = View.VISIBLE
            item.isHaveCmt = true
            if (item.comment == null) {
                cmtRcv.visibility = View.GONE
            } else {
                cmtRcv.visibility = View.VISIBLE
            }
        }

        holder.getView<LinearLayout>(R.id.cmtLayout).setOnClick {
            if (cmtRcv.visibility == View.VISIBLE) {
                cmtRcv.visibility = View.GONE
                layoutItemCmt.visibility = View.GONE
                lineView.visibility = View.GONE
                item.isHaveCmt = false
            } else {
                layoutItemCmt.visibility = View.VISIBLE
                lineView.visibility = View.VISIBLE
                item.isHaveCmt = true
                if (item.comment == null) {
                    cmtRcv.visibility = View.GONE
                } else {
                    cmtRcv.visibility = View.VISIBLE
                }
            }

            cmtEdt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    if (p0 != null && p0.isNotEmpty()) {
                        sendImg.visibility = View.VISIBLE
                    } else {
                        sendImg.visibility = View.GONE
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                }

            })

            //Hiển thị danh sách comment
            //Lấy thông tin user cmt
            if (item.comment != null) {
                cmtRcv.layoutManager = LinearLayoutManager(context)
                cmtRcv.adapter = adapter
                if (item.comment != null) {
                    val list = arrayListOf<CommentModel>()
                    item.comment?.values?.forEach {
                        list.add(
                            Gson().fromJson(
                                Gson().toJson(it),
                                object : TypeToken<CommentModel>() {}.type
                            )
                        )
                    }
                    list.sortBy { it.createDate.toTimeDashInMillis() }
                    adapter.setNewInstance(list)
                }
            }
        }
        sendImg.setOnClick {
            context.hideKeyboard(holder.itemView)
            if (item.comment == null) {
                item.comment = LinkedTreeMap()
            }
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)
                ?.child(item.userID!!)?.child(item.id)?.child("comment")?.run {
                    val key = push().key!!
                    val value = CommentModel(
                        holder.getView<EditText>(R.id.cmtEdt).text.toString(),
                        myInfo?.uid!!,
                        Calendar.getInstance().time.toDateTime()
                    )
                    child(key).setValue(value).addOnSuccessListener {
                        cmtRcv.visibility = View.VISIBLE
                        item.comment?.put(key, value)
                        adapter.addData(value)
                        holder.setText(
                            R.id.cmtNumberTV,
                            "${adapter.data.size} ${context.getString(R.string.cmt)}"
                        )
                        holder.setTextColor(
                            R.id.cmtNumberTV,
                            if (adapter.data.size > 0)
                                ContextCompat.getColor(context, R.color.black)
                            else
                                ContextCompat.getColor(context, R.color.grey)
                        )
                    }
                }
            if (item.userID != SharedPref.userID) {
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.notify)
                    ?.child(item.userID!!)?.run {
                        val key = push().key!!
                        val notify = NotifyModel(
                            myInfo?.uid!!,
                            myInfo?.fullName!!,
                            context.getString(R.string.comment_ur_post),
                            false,
                            ConstantKey.isCmt,
                            Calendar.getInstance().time.toDateTime(),
                            key,
                            item.id,
                            item.postShare != null
                        )
                        child(key).setValue(
                            notify
                        )
                        notify.sendFCM(userModel?.fcmToken)
                    }
            }
            holder.getView<EditText>(R.id.cmtEdt).setText("")
        }
        EmojIconActions(
            context, holder.itemView, cmtEdt, holder.getView(R.id.emojiImg),
            "#495C66", "#DCE1E2", "#E6EBEF"
        ).ShowEmojIcon()
    }

    private fun sharePost(item: PostModel, fcmToken: String?) {
        BaseApplication.instance?.run {
            dataBase?.getReference(ConstantKey.postRefer)?.child(SharedPref.userID!!)?.run {
                val key = push().key!!
                child(key).setValue(
                    PostModel(
                        key,
                        "",
                        myInfo?.privateTypePost,
                        null,
                        null,
                        item,
                        "",
                        Calendar.getInstance().time.toDateTime(),
                        myInfo?.avtUrl,
                        myInfo?.fullName,
                        SharedPref.userID,
                        typeDescription = getString(R.string.shared_the_post)
                    )
                ).addOnCompleteListener {
                    if (it.isSuccessful) {
                        activity?.let { it1 ->
                            DialogHelper(it1).showAlertMessage(
                                getString(R.string.post_shared),
                                isHidden = true,
                                confirmText = getString(R.string.ok)
                            )
                            if (item.userID != userID) {
                                BaseApplication.instance?.dataBase?.getReference(
                                    ConstantKey.notify
                                )
                                    ?.child(item.userID!!)?.run {
                                        val key = push().key!!
                                        val notify = NotifyModel(
                                            myInfo?.uid!!,
                                            myInfo?.fullName!!,
                                            getString(R.string.shared_ur_post),
                                            false,
                                            ConstantKey.isShare,
                                            Calendar.getInstance().time.toDateTime(),
                                            key,
                                            item.id,
                                            item.postShare != null
                                        )
                                        child(key).setValue(
                                            notify
                                        )
                                        notify.sendFCM(fcmToken)
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun deletePost(item: PostModel, holder: BaseViewHolder, moreView: ImageView) {
        val popUp = PopupWindow(context)
        val view = View.inflate(context, R.layout.post_hide_post_layout, null)
        view.findViewById<TextView>(R.id.hideTV).text = context.getString(R.string.delete)
        val dialogHelper = DialogHelper(context as BaseActivity)
        view.setOnClick {
            popUp.dismiss()
            dialogHelper.showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)?.child(
                userID!!
            )?.child(item.id)?.ref?.removeValue()?.addOnSuccessListener {
                BaseApplication.instance?.storage?.run {
                    when {
                        item.updateAvt == true -> {
                            getReferenceFromUrl(item.avtUrl!!).delete()
                        }
                        item.updateBG == true -> {
                            getReferenceFromUrl(item.bgUrl).delete()
                        }
                        else -> {
                            reference.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.imgRef}").listAll()
                                .addOnSuccessListener {
                                    var count = 0
                                    it.items.forEach { storage ->
                                        storage.delete().addOnSuccessListener { task ->
                                            count++
                                            if (count == it.items.size) {
                                                EventBus.getDefault().post(OnRefreshProfile())
                                            }
                                        }
                                    }
                                }

                            //Xoa video neu co
                            reference.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.videoRefer}").listAll()
                                .addOnSuccessListener {
                                    var count = 0
                                    it.items.forEach { storage ->
                                        storage.delete().addOnSuccessListener { task ->
                                            count++
                                            if (count == it.items.size) {
                                                EventBus.getDefault().post(OnRefreshProfile())
                                            }
                                        }
                                    }
                                }
                        }
                    }
                    data.removeAt(holder.adapterPosition)
                    notifyItemRemoved(holder.adapterPosition)
                    dialogHelper.hideLoadingDialog()
                }
            }
        }
        popUp.contentView = view
        popUp.isOutsideTouchable = true
        popUp.isTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popUp.showAsDropDown(moreView)
    }
}