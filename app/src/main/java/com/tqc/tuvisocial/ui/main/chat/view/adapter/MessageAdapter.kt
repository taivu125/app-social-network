package com.tqc.tuvisocial.ui.main.chat.view.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.MessageModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateForCMT
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.ui.main.home.viewProfile.UserFragment
import de.hdodenhof.circleimageview.CircleImageView
import java.util.LinkedHashMap

class MessageAdapter(
    private val isGroup: Boolean,
    private val messageKey: String,
    private val onLongClick: ((MessageModel)) -> Unit,
    private val onShowImageFullScreen: ((String, String?, MutableList<String>) -> Unit)
) :
    BaseQuickAdapter<MessageModel, BaseViewHolder>(R.layout.item_message_chat_layout) {
    private var colorMessage: Int = 0
    private var name: String = ""
    private var avtMap: HashMap<String, String> = hashMapOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setMessageColor(color: Int) {
        colorMessage = color
        notifyDataSetChanged()
    }

    fun setName(name: String) {
        this.name = name
    }

    fun deleteItem(item: MessageModel) {
        this.remove(item)
    }

    override fun convert(holder: BaseViewHolder, item: MessageModel) {
        holder.getView<CircleImageView>(R.id.avtImg).apply {
            visibility = View.VISIBLE
            if (avtMap["${item.sendID}"] != null) {
                Glide.with(context).load(avtMap["${item.sendID}"]).placeholder(R.drawable.gif_loading).into(this)
            } else {
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(item.sendID!!)
                    ?.get()?.addOnSuccessListener {
                        val member: UserModel? = Gson().fromJson(
                            Gson().toJson(it.value),
                            object : TypeToken<UserModel>() {}.type
                        )
                        if (member?.avtUrl != null && member.avtUrl != "") {
                            Glide.with(context).load(member.avtUrl).into(this).onLoadFailed(
                                ContextCompat.getDrawable(context, R.drawable.avatar)
                            )
                            avtMap["${item.sendID}"] = member.avtUrl!!

                        } else {
                            Glide.with(context).load(R.drawable.avatar).placeholder(R.drawable.gif_loading).into(this)
                        }
                        setOnClick {
                            (context as? BaseActivity)?.push(UserFragment.newInstance(item.sendID!!))
                        }
                    }
            }
        }

        if (item.image == true) {
            holder.getView<CircleImageView>(R.id.avtForImg).apply {
                if (avtMap["${item.sendID}"] != null) {
                    Glide.with(context).load(avtMap["${item.sendID}"]).placeholder(R.drawable.gif_loading).into(this)
                } else {
                    BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(item.sendID!!)
                        ?.get()?.addOnSuccessListener {
                            val member: UserModel? = Gson().fromJson(
                                Gson().toJson(it.value),
                                object : TypeToken<UserModel>() {}.type
                            )
                            if (member?.avtUrl != null && member.avtUrl != "") {
                                Glide.with(context).load(member.avtUrl).into(this).onLoadFailed(
                                    ContextCompat.getDrawable(context, R.drawable.avatar)
                                )
                                avtMap["${item.sendID}"] = member.avtUrl!!

                            } else {
                                Glide.with(context).load(R.drawable.avatar).placeholder(R.drawable.gif_loading).into(this)
                            }
                            setOnClick {
                                (context as? BaseActivity)?.push(UserFragment.newInstance(item.sendID!!))
                            }
                        }
                }
            }
            val imageRcv = if (item.sendID == myInfo?.uid) holder.getView<RecyclerView>(R.id.imageSendRcv) else
                holder.getView(R.id.imageReceiverRcv)

            val adapterImage = ImageMessageAdapter {it, data ->
                onShowImageFullScreen.invoke(it, item.sendID, data)
            }
            imageRcv.adapter = adapterImage
            imageRcv.layoutManager = GridLayoutManager(context, 1)
            BaseApplication.instance?.storage?.reference?.child(ConstantKey.mediaRefer)?.child(messageKey)?.child(item.id)
                ?.listAll()?.addOnSuccessListener {
                    if (it.items.size > 0) {
                        imageRcv.layoutManager = GridLayoutManager(context, if (it.items.size >= 2) 2 else 1)
                        it.items.forEach { downLink ->
                            downLink.downloadUrl.addOnSuccessListener { uri ->
                                adapterImage.addData(uri.toString())
                            }
                        }
                    }
                }

            if (item.sendID == userID) {
                holder.getView<LinearLayout>(R.id.sendLayout).visibility = View.VISIBLE
                holder.getView<LinearLayout>(R.id.sendMessageLayout).visibility = View.GONE
                holder.getView<LinearLayout>(R.id.imageMessageSendLayout).visibility = View.VISIBLE
                holder.getView<LinearLayout>(R.id.receiverLayout).visibility = View.GONE
            } else {
                holder.getView<LinearLayout>(R.id.sendLayout).visibility = View.GONE
                holder.getView<LinearLayout>(R.id.receiverLayout).visibility = View.VISIBLE
                holder.getView<LinearLayout>(R.id.messageReceiverLayout).visibility = View.GONE
                holder.getView<LinearLayout>(R.id.imageMessageReceiverLayout).visibility = View.VISIBLE
            }
        } else {
            holder.getView<LinearLayout>(R.id.imageMessageSendLayout).visibility = View.GONE
            holder.getView<LinearLayout>(R.id.imageMessageReceiverLayout).visibility = View.GONE

            if (item.sendID == myInfo?.uid) {
                holder.setVisible(R.id.sendMessageLayout, true)
                holder.setVisible(R.id.sendLayout, true)

                holder.setVisible(R.id.receiverLayout, false)
                holder.setVisible(R.id.imageMessageReceiverLayout, false)

                holder.getView<LinearLayout>(R.id.sendLayout).visibility = View.VISIBLE
                holder.getView<LinearLayout>(R.id.receiverLayout).visibility = View.GONE
                if (item.call == true) {
                    holder.setText(R.id.messageSendTV, "$name: ${item.text}")
                } else {
                    holder.setText(R.id.messageSendTV, item.text)
                }
                holder.setText(R.id.timeSendTV, item.date.toDateForCMT())
                if (!isGroup) {
                    holder.getView<ImageView>(R.id.readImg).visibility = if (item.read == true) View.VISIBLE else View.GONE
                } else {
                    holder.getView<ImageView>(R.id.readImg).visibility = View.GONE
                }

            } else {
                holder.setVisible(R.id.receiverLayout, true)

                holder.setVisible(R.id.sendLayout, false)
                holder.setVisible(R.id.sendMessageLayout, false)
                holder.setVisible(R.id.imageMessageSendLayout, false)

                holder.getView<LinearLayout>(R.id.sendLayout).visibility = View.GONE
                holder.getView<LinearLayout>(R.id.receiverLayout).visibility = View.VISIBLE
                if (item.call == true) {
                    holder.setText(
                        R.id.messageReceiverTV,
                        "${context.getString(R.string.you)}: ${item.text}"
                    )
                } else {
                    holder.setText(R.id.messageReceiverTV, item.text)
                }
                holder.setText(R.id.timeTV, item.date.toDateForCMT())
            }
        }

        if (colorMessage != 0) {
            holder.getView<CardView>(R.id.receiverView)
                .setCardBackgroundColor(ContextCompat.getColor(context, colorMessage))
            holder.getView<CardView>(R.id.sendView)
                .setCardBackgroundColor(ContextCompat.getColor(context, colorMessage))
            holder.getView<TextView>(R.id.messageSendTV)
                .setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.getView<TextView>(R.id.messageReceiverTV)
                .setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            holder.getView<CardView>(R.id.receiverView)
                .setCardBackgroundColor(ContextCompat.getColor(context, R.color.grey))
            holder.getView<CardView>(R.id.sendView)
                .setCardBackgroundColor(ContextCompat.getColor(context, R.color.grey))
            holder.getView<TextView>(R.id.messageSendTV)
                .setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.getView<TextView>(R.id.messageReceiverTV)
                .setTextColor(ContextCompat.getColor(context, R.color.white))
        }

        holder.itemView.setOnLongClickListener {
            onLongClick.invoke(item)
            true
        }

        if (isGroup) {
            if (item.sendID != userID && item.listRead?.values?.isEmpty() == true || item.listRead?.values?.find { it == userID } == null) {
                BaseApplication.instance?.dataBase?.run {
                    getReference(ConstantKey.chatsRefer).child(messageKey)
                        .child(ConstantKey.conversion).child(item.id).child("listRead").run {
                            val key = push().key!!
                            child(key).setValue(userID)
                            if (item.listRead == null) {
                                item.listRead = LinkedHashMap()
                            }
                            item.listRead?.put(key, userID!!)
                        }
                }
            }
        } else {
            if (item.read == false && item.sendID != userID) {
                BaseApplication.instance?.dataBase?.run {
                    item.read = true
                    getReference(ConstantKey.chatsRefer).child(messageKey)
                        .child(ConstantKey.conversion).child(item.id).child("read").setValue(true)
                }
            }
        }
    }
}