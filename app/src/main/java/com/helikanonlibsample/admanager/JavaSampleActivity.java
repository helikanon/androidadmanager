package com.helikanonlibsample.admanager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.helikanonlib.admanager.AdErrorMode;
import com.helikanonlib.admanager.AdManager;
import com.helikanonlib.admanager.AdPlatformModel;
import com.helikanonlib.admanager.AdPlatformShowListener;
import com.helikanonlib.admanager.AdPlatformTypeEnum;
import com.helikanonlib.admanager.AdPlatformWrapper;
import com.helikanonlib.admanager.adplatforms.AdmobAdWrapper;
import com.helikanonlib.admanager.adplatforms.FacebookAdWrapper;
import com.helikanonlib.admanager.adplatforms.IronSourceAdWrapper;
import com.helikanonlib.admanager.adplatforms.StartAppAdWrapper;

import org.jetbrains.annotations.Nullable;

public class JavaSampleActivity extends AppCompatActivity {

    private AdManager adManager;


    RelativeLayout bannerContainer;
    RelativeLayout mrecContainer;
    Button btnShowInterstitial;
    Button btnShowRewarded;
    Button btnShowInterstitialForTimeStrategy;
    Button btnLoadAndShowInterstitial;
    Button btnLoadAndShowRewarded;

    String oldBannerPlacementId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_sample_activity);

        adManager = MyApplication.adManager;
        //adManager.destroyBannersAndMrecs(this);

        // initAdManager(); // already inited in MainActivity
        initViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        adManager.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        adManager.onResume(this);

        /*AdPlatformWrapper ironsrc = adManager.getAdPlatformByType(AdPlatformTypeEnum.IRONSOURCE).getPlatformInstance();
        oldBannerPlacementId = ironsrc.getBannerPlacementId();
        ironsrc.setBannerPlacementId("new_placementId");*/

        adManager.showBanner(this, bannerContainer);
        adManager.showMrec(this, mrecContainer);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initViews() {

        bannerContainer = findViewById(R.id.bannerContainer);
        mrecContainer = findViewById(R.id.mrecContainer);
        btnShowInterstitial = findViewById(R.id.btnShowInterstitial);
        btnShowRewarded = findViewById(R.id.btnShowRewarded);
        btnShowInterstitialForTimeStrategy = findViewById(R.id.btnShowInterstitialForTimeStrategy);
        btnLoadAndShowInterstitial = findViewById(R.id.btnLoadAndShowInterstitial);
        btnLoadAndShowRewarded = findViewById(R.id.btnLoadAndShowRewarded);

        btnShowInterstitial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adManager.showInterstitial(JavaSampleActivity.this);
            }
        });

        btnShowRewarded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adManager.showRewarded(JavaSampleActivity.this, new AdPlatformShowListener() {
                    @Override
                    public void onRewarded(@Nullable String type, @Nullable Integer amount, @Nullable AdPlatformTypeEnum adPlatformEnum) {
                        Toast.makeText(JavaSampleActivity.this, "Rewarded!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnShowInterstitialForTimeStrategy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adManager.showInterstitial(JavaSampleActivity.this);
            }
        });


        btnLoadAndShowInterstitial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adManager.loadAndShowInterstitial(JavaSampleActivity.this);
            }
        });

        btnLoadAndShowRewarded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adManager.loadAndShowRewarded(JavaSampleActivity.this);
            }
        });
    }

    private void initAdManager() {
        AdPlatformWrapper facebookAdWrapper = new FacebookAdWrapper("your_app_id");
        facebookAdWrapper.setInterstitialPlacementId("YOUR_PLACEMENT_ID");
        facebookAdWrapper.setBannerPlacementId("YOUR_PLACEMENT_ID");
        facebookAdWrapper.setRewardedPlacementId("YOUR_PLACEMENT_ID");
        facebookAdWrapper.setRewardedPlacementId("YOUR_PLACEMENT_ID");


        AdPlatformWrapper admobAdWrapper = new AdmobAdWrapper("ca-app-pub-3940256099942544~3347511713");
        admobAdWrapper.setInterstitialPlacementId("ca-app-pub-3940256099942544/1033173712");
        admobAdWrapper.setBannerPlacementId("ca-app-pub-3940256099942544/6300978111");
        admobAdWrapper.setRewardedPlacementId("ca-app-pub-3940256099942544/5224354917");
        facebookAdWrapper.setRewardedPlacementId("ca-app-pub-3940256099942544/6300978111");

        AdPlatformWrapper startappAdWrapper = new StartAppAdWrapper("207754325");

        AdPlatformWrapper ironsourceAdWrapper = new IronSourceAdWrapper("a1a67f75");
        ironsourceAdWrapper.setInterstitialPlacementId("DefaultInterstitial");
        ironsourceAdWrapper.setBannerPlacementId("DefaultBanner");
        ironsourceAdWrapper.setRewardedPlacementId("DefaultRewardedVideo");
        ironsourceAdWrapper.setMrecPlacementId("MREC_BANNER");


        adManager = new AdManager
                .Builder()
                .autoLoad(true)
                .autoLoadDelay(20)
                .interstitialMinElapsedSecondsToNextShow(60)
                .randomInterval(30)
                .showAds(true)
                .testMode(BuildConfig.DEBUG)
                .deviceId("47088e48-5195-4757-90b2-0da94116befd") // necessary if test mode enabled
                .addAdPlatforms(
                        new AdPlatformModel(facebookAdWrapper, true, true, true, true),
                        new AdPlatformModel(admobAdWrapper, true, false, true, true),
                        new AdPlatformModel(startappAdWrapper, true, true, true, true),
                        new AdPlatformModel(ironsourceAdWrapper, true, true, true, false)
                )
                .build();

        adManager.setGlobalRewardedShowListener(new AdPlatformShowListener() {
            @Override
            public void onRewarded(@Nullable String type, @Nullable Integer amount, @Nullable AdPlatformTypeEnum adPlatformEnum) {
                Toast.makeText(JavaSampleActivity.this, "Rewarded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@Nullable AdErrorMode errorMode, @Nullable String errorMessage, @Nullable AdPlatformTypeEnum adPlatformEnum) {
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[SHOW][REWARDED] AdErrorMode.MANAGER globalRewardedLoadListener > $errorMessage");
                } else {
                    Log.d("adManager", "[SHOW][REWARDED] AdErrorMode.PLATFORM globalRewardedLoadListener > " + errorMessage + " | " + adPlatformEnum.name());
                }
            }
        });

        adManager.initializePlatforms(getApplicationContext());
        adManager.initializePlatformsWithActivity(this);
    }
}