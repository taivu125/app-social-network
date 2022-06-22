package com.tqc.tuvisocial.ui.main.chat

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.ChatModel
import com.tqc.tuvisocial.model.MessageModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateForChat
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeDashInMillis
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID

class ChatAdapter(val onClick: ((ChatModel, UserModel?) -> Unit)? = null) : BaseQuickAdapter<ChatModel, BaseViewHolder>(R.layout.item_chat_layout) {
    override fun convert(holder: BaseViewHolder, item: ChatModel) {
        holder.setText(R.id.lastMessageTV, "")
        if (item.group != true) {
            //Lấy thông tin user, để lấy avt
            val keyChat = item.keyIdentity.split("-")
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(if (myInfo?.uid == keyChat[0]) keyChat[1] else keyChat[0])?.addListenerForSingleValueEvent(
                object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value != null) {
                            val userModel: UserModel = Gson().fromJson(
                                Gson().toJson(snapshot.value),
                                object : TypeToken<UserModel>() {}.type
                            )
                            if (userModel.avtUrl?.isNotEmpty() == true) {
                                Glide.with(holder.itemView.context).load(userModel.avtUrl).placeholder(R.drawable.avatar)
                                    .into(holder.getView(R.id.avtImg))
                            } else {
                                Glide.with(holder.itemView.context).load(R.drawable.avatar)
                                    .into(holder.getView(R.id.avtImg))
                            }
                            //Kiểm tra và lấy thông tin nick name của user
                            var nickName = ""
                            if (item.nickName1.contains(userModel.uid ?: "")) {
                                nickName = item.nickName1.split("-")[1]
                            } else if (item.nickName2.contains(userModel.uid ?: "")) {
                                nickName = item.nickName2.split("-")[1]
                            }
                            if (nickName != "" && nickName != " ") {
                                holder.getView<TextView>(R.id.nameTv).text = nickName
                                item.name = nickName
                            } else {
                                holder.getView<TextView>(R.id.nameTv).text = userModel.fullName
                                item.name = userModel.fullName.toString()
                            }
                            holder.itemView.setOnClick{
                                onClick?.invoke(item, userModel)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
            )
        } else if (item.group == true) {
            holder.itemView.setOnClick{
                onClick?.invoke(item, null)
            }
            holder.getView<TextView>(R.id.nameTv).text = item.groupName
            if (item.avtGroup?.isNotEmpty() == true) {
                Glide.with(holder.itemView.context).load(item.avtGroup)
                    .into(holder.getView(R.id.avtImg))
            } else {
                Glide.with(holder.itemView.context).load(R.drawable.avatar)
                    .into(holder.getView(R.id.avtImg))
            }
        }
        //Hien thi last message
        var count = 0
        val messCountUnRead = holder.getView<TextView>(R.id.messageUnreadTV)
        if (item.conversation?.isNotEmpty() == true) {
            var itemCon: MessageModel? = null
            var isFirst = true
            var maxTime = 0.0
            //Lấy tin nhắn cuối cùng
            item.conversation?.values?.forEach {
                val conTemp = Gson().fromJson<MessageModel>(Gson().toJson(it), object : TypeToken<MessageModel>() {}.type)
                if (isFirst) {
                    maxTime = conTemp.timeStamp as Double
                    isFirst = false
                    itemCon = conTemp
                } else if ((conTemp.timeStamp as Double) > maxTime) {
                    maxTime = conTemp.timeStamp as Double
                    itemCon = conTemp
                }
            }
            holder.setText(R.id.lastUpdateTV, itemCon?.date?.toDateForChat())

            if (itemCon?.call == true) {
                val message = if (itemCon?.image == true) context.getString(R.string.send_message) else itemCon?.text
                if (itemCon?.sendID == userID) {
                    holder.setText(R.id.lastMessageTV, "${holder.getView<TextView>(R.id.nameTv).text} $message")
                } else {
                    holder.setText(R.id.lastMessageTV, "${myInfo?.fullName} $message")
                }
            } else {
                if (item.group == true) {
                    //Vào ds member lấy nickName nếu ko có mới lấy tên user
                    item.memberGroup?.values?.forEach { member ->
                        (member as? LinkedTreeMap<*,*>)?.apply {
                            if (this["userID"] == itemCon?.sendID) {
                                if (this["nickName"] != "") {
                                    holder.setText(R.id.lastMessageTV,"${this["nickName"]}: ${itemCon?.text}");
                                } else {
                                    BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(
                                        itemCon?.sendID!!
                                    )?.get()?.addOnSuccessListener {
                                        if (it.value != null) {
                                            val userModel: UserModel = Gson().fromJson(
                                                Gson().toJson(it.value),
                                                object : TypeToken<UserModel>() {}.type
                                            )
                                            val message = if (itemCon?.image == true) context.getString(R.string.send_message) else itemCon?.text
                                            holder.setText(R.id.lastMessageTV,"${userModel.fullName}: $message")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    holder.setText(R.id.lastMessageTV, if (itemCon?.image == true) context.getString(R.string.send_message) else itemCon?.text)
                }
            }

            if (item.group != true) {
                item.conversation?.values?.forEach { mess ->
                    val itemMess = Gson().fromJson<MessageModel>(Gson().toJson(mess), object : TypeToken<MessageModel>() {}.type)
                    if (itemMess.read != true && itemMess.sendID != userID) {
                        count++
                    }
                }
            } else {
                item.conversation?.values?.forEach { mess ->
                    val itemMess = Gson().fromJson<MessageModel>(Gson().toJson(mess), object : TypeToken<MessageModel>() {}.type)
                    if (itemMess.sendID != userID && itemMess.listRead == null || itemMess.listRead?.values?.find { it == userID } == null) {
                        count++
                    }
                }
            }
        }
        if (count != 0) {
            messCountUnRead.text = "$count"
            messCountUnRead.visibility = View.VISIBLE
        } else {
            messCountUnRead.visibility = View.GONE
        }
    }
}