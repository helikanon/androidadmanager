package com.helikanonlibsample.admanager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.helikanonlib.admanager.AdErrorMode;
import com.helikanonlib.admanager.AdManager;
import com.helikanonlib.admanager.AdPlacementGroupModel;
import com.helikanonlib.admanager.AdPlatformModel;
import com.helikanonlib.admanager.AdPlatformShowListener;
import com.helikanonlib.admanager.AdPlatformTypeEnum;
import com.helikanonlib.admanager.AdPlatformWrapper;
import com.helikanonlib.admanager.adplatforms.AdmobAdWrapper;
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
    private int placementGroupIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_sample_activity);

        adManager = MyApplication.adManager;
        //adManager.destroyBannersAndMrecs(this);
        adManager.loadInterstitial(this, null, null, false, placementGroupIndex);
        adManager.loadRewarded(this, null, null, false, placementGroupIndex);
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

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            adManager.showBanner(this, bannerContainer, null, null, placementGroupIndex);
        }, 2000);


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            adManager.showMrec(this, mrecContainer, null, null, placementGroupIndex);
        }, 5000);


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
                adManager.showInterstitial(JavaSampleActivity.this, null, null, placementGroupIndex);
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
                }, null, placementGroupIndex);
            }
        });

        btnShowInterstitialForTimeStrategy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adManager.showInterstitial(JavaSampleActivity.this, null, null, placementGroupIndex);
            }
        });


        btnLoadAndShowInterstitial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adManager.loadAndShowInterstitial(JavaSampleActivity.this, null, null, placementGroupIndex);
            }
        });

        btnLoadAndShowRewarded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adManager.loadAndShowRewarded(JavaSampleActivity.this, null, null, placementGroupIndex);
            }
        });
    }

    private void initAdManager() {
        /*AdPlatformWrapper facebookAdWrapper = new FacebookAdWrapper("your_app_id");
        facebookAdWrapper.setInterstitialPlacementId("YOUR_PLACEMENT_ID");
        facebookAdWrapper.setBannerPlacementId("YOUR_PLACEMENT_ID");
        facebookAdWrapper.setRewardedPlacementId("YOUR_PLACEMENT_ID");
        facebookAdWrapper.setRewardedPlacementId("YOUR_PLACEMENT_ID");*/


        AdPlatformWrapper admobAdWrapper = new AdmobAdWrapper("ca-app-pub-3940256099942544~3347511713");
        admobAdWrapper.getPlacementGroups().add(new AdPlacementGroupModel(
                "default",
                "ca-app-pub-3940256099942544/1033173712",
                "ca-app-pub-3940256099942544/5224354917",
                "ca-app-pub-3940256099942544/6300978111",
                "ca-app-pub-3940256099942544/6300978111",
                "ca-app-pub-3940256099942544/2247696110",
                "ca-app-pub-3940256099942544/3419835294"
        ));
        /*admobAdWrapper.setInterstitialPlacementId("ca-app-pub-3940256099942544/1033173712");
        admobAdWrapper.setBannerPlacementId("ca-app-pub-3940256099942544/6300978111");
        admobAdWrapper.setRewardedPlacementId("ca-app-pub-3940256099942544/5224354917");
        // facebookAdWrapper.setRewardedPlacementId("ca-app-pub-3940256099942544/6300978111");*/

        AdPlatformWrapper startappAdWrapper = new StartAppAdWrapper("207754325");
        startappAdWrapper.getPlacementGroups().add(new AdPlacementGroupModel(
                "default",
                "DefaultInterstitial",
                "DefaultRewardedVideo",
                "DefaultBanner",
                "MREC_BANNER",
                "DefaultNative",
                ""
        ));

        AdPlatformWrapper ironsourceAdWrapper = new IronSourceAdWrapper("a1a67f75");
        ironsourceAdWrapper.getPlacementGroups().add(new AdPlacementGroupModel(
                "default",
                "DefaultInterstitial",
                "DefaultRewardedVideo",
                "DefaultBanner",
                "MREC_BANNER",
                "",
                ""
        ));

        /*ironsourceAdWrapper.setInterstitialPlacementId("DefaultInterstitial");
        ironsourceAdWrapper.setBannerPlacementId("DefaultBanner");
        ironsourceAdWrapper.setRewardedPlacementId("DefaultRewardedVideo");
        ironsourceAdWrapper.setMrecPlacementId("MREC_BANNER");*/


        adManager = new AdManager();

        adManager.setAutoLoadForInterstitial(false);
        adManager.setAutoLoadDelay(20);
        adManager.setInterstitialMinElapsedSecondsToNextShow(60);
        adManager.setRandomInterval(30);
        adManager.setShowAds(true);
        adManager.setTestMode(BuildConfig.DEBUG);
        adManager.setDeviceId("47088e48-5195-4757-90b2-0da94116befd"); // necessary if test mode enabled
        adManager.addAdPlatform(new AdPlatformModel(admobAdWrapper, true, false, true, true));
        adManager.addAdPlatform(new AdPlatformModel(startappAdWrapper, true, true, true, true));
        adManager.addAdPlatform(new AdPlatformModel(ironsourceAdWrapper, true, true, true, false));



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