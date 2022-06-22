package com.tqc.tuvisocial.ui.main.chat

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.ChatFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.ChatModel
import com.tqc.tuvisocial.model.MemberGroupModel
import com.tqc.tuvisocial.model.MessageModel
import com.tqc.tuvisocial.model.PostModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeDashInMillis
import com.tqc.tuvisocial.sharedPref.SharedPref.chatOpening
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.ui.main.chat.view.MessageFragment
import com.tqc.tuvisocial.ui.main.chat.view.MessageGroupFragment
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChatFragment : BaseFragment() {

    companion object {
        fun newInstance() = ChatFragment()
    }

    private lateinit var binding: ChatFragmentBinding
    private val listChat: MutableList<ChatModel> = ArrayList()


    private val chatAdapter = ChatAdapter { chat, user ->
        chatOpening = chat.keyIdentity
        if (chat.group != true) {
            push(MessageFragment.newInstance(user!!, chat.keyIdentity))
        } else {
            push(MessageGroupFragment.newInstance(chat.keyIdentity))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding  = ChatFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chatRcv.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRcv.adapter = chatAdapter

        getData(false)

        //Tạo group
        binding.groupImg.setOnClick {
            Dialog(requireContext()).apply {
                val dialogView = View.inflate(requireContext(), R.layout.dialog_create_group_chat_layout, null)
                val groupNameEdt = dialogView.findViewById<EditText>(R.id.groupNameEdt)
                dialogView.findViewById<TextView>(R.id.btnOke).setOnClick {
                    if (groupNameEdt.text.isNullOrEmpty()) {
                        showMessageDialog("Please input group name")
                    } else {
                        hide()
                        showLoadingDialog()
                        BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)?.run {
                            //user hiện tại là người tạo cuộc trò chuyện
                            push().let { push ->
                                child(push.key!!).setValue(ChatModel(
                                    push.key!!,
                                    "",
                                    Calendar.getInstance().time.toDateTime(),
                                    0,
                                    "",
                                    "",
                                    hashMapOf(),true, groupNameEdt.text.toString(), null
                                )).addOnSuccessListener {
                                    //Thêm user vừa tạo group vào ds member
                                    child(push.key!!).child("memberGroup").push().setValue(MemberGroupModel(
                                        myInfo?.uid,
                                        ""
                                    ))
                                }
                                hideLoadingDialog()
                            }
                        }
                    }
                }
                dialogView.findViewById<TextView>(R.id.btnCancel).setOnClick {
                    dismiss()
                }
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setContentView(dialogView)
                show()
            }
        }

        setEvent()
    }

    private fun getData(isShowLoading: Boolean) {
        //Lấy thông tin chat
        if (isShowLoading) {
            showLoadingDialog()
        }
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value != null) {
                    val chatList = ArrayList<ChatModel>()
                    for ((_, value) in snapshot.value as HashMap<*, *>) {
                        val chatModel = Gson().fromJson<ChatModel>(Gson().toJson(value), object : TypeToken<ChatModel>() {}.type)
                        //Kiểm tra nếu chat là của user hoặc nhóm user có tham gia
                        if (chatModel != null && chatModel.keyIdentity.contains(myInfo?.uid!!) || chatModel.memberGroup?.values?.find { (it as LinkedTreeMap<*, *>)["userID"] == myInfo?.uid!!} != null) {
                            chatList.add(chatModel)
                        }
                    }
                    chatList.sortByDescending {
                        if (it.conversation?.isNotEmpty() == true) {
                            var itemCon: MessageModel? = null
                            var isFirst = true
                            var maxTime = 0.0
                            //Lấy tin nhắn cuối cùng
                            it.conversation?.values?.forEach {
                                val conTemp = Gson().fromJson<MessageModel>(
                                    Gson().toJson(it),
                                    object : TypeToken<MessageModel>() {}.type
                                )
                                if (isFirst) {
                                    maxTime = conTemp.timeStamp as Double
                                    isFirst = false
                                    itemCon = conTemp
                                } else if ((conTemp.timeStamp as Double) > maxTime) {
                                    maxTime = conTemp.timeStamp as Double
                                    itemCon = conTemp
                                }
                            }
                            itemCon?.date?.toTimeDashInMillis()
                        } else
                            it.createDate.toTimeDashInMillis() }
                    chatAdapter.setNewInstance(chatList)
                    hideLoadingDialog()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun setEvent() {
        binding.searchImg.setOnClick {
            if (binding.layoutSearch.visibility == View.GONE) {
                binding.layoutSearch.visibility = View.VISIBLE
                listChat.clear()
                listChat.addAll(chatAdapter.data)
                context?.run {
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

        binding.searchEdt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0?.isNotEmpty() == true) {
                    val listChatTemp = ArrayList<ChatModel>()
                    listChat.forEach {
                        //Tìm kiếm theo nick name, tên hoặc tên group
                        if ((it.name.lowercase().contains(p0.toString().lowercase())
                                    || ((it.group == true && it.groupName?.lowercase()?.contains(p0.toString().lowercase()) == true)))
                        ) { listChatTemp.add(it)
                        }
                    }
                    chatAdapter.setNewInstance(listChatTemp)
                } else {
                    chatAdapter.setNewInstance(listChat)
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

}