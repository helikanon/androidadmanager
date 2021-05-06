package com.helikanonlibsample.admanager

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.helikanonlib.admanager.AdErrorMode
import com.helikanonlib.admanager.AdPlatformShowListener
import com.helikanonlib.admanager.AdPlatformTypeEnum
import com.helikanonlibsample.admanager.databinding.ActivityEmptyBinding

class EmptyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmptyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmptyBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_empty)

        MyApplication.adManager.showBanner(this, binding.bannerContainer, object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                Log.d("adManager", "showBanner>> $errorMode $errorMessage ${adPlatformEnum?.name}")
            }
        })
    }
}