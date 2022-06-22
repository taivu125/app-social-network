package com.tqc.tuvisocial.ui.account

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.bumptech.glide.Glide
import com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.CreateInformationFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeInMillis
import com.tqc.tuvisocial.sharedPref.SharedPref
import com.tqc.tuvisocial.ui.account.login.LoginActivity
import me.echodev.resizer.Resizer
import pl.aprilapps.easyphotopicker.*
import java.io.File
import java.util.*
import android.text.format.DateFormat;
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.yalantis.ucrop.UCrop

class CreateInformationFragment(private val userID: String) : BaseFragment() {

    companion object {
        fun newInstance(userID: String) = CreateInformationFragment(userID)
    }

    private lateinit var binding: CreateInformationFragmentBinding
    private val userDB = FirebaseDatabase.getInstance().getReference("Users")
    private var fileAvt: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CreateInformationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //SetUp picker image
        easyImage = EasyImage.Builder(requireContext())
            .allowMultiple(false)
            .build()

        setEventClick()
    }

    @SuppressLint("SetTextI18n")
    private fun setEventClick() {
        binding.avtImg.setOnClick {
            showImagePicker()
        }
        binding.birthDayTV.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })
        binding.birthDayTV.setOnClick {
            SingleDateAndTimePickerDialog.Builder(context).curved()
                .bottomSheet()
                .displayHours(false)
                .displayMinutes(false)
                .displayDays(false)
                .displayMonth(true)
                .displayYears(true)
                .displayDaysOfMonth(true)
                .displayListener {
                }
                .listener {
                    val day = DateFormat.format("dd", it) as String // 20
                    val monthOfYear = DateFormat.format("MM", it) as String // 06
                    val year = DateFormat.format("yyyy", it) as String // 2013
                    binding.birthDayTV.setText("$day/$monthOfYear/$year")
                }.display()
        }

        binding.btnCreate.setOnClick {
            if (validate()) {
                if (fileAvt != null) {
                    showLoadingDialog()
                    val uri = Uri.fromFile(fileAvt)
                    BaseApplication.instance?.storage?.reference?.child("${ConstantKey.mediaRefer}/${SharedPref.userID}/avt/${uri.lastPathSegment}")
                        ?.putFile(uri)?.addOnSuccessListener {
                        it.storage.downloadUrl.addOnSuccessListener { uri ->
                            createInfo(uri.toString())
                        }
                    }
                } else {
                    createInfo("")
                }
            }
        }
    }

    private fun createInfo(avtUrl: String) {
        userDB.child(userID).run {
            SharedPref.userID = userID
            FirebaseMessaging.getInstance().token.addOnSuccessListener {
                setValue(
                    UserModel(
                        userID,
                        BaseApplication.instance?.firebaseAuth?.currentUser?.email,
                        binding.nameTV.text.toString(),
                        binding.relationshipTV.text.toString(),
                        binding.cityTV.text.toString(),
                        binding.educationTV.text.toString(),
                        ConstantKey.isPublic,
                        hashMapOf(),
                        hashMapOf(),
                        arrayListOf(),
                        arrayListOf(),
                        avtUrl,
                        "",
                        binding.birthDayTV.text.toString(),
                        ConstantKey.isPublic,
                        ConstantKey.isPublic,
                        ConstantKey.isPublic,
                        false,
                        hashMapOf(),
                        it
                    )
                ).addOnSuccessListener {
                    hideLoadingDialog()
                    loadingDialogHelper?.showAlertMessage(
                        getString(R.string.successful),
                        onClick = {
                            startActivity(
                                Intent(
                                    activity,
                                    LoginActivity::class.java
                                )
                            )
                            activity?.finish()
                        })
                }
            }
        }
    }

    private fun validate(): Boolean {
        val calendar = Calendar.getInstance()
        val day = calendar[Calendar.DAY_OF_MONTH]
        val month = calendar[Calendar.MONTH]
        val year = calendar[Calendar.YEAR]
        if (binding.nameTV.text.isNullOrEmpty()) {
            showMessageDialog(getString(R.string.name_reuired))
            return false
        }
        if (binding.birthDayTV.text.isNullOrEmpty()) {
            showMessageDialog(getString(R.string.birthday_reuired))
            return false
        }
        if (binding.birthDayTV.text.isNotEmpty() && ("${if (day < 10) "0$day" else day}/$month/$year".toTimeInMillis() - binding.birthDayTV.text.toString()
                .toTimeInMillis()) < 13
        ) {
            showMessageDialog(getString(R.string.mus_old_13))
            return false
        }
        if (fileAvt == null) {
            showMessageDialog(getString(R.string.avt_required))
            return false
        }
        return true
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
                    context?.let { context ->
                        UCrop.of(fileAvt!!.toUri(), fileAvt!!.toUri())
                            .useSourceImageAspectRatio()
                            .start(context, this@CreateInformationFragment)
                    }
                }

                override fun onImagePickerError(error: Throwable, source: MediaSource) {
                }
            })
        }
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            fileAvt = data?.let { UCrop.getOutput(data)?.toFile() }
            context?.let { it1 ->
                Glide.with(it1).load(fileAvt).into(binding.avtImg) }
            binding.updatePhotoTV.visibility = View.GONE
        }
    }
}