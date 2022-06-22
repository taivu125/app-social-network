package com.tqc.tuvisocial.ui.main.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.HomeFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.PostModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeDashInMillis
import com.tqc.tuvisocial.sharedPref.eventBus.OnGetPost
import com.tqc.tuvisocial.ui.main.home.create.CreatePostFragment
import com.tqc.tuvisocial.ui.main.home.list.adapter.PostAdapter
import com.tqc.tuvisocial.ui.main.home.viewProfile.UserFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.roundToInt

class HomeFragment : BaseFragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    private lateinit var binding: HomeFragmentBinding

    private var isFirstLoad = true
    private var mPostAdapter = PostAdapter{ id, item ->
        when (id) {
            //Khi nhấn vào avt mở màn hình tt user
            PostAdapter.avtKey -> {
                if (item.userID?.isNotEmpty() == true) {
                    push(UserFragment.newInstance(item.userID!!))
                }
            }
            else -> {
            }
        }
    }
    private val listPost: MutableList<PostModel> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Sử dụng view binding để tự động kết nối view với code - mục đích là để ko cần sử dụng findById
        binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.postRev.layoutManager = LinearLayoutManager(context)
        binding.postRev.adapter = mPostAdapter
        binding.postRev.setItemViewCacheSize(20)
        binding.searchEdt.textSize = 18f
        setEventClick()
        onGetPost()
    }

    private fun setEventClick() {
        binding.createImg.setOnClick {
            push(CreatePostFragment.newInstance())
        }

        binding.searchImg.setOnClick {
            if (binding.layoutSearch.visibility == View.GONE) {
                binding.layoutSearch.visibility = View.VISIBLE
                listPost.clear()
                listPost.addAll(mPostAdapter.data)
                context?.run {
                    //set lại icon
                    binding.searchImg.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_close))
                    binding.searchImg.layoutParams.height = resources.getDimensionPixelSize(R.dimen.dp_16)
                    binding.searchImg.layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_16)
                    binding.searchImg.requestLayout()
                }
            } else {
                binding.layoutSearch.visibility = View.GONE
                binding.searchEdt.setText("")
                context?.run {
                    binding.searchImg.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_search))
                    binding.searchImg.layoutParams.height = resources.getDimensionPixelSize(R.dimen.dp_20)
                    binding.searchImg.layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_20)
                    binding.searchImg.requestLayout()
                }
                getData(true)
            }
        }

        binding.searchEdt.textSize = 14f
        binding.searchEdt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0?.isNotEmpty() == true) {
                    val listPostTemp = ArrayList<PostModel>()
                    listPost.forEach {
                        if (it.description?.lowercase()?.contains(p0.toString().lowercase()) == true ||
                            it.fullName?.lowercase()?.contains(p0.toString().lowercase()) == true) {
                            listPostTemp.add(it)
                        }
                    }
                    mPostAdapter.setNewInstance(listPostTemp)
                } else {
                    mPostAdapter.setNewInstance(listPost)
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        binding.pullToRefresh.apply {
            setOnRefreshListener {
                getData(true)
                isRefreshing = false
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getData(isShow: Boolean) {
        if (isShow)
            showLoadingDialog()
        mPostAdapter.data.clear()
        isFirstLoad = false
        //Truy vấn lấy toàn bộ post
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.postRefer)?.run {
            //Lấy toàn bộ thông tin post của toàn bộ user
            get().addOnSuccessListener { it ->
                if (it.value != null) {
                    val posts = ArrayList<PostModel>()
                    for ((_, user) in it.value as HashMap<*, *>) {
                        //Lọc tiếp và lấy toàn bộ danh sách các post user.v
                        for ((_, item) in user as HashMap<*, *>) {
                            val post: PostModel = Gson().fromJson(
                                Gson().toJson(item),
                                object : TypeToken<PostModel>() {}.type
                            )
                            posts.add(post)
                        }
                    }
                    posts.sortByDescending {post -> post.createDate?.toTimeDashInMillis() }
                    mPostAdapter.setNewInstance(posts)
                    if (isShow)
                        hideLoadingDialog()
                } else {
                    if (isShow)
                        hideLoadingDialog()
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGetPost(event: OnGetPost? = null) {
        //Lay du lieu khi có post được thêm vào
        getData(isFirstLoad)
    }

    override fun onResume() {
        super.onResume()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}