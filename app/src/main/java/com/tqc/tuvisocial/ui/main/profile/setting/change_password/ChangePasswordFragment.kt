package com.tqc.tuvisocial.ui.main.profile.setting.change_password

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.firebase.auth.EmailAuthProvider
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.ChangePasswordFragmentBinding
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo

class ChangePasswordFragment : BaseFragment() {

    companion object {
        fun newInstance() = ChangePasswordFragment()
    }
    private lateinit var binding: ChangePasswordFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ChangePasswordFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.updateTV.setOnClick{
            //lấy thong tin password từ view
            val oldPass = binding.oldPassEdt.text.toString()
            val newPass = binding.newPassEdt.text.toString()
            val rePass = binding.rePassEdt.text.toString()


            //Kiểm tra password
            when {
                oldPass.isEmpty() -> {
                    showMessageDialog(getString(R.string.input_old_pass))
                }
                newPass.isEmpty() -> {
                    showMessageDialog(getString(R.string.input_new_pass))
                }
                rePass.isEmpty() -> {
                    showMessageDialog(getString(R.string.input_re_pass))
                }
                newPass != rePass -> {
                    showMessageDialog(getString(R.string.pass_dont_match))
                }
                newPass.length < 6 -> {
                    showMessageDialog(getString(R.string.max_length))
                }
                else -> {
                    showLoadingDialog()
                    //Tiến hành đăng nhập để kiểm tra mk cũ
                    BaseApplication.instance?.firebaseAuth?.currentUser?.run {
                        reauthenticate(EmailAuthProvider.getCredential(myInfo?.userName ?: "", oldPass)).addOnCompleteListener {
                            //Thành công, tiến hành cập nhật mật khẩu
                            if (it.isSuccessful) {
                                updatePassword(newPass).addOnCompleteListener { task ->
                                    hideLoadingDialog()
                                    if (task.isSuccessful) {
                                        showMessageDialog(getString(R.string.successful), onClick = {
                                            pop()
                                        })
                                    } else
                                        showMessageDialog(getString(R.string.have_a_error))

                                }
                            } else {
                                hideLoadingDialog()
                                //Thông báo sai mật khẩu
                                showMessageDialog(getString(R.string.pass_dont_match))
                            }
                        }
                    }
                }
            }
        }
        binding.backImg.setOnClick {
            pop()
        }
        binding.showPassImg.setOnClick {
            context?.let { context ->
                if (binding.oldPassEdt.transformationMethod  == null ) {
                    binding.oldPassEdt.transformationMethod = PasswordTransformationMethod()
                    binding.showPassImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.view))
                } else {
                    binding.oldPassEdt.transformationMethod = null
                    binding.showPassImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.hidden))
                }
            }
        }
        binding.showNewPassImg.setOnClick {
            context?.let { context ->
                if (binding.newPassEdt.transformationMethod  == null ) {
                    binding.newPassEdt.transformationMethod = PasswordTransformationMethod()
                    binding.showNewPassImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.view))
                } else {
                    binding.newPassEdt.transformationMethod = null
                    binding.showNewPassImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.hidden))
                }
            }
        }
        binding.showRePassImg.setOnClick {
            context?.let { context ->
                if (binding.rePassEdt.transformationMethod  == null ) {
                    binding.rePassEdt.transformationMethod = PasswordTransformationMethod()
                    binding.showRePassImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.view))
                } else {
                    binding.rePassEdt.transformationMethod = null
                    binding.showRePassImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.hidden))
                }
            }
        }
    }

}