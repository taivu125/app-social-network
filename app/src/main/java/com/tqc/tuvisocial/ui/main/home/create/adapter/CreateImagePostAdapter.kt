package com.tqc.tuvisocial.ui.main.home.create.adapter

import android.annotation.SuppressLint
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import java.io.File

class CreateImagePostAdapter : BaseQuickAdapter<File, BaseViewHolder>(R.layout.item_create_post_image_layout) {

    @SuppressLint("NotifyDataSetChanged")
    fun addNewData(data: ArrayList<File>) {
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun convert(holder: BaseViewHolder, item: File) {
        Glide.with(holder.itemView.context).load(item).into(holder.getView(R.id.imgView) as ImageView)
        holder.getView<ImageView>(R.id.deleteImg).setOnClick{
            data.remove(item)
            notifyDataSetChanged()
        }
    }
}