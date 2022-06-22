package com.tqc.tuvisocial.ui.main.chat.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
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
import android.widget.*
import androidx.exifinterface.media.ExifInterface
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
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.MessageFragmentBinding
import com.tqc.tuvisocial.fcm.API
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
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.ui.main.chat.call.VoiceCallActivity
import com.tqc.tuvisocial.ui.main.chat.group.GroupMemberFragment
import com.tqc.tuvisocial.ui.main.chat.view.adapter.ChooseImageAdapter
import com.tqc.tuvisocial.ui.main.chat.view.adapter.ColorAdapter
import com.tqc.tuvisocial.ui.main.chat.view.adapter.DiffMessageCallBack
import com.tqc.tuvisocial.ui.main.chat.view.adapter.MessageAdapter
import com.tqc.tuvisocial.ui.main.home.viewProfile.UserFragment
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import me.echodev.resizer.Resizer
import okhttp3.OkHttpClient
import okhttp3.Request
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MessageFragment constructor(
    private val userModel: UserModel,
    private val messageKey: String,
    private val isFromNotify: Boolean? = false
) : BaseFragment() {

    companion object {
        fun newInstance(userModel: UserModel, messageKey: String, isFromNotify: Boolean? = false) =
            MessageFragment(userModel, messageKey, isFromNotify)
    }

    lateinit var binding: MessageFragmentBinding
    private var chatModel: ChatModel? = null
    private var popUpSettingView: PopupWindow? = null
    private var changeNNDialog: Dialog? = null
    private var changeColorDialog: Dialog? = null
    private var adapterChangeColor: ColorAdapter? = null
    private var isFirst = true

    private val messageAdapter = MessageAdapter(false, messageKey, {
        showMessageDialog(getString(R.string.delete_message), onClick = {
            deleteMessage(it)
        })
    }, { it, _, data ->
        context?.let { it1 ->
            Helper.showImageFullScreen(
                it1, it, binding.nameTV.text.toString(),
                listMedia = data
            )
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MessageFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        SharedPref.chatOpening = messageKey
    }

    override fun onPause() {
        super.onPause()
        SharedPref.chatOpening = ""
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoadingDialog()
        easyImage = EasyImage.Builder(requireContext())
            .allowMultiple(true)
            .build()
        //Setup view
        LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            binding.messageRcv.layoutManager = this
        }
        binding.messageRcv.adapter = messageAdapter
        binding.messageRcv.setItemViewCacheSize(30)
        messageAdapter.setDiffCallback(DiffMessageCallBack())

        binding.nameTV.text = userModel.fullName
        //Hiển thị avt
        if (userModel.avtUrl?.isNotEmpty() == true) {
            Glide.with(requireContext()).load(userModel.avtUrl).placeholder(R.drawable.gif_loading)
                .into(binding.avtImg)
        } else {
            Glide.with(requireContext()).load(R.drawable.avatar).placeholder(R.drawable.gif_loading)
                .into(binding.avtImg)
        }
        //Lay tt doan chat
        getData()
        //Gắn sự kiện click
        eventClick()
    }

    private fun listenMessageReceiver() {
        BaseApplication.instance?.dataBase?.run {
            //Gửi tin nhắn đi bằng cách thêm tin nhắn 'vào conversion' trong json chat
            getReference(ConstantKey.chatsRefer).child(messageKey).child(ConstantKey.conversion)
                .addChildEventListener(
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

    private fun eventClick() {
        binding.backImg.setOnClick {
            if (isFromNotify == true) {
                activity?.finish()
            } else {
                pop()
            }
        }
        //Xử lý nhập liệu sẽ hiển thị icon send
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
        //Gửi tin nhắn
        binding.sendlayout.setOnClick {
            context?.hideKeyboard(binding.root)
            sendMessage(false)
        }
        //Icon more
        binding.moreImg.setOnClick {
            popUpSettingView?.showAsDropDown(binding.moreImg)
        }
        //Call
        binding.callImg.setOnClick {
            val intent = Intent(activity, VoiceCallActivity::class.java)
            intent.putExtra("CallerID", userID)
            intent.putExtra("ReceiverID", userModel.uid)
            intent.putExtra("Status", true)
            intent.putExtra("MessageKey", messageKey)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            callToUser()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.callRefer)?.child(userID!!)
                ?.setValue(
                    hashMapOf(
                        "CallerID" to userID,
                        "ReceiverID" to userModel.uid,
                        "HangOut" to null
                    )
                )
        }

        //Open user
        binding.avtImg.setOnClick {
            push(UserFragment.newInstance(userModel.uid ?: ""))
        }
        binding.layoutName.setOnClick {
            push(UserFragment.newInstance(userModel.uid ?: ""))
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
                    //Nghe tin nhan
                    listenMessageReceiver()
                }
                hideLoadingDialog()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setUpView() {
        //Kiểm tra và lấy thông tin nick name của user
        var nickName = ""
        if (chatModel?.nickName1?.contains(userModel.uid ?: "") == true) {
            nickName = chatModel?.nickName1?.split("-")?.get(1) ?: ""
        } else if (chatModel?.nickName2?.contains(userModel.uid ?: "") == true) {
            nickName = chatModel?.nickName2?.split("-")?.get(1) ?: ""
        }
        if (nickName != "" && nickName != " ") {
            binding.nameTV.text = nickName
            messageAdapter.setName(nickName)
        } else {
            binding.nameTV.text = userModel.fullName
            messageAdapter.setName(userModel.fullName ?: "")
        }
        if (chatModel?.color != null && chatModel?.color != 0) {
            messageAdapter.setMessageColor(chatModel?.color ?: 0)
            listColor.forEach {
                if (chatModel?.color == it.colorId) {
                    it.isSelected = true
                }
            }
        }

        //Cập nhật trạng thái online
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
            ?.child(userModel.uid ?: "")?.child("online")?.addValueEventListener(object :
                ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value is Boolean && snapshot.value == true) {
                        binding.statusImg.visibility = View.VISIBLE
                        binding.statusTV.text = "Online"
                    } else {
                        binding.statusImg.visibility = View.GONE
                        binding.statusTV.text = "Offline"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        if (isFirst) {
            //Setup các chức năng setting
            setUpPopUpSettingView()
            setUpChangeColorView()
            setUpChangeNNView()
            isFirst = false
        }
    }

    private fun setUpChangeNNView() {
        //Khởi tạo dialog chỉnh nick name
        changeNNDialog = Dialog(requireContext())
        val changeNNView =
            View.inflate(requireContext(), R.layout.chat_chang_nick_name_dialog, null)
        val yourNN = changeNNView.findViewById<EditText>(R.id.myNNEdt)
        val friendNN = changeNNView.findViewById<EditText>(R.id.friendNNEdt)

        //Việc đặt nick name sẽ bị xáo trọn giữa 2 ng nên sẽ dùng uid của tk để xác định nick name đó là của ai
        //Set tag để định danh, sử dụng khi lưu nick name
        val nickname1 = chatModel?.nickName1?.split("-")
        val nickname2 = chatModel?.nickName2?.split("-")
        if (chatModel?.nickName1?.contains(myInfo?.uid!!) == true) {
            yourNN.setText(nickname1?.get(1))
            yourNN.tag = nickname1?.get(0)
        }
        if (chatModel?.nickName2?.contains(myInfo?.uid!!) == true) {
            yourNN.setText(nickname2?.get(1))
            yourNN.tag = nickname2?.get(0)
        }
        if (chatModel?.nickName1?.contains(userModel.uid!!) == true) {
            friendNN.setText(nickname1?.get(1))
            friendNN.tag = nickname1?.get(0)
        }
        if (chatModel?.nickName2?.contains(userModel.uid!!) == true) {
            friendNN.setText(nickname2?.get(1))
            friendNN.tag = nickname2?.get(0)
        }
        changeNNView.findViewById<Button>(R.id.saveBtn).setOnClick {
            try {
                showLoadingDialog()
                if (chatModel?.nickName1?.contains(yourNN.tag.toString()) == true) {
                    chatModel?.nickName1 = "${nickname1?.get(0)}-${yourNN.text}"
                }
                if (chatModel?.nickName2?.contains(yourNN.tag.toString()) == true) {
                    chatModel?.nickName2 = "${nickname1?.get(0)}-${friendNN.text}"
                }

                if (chatModel?.nickName1?.contains(friendNN.tag.toString()) == true) {
                    chatModel?.nickName1 = "${nickname2?.get(0)}-${yourNN.text}"
                }

                if (chatModel?.nickName2?.contains(friendNN.tag.toString()) == true) {
                    chatModel?.nickName2 = "${nickname2?.get(0)}-${friendNN.text}"
                }
                BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)
                    ?.child(messageKey)?.updateChildren(
                        mapOf(
                            "nickName1" to chatModel?.nickName1,
                            "nickName2" to chatModel?.nickName2
                        )
                    )?.addOnSuccessListener {
                        hideLoadingDialog()
                        showMessageDialog(getString(R.string.successful), onClick = {
                            changeNNDialog?.dismiss()
                        })
                    }
            } catch (ex: Exception) {
                hideLoadingDialog()
                changeNNDialog?.dismiss()
                showMessageDialog(getString(R.string.change_nn_fail), onClick = {
                    BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)
                        ?.child(messageKey)?.updateChildren(
                        mapOf(
                            "nickName1" to "${myInfo?.uid}-",
                            "nickName2" to "${userModel.uid}-"
                        )
                    )?.addOnSuccessListener {
                        chatModel?.nickName1 = "${myInfo?.uid}-"
                        chatModel?.nickName2 = "${userModel.uid}-"
                        setUpChangeNNView()
                    }
                }, isHidden = true)
            }
        }

        changeNNDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        changeNNDialog?.setContentView(changeNNView)
    }

    private fun setUpChangeColorView() {
        //Khởi tạo dialog chỉnh màu
        changeColorDialog = Dialog(requireContext())
        val changeColorView = View.inflate(requireContext(), R.layout.chat_chang_color_dialog, null)
        val revColor = changeColorView.findViewById<RecyclerView>(R.id.colorRev)
        adapterChangeColor = ColorAdapter()
        revColor.layoutManager =
            GridLayoutManager(requireContext(), 5)
        revColor.adapter = adapterChangeColor
        adapterChangeColor?.setNewInstance(listColor)
        changeColorView.findViewById<Button>(R.id.saveBtn).setOnClick {
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
        changeColorDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        changeColorDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        changeColorDialog?.setContentView(changeColorView)
    }

    private fun setUpPopUpSettingView() {
        //Khởi tạo bottomSheetDialog hiển thi setting chat
        popUpSettingView = PopupWindow(requireContext())
        val dialogView = View.inflate(requireContext(), R.layout.chat_setting_single_layout, null)
        dialogView.findViewById<TextView>(R.id.changeColorTV).setOnClick {
            popUpSettingView?.dismiss()
            changeColorDialog?.show()
        }
        dialogView.findViewById<TextView>(R.id.changeNicknameTV).setOnClick {
            popUpSettingView?.dismiss()
            changeNNDialog?.show()
        }

        popUpSettingView?.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )
        if (chatModel?.group == true) {
            dialogView.findViewById<TextView>(R.id.changeNicknameTV).visibility = View.GONE
        }
        popUpSettingView?.isTouchable = true
        popUpSettingView?.isOutsideTouchable = true

        popUpSettingView?.contentView = dialogView
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
                        sendNotifyFCM(
                            getMyName(),
                            context?.getString(R.string.send_message).toString(),
                            userModel.fcmToken,
                            messageKey,
                            false,
                            userModel
                        )
                        this
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
                            sendNotifyFCM(
                                getMyName(),
                                message,
                                userModel.fcmToken,
                                messageKey,
                                false,
                                userModel
                            )
                        }
                    }
                }
            //Xóa dữ liêu sau khi nhấn gửi
            binding.messageEdt.setText("")
        }
    }

    private fun callToUser() {
        val httpClient = OkHttpClient.Builder()

        val hasMap = HashMap<String, Any>()
        hasMap["to"] = userModel.fcmToken
        hasMap["collapse_key"] = "type_a"
        val data = HashMap<String, Any?>()
        data["CallerID"] = userID
        data["ReceiverID"] = userModel.uid
        data["HangOut"] = null
        data["MessageKey"] = messageKey
        data["Name"] = getMyName()
        hasMap["data"] = data

        httpClient.addInterceptor { chain ->
            val request: Request =
                chain.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        "key=AAAA8uyvheg:APA91bEONV0nx6h6DP1xbqVNPpFbqTaiaXzp0GasC4FL-D1K3V1aIEqs5GyuWr4v1iSKYQi1PUPVWHPLDgg7OpQdgQvxr9QcmQX36SL2RxIw1UmCtmpCw8zFc3v9mFiLpS43TYctvyzg"
                    )
                    .addHeader("Content-Type", "application/json")
                    .build()
            chain.proceed(request)
        }
        Retrofit.Builder().baseUrl("https://fcm.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build().create(API::class.java).sendData(hasMap).enqueue(
                object : Callback<Response<Any>> {
                    override fun onResponse(
                        call: retrofit2.Call<Response<Any>>,
                        response: Response<Response<Any>>
                    ) {
                    }

                    override fun onFailure(call: retrofit2.Call<Response<Any>>, t: Throwable) {
                    }
                }
            )
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

    private fun getMyName() : String {
        var name = ""
        if (chatModel?.nickName1?.contains(userID ?: "") == true) {
            name = chatModel?.nickName1?.split("-")?.get(1) ?: ""
        } else if (chatModel?.nickName2?.contains(userID ?: "") == true) {
            name = chatModel?.nickName2?.split("-")?.get(1) ?: ""
        }
        if (name == "" || name == " ") {
            name = myInfo?.fullName.toString()
        }
        return name
    }
}