package com.tqc.tuvisocial.base

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction


abstract class BaseActivity : AppCompatActivity() {

    abstract fun getContainerId(): Int

    fun push(fragment: BaseFragment) {
        if (supportFragmentManager.isStateSaved) return
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(getContainerId(), fragment, fragment::class.simpleName)
        transaction.addToBackStack(fragment::class.java.name)
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction.commitAllowingStateLoss()
    }

    fun replace(fragment: BaseFragment) {
        if (supportFragmentManager.isStateSaved) return
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(getContainerId(), fragment, fragment::class.simpleName)
        transaction.addToBackStack(fragment::class.java.name)
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction.commitAllowingStateLoss()
    }

    fun pop() {
        supportFragmentManager.popBackStack()
    }
}