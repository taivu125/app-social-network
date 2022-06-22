package com.tqc.tuvisocial.ui.main.profile.setting.poilicy

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseApplication
import com.tqc.tuvisocial.base.BaseFragment
import com.tqc.tuvisocial.databinding.PoicilyFragmentBinding
import com.tqc.tuvisocial.helper.ConstantKey
import com.tqc.tuvisocial.model.UserModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.sharedPref.SharedPref.userID

class PolicyFragment : BaseFragment() {

    companion object {
        fun newInstance() = PolicyFragment()
    }

    private lateinit var binding: PoicilyFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PoicilyFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backImg.setOnClick{
            pop()
        }

        //Set mặc định màu đen
        binding.postGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        binding.postFriendImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        binding.postMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        binding.friendGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        binding.friendFrImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        binding.friendMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        binding.infoGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        binding.infoFriendsImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        binding.infoMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))

        BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.get()?.addOnSuccessListener {
            if (it.value != null) {
                val user: UserModel = Gson().fromJson(
                    Gson().toJson(it.value),
                    object : TypeToken<UserModel>() {}.type
                )

                when (user.privateTypePost) {
                    ConstantKey.isPublic -> {
                        binding.postGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                    ConstantKey.isFriend -> {
                        binding.postFriendImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                    ConstantKey.isOnlyMe -> {
                        binding.postMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                }

                when (user.privateTypeFriends) {
                    ConstantKey.isPublic -> {
                        binding.friendGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                    ConstantKey.isFriend -> {
                        binding.friendFrImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                    ConstantKey.isOnlyMe -> {
                        binding.friendMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                }

                when (user.privateTypeInfo) {
                    ConstantKey.isPublic -> {
                        binding.infoGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                    ConstantKey.isFriend -> {
                        binding.infoFriendsImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                    ConstantKey.isOnlyMe -> {
                        binding.infoMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                }
            }
        }

        //Xử lý cụm private post
        binding.postGlobalImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypePost" to ConstantKey.isPublic
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.postGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
            binding.postFriendImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.postMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))

        }
        binding.postFriendImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypePost" to ConstantKey.isFriend
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.postGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.postFriendImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
            binding.postMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        }
        binding.postMeImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypePost" to ConstantKey.isOnlyMe
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.postGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.postFriendImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.postMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
        }

        //Xử lý cụm private information
        binding.infoGlobalImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypeInfo" to ConstantKey.isPublic
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.infoGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
            binding.infoFriendsImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.infoMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))

        }
        binding.infoFriendsImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypeInfo" to ConstantKey.isFriend
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.infoGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.infoFriendsImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
            binding.infoMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        }
        binding.infoMeImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypeInfo" to ConstantKey.isOnlyMe
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.infoGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.infoFriendsImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.infoMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
        }

        //Xử lý cụm private friends
        binding.friendGlobalImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypeFriends" to ConstantKey.isPublic
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.friendGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
            binding.friendFrImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.friendMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))

        }
        binding.friendFrImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypeFriends" to ConstantKey.isFriend
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.friendGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.friendFrImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
            binding.friendMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
        }
        binding.friendMeImg.setOnClick {
            showLoadingDialog()
            BaseApplication.instance?.dataBase?.getReference(ConstantKey.usersRefer)?.child(userID!!)?.updateChildren(
                mapOf(
                    "privateTypeFriends" to ConstantKey.isOnlyMe
                )
            )?.addOnSuccessListener {
                hideLoadingDialog()
            }
            binding.friendGlobalImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.friendFrImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
            binding.friendMeImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
        }
    }

}