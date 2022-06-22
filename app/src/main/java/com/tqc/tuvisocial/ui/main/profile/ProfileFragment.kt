package com.tqc.tuvisocial.ui.main.profile

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
import com.tqc.tuvisocial.databinding.ProfileFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.helper.Helper
import com.tqc.tuvisocial.model.PostModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeDashInMillis
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.OnRefreshProfile
import com.tqc.tuvisocial.ui.main.home.viewProfile.UserFragment
import com.tqc.tuvisocial.ui.main.profile.adapter.FriendAdapter
import com.tqc.tuvisocial.ui.main.profile.adapter.ProfilePostAdapter
import com.tqc.tuvisocial.ui.main.profile.adapter.ProfileImageAdapter
import com.tqc.tuvisocial.ui.main.profile.setting.SettingFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ProfileFragment : BaseFragment() {

    companion object {
        fun newInstance() = ProfileFragment()
    }

    private lateinit var binding: ProfileFragmentBinding
    private var mImageAdapter = ProfileImageAdapter()
    private var user: UserModel ? = null
    private var mPostAdapter = ProfilePostAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageRev.layoutManager = GridLayoutManager(context, 4)
        binding.imageRev.adapter = mImageAdapter
        binding.postRev.layoutManager = LinearLayoutManager(context)
        binding.postRev.adapter = mPostAdapter

        getData()

        //Setting click
        binding.settingImg.setOnClick{
            user?.run {
                push(SettingFragment.newInstance(this))
            }
        }

        //pull to refresh
        binding.refresh.setOnClick {
            showLoadingDialog()
            getData()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getData() {
        //Truy vấn tới cây User và lấy profile của user
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)
            ?.get()?.addOnSuccessListener {
                //Convert json model thành UserModel
                if (it.value != null) {
                    val user: UserModel = Gson().fromJson(
                        Gson().toJson(it.value),
                        object : TypeToken<UserModel>() {}.type
                    )
                    this@ProfileFragment.user = user
                    mPostAdapter.setUser(user)
                    setUpView(user)
                    hideLoadingDialog()
                }
            }
        //Truy vấn lấy toàn bộ hình ảnh của tài khoản
        BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${userID}/")?.listAll()?.addOnSuccessListener {
            //Tiếp tục truy cập vào các post đã tạo để lấy hình ảnh
            if (it.prefixes.size > 0) {
                binding.imgLayout.visibility = View.VISIBLE
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
            } else {
                binding.imgLayout.visibility = View.GONE
            }
        }

        //Truy vấn lấy toàn bộ post thuộc tài khoản
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)?.child(userID!!)
            ?.get()?.addOnSuccessListener {
                //Convert json model thành UserModel
                if (it.value != null) {
                    val posts: MutableList<PostModel> = arrayListOf()
                    for ((_, value) in it.value as HashMap<*, *>) {
                        val post: PostModel = Gson().fromJson(
                            Gson().toJson(value),
                            object : TypeToken<PostModel>() {}.type
                        )
                        posts.add(post)
                    }
                    setUpPost(posts)
                } else {
                    mPostAdapter.data.clear()
                    mPostAdapter.notifyDataSetChanged()
                }
            }
    }

    @SuppressLint("SetTextI18n")
    fun setUpView(user: UserModel) {
        binding.nameTV.text = user.fullName
        context?.let { context ->
            if (user.avtUrl?.isNotEmpty() == true) {
                Glide.with(context)
                    .load(user.avtUrl).into(binding.profileImage)
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
        }
        if (user.education?.isNotEmpty() == true) {
            binding.educationTV.text = "${context?.getString(R.string.education)}: ${user.education}"
        }
        if (user.relationship?.isNotEmpty() == true) {
            binding.relationTV.text = "${context?.getString(R.string.relationship)}: ${user.relationship}"
        }

        binding.friendNumberTV.text = user.friends?.size?.toString() ?: "0"
        binding.friendRev.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val adapter = FriendAdapter {
            push(UserFragment.newInstance(it))
        }
        binding.friendRev.adapter = adapter
        adapter.setNewInstance(user.friends?.values?.toMutableList())

        binding.bgImg.setOnClick {
            context?.run {
                if (user.bgUrl != null && user.bgUrl != "") {
                    Helper.showImageFullScreen(this, user.bgUrl!!, user.fullName!!)
                }
            }
        }

        binding.profileImage.setOnClick {
            context?.run {
                Helper.showImageFullScreen(this, user.avtUrl!!, user.fullName!!)
            }
        }
    }

    private fun setUpPost(posts: MutableList<PostModel>) {
        posts.sortByDescending {post -> post.createDate?.toTimeDashInMillis() }
        mPostAdapter.setNewInstance(posts)
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshProfile(event: OnRefreshProfile) {
        getData()
    }

    override fun onResume() {
        super.onResume()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

}