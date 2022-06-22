package com.tqc.tuvisocial.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {

    fun getResponse(call : Deferred<Any> ,onSuccess : ((onSuccess: Any) -> Unit), onFailure : ((onFailure: Any) -> Unit)) {
        GlobalScope.launch(Dispatchers.Main) {
            call.await().run {

            }
        }
    }
}