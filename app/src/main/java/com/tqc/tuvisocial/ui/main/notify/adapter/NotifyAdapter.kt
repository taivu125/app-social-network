package com.tqc.tuvisocial.ui.main.notify.adapter

import android.content.Intent
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.shared.DialogHelper
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.NotifyModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.sendFCM
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateForChat
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.ui.main.post.PostDetailActivity
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*
import kotlin.collections.HashMap

class NotifyAdapter :
    BaseQuickAdapter<NotifyModel, BaseViewHolder>(R.layout.notify_item_layout_dialog) {

    override fun convert(holder: BaseViewHolder, item: NotifyModel) {
        //Hiển thị các thông tin của thông báo
        holder.setText(R.id.nameTV, item.name)
        holder.setText(
            R.id.messageTV, when (item.type) {
                ConstantKey.isRequestFriend -> context.getString(R.string.send_a_friend_request)
                ConstantKey.isLike -> context.getString(R.string.liked_ur_post)
                ConstantKey.isCmt -> context.getString(R.string.comment_ur_post)
                -1 -> context.getString(R.string.send_a_friend_request)
                else -> item.message
            }
        )
        holder.setText(R.id.createDateTV, item.createDate.toDateForChat())
        val layout = holder.getView<LinearLayout>(R.id.layout)
        if (item.read) {
            layout.background = ContextCompat.getDrawable(context, R.drawable.border_top_bottom)
        } else {
            layout.background = ContextCompat.getDrawable(context, R.drawable.border_top_bottom_bg_blue50)
        }
        //Lấy thông tin user
        BaseApplication.instance?.dataBase?.run {
            getReference(ConstantKey.usersRefer).child(item.userID).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user: UserModel = Gson().fromJson(
                            Gson().toJson(snapshot.value),
                            object : TypeToken<UserModel>() {}.type
                        )
                        if (user.avtUrl?.isNotEmpty() == true) {
                            Glide.with(context).load(user.avtUrl)
                                .into(holder.getView<CircleImageView>(R.id.circleImageView))
                        } else {
                            Glide.with(context).load(R.drawable.avatar)
                                .into(holder.getView<CircleImageView>(R.id.circleImageView))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
            )
        }

        //Nhấn để xác nhận đã đọc, nếu là thông báo kết bạn thì hiển thị thông báo kết bạn
        holder.itemView.setOnClick {
            if (!item.read) {
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.notify)
                    ?.child(myInfo?.uid!!)?.child(item.id)?.child("read")?.setValue(true)
                    ?.addOnCompleteListener {
                        item.read = true
                        notifyItemChanged(holder.adapterPosition)
                    }
            }

            if (item.type == ConstantKey.isRequestFriend) {
                BaseApplication.instance?.run {
                    dataBase?.getReference(ConstantKey.usersRefer)?.child(myInfo?.uid!!)
                        ?.let { userDB ->
                            userDB.child("requestFriends")
                                .addListenerForSingleValueEvent(object :
                                    ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        var isHaveRequest = false
                                        var ref: DatabaseReference? = null

                                        if (snapshot.value != null) {
                                            for ((_, value) in snapshot.value as HashMap<*, *>) {
                                                if (value == item.userID) {
                                                    isHaveRequest = true
                                                    ref = snapshot.ref
                                                }
                                            }
                                        }
                                        if (isHaveRequest) {
                                            activity?.let { activity ->
                                                DialogHelper(activity).apply {
                                                    showAlertMessage(
                                                        getString(R.string.accep_friends),
                                                        onClick = {
                                                            userDB.child("friends").push()
                                                                .setValue(item.userID)
                                                                .addOnSuccessListener {
                                                                    //Cập nhật notfy thành trạng thái khác để ko hiển thị dialog kb nữa
                                                                    dataBase?.getReference(
                                                                        ConstantKey.notify
                                                                    )
                                                                        ?.child(myInfo?.uid!!)
                                                                        ?.child(item.id)
                                                                        ?.updateChildren(
                                                                            mapOf(
                                                                                "type" to -1
                                                                            )
                                                                        )
                                                                    //Thêm bản thân vào danh sách bạn của người yêu cầu
                                                                    dataBase?.getReference(
                                                                        ConstantKey.usersRefer
                                                                    )
                                                                        ?.child(item.userID)
                                                                        ?.child("friends")
                                                                        ?.push()
                                                                        ?.setValue(
                                                                            myInfo?.uid
                                                                        )
                                                                }
                                                            ref?.removeValue()
                                                        })
                                                }
                                            }
                                        } else {
                                            //Gửi lại lời mời kb
                                            sendRequestFriend(item)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }

                                })
                        }
                }
            }
            else if (item.type == ConstantKey.isLike || item.type == ConstantKey.isCmt || item.type == ConstantKey.isShare) {
                val intent = Intent(context, PostDetailActivity::class.java)
                intent.putExtra("PostID", item.postID)
                intent.putExtra("IsShared", item.share.toString())
                context.startActivity(intent)
            }
        }
    }

    private fun sendRequestFriend(item: NotifyModel) {
        BaseApplication.instance?.run {
            if (activity != null) {
                DialogHelper(activity!!).apply {
                    showAlertMessage(
                        getString(R.string.do_u_send_request),
                        onClick = {
                            BaseApplication.instance?.dataBase?.run {
                                getReference(ConstantKey.usersRefer).child(
                                    item.userID
                                ).get()
                                    .addOnSuccessListener { user ->
                                        user.ref.child(
                                            ConstantKey.requestFriends
                                        )
                                            .push()
                                            .setValue(myInfo?.uid!!)
                                            .addOnSuccessListener {
                                                getReference(
                                                    ConstantKey.notify
                                                ).child(item.userID)
                                                    .run {
                                                        val user: UserModel =
                                                            Gson().fromJson(
                                                                Gson().toJson(
                                                                    user.value
                                                                ),
                                                                object :
                                                                    TypeToken<UserModel>() {}.type
                                                            )

                                                        val key =
                                                            push().key!!
                                                        val notify = NotifyModel(
                                                            myInfo?.uid!!,
                                                            myInfo?.fullName!!,
                                                            getString(
                                                                R.string.send_a_friend_request
                                                            ),
                                                            false,
                                                            ConstantKey.isRequestFriend,
                                                            Calendar.getInstance().time.toDateTime(),
                                                            key
                                                        )
                                                        child(
                                                            key
                                                        ).setValue(
                                                            notify
                                                        )
                                                        notify.sendFCM(
                                                            user.fcmToken
                                                        )
                                                        //Cập nhật notfy thành trạng thái khác để ko hiển thị dialog kb nữa
                                                        dataBase?.getReference(
                                                            ConstantKey.notify
                                                        )
                                                            ?.child(
                                                                myInfo?.uid!!
                                                            )
                                                            ?.child(
                                                                item.id
                                                            )
                                                            ?.updateChildren(
                                                                mapOf(
                                                                    "type" to -1
                                                                )
                                                            )
                                                    }
                                            }
                                    }
                            }
                        })
                }
            }
        }
    }
}