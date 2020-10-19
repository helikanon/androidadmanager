package com.helikanonlibsample.admanager

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.helikanonlib.admanager.AdErrorMode
import com.helikanonlib.admanager.AdPlatformShowListener
import com.helikanonlib.admanager.AdPlatformTypeEnum
import kotlinx.android.synthetic.main.activity_main.*

class EmptyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)

        MainActivity.adManager.showBanner(this, bannerContainer, object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                Log.d("adManager", "showBanner>> $errorMode $errorMessage ${adPlatformEnum?.name}")
            }
        })
    }
}