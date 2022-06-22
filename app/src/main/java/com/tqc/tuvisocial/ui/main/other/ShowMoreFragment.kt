package com.tqc.tuvisocial.ui.main.other

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.ShowMoreFragmentBinding
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick

class ShowMoreFragment(private val data: ArrayList<String>) : BaseFragment() {

    companion object {
        fun newInstance(data: ArrayList<String>) = ShowMoreFragment(data)
    }

    private lateinit var binding: ShowMoreFragmentBinding
    private val adapter = ImageAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ShowMoreFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.showMoreRcv.layoutManager = GridLayoutManager(context, 4)
        binding.showMoreRcv.adapter = adapter

        adapter.setNewInstance(setUpData(data.size))

        binding.backImg.setOnClick {
            pop()
        }
    }

    private fun setUpData(size: Int) : ArrayList<String> {
        if (size % 4 != 0) {
            data.add("--")
            setUpData(data.size)
        }
        return data
    }

}