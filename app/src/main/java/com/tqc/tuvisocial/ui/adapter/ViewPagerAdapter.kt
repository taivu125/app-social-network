package com.tqc.tuvisocial.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var mListData : ArrayList<Fragment> = ArrayList()

    override fun getItem(position: Int): Fragment {
        return mListData[position]
    }

    override fun getCount(): Int {
        return mListData.size
    }

    fun addData(fragment: Fragment) {
        mListData.add(fragment)
    }

}