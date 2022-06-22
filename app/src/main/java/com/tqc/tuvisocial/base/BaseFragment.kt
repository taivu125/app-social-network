package com.tqc.tuvisocial.base

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.shared.DialogHelper
import com.tqc.tuvisocial.helper.Helper
import com.tqc.tuvisocial.ui.account.login.LoginActivity
import net.alhazmy13.mediapicker.Video.VideoPicker
import pl.aprilapps.easyphotopicker.EasyImage

open class BaseFragment : Fragment() {

    var loadingDialogHelper: DialogHelper? = null
    var easyImage: EasyImage? = null
    var isBackPress: Boolean? = false

    open fun onBackPress() {}

    fun showLoadingDialog() {
        loadingDialogHelper?.showLoadingDialog()
    }

    fun hideLoadingDialog() {
        loadingDialogHelper?.hideLoadingDialog()
    }

    fun showMessageDialog(message: String, isHidden: Boolean ? = false, onClick: (() -> Unit)? = null) {
        loadingDialogHelper?.showAlertMessage(message, isHidden, onClick )
    }

    fun showError() = showMessageDialog("Have a error \n Please try again")

    fun push(fragment : BaseFragment) {
        this.view?.clearFocus()
        val activity = activity as? BaseActivity ?: return
        activity.push(fragment)
    }

    fun replace(fragment : BaseFragment) {
        this.view?.clearFocus()
        val activity = activity as? BaseActivity ?: return
        activity.replace(fragment)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialogHelper = DialogHelper(requireActivity())
    }

    fun pop() {
        this.view?.clearFocus()
        val activity = activity as? BaseActivity ?: return
        activity.pop()
    }

    fun showImagePicker() {
        context?.run {
            val dialog = Dialog(this)
            val view = View.inflate(this, R.layout.dialog_choose_image, null)
            view.findViewById<CardView>(R.id.imageGallery).setOnClickListener {
                dialog.dismiss()
                easyImage?.openGallery(this@BaseFragment)

            }
            view.findViewById<CardView>(R.id.imageCamera).setOnClickListener {
                dialog.dismiss()
                easyImage?.openCameraForImage(this@BaseFragment)
            }
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setContentView(view)
            dialog.show()
        }

    }

    override fun onResume() {
        super.onResume()
        //Kiểm tra xem  tài khoản con đăng nhập hay không
        if (activity !is LoginActivity) {
            Helper.checkSession()
        }
    }
}