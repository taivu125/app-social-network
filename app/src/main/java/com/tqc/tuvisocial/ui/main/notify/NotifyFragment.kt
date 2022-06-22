package com.tqc.tuvisocial.ui.main.notify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.databinding.NotifyFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.NotifyModel
import com.tqc.tuvisocial.sharedPref.Extensions.toTimeDashInMillis
import com.tqc.tuvisocial.sharedPref.SharedPref.myInfo
import com.tqc.tuvisocial.ui.main.notify.adapter.NotifyAdapter
import java.lang.NullPointerException

class NotifyFragment : Fragment() {

    companion object {
        fun newInstance() = NotifyFragment()
    }

    private lateinit var binding: NotifyFragmentBinding
    private val notifyAdapter = NotifyAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NotifyFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.notifyRcv.layoutManager = LinearLayoutManager(requireContext())
        binding.notifyRcv.adapter = notifyAdapter

        //Lấy thông tin notify
        try {
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.notify)?.child(myInfo?.uid!!)?.addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value != null) {
                            val list = ArrayList<NotifyModel>()
                            for((_, value) in snapshot.value as HashMap<*, *>) {
                                list.add(Gson().fromJson(Gson().toJson(value), object : TypeToken<NotifyModel>() {}.type))
                            }
                            list.sortByDescending { it.createDate.toTimeDashInMillis() }
                            notifyAdapter.setNewInstance(list)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
            )
        } catch (ex: NullPointerException) {}
    }
}