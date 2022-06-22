package com.tqc.tuvisocial.ui.main.profile.setting

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.base.shared.DialogHelper
import com.tqc.tuvisocial.databinding.SettingFragmentBinding
import com.tqc.tuvisocial.helper.Helper.changeLanguage
import com.tqc.tuvisocial.helper.Helper.getLanguage
import com.tqc.tuvisocial.helper.Helper.saveLanguage
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.ui.main.profile.setting.avt.EditAvtFragment
import com.tqc.tuvisocial.ui.main.profile.setting.bg.EditBackgroundFragment
import com.tqc.tuvisocial.ui.main.profile.setting.change_password.ChangePasswordFragment
import com.tqc.tuvisocial.ui.main.profile.setting.help_center.HelpCenterFragment
import com.tqc.tuvisocial.ui.main.profile.setting.info.ChangInfoFragment
import com.tqc.tuvisocial.ui.main.profile.setting.poilicy.PolicyFragment
import com.tqc.tuvisocial.ui.splash_screen.SplashScreenActivity
import java.util.*

class SettingFragment constructor(val userModel: UserModel) : BaseFragment() {

    companion object {
        fun newInstance(userModel: UserModel) = SettingFragment(userModel)
    }

    private lateinit var binding: SettingFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //gán các sự kiện click tương ứng với chức năng
        binding.logOutBtn.setOnClickListener {
            val dialog = DialogHelper(requireActivity())
            dialog.showAlertMessage(getString(R.string.sign_out), onClick = {
                FirebaseAuth.getInstance().signOut()
                myInfo = null
                userID = ""
                FirebaseMessaging.getInstance().deleteToken()
                activity?.startActivity(Intent(requireActivity(), SplashScreenActivity::class.java))
                activity?.finish()
            })
        }
        binding.changeAvtBtn.setOnClickListener {
            push(
                EditAvtFragment.newInstance()
            )
        }
        binding.changeBgBtn.setOnClickListener {
            push(
                EditBackgroundFragment.newInstance()
            )
        }
        binding.changeInfoBtn.setOnClickListener {
            push(
                ChangInfoFragment.newInstance(userModel)
            )
        }
        binding.changePassBtn.setOnClickListener {
            push(
                ChangePasswordFragment.newInstance()
            )
        }
        binding.helpCenterBtn.setOnClickListener {
            push(
                HelpCenterFragment.newInstance()
            )
        }
        binding.backImg.setOnClickListener {
            pop()
        }
        binding.changePrivacyBtn.setOnClickListener {
            push(PolicyFragment.newInstance())
        }
        binding.changeLanguageBtn.setOnClick {
            PopupWindow(context).apply {
                val viewPop = View.inflate(context, R.layout.change_language_setting, null)
                val language = getLanguage()
                viewPop.findViewById<RadioButton>(R.id.endRdB).apply {
                    isChecked = language == Locale.ENGLISH.language
                    setOnCheckedChangeListener { _, b ->
                        if (b) {
                            context?.run {
                                saveLanguage(Locale.ENGLISH.language)
                                changeLanguage(this, )
                                dismiss()
                                startActivity(Intent(context, SplashScreenActivity::class.java))
                                activity?.finish()
                            }
                        }
                    }
                }
                viewPop.findViewById<RadioButton>(R.id.russRdB).apply {
                    isChecked = language == "ru"
                    setOnCheckedChangeListener { _, b ->
                        if (b) {
                            context?.run {
                                saveLanguage(Locale("ru","RU").language)
                                changeLanguage(this)
                                dismiss()
                                startActivity(Intent(context, SplashScreenActivity::class.java))
                                activity?.finish()
                            }
                        }
                    }
                }
                contentView = viewPop
                isOutsideTouchable = true
                isTouchable = true
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                showAsDropDown(binding.changeLanguageBtn)
            }
        }
    }
}