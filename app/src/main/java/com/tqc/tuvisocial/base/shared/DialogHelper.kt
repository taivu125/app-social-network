package com.tqc.tuvisocial.base.shared

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.R

class DialogHelper constructor(private val activity: Activity) {

    private var dialog: Dialog? = null
    private var messageDialog: AlertDialog? = null

    init {
        dialog = Dialog(activity)
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.setContentView(View.inflate(activity, R.layout.loading_dialog, null))
    }

    fun showLoadingDialog() {
        try {
            if (dialog?.isShowing == false) {
                object : CountDownTimer(20000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        hideLoadingDialog()
                    }

                }.start()
                dialog?.show()
            }
        } catch (e: Exception) {
        }
    }

    fun hideLoadingDialog() {
        try {
            if (dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        } catch (e: Exception) {
        }
    }

    fun showAlertMessage(
        message: String,
        isHidden: Boolean? = false,
        onClick: (() -> Any?)? = null,
        confirmText: String ? = null
    ) {
        if (messageDialog?.isShowing == true) return
        val view = View.inflate(activity, R.layout.dialog_message, null)
        view.findViewById<TextView>(R.id.btnOke)?.apply {
            text = confirmText ?: context.getString(R.string.yes)
            setOnClickListener {
                messageDialog?.dismiss()
                onClick?.invoke()
            }
        }
        view.findViewById<TextView>(R.id.btnCancel)?.apply {
            if (isHidden == true) {
                visibility = View.GONE
            }
            setOnClickListener {
                messageDialog?.dismiss()
            }
        }
        view.findViewById<TextView>(R.id.messageTV)?.text = message
        messageDialog = AlertDialog.Builder(activity).setView(view).create()
        messageDialog?.setCancelable(false)
        messageDialog?.setCanceledOnTouchOutside(false)
        messageDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

        try {
            if (messageDialog?.isShowing == false) {
                (activity as? BaseActivity)?.runOnUiThread{
                    messageDialog?.show()
                }
            }
        } catch (exp: WindowManager.BadTokenException) {
            exp.printStackTrace()
        }
    }
}