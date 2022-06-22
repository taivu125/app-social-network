package com.tqc.tuvisocial.ui.main.chat.view.adapter

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.tqc.tuvisocial.R
import java.io.File

class ChooseImageAdapter(val list: ArrayList<File>): BaseQuickAdapter<File, BaseViewHolder>(R.layout.item_chow_image_choose_chat_layout, list) {
    override fun convert(holder: BaseViewHolder, item: File) {
        Glide.with(holder.itemView.context).load(item).into(holder.getView(R.id.imgView) as ImageView)
        holder.getView<ImageView>(R.id.deleteImg).visibility = View.GONE
    }
}