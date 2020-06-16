package com.helikanonlibsample.admanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.helikanonlib.admanager.AdErrorMode
import com.helikanonlib.admanager.AdPlatformShowListener
import com.helikanonlibsample.admanager.MainActivity.Companion.adManager
import kotlinx.android.synthetic.main.activity_main.*

class EmptyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)



        MainActivity.adManager.showBanner(bannerContainer, object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                Log.d("adManager", "showBanner>> $errorMode $errorMessage")
            }
        })
    }
}