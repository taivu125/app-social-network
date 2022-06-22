package com.tqc.tuvisocial.ui.main.profile.setting.avt

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.EditAvtFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.PostModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.OnRefreshProfile
import com.yalantis.ucrop.UCrop
import me.echodev.resizer.Resizer
import org.greenrobot.eventbus.EventBus
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import java.io.File
import java.util.*

class EditAvtFragment : BaseFragment() {

    companion object {
        fun newInstance() = EditAvtFragment()
    }

    private lateinit var binding: EditAvtFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EditAvtFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var fileAvt: File ? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isBackPress = true
        //Load lại avt
        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)
            ?.child("avtUrl")?.get()?.addOnSuccessListener {
                context?.let { context ->
                    if (it.value != "" && it.value != null) {
                        Glide.with(context).load(it.value.toString()).into(binding.avtImg)
                    } else {
                        Glide.with(context).load(R.drawable.avatar).into(binding.avtImg)
                    }
                }
            }
        //SetUp picker image
        easyImage = EasyImage.Builder(requireContext())
            .allowMultiple(false)
            .build()
        //Sự kiện upload ảnh
        binding.uploadTV.setOnClick{
            showImagePicker()
        }
        //Upload avt
        binding.updateTV.setOnClick {
            if (fileAvt == null) {
                showMessageDialog(getString(R.string.choose_iamge))
            } else {
                showLoadingDialog()
                val uri = Uri.fromFile(fileAvt)
                BaseApplication.instance?.run {
                    storage?.reference?.child("${ConstantKey.mediaRefer}/$userID/avt/${uri.lastPathSegment}")?.putFile(uri)?.addOnSuccessListener {
                        it.storage.downloadUrl.addOnSuccessListener {  uri ->
                            dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                                hashMapOf(
                                    "avtUrl" to uri.toString()
                                ) as Map<String, Any>
                            )?.addOnCompleteListener { task ->
                                hideLoadingDialog()
                                myInfo?.avtUrl = uri.toString()
                                if (task.isSuccessful) {
                                    //Tạo post update avt
                                        dataBase?.getReference(ConstantKey.postRefer)?.child(userID!!)?.run {
                                            val key = push().key!!
                                            child(key).setValue(PostModel(
                                                key,
                                                if (binding.desTV.text.isNullOrEmpty()) "" else binding.desTV.text.toString(),
                                                ConstantKey.isPublic,
                                                null,
                                                null,
                                                null,
                                                "",
                                                Calendar.getInstance().time.toDateTime(),
                                                uri.toString(),
                                                myInfo?.fullName,
                                                userID,
                                                true,
                                                updateBG = false,
                                                typeDescription = getString(R.string.changed_avatar)
                                            )).addOnCompleteListener {
                                                EventBus.getDefault().post(OnRefreshProfile())
                                            }
                                        }
                                    showMessageDialog(getString(R.string.successful), onClick = {
                                        pop()
                                    })
                                } else {
                                    showError()
                                }
                            }
                        }
                    }
                }
            }
        }
        //Back
        binding.backImg.setOnClick {
            pop()
        }
    }

    override fun onBackPress() {
        super.onBackPress()
        if ((fileAvt != null || binding.desTV.text.isNotEmpty())) {
            showMessageDialog(getString(R.string.do_u_want_to_cancel_action), onClick = {
                pop()
            })
        } else {
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
                            .setOutputFilename(imageFiles[0].file.nameWithoutExtension)
                            .setSourceImage(imageFiles[0].file)
                            .resizedFile
                    }
                    UCrop.of(fileAvt!!.toUri(), fileAvt!!.toUri())
                        .useSourceImageAspectRatio()
                        .start(context!!, this@EditAvtFragment)
                }

                override fun onImagePickerError(error: Throwable, source: MediaSource) {
                }
            })

            if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
                fileAvt = data?.let { UCrop.getOutput(data)?.toFile() }
                Glide.with(it).load(fileAvt).fitCenter().into(binding.avtImg)
            }
        }
    }
}