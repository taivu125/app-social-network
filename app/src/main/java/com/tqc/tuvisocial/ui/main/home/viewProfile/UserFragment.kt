package com.tqc.tuvisocial.ui.main.home.viewProfile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.ViewProfileFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.helper.Helper
import com.tqc.tuvisocial.model.ChatModel
import com.tqc.tuvisocial.model.NotifyModel
import com.tqc.tuvisocial.model.PostModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.sendFCM
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeDashInMillis
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.ui.main.chat.view.MessageFragment
import com.tqc.tuvisocial.ui.main.profile.ProfileFragment
import com.tqc.tuvisocial.ui.main.profile.adapter.FriendAdapter
import com.tqc.tuvisocial.ui.main.profile.adapter.ProfilePostAdapter
import com.tqc.tuvisocial.ui.main.profile.adapter.ProfileImageAdapter
import java.util.*
import kotlin.collections.HashMap

class UserFragment constructor(val userID: String) : BaseFragment() {

    companion object {
        fun newInstance(userID: String) = UserFragment(userID)
    }

    private lateinit var binding: ViewProfileFragmentBinding
    private var mImageAdapter = ProfileImageAdapter()
    private var user: UserModel ? = null
    private var mPostAdapter = ProfilePostAdapter()
    private var isHaveRequest: Boolean ? = null
    private var isHaveFriend = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ViewProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageRev.layoutManager = GridLayoutManager(context, 4)
        binding.imageRev.adapter = mImageAdapter
        binding.postRev.layoutManager = LinearLayoutManager(context)

        getData()

