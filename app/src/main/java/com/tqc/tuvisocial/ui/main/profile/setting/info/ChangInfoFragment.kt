package com.tqc.tuvisocial.ui.main.profile.setting.info

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.ChangInfoFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeInMillis
import com.tqc.tuvisocial.sharedPref.SharedPref.userID
import com.tqc.tuvisocial.sharedPref.eventBus.OnRefreshProfile
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.HashMap

class ChangInfoFragment constructor(val userModel: UserModel) : BaseFragment() {

    companion object {
        fun newInstance(userModel: UserModel) = ChangInfoFragment(userModel)
    }

    private lateinit var binding: ChangInfoFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ChangInfoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillInfo()

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
                    binding.birthDayTV.text = "$day/$monthOfYear/$year"
                }.display()

        }
        binding.updateTV.setOnClick{
            val calendar = Calendar.getInstance()
            val day = calendar[Calendar.DAY_OF_MONTH]
            val month = calendar[Calendar.MONTH]
            val year = calendar[Calendar.YEAR]

            if (binding.birthDayTV.text.isNotEmpty() && ("${if (day < 10) "0$day" else day}/$month/$year".toTimeInMillis() - binding.birthDayTV.text.toString().toTimeInMillis()) < 13) {
                showMessageDialog(getString(R.string.mus_old_13))
                return@setOnClick
            }
            showLoadingDialog()
            userModel.fullName = binding.nameTV.text.toString()
            userModel.birthDay = binding.birthDayTV.text.toString()
            userModel.cityName = binding.cityTV.text.toString()
            userModel.education = binding.educationTV.text.toString()
            userModel.relationship = binding.relationshipTV.text.toString()

            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                Gson().fromJson<HashMap<String, Any>>(Gson().toJson(userModel), object : TypeToken<HashMap<String, Any>>() {}.type)
            )?.addOnCompleteListener {
                hideLoadingDialog()
                EventBus.getDefault().post(OnRefreshProfile())
                showMessageDialog(getString(R.string.successful), onClick = {
                    pop()
                })
            }
        }
        binding.backImg.setOnClick {
            pop()
        }
    }

    private fun fillInfo() {
        binding.nameTV.setText(userModel.fullName)
        binding.birthDayTV.text = userModel.birthDay
        binding.cityTV.setText(userModel.cityName)
        binding.educationTV.setText(userModel.education)
        binding.relationshipTV.setText(userModel.relationship)
    }

}