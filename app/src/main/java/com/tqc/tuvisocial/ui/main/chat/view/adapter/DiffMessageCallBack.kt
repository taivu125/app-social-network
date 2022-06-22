package com.tqc.tuvisocial.ui.main.chat.view.adapter

import androidx.recyclerview.widget.DiffUtil
import com.tqc.tuvisocial.model.MessageModel

class DiffMessageCallBack: DiffUtil.ItemCallback<MessageModel>() {
    override fun areItemsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
        return oldItem.id == newItem.id
    }
}