package com.tqc.tuvisocial.ui.main.post

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.shared.DialogHelper
import com.tqc.tuvisocial.databinding.ActivityPostDetailBinding
import com.tqc.tuvisocial.databinding.ActivityPostShareDetailBinding
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
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.OnGetPost
import com.tqc.tuvisocial.ui.main.home.list.adapter.CommentAdapter
import com.tqc.tuvisocial.ui.main.home.list.adapter.PostImageAdapter
import com.tqc.tuvisocial.ui.main.home.viewProfile.UserFragment
import com.tqc.tuvisocial.ui.main.other.VideoFullScreenActivity
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.HashMap

class PostDetailActivity : BaseActivity() {

    private lateinit var binding: Any
    private var dialogHelper: DialogHelper? = null

    override fun getContainerId() = R.id.container

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        dialogHelper = DialogHelper(this)
        dialogHelper?.showLoadingDialog()

        val postID = intent.extras?.getString("PostID")

        if (intent.extras?.getString("IsShared") == "true") {
            binding = ActivityPostShareDetailBinding.inflate(layoutInflater)
            (binding as ActivityPostShareDetailBinding).apply {
                setContentView(root)
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)?.get()
                    ?.addOnSuccessListener {
                        if (it.value != null) {
                            (it.value as HashMap<*, *>).forEach { value ->
                                (value.value as HashMap<*, *>).forEach { valuePost ->
                                    if (valuePost.key == postID) {
                                        setUpPostShare(
                                            this,
                                            Gson().fromJson(
                                                Gson().toJson(valuePost.value),
                                                object : TypeToken<PostModel>() {}.type
                                            )
                                        )
                                        return@addOnSuccessListener
                                    }
                                }
                            }
                            dialogHelper?.hideLoadingDialog()
                            dialogHelper?.
                            showAlertMessage(getString(R.string.the_post_has_beed_delete), onClick = {
                                finish()
                            }, isHidden =  true)
                        }
                    }
            }
        } else {
            binding = ActivityPostDetailBinding.inflate(layoutInflater)
            (binding as ActivityPostDetailBinding).apply {
                setContentView(root)
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)?.get()
                    ?.addOnSuccessListener {
                        if (it.value != null) {
                            (it.value as HashMap<*, *>).forEach { value ->
                                (value.value as HashMap<*, *>).forEach { valuePost ->
                                    if (valuePost.key == postID) {
                                        setUpPost(
                                            this,
                                            Gson().fromJson(
                                                Gson().toJson(valuePost.value),
                                                object : TypeToken<PostModel>() {}.type
                                            )
                                        )
                                        return@addOnSuccessListener
                                    }
                                }
                            }
                            dialogHelper?.hideLoadingDialog()
                            dialogHelper?.
                            showAlertMessage(getString(R.string.the_post_has_beed_delete), onClick = {
                                finish()
                            }, isHidden =  true)
                        }
                    }
            }
        }
    }

    private fun setUpPost(binding: ActivityPostDetailBinding, item: PostModel) {
        binding.apply {
            typeDescription.setText(item.typeDescription)
            //Truy vấn tới cây User và lấy profile của user
            var user: UserModel?
            videoLayout.visibility = View.GONE
            likeImg.setColorFilter(
                ContextCompat.getColor(this@PostDetailActivity, R.color.black)
            )
            likeTV.setTextColor(
                ContextCompat.getColor(
                    this@PostDetailActivity,
                    android.R.color.black
                )
            )

            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(item.userID!!)
                ?.addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("SetTextI18n")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //Convert json model thành UserModel
                        if (snapshot.value != null) {
                            user = Gson().fromJson(
                                Gson().toJson(snapshot.value),
                                object : TypeToken<UserModel>() {}.type
                            )

                            avtImg.setOnClick {
                                if (user?.uid != SharedPref.userID) {
                                    push(UserFragment.newInstance(user?.uid!!))
                                }
                            }
                            nameTV.text = user?.fullName
                            dateTV.text = item.createDate?.toDateForCMT()
                            if (user?.avtUrl?.isNotEmpty() == true) {
                                Glide.with(this@PostDetailActivity).load(user?.avtUrl)
                                    .placeholder(R.drawable.gif_loading).into(avtImg)
                            } else {
                                Glide.with(this@PostDetailActivity).load(R.drawable.avatar)
                                    .into(avtImg)
                            }
                            captionTV.text = item.description.toString()
                            likeNumberTV.text =
                                "${item.like?.size ?: 0} ${this@PostDetailActivity.getString(R.string.like)}"
                            cmtNumberTV.text =
                                "${item.comment?.size ?: 0} ${this@PostDetailActivity.getString(R.string.cmt)}"
                            likeNumberTV.setTextColor(
                                if ((item.like?.size ?: 0) > 0)
                                    ContextCompat.getColor(this@PostDetailActivity, R.color.black)
                                else
                                    ContextCompat.getColor(this@PostDetailActivity, R.color.grey)
                            )
                            cmtNumberTV.setTextColor(
                                if ((item.comment?.size ?: 0) > 0)
                                    ContextCompat.getColor(this@PostDetailActivity, R.color.black)
                                else
                                    ContextCompat.getColor(this@PostDetailActivity, R.color.grey)
                            )
                            //Kiểm tra nếu đã like thì icon chuyển màu xanh
                            if (item.like != null) {
                                for (value in item.like as LinkedTreeMap<*, *>) {
                                    val userIDTemp = try {
                                        (value.value as HashMap<*, *>)["userID"]
                                    } catch (ex: Exception) {
                                        (value.value as LinkedTreeMap<*, *>)["userID"]
                                    }
                                    if (userIDTemp == SharedPref.myInfo?.uid) {
                                        likeImg.setColorFilter(
                                            ContextCompat.getColor(
                                                this@PostDetailActivity,
                                                R.color.blue
                                            )
                                        )
                                        likeTV.setTextColor(
                                            ContextCompat.getColor(
                                                this@PostDetailActivity,
                                                R.color.blue
                                            )
                                        )
                                        break
                                    } else {
                                        likeImg.setColorFilter(
                                            ContextCompat.getColor(
                                                this@PostDetailActivity,
                                                R.color.black
                                            )
                                        )
                                        likeTV.setTextColor(
                                            ContextCompat.getColor(
                                                this@PostDetailActivity,
                                                android.R.color.black
                                            )
                                        )
                                    }
                                }
                            } else {
                                likeImg.setColorFilter(
                                    ContextCompat.getColor(this@PostDetailActivity, R.color.black)
                                )
                                likeTV.setTextColor(
                                    ContextCompat.getColor(
                                        this@PostDetailActivity,
                                        R.color.black
                                    )
                                )
                            }
                            likeLayout.setOnClick {
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
                                        if (likeValue["userID"] == SharedPref.myInfo?.uid) {
                                            statusLike = true
                                            idLike = likeValue["id"].toString()
                                            likeImg.setColorFilter(
                                                ContextCompat.getColor(
                                                    this@PostDetailActivity,
                                                    R.color.blue
                                                )
                                            )
                                            likeTV.setTextColor(
                                                ContextCompat.getColor(
                                                    this@PostDetailActivity,
                                                    R.color.blue
                                                )
                                            )
                                            likeNumberTV.setTextColor(
                                                if ((item.like?.size ?: 0) > 0)
                                                    ContextCompat.getColor(
                                                        this@PostDetailActivity,
                                                        R.color.black
                                                    )
                                                else
                                                    ContextCompat.getColor(
                                                        this@PostDetailActivity,
                                                        R.color.grey
                                                    )
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
                                                "userID" to SharedPref.myInfo?.uid
                                            )
                                            child(key).setValue(value)
                                            if (item.userID != SharedPref.userID) {
                                                BaseApplication.instance?.dataBase?.getReference(
                                                    ConstantKey.notify
                                                )
                                                    ?.child(item.userID!!)?.run {
                                                        val key = push().key!!
                                                        val notify = NotifyModel(
                                                            SharedPref.myInfo?.uid!!,
                                                            SharedPref.myInfo?.fullName!!,
                                                            this@PostDetailActivity.getString(R.string.liked_ur_post),
                                                            false,
                                                            ConstantKey.isLike,
                                                            Calendar.getInstance().time.toDateTime(),
                                                            key,
                                                            item.id,
                                                            item.postShare != null
                                                        )
                                                        child(key).setValue(notify)
                                                        notify.sendFCM(
                                                            user?.fcmToken,
                                                        )
                                                    }
                                            }

                                            //Kiểm tra null để khởi tạo, khi nhấn like/unlike sẽ tăng giá trị like lên mà ko cần refresh adapter, sẽ làm cho
                                            // app trong có vẻ mượt mà hơn
                                            if (item.like == null) {
                                                item.like = LinkedTreeMap()
                                            }
                                            item.like?.put(key, value)
                                            likeNumberTV.text = "${item.like?.values?.size ?: 0} ${
                                                this@PostDetailActivity.getString(R.string.like)
                                            }"
                                            likeImg.setColorFilter(
                                                ContextCompat.getColor(
                                                    this@PostDetailActivity,
                                                    R.color.blue
                                                )
                                            )
                                            likeTV.setTextColor(
                                                ContextCompat.getColor(
                                                    this@PostDetailActivity,
                                                    R.color.blue
                                                )
                                            )
                                            likeNumberTV.setTextColor(
                                                if ((item.like?.size ?: 0) > 0)
                                                    ContextCompat.getColor(
                                                        this@PostDetailActivity,
                                                        R.color.black
                                                    )
                                                else
                                                    ContextCompat.getColor(
                                                        this@PostDetailActivity,
                                                        R.color.grey
                                                    )
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
                                                    if (likeValue["userID"] == SharedPref.myInfo?.uid) {
                                                        item.like?.remove(value.key)
                                                        likeNumberTV.text =
                                                            "${item.like?.values?.size ?: 0} ${
                                                                this@PostDetailActivity.getString(
                                                                    R.string.like
                                                                )
                                                            }"
                                                        likeImg
                                                            .setColorFilter(
                                                                ContextCompat.getColor(
                                                                    this@PostDetailActivity,
                                                                    R.color.black
                                                                )
                                                            )
                                                        likeTV.setTextColor(
                                                            ContextCompat.getColor(
                                                                this@PostDetailActivity,
                                                                R.color.black
                                                            )
                                                        )
                                                        likeNumberTV.setTextColor(
                                                            if ((item.like?.size ?: 0) > 0)
                                                                ContextCompat.getColor(
                                                                    this@PostDetailActivity,
                                                                    R.color.black
                                                                )
                                                            else
                                                                ContextCompat.getColor(
                                                                    this@PostDetailActivity,
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
                            setUpComment(binding.root, item, user)
                            //Xử lý nút more
                            val moreView = moreImg
                            moreView.setOnClick {
                                val popUp = PopupWindow(this@PostDetailActivity)
                                val view =
                                    View.inflate(
                                        this@PostDetailActivity,
                                        R.layout.post_hide_post_layout,
                                        null
                                    )
                                view.setOnClick {
                                    BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                                        ?.child(
                                            SharedPref.myInfo?.uid!!
                                        )?.child("postHide")?.push()?.setValue(item.id)
                                        ?.addOnSuccessListener {
                                            finish()
                                        }
                                }
                                popUp.contentView = view
                                popUp.isOutsideTouchable = true
                                popUp.isTouchable = true
                                popUp.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                popUp.showAsDropDown(moreView)

                            }
                            //Xử lý nút share
                            shareLayout.setOnClick {
                                sharePost(item, user?.fcmToken)
                            }
                        }
                        dialogHelper?.hideLoadingDialog()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        dialogHelper?.hideLoadingDialog()
                    }

                })
            //setUpImage
            //Truy vấn lấy toàn bộ hình ảnh của post
            lineStartMedia.visibility = View.GONE
            lineEndMedia.visibility = View.GONE
            if (item.userID?.isNotEmpty() == true) {
                BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.imgRef}")
                    ?.listAll()?.addOnSuccessListener {
                        //Tiếp tục truy cập vào các post đã tạo để lấy hình ảnh
                        val rev = reView
                        val adapter = PostImageAdapter()
                        adapter.size = it.items.size
                        if (it.items.size > 0) {
                            lineStartMedia.visibility = View.VISIBLE
                            lineEndMedia.visibility = View.VISIBLE
                        } else {
                            lineStartMedia.visibility = View.GONE
                            lineEndMedia.visibility = View.GONE
                        }
                        when {
                            it.items.size >= 2 -> {
                                rev.layoutManager = GridLayoutManager(this@PostDetailActivity, 2)
                            }
                            else -> {
                                rev.layoutManager = GridLayoutManager(this@PostDetailActivity, 1)
                            }
                        }
                        rev.adapter = adapter
                        it.items.forEach { downLink ->
                            downLink.downloadUrl.addOnSuccessListener { uri ->
                                val uriTemp = "$uri"
                                adapter.addData(uriTemp)
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
                                val player = ExoPlayer.Builder(this@PostDetailActivity).build()
                                videoView.apply {
                                    setOnClick {
                                        val intent = Intent(
                                            this@PostDetailActivity,
                                            VideoFullScreenActivity::class.java
                                        )
                                        intent.putExtra("Video", uri.toString())
                                        intent.putExtra("Name", "")
                                        this@PostDetailActivity?.startActivity(intent)
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
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setUpPostShare(binding: ActivityPostShareDetailBinding, post: PostModel) {
        binding.apply {
            likeImg.setColorFilter(
                ContextCompat.getColor(this@PostDetailActivity, R.color.black)
            )
            likeTV.setTextColor(
                ContextCompat.getColor(
                    this@PostDetailActivity,
                    android.R.color.black
                )
            )

            //Hiển thị post
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                ?.child(post.userID!!)?.get()?.addOnSuccessListener {
                    val userPost: UserModel? = Gson().fromJson(
                        Gson().toJson(it.value),
                        object : TypeToken<UserModel>() {}.type
                    )

                    val avtImg = avtShareImg
                    avtImg.setOnClick {
                        if (userPost?.uid != SharedPref.userID) {
                            push(UserFragment.newInstance(post.userID!!))
                        }
                    }
                    nameShareTV.text = userPost?.fullName
                    dateShareTV.text = post.createDate?.toDateForCMT()
                    if (post.avtUrl?.isNotEmpty() == true) {
                        Glide.with(this@PostDetailActivity).load(userPost?.avtUrl)
                            .placeholder(R.drawable.gif_loading).into(avtImg)
                    } else {
                        Glide.with(this@PostDetailActivity).load(R.drawable.avatar)
                            .into(avtImg)
                    }
                    likeNumberTV.text =
                        "${post.like?.size ?: 0} ${this@PostDetailActivity.getString(R.string.like)}"
                    cmtNumberTV.text =
                        "${post.comment?.size ?: 0} ${this@PostDetailActivity.getString(R.string.cmt)}"
                    likeNumberTV.setTextColor(
                        if ((post.like?.size ?: 0) > 0)
                            ContextCompat.getColor(this@PostDetailActivity, R.color.black)
                        else
                            ContextCompat.getColor(this@PostDetailActivity, R.color.grey)
                    )
                    cmtNumberTV.setTextColor(
                        if ((post.comment?.size ?: 0) > 0)
                            ContextCompat.getColor(this@PostDetailActivity, R.color.black)
                        else
                            ContextCompat.getColor(this@PostDetailActivity, R.color.grey)
                    )
                    //Kiểm tra nếu đã like thì icon chuyển màu xanh
                    if (post.like != null) {
                        for (value in post.like as LinkedTreeMap<*, *>) {
                            if ((value.value as LinkedTreeMap<*, *>)["userID"] == SharedPref.myInfo?.uid) {
                                likeImg.setColorFilter(
                                    ContextCompat.getColor(
                                        this@PostDetailActivity,
                                        R.color.blue
                                    )
                                )
                                likeTV.setTextColor(
                                    ContextCompat.getColor(
                                        this@PostDetailActivity,
                                        R.color.blue
                                    )
                                )
                                break
                            } else {
                                likeImg.setColorFilter(
                                    ContextCompat.getColor(this@PostDetailActivity, R.color.black)
                                )
                                likeTV.setTextColor(
                                    ContextCompat.getColor(
                                        this@PostDetailActivity,
                                        R.color.black
                                    )
                                )
                            }
                        }
                    } else {
                        likeImg.setColorFilter(
                            ContextCompat.getColor(this@PostDetailActivity, R.color.black)
                        )
                        likeTV.setTextColor(
                            ContextCompat.getColor(
                                this@PostDetailActivity,
                                R.color.black
                            )
                        )
                    }
                    likeLayout.setOnClick {
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
                                if (likeValue["userID"] == SharedPref.myInfo?.uid) {
                                    statusLike = true
                                    idLike = likeValue["id"].toString()
                                    likeImg.setColorFilter(
                                        ContextCompat.getColor(
                                            this@PostDetailActivity,
                                            R.color.blue
                                        )
                                    )
                                    likeTV.setTextColor(
                                        ContextCompat.getColor(
                                            this@PostDetailActivity,
                                            R.color.blue
                                        )
                                    )
                                    likeNumberTV.setTextColor(
                                        if ((post.like?.size ?: 0) > 0)
                                            ContextCompat.getColor(
                                                this@PostDetailActivity,
                                                R.color.black
                                            )
                                        else
                                            ContextCompat.getColor(
                                                this@PostDetailActivity,
                                                R.color.grey
                                            )
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
                                        "userID" to SharedPref.myInfo?.uid
                                    )
                                    child(key).setValue(value)
                                    if (post.userID != SharedPref.userID) {
                                        BaseApplication.instance?.dataBase?.getReference(
                                            ConstantKey.notify
                                        )
                                            ?.child(post.userID!!)?.run {
                                                val key = push().key!!
                                                val notify = NotifyModel(
                                                    SharedPref.myInfo?.uid!!,
                                                    SharedPref.myInfo?.fullName!!,
                                                    this@PostDetailActivity.getString(R.string.liked_ur_post),
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
                                                notify.sendFCM(
                                                    userPost?.fcmToken,
                                                )
                                            }
                                    }

                                    //Kiểm tra null để khởi tạo, khi nhấn like/unlike sẽ tăng giá trị like lên mà ko cần refresh adapter, sẽ làm cho
                                    // app trong có vẻ mượt mà hơn
                                    if (post.like == null) {
                                        post.like = LinkedTreeMap()
                                    }
                                    post.like?.put(key, value)
                                    likeNumberTV.text = "${post.like?.values?.size ?: 0} ${
                                        this@PostDetailActivity.getString(R.string.like)
                                    }"
                                    likeImg.setColorFilter(
                                        ContextCompat.getColor(
                                            this@PostDetailActivity,
                                            R.color.blue
                                        )
                                    )
                                    likeTV.setTextColor(
                                        ContextCompat.getColor(
                                            this@PostDetailActivity,
                                            R.color.blue
                                        )
                                    )
                                    likeNumberTV.setTextColor(
                                        if ((post.like?.size ?: 0) > 0)
                                            ContextCompat.getColor(
                                                this@PostDetailActivity,
                                                R.color.black
                                            )
                                        else
                                            ContextCompat.getColor(
                                                this@PostDetailActivity,
                                                R.color.grey
                                            )
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
                                            if (likeValue["userID"] == SharedPref.myInfo?.uid) {
                                                post.like?.remove(value.key)
                                                likeNumberTV.text = "${
                                                    post.like?.values?.size
                                                        ?: 0
                                                } ${this@PostDetailActivity.getString(R.string.like)}"
                                                likeImg
                                                    .setColorFilter(
                                                        ContextCompat.getColor(
                                                            this@PostDetailActivity,
                                                            R.color.black
                                                        )
                                                    )
                                                likeTV.setTextColor(
                                                    ContextCompat.getColor(
                                                        this@PostDetailActivity,
                                                        R.color.black
                                                    )
                                                )
                                                likeNumberTV.setTextColor(
                                                    if ((post.like?.size ?: 0) > 0)
                                                        ContextCompat.getColor(
                                                            this@PostDetailActivity,
                                                            R.color.black
                                                        )
                                                    else
                                                        ContextCompat.getColor(
                                                            this@PostDetailActivity,
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
                    setUpComment(binding.root, post, userPost)

                }
            //Hiển thị post shared
            post.postShare?.let { item ->
                //Truy vấn tới cây User và lấy profile của user
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

                                avtImg.setOnClick {
                                    if (user?.uid != SharedPref.userID) {
                                        push(UserFragment.newInstance(item.userID!!))
                                    }
                                }
                                nameTV.text = user?.fullName
                                dateTV.text = item.createDate?.toDateForCMT()
                                if (user?.avtUrl?.isNotEmpty() == true) {
                                    Glide.with(this@PostDetailActivity).load(user?.avtUrl)
                                        .placeholder(R.drawable.gif_loading).into(avtImg)
                                } else {
                                    Glide.with(this@PostDetailActivity).load(R.drawable.avatar)
                                        .into(avtImg)
                                }
                                captionTV.text = item.description
                            }
                            dialogHelper?.hideLoadingDialog()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            dialogHelper?.hideLoadingDialog()
                        }

                    })
                //setUpImage
                //Truy vấn lấy toàn bộ hình ảnh của post
                lineStartMedia.visibility = View.GONE
                lineEndMedia.visibility = View.GONE
                if (item.userID?.isNotEmpty() == true) {
                    BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.imgRef}")
                        ?.listAll()?.addOnSuccessListener {
                            //Tiếp tục truy cập vào các post đã tạo để lấy hình ảnh
                            val rev = reView
                            val adapter = PostImageAdapter()
                            adapter.size = it.items.size
                            if (it.items.size > 0) {
                                lineStartMedia.visibility = View.VISIBLE
                                lineEndMedia.visibility = View.VISIBLE
                            } else {
                                lineStartMedia.visibility = View.GONE
                                lineEndMedia.visibility = View.GONE
                            }

                            when {
                                it.items.size >= 2 -> {
                                    rev.layoutManager =
                                        GridLayoutManager(this@PostDetailActivity, 2)
                                }
                                else -> {
                                    rev.layoutManager =
                                        GridLayoutManager(this@PostDetailActivity, 1)
                                }
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
                    videoLayout.visibility = View.GONE
                    BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${item.userID}/${item.galleryUUID}/${ConstantKey.videoRefer}")
                        ?.listAll()?.addOnSuccessListener {
                            it.items.forEach { downLink ->
                                downLink.downloadUrl.addOnSuccessListener { uri ->
                                    videoLayout.visibility = View.VISIBLE
                                    val player = ExoPlayer.Builder(this@PostDetailActivity).build()
                                    videoView.apply {
                                        setOnClick {
                                            val intent = Intent(
                                                this@PostDetailActivity,
                                                VideoFullScreenActivity::class.java
                                            )
                                            intent.putExtra("Video", uri.toString())
                                            intent.putExtra("Name", "")
                                            this@PostDetailActivity?.startActivity(intent)
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
                val moreView = moreImg
                moreView.setOnClick {
                    val popUp = PopupWindow(this@PostDetailActivity)
                    val view =
                        View.inflate(this@PostDetailActivity, R.layout.post_hide_post_layout, null)
                    view.setOnClick {
                        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                            ?.child(
                                SharedPref.myInfo?.uid!!
                            )?.child("postHide")?.push()?.setValue(item.id)?.addOnSuccessListener {
                                finish()
                            }
                    }
                    popUp.contentView = view
                    popUp.isOutsideTouchable = true
                    popUp.isTouchable = true
                    popUp.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    popUp.showAsDropDown(moreView)

                }
                //Xử lý nút share
                shareLayout.setOnClick {
                    sharePost(item, user?.fcmToken)
                }
            } ?: dialogHelper?.hideLoadingDialog()
        }
    }

    @SuppressLint("CutPasteId", "SetTextI18n")
    private fun setUpComment(holder: CardView, item: PostModel, userModel: UserModel?) {
        val cmtRcv = holder.findViewById<RecyclerView>(R.id.cmtRcv)
        val cmtEdt = holder.findViewById<EmojiconEditText>(R.id.cmtEdt)
        val sendImg = holder.findViewById<ImageView>(R.id.sendCmtImg)
        val adapter = CommentAdapter()

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
        cmtRcv.layoutManager = LinearLayoutManager(this@PostDetailActivity)
        cmtRcv.adapter = adapter
        //Lấy thông tin user cmt
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

        sendImg.setOnClick {
            this@PostDetailActivity.hideKeyboard(holder)
            if (item.comment == null) {
                item.comment = LinkedTreeMap()
            }
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)
                ?.child(item.userID!!)?.child(item.id)?.child("comment")?.run {
                    val key = push().key!!
                    val value = CommentModel(
                        holder.findViewById<EditText>(R.id.cmtEdt).text.toString(),
                        SharedPref.myInfo?.uid!!,
                        Calendar.getInstance().time.toDateTime()
                    )
                    child(key).setValue(value).addOnSuccessListener {
                        item.comment?.put(key, value)
                        adapter.addData(value)
                        holder.findViewById<TextView>(R.id.cmtNumberTV).apply {
                            text = "${adapter.data.size} ${context.getString(R.string.cmt)}"
                            setTextColor(
                                if ((item.comment?.size ?: 0) > 0)
                                    ContextCompat.getColor(this@PostDetailActivity, R.color.black)
                                else
                                    ContextCompat.getColor(this@PostDetailActivity, R.color.grey)
                            )
                        }
                    }
                }
            if (item.userID != SharedPref.userID) {
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.notify)
                    ?.child(item.userID!!)?.run {
                        val key = push().key!!
                        val notify = NotifyModel(
                            SharedPref.myInfo?.uid!!,
                            SharedPref.myInfo?.fullName!!,
                            this@PostDetailActivity.getString(R.string.comment_ur_post),
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
            cmtEdt.setText("")
        }
        EmojIconActions(
            this@PostDetailActivity,
            if (binding is ActivityPostDetailBinding) (this.binding as ActivityPostDetailBinding).root
            else (this.binding as ActivityPostShareDetailBinding).root, cmtEdt,
            holder.findViewById(R.id.emojiImg),
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
                        SharedPref.myInfo?.privateTypePost,
                        null,
                        null,
                        item,
                        "",
                        Calendar.getInstance().time.toDateTime(),
                        SharedPref.myInfo?.avtUrl,
                        SharedPref.myInfo?.fullName,
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
                            EventBus.getDefault().post(OnGetPost())
                            if (item.userID != userID) {
                                BaseApplication.instance?.dataBase?.getReference(
                                    ConstantKey.notify
                                )
                                    ?.child(item.userID!!)?.run {
                                        val key = push().key!!
                                        val notify = NotifyModel(
                                            SharedPref.myInfo?.uid!!,
                                            SharedPref.myInfo?.fullName!!,
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

}