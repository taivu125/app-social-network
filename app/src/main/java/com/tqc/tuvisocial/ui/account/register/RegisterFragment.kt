package com.tqc.tuvisocial.ui.account.register

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.ui.account.CreateInformationFragment


class RegisterFragment constructor(private val isForget: Boolean = false) : BaseFragment() {

    companion object {
        fun newInstance(isForget: Boolean = false) = RegisterFragment(isForget)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.register_fragment, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnFunction = view.findViewById<AppCompatButton>(R.id.btnNext)
        val auth = FirebaseAuth.getInstance()
        val edtPass = view.findViewById<EditText>(R.id.passwordEdt)
        val editConPass = view.findViewById<EditText>(R.id.confirmPasswordEdt)

        //Kiểm tra nếu là forgetPassword thì chỉnh text button và ẩn layout terms
        if (isForget) {
            btnFunction.text = "Send"
            view.findViewById<LinearLayout>(R.id.layoutTerms).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.passLayout).visibility = View.GONE
        } else {
            view.findViewById<LinearLayout>(R.id.passLayout).visibility = View.VISIBLE
        }

        btnFunction.setOnClickListener {
            val edtUN = view.findViewById<EditText>(R.id.userName)

            //Kiểm tra dữ liệu nhập
            if (edtUN.text.isNullOrEmpty()) {
                showMessageDialog(getString(R.string.email_reuired))
            } else if (!view.findViewById<CheckBox>(R.id.agreeCkb).isChecked && !isForget) {
                showMessageDialog(getString(R.string.must_agree))
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(edtUN.text.toString())
                    .matches()
            ) {
                showMessageDialog(getString(R.string.email_wrong_format))
            } else if (edtPass.text.isNullOrEmpty() && !isForget) {
                showMessageDialog(getString(R.string.pass_reuired))
            } else if (editConPass.text.isNullOrEmpty() && !isForget) {
                showMessageDialog(getString(R.string.confirm_pass_reuired))
            } else if (edtPass.text.toString() != editConPass.text.toString() && !isForget) {
                showMessageDialog(getString(R.string.password_dont_match))
            } else {
                //Nếu forget sẽ send email
                if (isForget) {
                    showLoadingDialog()
                    auth.sendPasswordResetEmail(edtUN.text.toString())
                        .addOnCompleteListener { task ->
                            hideLoadingDialog()
                            if (task.isSuccessful) {
                                showMessageDialog(getString(R.string.successful))
                            } else {
                                showMessageDialog(getString(R.string.have_a_error))
                            }
                        }
                } else {
                    //Kiểm tra email có tồn tại
                    showLoadingDialog()
                    auth.fetchSignInMethodsForEmail(edtUN.text.toString())
                        .addOnCompleteListener { task ->
                            hideLoadingDialog()
                            val isNewUser = task.result?.signInMethods?.isEmpty()
                            if (isNewUser == true) {
                                activity?.let {
                                    FirebaseAuth.getInstance()
                                        .createUserWithEmailAndPassword(
                                            edtUN.text.toString(),
                                            edtPass.text.toString()
                                        )
                                        .addOnCompleteListener(it) { task ->
                                            hideLoadingDialog()
                                            if (task.isSuccessful) {
                                                // Đăng nhập thành công, khởi tạo tt user ban đầu
                                                push(
                                                    CreateInformationFragment.newInstance(
                                                        task.result?.user?.uid!!
                                                    )
                                                )
                                            } else {
                                                // If sign in fails, display a message to the user.
                                                showMessageDialog(getString(R.string.have_a_error))
                                            }
                                        }
                                }
                            } else {
                                showMessageDialog(getString(R.string.email_exits))
                            }
                        }
                }
            }
        }

        view.findViewById<TextView>(R.id.backTV).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.backTV).setOnClick {
            pop()
        }
        view.findViewById<ImageView>(R.id.showPassImg).apply {
            setOnClick {
                if (edtPass.transformationMethod == null) {
                    edtPass.transformationMethod = PasswordTransformationMethod()
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.view))
                } else {
                    edtPass.transformationMethod = null
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.hidden))
                }
            }
        }
        view.findViewById<ImageView>(R.id.showConfirmPassImg).apply {
            setOnClick {
                if (editConPass.transformationMethod == null) {
                    editConPass.transformationMethod = PasswordTransformationMethod()
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.view))
                } else {
                    editConPass.transformationMethod = null
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.hidden))
                }
            }
        }
    }

}