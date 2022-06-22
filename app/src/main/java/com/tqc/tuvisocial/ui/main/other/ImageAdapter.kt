package com.tqc.tuvisocial.ui.main.other

import android.annotation.SuppressLint
import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.helper.Helper
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick

class ImageAdapter : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_show_more_image_layout) {

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: String) {
        if (item != "--") {
            Glide.with(holder.itemView.context).load(item).placeholder(R.drawable.gif_loading).into(holder.getView(R.id.imgView) as ImageView)
            holder.itemView.setOnClick {
                Helper.showImageFullScreen(context, item, "", listMedia = data)
            }
        } else {
            holder.getView<ImageView>(R.id.imgView).setImageResource(0)
        }
    }

    override fun getItemCount(): Int {
        return this.data.size
    }
}