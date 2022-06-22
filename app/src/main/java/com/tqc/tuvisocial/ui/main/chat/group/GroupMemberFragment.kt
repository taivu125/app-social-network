package com.tqc.tuvisocial.ui.main.chat.group

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.GroupMemberFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.MemberGroupModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.ui.main.chat.group.adapter.GroupMemberAdapter

class GroupMemberFragment(private val chatID: String) : BaseFragment() {

    companion object {
        fun newInstance(chatID: String) = GroupMemberFragment(chatID)
    }

    private lateinit var binding: GroupMemberFragmentBinding
    private var member: HashMap<*, *> = hashMapOf<String, Any>()
    private var changeNNDialog: Dialog? = null
    private var popUpSettingView: PopupWindow? = null
    private var memberGroupModel: MemberGroupModel? = null

    private var adapter = GroupMemberAdapter { view, member ->
        memberGroupModel = member
        popUpSettingView?.showAsDropDown(view)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GroupMemberFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.memberRcv.layoutManager = LinearLayoutManager(requireContext())
        binding.memberRcv.adapter = adapter

        //Lấy thông tin member, và loại trừ chính mình
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)?.child(chatID)?.child("memberGroup")?.addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        val list = ArrayList<MemberGroupModel>()
                        member = snapshot.value as HashMap<*, *>
                        for((_, value) in snapshot.value as HashMap<*, *>) {
                            val member = Gson().fromJson<MemberGroupModel>(Gson().toJson(value), object : TypeToken<MemberGroupModel>() {}.type)
                            if (member.userID != myInfo?.uid) {
                                list.add(member)
                            }
                        }
                        adapter.setNewInstance(list)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            }
        )

        //Thêm member
        binding.addMemberLayout.setOnClick {
            push(AddMemberFragment.newInstance(chatID, member))
        }
        //
        binding.backImg.setOnClick {
            pop()
        }
        setUpPopUpSettingMember()
    }

    private fun setUpPopUpSettingMember() {
        //Khởi tạo bottomSheetDialog hiển thi setting chat
        if (popUpSettingView == null) {
            popUpSettingView = PopupWindow(activity)
        }
        val dialogView = View.inflate(activity, R.layout.chat_setting_member_group_layout, null)
        dialogView.findViewById<TextView>(R.id.deleteTV).setOnClick {
            popUpSettingView?.dismiss()
            showMessageDialog(getString(R.string.do_u_delete_member)) {
                showLoadingDialog()
                memberGroupModel?.run {
                    BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)?.child(chatID)?.child("memberGroup")
                        ?.child(this.id)?.removeValue()?.addOnSuccessListener {
                            hideLoadingDialog()
                            memberGroupModel = null
                            showMessageDialog(getString(R.string.successful))
                        }
                }
            }
        }

        dialogView.findViewById<TextView>(R.id.changeNickNameTV).setOnClick {
            popUpSettingView?.dismiss()
            setUpChangeNNView(memberGroupModel!!)
        }

        popUpSettingView?.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT)
        )

        popUpSettingView?.isTouchable = true
        popUpSettingView?.isOutsideTouchable = true

        popUpSettingView?.contentView = dialogView
    }

    private fun setUpChangeNNView(member: MemberGroupModel) {
        //Khởi tạo dialog chỉnh nick name
        if (changeNNDialog == null) {
            changeNNDialog = Dialog(requireContext())
        }
        val changeNNView = View.inflate(requireContext(), R.layout.chat_change_nick_name_group_dialog, null)
        val nickName = changeNNView.findViewById<EditText>(R.id.nickNameTV)
        val fullName = changeNNView.findViewById<EditText>(R.id.fullNameTV)

        nickName.setText(member.nickName)
        BaseApplication.instance?.dataBase?.run {
            //Lấy fullName
            getReference(ConstantKey.usersRefer).child(member.userID!!).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    fullName.setText((snapshot.value as HashMap<*, *>)["fullName"].toString())
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        //Lưu nick name
        changeNNView.findViewById<Button>(R.id.saveBtn).setOnClick {
            BaseApplication.instance?.dataBase?.run {
                getReference(ConstantKey.chatsRefer).child(chatID).child("memberGroup").child(member.id).updateChildren(
                    mapOf(
                        "nickName" to nickName.text.toString()
                    )
                ).addOnSuccessListener {
                    changeNNDialog?.dismiss()
                }
            }
        }
        //Set tag để định danh, sử dụng khi lưu nick name
        changeNNDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        changeNNDialog?.setContentView(changeNNView)
        changeNNDialog?.show()
    }
}