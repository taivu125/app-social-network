package com.tqc.tuvisocial.ui.main.chat.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.google.firebase.database.ChildEventListener
import com.google.gson.internal.LinkedTreeMap
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.MessageGroupFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.helper.ConstantKey.listColor
import com.tqc.tuvisocial.helper.Helper
import com.tqc.tuvisocial.helper.Helper.sendNotifyFCM
import com.tqc.tuvisocial.model.ChatModel
import com.tqc.tuvisocial.model.MessageModel
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.hideKeyboard
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.SharedPref
import com.tqc.tuvisocial.sharedPref.SharedPref.chatOpening
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.ui.main.chat.group.GroupMemberFragment
import com.tqc.tuvisocial.ui.main.chat.group.avt.EditAvtChatGroupFragment
import com.tqc.tuvisocial.ui.main.chat.view.adapter.ChooseImageAdapter
import com.tqc.tuvisocial.ui.main.chat.view.adapter.ColorAdapter
import com.tqc.tuvisocial.ui.main.chat.view.adapter.DiffMessageCallBack
import com.tqc.tuvisocial.ui.main.chat.view.adapter.MessageAdapter
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import me.echodev.resizer.Resizer
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MessageGroupFragment constructor(private val messageKey: String, private val isFromNotify: Boolean? = false) : BaseFragment() {

    companion object {
        fun newInstance(messageKey: String, isFromNotify: Boolean? = false) = MessageGroupFragment(messageKey, isFromNotify)
    }

    lateinit var binding: MessageGroupFragmentBinding
    private var chatModel: ChatModel? = null
    private var popUpSettingView: PopupWindow? = null
    private var popUpSettingGroupView: PopupWindow? = null
    private var changeColorDialog: Dialog? = null
    private var adapterChangeColor: ColorAdapter? = null
    private val listMember: ArrayList<UserModel> = arrayListOf()

    private val messageAdapter = MessageAdapter(true, messageKey, {
        showMessageDialog(getString(R.string.do_u_delete_message), onClick = {
            deleteMessage(it)
        })
    }, { it, senderID, data ->
        context?.let { it1 -> Helper.showImageFullScreen(it1, it, "", senderID, listMedia = data) }
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MessageGroupFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        SharedPref.chatOpening = messageKey
    }

    override fun onPause() {
        super.onPause()
        chatOpening = ""
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoadingDialog()
        //Setup view
        easyImage = EasyImage.Builder(requireContext())
            .allowMultiple(true)
            .build()

        LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            binding.messageRcv.layoutManager = this
        }
        binding.messageRcv.adapter = messageAdapter
        binding.messageRcv.setItemViewCacheSize(30)
        messageAdapter.setDiffCallback(DiffMessageCallBack())

        //Lay tt doan chat
        getData()
        //Gắn sự kiện click
        eventClick()
    }

    private fun eventClick() {
        binding.backImg.setOnClick {
            if (isFromNotify == true) {
                activity?.finish()
            } else {
                pop()
            }
        }
        binding.messageEdt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if ((p0?.length ?: 0) > 0) {
                    binding.sendlayout.visibility = View.VISIBLE
                } else {
                    binding.sendlayout.visibility = View.GONE
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        binding.sendlayout.setOnClick {
            BaseApplication.instance?.dataBase?.run {
                context?.hideKeyboard(binding.root)
                sendMessage(false)
            }
        }
        binding.moreImg.setOnClick {
            popUpSettingGroupView?.showAsDropDown(binding.moreImg)
        }
        //Emoji
        binding.stickyLayout.setOnClick {
            EmojIconActions(
                context, binding.root, binding.messageEdt, binding.emojiImg,
                "#495C66", "#DCE1E2", "#E6EBEF"
            ).ShowEmojIcon()
        }
        //Show choose image
        binding.imageLayout.setOnClick {
            showImagePicker()
        }
    }

    private fun getData() {
        //Lấy thông tin cuộc trò chuyện và Lắng nghe khi có tin nhắn mới đc thêm
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)?.child(messageKey)
            ?.get()?.addOnSuccessListener { snapshot ->
                if (snapshot.value != null) {
                    chatModel = Gson().fromJson<ChatModel>(
                        Gson().toJson(snapshot.value),
                        object : TypeToken<ChatModel>() {}.type
                    )
                    setUpView()
                    listenMessageReceiver()
                } else {
                    hideLoadingDialog()
                }
            }
    }

    private fun listenMessageReceiver() {
        BaseApplication.instance?.dataBase?.run {
            //Gửi tin nhắn đi bằng cách thêm tin nhắn 'vào conversion' trong json chat
            getReference(ConstantKey.chatsRefer).child(messageKey).child(ConstantKey.conversion).addChildEventListener(
                object : ChildEventListener {
                    override fun onChildAdded(
                        snapshot: DataSnapshot,
                        previousChildName: String?
                    ) {
                        if (snapshot.value != null) {
                            if (chatModel?.conversation != null) {
                                val messageModel = Gson().fromJson<MessageModel>(
                                    Gson().toJson(snapshot.value),
                                    object : TypeToken<MessageModel>() {}.type
                                )
                                if (messageModel.sendID == userID) {
                                    var isExits = false
                                    messageAdapter.data.forEach {
                                        if (it.id == messageModel.id) {
                                            isExits = true
                                            return@forEach
                                        }
                                    }
                                    if (!isExits) {
                                        messageAdapter.addData(messageModel)
                                    }
                                } else {
                                    messageAdapter.addData(messageModel)
                                }
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (messageAdapter.data.size > 0) {
                                        binding.messageRcv.smoothScrollToPosition(messageAdapter.data.size - 1)
                                    }
                                }, 500)
                            }
                            hideLoadingDialog()
                        }
                    }

                    override fun onChildChanged(
                        snapshot: DataSnapshot,
                        previousChildName: String?
                    ) {
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                    }

                    override fun onChildMoved(
                        snapshot: DataSnapshot,
                        previousChildName: String?
                    ) {
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                }
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setUpView() {
        binding.nameTV.text = chatModel?.groupName
        //Hiển thị avt
        if (chatModel?.avtGroup?.isNotEmpty() == true) {
            context?.let { Glide.with(it).load(chatModel?.avtGroup).into(binding.avtImg) }
        }
        if (chatModel?.color != null && chatModel?.color != 0) {
            messageAdapter.setMessageColor(chatModel?.color ?: 0)
            listColor.forEach {
                if (chatModel?.color == it.colorId) {
                    it.isSelected = true
                }
            }
        }

        //Setup các chức năng setting
        setUpPopUpSettingView()
        setUpPopUpSettingGroupView()
        setUpChangeColorView()
    }

    private fun setUpChangeColorView() {
        //Khởi tạo dialog chỉnh màu
        if (changeColorDialog == null) {
            changeColorDialog = Dialog(requireContext())
            changeColorDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            changeColorDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val changeColorView: View? =
                View.inflate(requireContext(), R.layout.chat_chang_color_dialog, null)
            val revColor = changeColorView?.findViewById<RecyclerView>(R.id.colorRev)
            adapterChangeColor = ColorAdapter()
            revColor?.layoutManager = GridLayoutManager(requireContext(), 5)
            revColor?.adapter = adapterChangeColor
            adapterChangeColor?.setNewInstance(listColor)
            changeColorView?.findViewById<Button>(R.id.saveBtn)?.setOnClick {
                changeColorDialog?.dismiss()
                showLoadingDialog()
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)
                    ?.child(chatModel?.keyIdentity ?: "")?.updateChildren(
                        mapOf(
                            "color" to (adapterChangeColor?.data?.find { it.isSelected }?.colorId
                                ?: R.color.white)
                        ) as Map<String, Any>
                    )?.addOnSuccessListener {
                        getData()
                        hideLoadingDialog()
                    }
            }
            changeColorView?.run {
                changeColorDialog?.setContentView(this)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setUpPopUpSettingView() {
        //Khởi tạo bottomSheetDialog hiển thi setting chat
        if (popUpSettingView == null) {
            var dialogView: View? = null
            popUpSettingView = PopupWindow(activity)
            dialogView = View.inflate(context, R.layout.chat_setting_single_layout, null)
            dialogView?.findViewById<TextView>(R.id.changeColorTV)?.setOnClick {
                popUpSettingView?.dismiss()
                changeColorDialog?.show()
            }
            dialogView?.findViewById<TextView>(R.id.changeNicknameTV)?.setOnClick {
                popUpSettingView?.dismiss()
                push(EditAvtChatGroupFragment.newInstance(chatModel?.keyIdentity!!))
            }

            popUpSettingView?.setBackgroundDrawable(
                ColorDrawable(
                    Color.TRANSPARENT
                )
            )
            if (chatModel?.group == true) {
                dialogView?.findViewById<TextView>(R.id.changeNicknameTV)?.text =
                    getString(R.string.change_information)
            }
            popUpSettingView?.isTouchable = true
            popUpSettingView?.isOutsideTouchable = true

            popUpSettingView?.contentView = dialogView
        }
    }

    private fun setUpPopUpSettingGroupView() {
        //Khởi tạo bottomSheetDialog hiển thi setting chat
        if (popUpSettingGroupView == null) {
            var dialogView: View? = null
            popUpSettingGroupView = PopupWindow(activity)
            dialogView = View.inflate(activity, R.layout.chat_setting_group_layout, null)
            dialogView?.findViewById<TextView>(R.id.memberTV)?.setOnClick {
                popUpSettingGroupView?.dismiss()
                push(GroupMemberFragment.newInstance(messageKey))
            }
            dialogView?.findViewById<TextView>(R.id.settingTV)?.setOnClick {
                popUpSettingGroupView?.dismiss()
                popUpSettingView?.showAsDropDown(binding.moreImg)
            }

            popUpSettingGroupView?.setBackgroundDrawable(
                ColorDrawable(
                    Color.TRANSPARENT
                )
            )
            popUpSettingGroupView?.isTouchable = true
            popUpSettingGroupView?.isOutsideTouchable = true

            popUpSettingGroupView?.contentView = dialogView
        }
    }

    private fun sendMessage(isImage: Boolean, images: MutableList<File>? = null) {
        BaseApplication.instance?.dataBase?.run {
            //Gửi tin nhắn đi bằng cách thêm tin nhắn 'vào conversion' trong json chat
            getReference(ConstantKey.chatsRefer).child(messageKey).child(ConstantKey.conversion)
                .run {
                    val key = push().key!!
                    var count = 0
                    if (isImage) {
                        images?.forEach { file ->
                            val uri = Uri.fromFile(file)
                            BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${messageKey}/$key/${uri.lastPathSegment}")
                                ?.putFile(uri)?.addOnSuccessListener {
                                    //Up load ảnh xong thi luu text
                                    count++
                                    if (count == images.size) {
                                        child(key).setValue(
                                            MessageModel(
                                                binding.messageEdt.text.toString(),
                                                key,
                                                "",
                                                Calendar.getInstance().time.toDateTime(),
                                                read = false,
                                                userID,
                                                image = isImage
                                            )
                                        )
                                    }
                                }
                        }
                        sendNotifyFCM(context?.getString(R.string.send_message))
                    } else {
                        val message = binding.messageEdt.text.toString()
                        val messageModel = MessageModel(
                            message,
                            key,
                            "",
                            Calendar.getInstance().time.toDateTime(),
                            read = false,
                            userID,
                            image = isImage
                        )
                        messageAdapter.addData(messageModel)
                        child(key).setValue(
                            messageModel
                        ).addOnSuccessListener {
                            sendNotifyFCM(message)
                        }
                    }
                }
            //Xóa dữ liêu sau khi nhấn gửi
            binding.messageEdt.setText("")
        }
    }


    private fun deleteMessage(item: MessageModel) {
        showLoadingDialog()
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)
            ?.child(messageKey)?.child(ConstantKey.conversion)?.child(item.id)?.removeValue()
            ?.addOnSuccessListener {
                messageAdapter.deleteItem(item)
                hideLoadingDialog()
                showMessageDialog(getString(R.string.successful))
            }
    }

    private fun showImageChoose(images: ArrayList<File>) {
        context?.let { context ->
            BottomSheetDialog(context).apply {
                View.inflate(context, R.layout.chat_image_choose_show_layout, null).apply {
                    val imageRcv = findViewById<RecyclerView>(R.id.imageRcv)
                    imageRcv.layoutManager =
                        GridLayoutManager(context, if (images.size > 1) 2 else 1)
                    val adapter = ChooseImageAdapter(images)
                    imageRcv.adapter = adapter

                    //Gửi tin nhắn hình ảnh
                    findViewById<ImageView>(R.id.sendImg).setOnClick {
                        if (images.isNotEmpty()) {
                            sendMessage(true, adapter.data)
                            dismiss()
                        }
                    }

                    //Đóng
                    findViewById<TextView>(R.id.closeTV).setOnClick {
                        dismiss()
                    }
                    setContentView(this)
                }
                show()
            }
        }
    }

    private fun sendNotifyFCM(message: String?) {
        if (listMember.isEmpty()) {
            chatModel?.memberGroup?.values?.forEach { value ->
                val userIDTemp = (value as LinkedTreeMap<*, *>)["userID"].toString()
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                    ?.child(userIDTemp)
                    ?.get()?.addOnSuccessListener {
                        val userTemp: UserModel = Gson().fromJson(
                            Gson().toJson(it.value),
                            object : TypeToken<UserModel>() {}.type
                        )
                        listMember.add(userTemp)
                        //Gửi thông báo cho user offline
                        if (chatModel?.memberGroup?.values?.size == listMember.size) {
                            var nickName = ""
                            listMember.forEach { user ->
                                if (user.uid == userID) {
                                    nickName = user.fullName.toString()
                                }
                                if (user.uid != userID) {
                                    var name = ""
                                    chatModel?.memberGroup?.values?.forEach { value ->
                                        if ((value as LinkedTreeMap<*, *>)["userID"] == userID)
                                            name = (value)["nickName"].toString()
                                    }
                                    sendNotifyFCM(
                                        binding.nameTV.text.toString(),
                                        "${if (name == "") nickName else name}: ${message.toString()}",
                                        user.fcmToken,
                                        messageKey,
                                        true,
                                        null
                                    )
                                }
                            }
                        }
                    }

            }
        } else {
            var nickName = ""
            listMember.forEach {
                if (it.uid == userID) {
                    nickName = it.fullName.toString()
                }
                if (it.uid != userID) {
                    var name = ""
                    chatModel?.memberGroup?.values?.forEach { value ->
                        if ((value as LinkedTreeMap<*, *>)["userID"] == userID)
                            name = (value)["nickName"].toString()
                    }
                    sendNotifyFCM(
                        binding.nameTV.text.toString(),
                        "${if (name == "") nickName else name}: ${message.toString()}",
                        it.fcmToken,
                        messageKey,
                        true,
                        null
                    )
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        (context as? BaseActivity)?.let {
            easyImage?.handleActivityResult(requestCode, resultCode, data, it, object :
                DefaultCallback() {

                override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                    val listFile = ArrayList<File>()
                    imageFiles.forEach { mediaFile ->
                        if (source == MediaSource.CAMERA_IMAGE) {
                            listFile.add(mediaFile.file)
                        } else {
                            val fileBG = Resizer(context)
                                .setTargetLength(1080)
                                .setQuality(80)
                                .setOutputFormat("JPEG")
                                .setOutputFilename(mediaFile.file.nameWithoutExtension)
                                .setSourceImage(mediaFile.file)
                                .resizedFile
                            listFile.add(fileBG)
                        }
                    }
                    showImageChoose(listFile)
                }

                override fun onImagePickerError(error: Throwable, source: MediaSource) {
                }
            })
        }
    }
}