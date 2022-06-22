package com.tqc.tuvisocial.ui.main.chat.group.adapter

import android.view.View
import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.MemberGroupModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick

class GroupMemberAdapter(private val onMoreClick: ((View, MemberGroupModel) ->Unit)) : BaseQuickAdapter<MemberGroupModel, BaseViewHolder>(R.layout.item_friend_group_chat_layout) {

    override fun convert(holder: BaseViewHolder, item: MemberGroupModel) {
        //Truy vấn tới cây User và lấy profile của user
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(item.userID!!)
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Convert json model thành UserModel
                    if (snapshot.value != null) {
                        val user: UserModel = Gson().fromJson(
                            Gson().toJson(snapshot.value),
                            object : TypeToken<UserModel>() {}.type
                        )
                        if (user.avtUrl?.isNotEmpty() == true) {
                            Glide.with(context).load(user.avtUrl).into(holder.getView<ImageView>(R.id.avtImg))
                        } else {
                            Glide.with(context).load(R.drawable.avatar).into(holder.getView<ImageView>(R.id.avtImg))
                        }
                        if (item.nickName == null || item.nickName == "") {
                            holder.setText(R.id.nameTV, user.fullName)
                        } else {
                            holder.setText(R.id.nameTV, item.nickName)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        //Sự kiện click cho nút ba chấm
        holder.getView<ImageView>(R.id.moreImg).apply {
            setOnClick {
                onMoreClick.invoke(this, item)
            }
        }
    }
}