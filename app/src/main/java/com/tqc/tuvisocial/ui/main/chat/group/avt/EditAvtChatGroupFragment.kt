package com.tqc.tuvisocial.ui.main.chat.group.avt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.EditAvtChatGroupFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.ChatModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import me.echodev.resizer.Resizer
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import java.io.File

class EditAvtChatGroupFragment(private val chatID: String) : BaseFragment() {

    companion object {
        fun newInstance(chatID: String) = EditAvtChatGroupFragment(chatID)
    }

    private lateinit var binding: EditAvtChatGroupFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EditAvtChatGroupFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var fileAvt: File ? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.chatsRefer)?.child(chatID)?.get()?.addOnSuccessListener {
            val chatModel = Gson().fromJson<ChatModel>(Gson().toJson(it.value), object : TypeToken<ChatModel>() {}.type)
            //Load lại avt
            if (chatModel.avtGroup != null && chatModel.avtGroup != "") {
                context?.let { it1 -> Glide.with(it1).load(chatModel?.avtGroup).into(binding.avtImg) }
            } else {
                context?.let { it1 -> Glide.with(it1).load(R.drawable.avatar).into(binding.avtImg) }
            }
            binding.desTV.setText(chatModel.groupName)
        }

        //SetUp picker image
        easyImage = EasyImage.Builder(requireContext())
            .allowMultiple(false)
            .build()
        //Sự kiện upload ảnh
        binding.avtImg.setOnClick{
            showImagePicker()
        }
        //Upload avt
        binding.updateTV.setOnClick {
            when {
                binding.desTV.text.isNullOrEmpty() -> {
                    showMessageDialog(getString(R.string.group_name_required))
                } else -> {
                    showLoadingDialog()
                    BaseApplication.instance?.run {
                        if (fileAvt != null) {
                            val uri = Uri.fromFile(fileAvt)
                            storage?.reference?.child("${ConstantKey.mediaRefer}/$userID/avt/${uri.lastPathSegment}")?.putFile(uri)?.addOnSuccessListener {
                                it.storage.downloadUrl.addOnSuccessListener {  uri ->
                                    dataBase?.getReference(ConstantKey.chatsRefer)?.child(chatID)?.updateChildren(
                                        hashMapOf(
                                            "avtGroup" to uri.toString(),
                                            "groupName" to if (binding.desTV.text.isNullOrEmpty()) "" else binding.desTV.text.toString()
                                        ) as Map<String, Any>
                                    )?.addOnCompleteListener {
                                        hideLoadingDialog()
                                        showMessageDialog(getString(R.string.successful))
                                    }
                                }
                            }
                        } else {
                            dataBase?.getReference(ConstantKey.chatsRefer)?.child(chatID)?.updateChildren(
                                hashMapOf(
                                    "groupName" to if (binding.desTV.text.isNullOrEmpty()) "" else binding.desTV.text.toString()
                                ) as Map<String, Any>
                            )?.addOnCompleteListener {
                                hideLoadingDialog()
                                showMessageDialog(getString(R.string.successful), onClick = {
                                    pop()
                                })
                            }
                        }
                    }
                }
            }
        }

        binding.backImg.setOnClick {
            pop()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        (context as? BaseActivity)?.let {
            easyImage?.handleActivityResult(requestCode, resultCode, data, it, object :
                DefaultCallback() {

                override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                    fileAvt = if (source == MediaSource.CAMERA_IMAGE) {
                        imageFiles[0].file
                    } else {
                        Resizer(context)
                            .setTargetLength(1080)
                            .setQuality(80)
                            .setOutputFormat("JPEG")
                            .setOutputFilename("resized_image")
                            .setSourceImage(imageFiles[0].file)
                            .resizedFile
                    }
                    context?.let { it1 -> Glide.with(it1).load(fileAvt).into(binding.avtImg) }
                }

                override fun onImagePickerError(error: Throwable, source: MediaSource) {
                }
            })
        }
    }
}