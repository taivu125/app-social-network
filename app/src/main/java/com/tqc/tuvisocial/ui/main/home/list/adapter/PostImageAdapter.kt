package com.tqc.tuvisocial.ui.main.home.list.adapter

import android.annotation.SuppressLint
import android.app.ActionBar
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.helper.Helper
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.ui.main.other.ShowMoreFragment

class PostImageAdapter : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_image_layout) {

    var size = 0

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n", "CheckResult")
    override fun convert(holder: BaseViewHolder, item: String) {
        val imageView = holder.getView(R.id.imgView) as ImageView
        Glide.with(holder.itemView.context).load(item).apply {
            RequestOptions.overrideOf(200,200)
        }.placeholder(R.drawable.gif_loading).into(imageView)
        //Chỉ hiển thị tối da 3 ảnh và 1 mục xem thêm
        if (holder.adapterPosition >= 3 && data.size > 4) {
            val moreView = holder.getView<TextView>(R.id.moreView)
            moreView.visibility = View.VISIBLE
            moreView.text = "+${this.data.size - 4}"
            moreView.setOnClick {
                (context as BaseActivity).push(ShowMoreFragment.newInstance(this.data as ArrayList<String>))
            }
        }

        holder.getView<ImageView>(R.id.imgView).setOnClick {
            Helper.showImageFullScreen(context, item, "", listMedia = data)
        }

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        )
        when {
            holder.adapterPosition == 0 && size > 1-> {
                layoutParams.marginEnd = 4
            }
            holder.adapterPosition == 2 -> {
                layoutParams.marginEnd = 4
                layoutParams.topMargin = 4
            }
            holder.adapterPosition == 3 -> {
                layoutParams.topMargin = 4
            }
            else -> {
                layoutParams.marginEnd = 0
            }
        }
        holder.itemView.layoutParams = layoutParams
    }

    override fun getItemCount(): Int {
        return if (this.data.size > 4) 4 else this.data.size
    }
}