package com.tqc.tuvisocial.ui.main.home.create

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.CreatePostFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.PostModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toDateTime
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.OnGetPost
import com.tqc.tuvisocial.sharedPref.eventBus.OnRefreshProfile
import com.tqc.tuvisocial.ui.main.home.create.adapter.CreateImagePostAdapter
import com.tqc.tuvisocial.ui.main.other.VideoFullScreenActivity
import kotlinx.coroutines.*
import me.echodev.resizer.Resizer
import org.greenrobot.eventbus.EventBus
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class CreatePostFragment : BaseFragment() {

    companion object {
        fun newInstance() = CreatePostFragment()
    }

    private lateinit var binding: CreatePostFragmentBinding

    private var mAdapter = CreateImagePostAdapter()
    private var mStorage: StorageReference = FirebaseStorage.getInstance().reference
    private var dataBase = FirebaseDatabase.getInstance()
    private var videoFile: Uri? = null
    private var chooseImage: ActivityResultLauncher<Intent>? = null

    override fun onBackPress() {
        super.onBackPress()
        if ((videoFile != null || mAdapter.data.size > 0 || binding.statusEdt.text.isNotEmpty())) {
            showMessageDialog(getString(R.string.do_u_want_to_cancel_action), onClick = {
                pop()
            })
        } else {
            pop()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CreatePostFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //handler nút back
        isBackPress = true
        //Đăng ký nhận video khi user chọn
        chooseImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                binding.videoLayout.visibility = View.VISIBLE
                binding.videoView.setVideoURI(result.data?.data)
                binding.videoView.requestFocus()
                binding.videoView.start()
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.videoView.pause()
                }, 500)
                videoFile = result.data?.data
            }
        }
        //Việc tách ra các function chỉ để cho code gọn gàng và dễ đọc thôi
        setUpView()
        setEventClick()
    }

    private fun setUpView() {
        easyImage = EasyImage.Builder(requireContext())
            .allowMultiple(true)
            .build()

        binding.photoRev.layoutManager = GridLayoutManager(requireActivity(), 3)
        binding.photoRev.adapter = mAdapter
    }

    private fun setEventClick() {
        binding.addPhotoBtn.setOnClick {
            showImagePicker()
        }
        binding.addVideoBtn.setOnClick {
            showVideoPicker()
        }
        binding.backImg.setOnClick { pop() }
        binding.postTV.setOnClick {
            if (binding.statusEdt.text.isNullOrBlank() && mAdapter.data.size == 0 && videoFile == null) {
                showMessageDialog(getString(R.string.message_create_post))
            } else {
                showLoadingDialog()
                //uuid để định nhanh image của post -> gôm tất cả image của post 1 theo nhóm
                val uuid = UUID.randomUUID().toString()
                dataBase.getReference(ConstantKey.postRefer).child(userID!!).run {
                    val key = push().key!!
                    child(key).setValue(
                        PostModel(
                            key,
                            if (binding.statusEdt.text.isNullOrEmpty()) "" else binding.statusEdt.text.toString(),
                            myInfo?.privateTypePost,
                            null,
                            null,
                            null,
                            uuid,
                            Calendar.getInstance().time.toDateTime(),
                            myInfo?.avtUrl,
                            myInfo?.fullName,
                            userID
                        )
                    ).addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (mAdapter.data.isNotEmpty()) {
                                mAdapter.data.forEach { file ->
                                    GlobalScope.launch {
                                        upLoadImage(uuid, Uri.fromFile(file))
                                    }
                                }
                                if (videoFile != null) {
                                    GlobalScope.launch { uploadVideo(uuid) }
                                }
                            } else {
                                if (videoFile != null) {
                                    GlobalScope.launch { uploadVideo(uuid) }
                                }
                            }
                            hideLoadingDialog()
                            showMessageDialog(getString(R.string.post_successful), onClick = {
                                pop()
                            })
                            EventBus.getDefault().post(OnGetPost())
                        } else
                            showError()

                    }
                }
            }
        }
        binding.videoView.setOnClick {
            val intent = Intent(activity, VideoFullScreenActivity::class.java)
            intent.putExtra("Video", videoFile.toString())
            intent.putExtra("Name", "")
            startActivity(intent)
        }
        binding.deleteVideo.setOnClick {
            binding.videoLayout.visibility = View.GONE
            videoFile = null
        }
    }

    private fun upLoadImage(uuid: String, uri: Uri) {
        //Lưu ảnh vào thu mục Images/{userID}/{ảnh}
        GlobalScope.async {
            //Lưu ảnh vào thu mục Images/{userID}/{ảnh}
            mStorage.child("${ConstantKey.mediaRefer}/${userID}/$uuid/${ConstantKey.imgRef}/${uri.lastPathSegment}")
                .putFile(uri)
        }
    }

    private suspend fun uploadVideo(uuid: String) {
        withContext(Dispatchers.Default) {
            //Lưu video vào thu mục Images/{userID}/{ảnh}
            mStorage.child("${ConstantKey.mediaRefer}/${userID}/$uuid/${ConstantKey.videoRefer}/${videoFile?.lastPathSegment}")
                .putFile(videoFile!!).addOnSuccessListener {
                    EventBus.getDefault().post(OnGetPost())
                    EventBus.getDefault().post(OnRefreshProfile())
                }
        }
    }

    private fun showVideoPicker() {
        context?.run {
            val dialog = Dialog(this)
            val view = View.inflate(this, R.layout.dialog_choose_image, null)
            view.findViewById<CardView>(R.id.imageGallery).setOnClickListener {
                dialog.dismiss()
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                chooseImage?.launch(intent)
            }
            view.findViewById<CardView>(R.id.imageCamera).setOnClickListener {
                dialog.dismiss()
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                chooseImage?.launch(intent)
            }
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setContentView(view)
            dialog.show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        (context as? BaseActivity)?.let {
            easyImage?.handleActivityResult(requestCode, resultCode, data, it, object :
                DefaultCallback() {
                override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                    binding.photoRev.visibility = View.VISIBLE
                    val list = ArrayList<File>()
                    for (item in imageFiles) {
                        if (source == MediaSource.CAMERA_IMAGE) {
                            list.add(item.file)
                        } else {
                            val resizedImage: File = Resizer(context)
                                .setTargetLength(1080)
                                .setQuality(80)
                                .setOutputFormat("JPEG")
                                .setOutputFilename(item.file.nameWithoutExtension)
                                .setSourceImage(item.file)
                                .resizedFile
                            list.add(resizedImage)
                        }
                    }
                    if (mAdapter.data.isEmpty()) {
                        mAdapter.setNewInstance(list)
                    } else {
                        mAdapter.addNewData(list)
                    }
                }

                override fun onImagePickerError(error: Throwable, source: MediaSource) {
                }
            })
        }
    }
}