package com.tqc.tuvisocial.ui.main.chat.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.AddMemberFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.MemberGroupModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.ui.main.chat.group.adapter.AddMemberGroupAdapter

class AddMemberFragment(private val chatID: String, private val member: HashMap<*, *>) : BaseFragment() {

    companion object {
        fun newInstance(chatID: String, member: HashMap<*, *>) = AddMemberFragment(chatID, member)
    }

    private lateinit var binding: AddMemberFragmentBinding

    private val addMemberAdapter = AddMemberGroupAdapter {
        showLoadingDialog()
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)?.child(chatID)?.child("memberGroup")?.run {
            push().let { push ->
                child(push.key!!).setValue(MemberGroupModel(
                    it, "",
                    push.key!!
                )).addOnSuccessListener {
                    hideLoadingDialog()
                    showMessageDialog(getString(R.string.successful))
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AddMemberFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.memberRcv.layoutManager = LinearLayoutManager(context)
        binding.memberRcv.adapter = addMemberAdapter

        //Lấy thông tin bạn
        showLoadingDialog()
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(myInfo?.uid!!)?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value != null) {
                    val list = ArrayList<String>()
                    val userModel = Gson().fromJson<UserModel>(Gson().toJson(snapshot.value), object : TypeToken<UserModel>() {}.type)
                    userModel.friends?.forEach {
                        if (!member.containsValue(it.value)) {
                            list.add(it.value)
                        }
                    }
                    addMemberAdapter.setNewInstance(list)
                    hideLoadingDialog()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        binding.backImg.setOnClick {
            pop()
        }
    }
}