        //add request friends click
        binding.addFriendLayout.setOnClick {
            when {
                isHaveRequest == false -> {
                    BaseApplication.instance?.dataBase?.run {
                        getReference(ConstantKey.usersRefer).child(user?.uid!!)
                            .child(ConstantKey.requestFriends).push().setValue(myInfo?.uid!!)
                            .addOnSuccessListener {
                                hideLoadingDialog()
                                getReference(ConstantKey.notify).child(user?.uid!!).run {
                                    val key = push().key!!
                                    val notify = NotifyModel(
                                        myInfo?.uid!!,
                                        myInfo?.fullName!!,
                                        getString(R.string.send_a_friend_request),
                                        false,
                                        ConstantKey.isRequestFriend,
                                        Calendar.getInstance().time.toDateTime(),
                                        key
                                    )
                                    child(key).setValue(
                                        notify
                                    )
                                    notify.sendFCM(user?.fcmToken)
                                }
                            }
                    }
                }
                isHaveFriend -> {
                    unFriend()
                }
                isHaveRequest == true -> {
                    BaseApplication.instance?.dataBase?.run {
                        getReference(ConstantKey.usersRefer).child(user?.uid!!)
                            .child(ConstantKey.requestFriends).get().addOnSuccessListener {
                                if (it.value != null) {
                                    for ((_, value) in it.value as HashMap<*, *>) {
                                        if (value == myInfo?.uid) {
                                            it.ref.removeValue()
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
        //add open chat
        binding.chatImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)?.run {
                var messageKey = "${myInfo?.uid}-${user?.uid}"
                //Key chat của 2 user sẽ được tạo từ 2 uid của 2 user, vị trị sẽ phụ thuộc vào người tạo cuộc trò chuyện

                //user hiện tại là người tạo cuộc trò chuyện
                child(messageKey).get().addOnSuccessListener {
                    if (it.value != null) {
                        hideLoadingDialog()
                        push(MessageFragment.newInstance(user!!, messageKey))
                    } else {
                        child("${user?.uid}-${myInfo?.uid}").get().addOnSuccessListener { it2 ->
                            if (it2.value != null) {
                                messageKey = "${user?.uid}-${myInfo?.uid}"
                                hideLoadingDialog()
                                push(MessageFragment.newInstance(user!!, messageKey))
                            } else {
                                messageKey = "${myInfo?.uid}-${user?.uid}"
                                child(messageKey).setValue(ChatModel(
                                    messageKey,
                                    user?.fullName ?: "",
                                    Calendar.getInstance().time.toDateTime(),
                                    0,
                                    "${myInfo?.uid}-",
                                    "${user?.uid}-",
                                    hashMapOf(),false, "", null
                                )).addOnCompleteListener {
                                    //Sau khi tạo thành công thì mở cuộc trò chuyện như bth
                                    hideLoadingDialog()
                                    push(MessageFragment.newInstance(user!!, messageKey))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getData() {
        //Truy vấn tới cây User và lấy profile của user
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID)
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Convert json model thành UserModel
                    if (snapshot.value != null) {
                        val user: UserModel = Gson().fromJson(
                            Gson().toJson(snapshot.value),
                            object : TypeToken<UserModel>() {}.type
                        )
                        this@UserFragment.user = user
                        mPostAdapter.setUser(user)
                        setUpView(user)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        //Truy vấn lấy toàn bộ hình ảnh của tài khoản
        BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${userID}/")?.listAll()?.addOnSuccessListener {
            //Tiếp tục truy cập vào các post đã tạo để lấy hình ảnh
            if (it.prefixes.size > 0) {
                mImageAdapter.data.clear()
                it.prefixes.forEach { item ->
                    item.child(ConstantKey.imgRef).listAll().addOnSuccessListener { result ->
                        result.items.forEach { downLink ->
                            downLink.downloadUrl.addOnSuccessListener { uri ->
                                mImageAdapter.data.add(uri.toString())
                                mImageAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }

        //Truy vấn lấy toàn bộ post thuộc tài khoản
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)?.child(userID)
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Convert json model thành UserModel
                    if (snapshot.value != null) {
                        val posts: MutableList<PostModel> = arrayListOf()
                        for ((_, value) in snapshot.value as HashMap<*, *>) {
                            val post: PostModel = Gson().fromJson(
                                Gson().toJson(value),
                                object : TypeToken<PostModel>() {}.type
                            )
                            posts.add(post)
                        }
                        setUpPost(posts)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }

    @SuppressLint("SetTextI18n")
    fun setUpView(user: UserModel) {
        binding.nameTV.text = user.fullName
        binding.nameTileTV.text = user.fullName
        context?.let { context ->
            if (user.avtUrl?.isNotEmpty() == true) {
                Glide.with(context)
                    .load(user.avtUrl).placeholder(R.drawable.gif_loading)
                    .into(binding.profileImage).onLoadFailed(ContextCompat.getDrawable(context, R.drawable.avatar))
            } else {
                Glide.with(context)
                    .load(R.drawable.avatar)
                    .into(binding.profileImage)
            }
            if (user.bgUrl?.isNotEmpty() == true) {
                Glide.with(context)
                    .load(user.bgUrl)
                    .into(binding.bgImg)
            } else {
                Glide.with(context)
                    .load(R.drawable.bg_default)
                    .into(binding.bgImg)
            }
        }

        if (user.cityName?.isNotEmpty() == true) {
            binding.cityTV.text = "${context?.getString(R.string.city)}: ${user.cityName}"
        } else {
            binding.cityTV.text = "${context?.getString(R.string.city)}: ${context?.getString(R.string.unknown)}"
        }
        if (user.education?.isNotEmpty() == true) {
            binding.educationTV.text = "${context?.getString(R.string.education)}: ${user.education}"
        } else {
            binding.educationTV.text = "${context?.getString(R.string.education)}: ${context?.getString(R.string.unknown)}"
        }
        if (user.relationship?.isNotEmpty() == true) {
            binding.relationTV.text = "${context?.getString(R.string.relationship)}: ${user.relationship}"
        } else {
            binding.relationTV.text = "${context?.getString(R.string.relationship)}: ${context?.getString(R.string.unknown)}"
        }

        binding.friendNumberTV.text = (user.friends?.size ?: 0).toString()
        binding.friendRev.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val adapter = FriendAdapter {
            if (it == myInfo?.uid) {
                push(ProfileFragment.newInstance())
            } else {
                push(newInstance(it))
            }
        }
        binding.friendRev.adapter = adapter
        adapter.setNewInstance(user.friends?.values?.toMutableList())

        //Xử lý rule private theo bạn
        if (user.friends?.containsValue(myInfo?.uid) == true) {
            //Nếu đã là bạn thì ẩn nút kết bạn đi, đã là bạn bè thì chỉ onlyMe mới ẩn đi
            binding.addFriendLayout.setBackgroundResource(R.drawable.border_red_bg_white)
            isHaveFriend = true
            context?.let { binding.addFriendTV.setTextColor(ContextCompat.getColor(it, R.color.red) ) }
            binding.addFriendTV.text = context?.getString(R.string.un_friend)
            if (user.privateTypeInfo == ConstantKey.isOnlyMe) {
                binding.infoLayout.visibility = View.GONE
            }
            if (user.privateTypePost == ConstantKey.isOnlyMe) {
                binding.postLayout.visibility = View.GONE
            }
            if (user.privateTypeFriends == ConstantKey.isOnlyMe) {
                binding.friendsLayout.visibility = View.GONE
            }
        } else {
            //Không phải bạn bè và chưa gửi yêu cầu kết bạn thì sẽ hiển thị nút kết bạn, ngược lại sẽ sửa thành "send request"
            if (user.requestFriends == null || user.requestFriends?.containsValue(myInfo?.uid ?: "") == false) {
                isHaveRequest = false
                binding.addFriendLayout.setBackgroundResource(R.drawable.border_blue_bg_white)
                binding.addFriendTV.text = context?.getString(R.string.add_friend)
                context?.run {
                    binding.addFriendTV.setTextColor(ContextCompat.getColor(this, R.color.blue))
                }
            } else {
                isHaveRequest = true
                binding.addFriendLayout.setBackgroundResource(R.drawable.border_blue_bg_white)
                binding.addFriendTV.text = context?.getString(R.string.send_request)
                context?.run {
                    binding.addFriendTV.setTextColor(ContextCompat.getColor(this, R.color.blue))
                }
            }
            //Kiểm tra nếu là onlyMe hay onlyFriend sẽ ẩn đi
            if (user.privateTypeInfo == ConstantKey.isOnlyMe || user.privateTypeInfo == ConstantKey.isFriend) {
                binding.infoLayout.visibility = View.GONE
            }
            if (user.privateTypePost == ConstantKey.isOnlyMe || user.privateTypePost == ConstantKey.isFriend) {
                binding.postLayout.visibility = View.GONE
            }
            if (user.privateTypeFriends == ConstantKey.isOnlyMe || user.privateTypeFriends == ConstantKey.isFriend) {
                binding.friendsLayout.visibility = View.GONE
            }
        }

        binding.bgImg.setOnClick {
            context?.run {
                Helper.showImageFullScreen(this, user.bgUrl!!, user.fullName!!)
            }
        }

        binding.profileImage.setOnClick {
            context?.run {
                if (user.bgUrl != null && user.bgUrl != "") {
                    Helper.showImageFullScreen(this, user.bgUrl!!, user.fullName!!)
                }
            }
        }
    }

    private fun setUpPost(posts: MutableList<PostModel>) {
        posts.sortByDescending {post -> post.createDate?.toTimeDashInMillis() }
        mPostAdapter.addNewData(posts)
        binding.postRev.adapter = mPostAdapter
    }

    private fun unFriend() {
        BaseApplication.instance?.run {
            dataBase?.getReference(ConstantKey.usersRefer)?.child(myInfo?.uid!!)?.child("friends")
                ?.addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value != null) {
                            for ((_, value) in snapshot.value as HashMap<*, *>) {
                                if (value == userID) {
                                    snapshot.ref.removeValue()
                                    isHaveFriend = false
                                }
                            }
                            dataBase?.getReference(ConstantKey.usersRefer)?.child(userID)?.child("friends")
                                ?.addListenerForSingleValueEvent(object :
                                    ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.value != null) {
                                            for ((_, value) in snapshot.value as HashMap<*, *>) {
                                                if (value == myInfo?.uid) {
                                                    snapshot.ref.removeValue()
                                                    isHaveFriend = false
                                                }
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }

                                })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })
        }
    }
}