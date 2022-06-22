package com.tqc.tuvisocial.ui.main.chat.view.adapter

import android.annotation.SuppressLint
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick

class ImageMessageAdapter(private val onShowImageFullScreen: ((String, MutableList<String>) -> Unit)) :
    BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_chat_image_layout) {

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: String) {
        holder.itemView.setOnClick {
            onShowImageFullScreen.invoke(item, data)
        }
        Glide.with(holder.itemView.context).load(item).
        placeholder(R.drawable.gif_loading).into(holder.getView(R.id.imgView) as ImageView).onLoadFailed(
            ContextCompat.getDrawable(context, R.drawable.gif_loading)
        )
    }

    override fun getItemCount(): Int {
        return this.data.size
    }
}