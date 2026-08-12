package com.helikanonlibsample.admanager

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.helikanonlib.admanager.AdManagerError
import com.helikanonlib.admanager.AdPlatformError
import com.helikanonlib.admanager.AdPlatformShowListener
import com.helikanonlibsample.admanager.databinding.ActivityEmptyBinding

class EmptyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmptyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmptyBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_empty)

        MyApplication.adManager.showBanner(this, binding.bannerContainer, object : AdPlatformShowListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "showBanner platform error >> ${error.message} ${error.platform.name}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("adManager", "showBanner manager error >> ${error.message}")
            }
        })
    }
}
