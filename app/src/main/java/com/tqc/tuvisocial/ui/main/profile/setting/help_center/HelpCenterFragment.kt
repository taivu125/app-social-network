package com.tqc.tuvisocial.ui.main.profile.setting.help_center

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.HelpCenterFragmentBinding
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick

class HelpCenterFragment : BaseFragment() {

    companion object {
        fun newInstance() = HelpCenterFragment()
    }

    private lateinit var binding: HelpCenterFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HelpCenterFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backImg.setOnClick{
            pop()
        }
        binding.sendBtn.setOnClick {
            showMessageDialog(if (binding.desTV.text.isNotEmpty()) {
                getString(R.string.successful)
            } else {
                getString(R.string.describe)
            }, onClick = {
                pop()
            })
        }
    }
}