package com.tqc.tuvisocial.ui.main.profile.adapter

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.bumptech.glide.Glide
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.helper.Helper
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick
import com.tqc.tuvisocial.ui.main.other.ShowMoreFragment

class ProfileImageAdapter : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_image_profile_layout) {

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: String) {
        Glide.with(holder.itemView.context).load(item).placeholder(R.drawable.gif_loading).into(holder.getView(R.id.imgView) as ImageView)
        //Chỉ hiển thị tối da 3 ảnh và 1 mục xem thêm
        if (holder.adapterPosition == 3) {
            val moreView = holder.getView<TextView>(R.id.moreView)
            moreView.visibility = View.VISIBLE
            moreView.text = "+${this.data.size - 4}"
            moreView.setOnClick {
                (context as BaseActivity).push(ShowMoreFragment.newInstance(this.data as ArrayList<String>))
            }
        } else {
            holder.getView<ImageView>(R.id.imgView).setOnClick {
                Helper.showImageFullScreen(context, item, "", listMedia = data)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (this.data.size > 4) 4 else this.data.size
    }
}