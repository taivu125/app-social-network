package com.tqc.tuvisocial.ui.account.login

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.shared.DialogHelper
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.ui.account.CreateInformationFragment
import com.tqc.tuvisocial.ui.account.register.RegisterFragment
import com.tqc.tuvisocial.ui.main.MainActivity

class LoginActivity : BaseActivity() {
    override fun getContainerId() = R.id.container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.red)

        setContentView(R.layout.activity_login)
        val loadingDialog = DialogHelper(this)

        //Khởi tạo dialog hỗ trợ
        val dialogHelper = DialogHelper(this)
        //Khởi tạo các sự kiện theo view và gắn sự kiện click
        val edtEmail = findViewById<EditText>(R.id.emailEdt)
        val edtPass = findViewById<EditText>(R.id.passwordEdt)

        //Gắn sự kiện click cho nút đăng ký
        findViewById<AppCompatButton>(R.id.registerBtn).setOnClickListener {
            push(RegisterFragment.newInstance(false))
        }
        //Gắn sự kiện click cho nút đăng nhập
        findViewById<AppCompatButton>(R.id.btnLogin).setOnClickListener {
            //Kiểm tra email và password
            when {
                edtEmail.text.isNullOrEmpty() -> {
                    dialogHelper.showAlertMessage(getString(R.string.email_reuired))
                }
                edtPass.text.isNullOrEmpty() -> {
                    dialogHelper.showAlertMessage(getString(R.string.pass_reuired))
                }
                else -> {
                    loadingDialog.showLoadingDialog()
                    BaseApplication.instance?.firebaseAuth?.signInWithEmailAndPassword(
                        edtEmail.text.toString(),
                        edtPass.text.toString()
                    )?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            //Sau khi đăng nhập thành công, lấy thông tin của user theo userName (email)
                            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)
                                ?.child(task.result?.user?.uid!!)
                                ?.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.value != null) {
                                            val userModel: UserModel = Gson().fromJson(
                                                Gson().toJson(snapshot.value),
                                                object : TypeToken<UserModel>() {}.type
                                            )
                                            //Luu lại uid - định danh và thông tin của user
                                            userID = userModel.uid
                                            myInfo = userModel
                                            //Cập nhật FCM token, để nhận thông báo
                                            FirebaseMessaging.getInstance().token.addOnSuccessListener {
                                                BaseApplication.instance?.dataBase?.getReference(
                                                    ConstantKey.usersRefer
                                                )?.child(userModel.uid!!)
                                                    ?.child("fcmToken")?.setValue(it)
                                            }
                                            loadingDialog.hideLoadingDialog()
                                            startActivity(
                                                Intent(
                                                    this@LoginActivity,
                                                    MainActivity::class.java
                                                )
                                            )
                                            finish()
                                        } else {
                                            loadingDialog.hideLoadingDialog()
                                            push(CreateInformationFragment.newInstance(task.result?.user?.uid!!))
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }

                                })
                        } else {
                            loadingDialog.hideLoadingDialog()
                            dialogHelper.showAlertMessage(getString(R.string.incorrect_email_pass))
                        }
                    }
                }
            }
        }
        findViewById<TextView>(R.id.forgetTV).setOnClickListener {
            push(RegisterFragment.newInstance(true))
        }
        findViewById<ImageView>(R.id.showPassImg).apply {
            setOnClick {
                if (edtPass.transformationMethod  == null ) {
                    edtPass.transformationMethod = PasswordTransformationMethod()
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.view))
                } else {
                    edtPass.transformationMethod = null
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.hidden))
                }
            }
        }
    }

    override fun onBackPressed() {
    }
}