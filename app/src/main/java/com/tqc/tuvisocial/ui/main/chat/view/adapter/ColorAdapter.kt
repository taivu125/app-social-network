package com.tqc.tuvisocial.ui.main.chat.view.adapter

import android.annotation.SuppressLint
import android.view.View
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.model.localModel.ColorModel
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import de.hdodenhof.circleimageview.CircleImageView

class ColorAdapter : BaseQuickAdapter<ColorModel, BaseViewHolder>(R.layout.chat_item_color_dialog_layout) {

    @SuppressLint("NotifyDataSetChanged")
    override fun convert(holder: BaseViewHolder, item: ColorModel) {
        holder.getView<CircleImageView>(R.id.colorImg).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_circle))
            setColorFilter(ContextCompat.getColor(context, item.colorId))
        }
        if (item.isSelected) {
            holder.getView<CircleImageView>(R.id.colorSelected).visibility = View.VISIBLE
        } else {
            holder.getView<CircleImageView>(R.id.colorSelected).visibility = View.GONE
        }
        holder.itemView.setOnClick {
            data.forEach {
                it.isSelected = false
            }
            item.isSelected = !item.isSelected
            notifyDataSetChanged()
        }
    }
}