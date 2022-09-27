package com.helikanonlib.admanager

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.helikanonlib.admanager.databinding.AdmanagerLoadingBinding

class AdsLoadingCustomView(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private lateinit var binding: AdmanagerLoadingBinding

    init {
        binding = AdmanagerLoadingBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)

        binding.textViewCloseAdsLoading.setOnClickListener {
            //binding.root.visibility = View.GONE

            (this@AdsLoadingCustomView.parent as ViewGroup).removeView(this@AdsLoadingCustomView)
        }
    }

}