package com.tqc.tuvisocial.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.MainFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.ChatModel
import com.tqc.tuvisocial.model.MessageModel
import com.tqc.tuvisocial.model.NotifyModel
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.OnToHome
import com.tqc.tuvisocial.ui.adapter.ViewPagerAdapter
import com.tqc.tuvisocial.ui.main.chat.ChatFragment
import com.tqc.tuvisocial.ui.main.home.HomeFragment
import com.tqc.tuvisocial.ui.main.notify.NotifyFragment
import com.tqc.tuvisocial.ui.main.profile.ProfileFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception

class MainFragment : BaseFragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: MainFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Sử dụng view binding để tự động kết nối view với code - mục đích là để ko cần sử dụng findById
        binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        onClick()
        listenData()
    }

    private fun onClick() {
        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home -> binding.viewpager.setCurrentItem(0, true)
                R.id.chat -> binding.viewpager.setCurrentItem(1, true)
                R.id.notify -> binding.viewpager.setCurrentItem(2, true)
                R.id.profile -> binding.viewpager.setCurrentItem(3, true)
            }
            return@setOnItemSelectedListener false
        }
    }

    private fun setupViewPager() {
        val adapter =
            ViewPagerAdapter(childFragmentManager)
        adapter.addData(HomeFragment.newInstance())
        adapter.addData(ChatFragment.newInstance())
        adapter.addData(NotifyFragment.newInstance())
        adapter.addData(ProfileFragment.newInstance())

        binding.viewpager.adapter = adapter
        binding.viewpager.offscreenPageLimit = 2
        binding.viewpager.addOnPageChangeListener(object : ViewPager.OnAdapterChangeListener,
            ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                binding.bottomNavigation.menu.getItem(position)?.isCheckable = true
            }

            override fun onPageSelected(position: Int) {
                binding.bottomNavigation.menu.getItem(position)?.isChecked = true
            }

            override fun onAdapterChanged(
                viewPager: ViewPager,
                oldAdapter: PagerAdapter?,
                newAdapter: PagerAdapter?
            ) {
            }
        })
        binding.viewpager.currentItem = 0
    }

    private fun listenData() {
        //Lăng nghe notify
        BaseApplication.instance?.dataBase?.run {
            getReference(ConstantKey.notify).child(myInfo?.uid!!).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        var isHaveNotify = false
                        for((_,value) in snapshot.value as HashMap<*,*>) {
                            val notifyModel = Gson().fromJson<NotifyModel>(Gson().toJson(value), object :TypeToken<NotifyModel>() {}.type)
                            if (!notifyModel.read) {
                                isHaveNotify = true
                                break
                            }
                        }
                        binding.bottomNavigation.getOrCreateBadge(binding.bottomNavigation.menu.getItem(2).itemId).isVisible =
                            isHaveNotify
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

            //Lăng nghe nếu có cuộc trò chuyện mà có tin nhắn chưa đọc thì báo
            getReference(ConstantKey.chatsRefer).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        var isHaveChatUnread = false
                        try {
                            for((_,value) in snapshot.value as HashMap<*,*>) {
                                val chaModel = Gson().fromJson<ChatModel>(Gson().toJson(value), object :TypeToken<ChatModel>() {}.type)
                                if (chaModel.conversation != null) {
                                    for((_, message) in chaModel.conversation as HashMap<*,*>) {
                                        val messageModel = Gson().fromJson<MessageModel>(Gson().toJson(message), object :TypeToken<MessageModel>() {}.type)
                                        if ((messageModel.read != true && messageModel.sendID != userID && (chaModel.keyIdentity.contains(myInfo?.uid!!)) ||
                                                    (chaModel.memberGroup != null && chaModel.memberGroup?.values?.find { (it as LinkedTreeMap<*, *>)["userID"] == myInfo?.uid!!} != null &&
                                                            messageModel?.listRead?.values?.find { it == userID } == null))) {
                                            isHaveChatUnread = true
                                            break
                                        }
                                    }
                                }
                            }
                        }catch (ex: Exception) {
                            print(ex.message)
                        }
                        binding.bottomNavigation.getOrCreateBadge(binding.bottomNavigation.menu.getItem(1).itemId).isVisible =
                            isHaveChatUnread
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onToHome(event: OnToHome) {
        if (binding.viewpager.currentItem != 0) {
            binding.viewpager.setCurrentItem(0, true)
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().apply {
            if (!isRegistered(this@MainFragment)) {
                register(this@MainFragment)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }
}