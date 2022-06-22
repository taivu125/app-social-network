package com.tqc.tuvisocial.ui.main.home.list.adapter

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
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.CommentModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateForCMT
import com.tqc.tuvisocial.sharedPref.SharedPref
import com.tqc.tuvisocial.ui.main.home.viewProfile.UserFragment

class CommentAdapter : BaseQuickAdapter<CommentModel, BaseViewHolder>(R.layout.item_comment_post_layout) {
    override fun convert(holder: BaseViewHolder, item: CommentModel) {
        holder.setText(R.id.cmtTV, item.text)
        holder.setText(R.id.timeTV, item.createDate.toDateForCMT())
        val avtImg = holder.getView<ImageView>(R.id.avtImg)

        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(item.userID)?.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        Gson().fromJson<UserModel>(
                            Gson().toJson(snapshot.value),
                            object : TypeToken<UserModel>() {}.type
                        ).apply {
                            if (avtUrl?.isNotEmpty() == true) {
                                Glide.with(context).load(avtUrl!!).into(avtImg)
                            } else {
                                Glide.with(context).load(R.drawable.avatar).into(holder.getView<ImageView>(R.id.avtImg))
                            }
                            holder.setText(R.id.nameTV, fullName)
                            avtImg.setOnClick {
                                if (uid != SharedPref.userID) {
                                    (context as BaseActivity).push(
                                        UserFragment.newInstance(uid!!)
                                    )
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            }
        )
    }
}