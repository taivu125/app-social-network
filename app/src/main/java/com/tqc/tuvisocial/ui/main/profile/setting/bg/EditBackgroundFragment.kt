package com.tqc.tuvisocial.ui.main.profile.setting.bg

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
import com.tqc.tuvisocial.databinding.EditBackgroundFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.PostModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.SharedPref
import com.tqc.tuvisocial.sharedPref.eventBus.OnGetPost
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

class EditBackgroundFragment : BaseFragment() {

    companion object {
        fun newInstance() = EditBackgroundFragment()
    }

    private lateinit var binding: EditBackgroundFragmentBinding
    private var fileBG: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EditBackgroundFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isBackPress = true
        //Load lại avt
        if (SharedPref.myInfo?.bgUrl?.isNotEmpty() == true) {
            context?.let { Glide.with(it).load(SharedPref.myInfo?.bgUrl).into(binding.avtImg) }
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
            if (fileBG == null) {
                showMessageDialog(getString(R.string.choose_iamge))
            } else {
                showLoadingDialog()
                val uri = Uri.fromFile(fileBG)
                BaseApplication.instance?.run {
                    storage?.reference?.child("${ConstantKey.mediaRefer}/${SharedPref.userID}/bg/${uri.lastPathSegment}")?.putFile(uri)?.addOnSuccessListener {
                        it.storage.downloadUrl.addOnSuccessListener {  uri ->
                            dataBase?.getReference(ConstantKey.usersRefer)?.child(SharedPref.userID!!)?.updateChildren(
                                hashMapOf(
                                    "bgUrl" to uri.toString()
                                ) as Map<String, Any>
                            )?.addOnCompleteListener { task ->
                                hideLoadingDialog()
                                SharedPref.myInfo?.bgUrl = uri.toString()
                                if (task.isSuccessful) {
                                    //Tạo post update bg
                                    dataBase?.getReference(ConstantKey.postRefer)?.child(SharedPref.userID!!)?.run {
                                        val key = push().key!!
                                        child(key).setValue(
                                            PostModel(
                                                key,
                                                if (binding.captionTV.text.isNullOrEmpty()) "" else binding.captionTV.text.toString(),
                                                SharedPref.myInfo?.privateTypePost,
                                                null,
                                                null,
                                                null,
                                                "",
                                                Calendar.getInstance().time.toDateTime(),
                                                uri.toString(),
                                                SharedPref.myInfo?.fullName,
                                                SharedPref.userID,
                                                false,
                                                updateBG = true,
                                                typeDescription = getString(R.string.changed_bg)
                                            )
                                        ).addOnCompleteListener {
                                            showMessageDialog(getString(R.string.successful), onClick = {
                                                EventBus.getDefault().post(OnRefreshProfile())
                                                EventBus.getDefault().post(OnGetPost())
                                                pop()
                                            })
                                        }
                                    }
                                } else {
                                    showError()
                                }
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

    override fun onBackPress() {
        super.onBackPress()
        if ((fileBG != null || binding.captionTV.text.isNotEmpty())) {
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
                    fileBG = if (source == MediaSource.CAMERA_IMAGE) {
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
                    UCrop.of(fileBG!!.toUri(), fileBG!!.toUri())
                        .useSourceImageAspectRatio()
                        .start(context!!, this@EditBackgroundFragment)
                }

                override fun onImagePickerError(error: Throwable, source: MediaSource) {
                }
            })
            if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
                fileBG = data?.let { UCrop.getOutput(data)?.toFile() }
                context?.let { it1 -> Glide.with(it1).load(fileBG).into(binding.avtImg) }
            }
        }
    }

}