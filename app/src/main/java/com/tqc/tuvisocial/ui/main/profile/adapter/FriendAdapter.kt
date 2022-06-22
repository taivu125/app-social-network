package com.tqc.tuvisocial.ui.main.profile.adapter

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Callback
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick

class FriendAdapter(private val onShowInfo: ((String)-> Unit)) : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_friend_layout) {
    override fun convert(holder: BaseViewHolder, item: String) {
        //Truy vấn tới cây User và lấy profile của user
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(item)
                ?.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //Convert json model thành UserModel
                        if (snapshot.value != null) {
                            val user: UserModel = Gson().fromJson(
                                    Gson().toJson(snapshot.value),
                                    object : TypeToken<UserModel>() {}.type
                            )
                            if (user.avtUrl != null && user.avtUrl != "") {
                                Glide.with(context).load(user.avtUrl).placeholder(R.drawable.avatar)
                                    .into(holder.getView(R.id.avtImg))
                            } else {
                                Glide.with(context).load(R.drawable.avatar).into(holder.getView(R.id.avtImg))
                            }
                            holder.setText(R.id.nameTV, user.fullName)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })
        holder.getView<ImageView>(R.id.avtImg).setOnClick {
            onShowInfo.invoke(item)
        }
    }
}