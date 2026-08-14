# Helikanon AdManager kullanım rehberi

Helikanon AdManager; AdMob, AppLovin MAX ve Unity Ads reklamlarını tek API üzerinden yönetmek için kullanılan Android kütüphanesidir. Interstitial, Rewarded, Banner, MREC, Native ve App Open reklam akışlarını destekler.

Bu doküman iki amaçla yazılmıştır:

- Android geliştiricisinin kütüphaneyi güvenli biçimde kurup kullanabilmesi.
- Kod üreten bir yapay zekâ modelinin doğru sınıfları, parametreleri ve callback sözleşmelerini anlayabilmesi.

> Paket adı: `com.helikanonlib.admanager`

## Gereksinimler

- Minimum Android SDK: 24
- Kütüphanenin compile SDK sürümü: 36
- Java/Kotlin JVM target: 17
- AndroidX etkin olmalıdır.

Kütüphane şu platform wrapper'larını aktif olarak içerir:

- `AdmobAdWrapper`
- `ApplovinAdWrapper`
- `UnityAdsAdWrapper`

App Open reklamlarının varsayılan implementasyonu AdMob ve AppLovin'i destekler.

## Kurulum

### Aynı proje içindeki modülü kullanma

`settings.gradle.kts`:

```kotlin
include(":admanager")
```

Uygulama modülünün `build.gradle.kts` dosyası:

```kotlin
dependencies {
    implementation(project(":admanager"))
}
```

### Maven artifact kullanma

Artifact internal Maven repository'ye publish edildiyse:

```kotlin
dependencies {
    implementation("com.helikanonlib:admanager:5.100")
}
```

Bu kullanımda artifact'ın yayınlandığı Maven repository ayrıca `repositories` listesine eklenmelidir.

### Gerekli repository'ler

Kullanılan mediation adapter'larına göre aşağıdaki repository'ler gerekebilir:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        maven("https://artifact.bytedance.com/repository/pangle")
    }
}
```

### AdMob App ID

Kütüphane manifest'i `admobAppId` placeholder'ını kullanır. Uygulama modülünde tanımlayın:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["admobAppId"] = "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"
    }
}
```

Debug ortamında Google'ın resmi test App ID'si kullanılabilir:

```text
ca-app-pub-3940256099942544~3347511713
```

Kütüphane manifest üzerinden şu izinleri ekler:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `AD_ID`

## Temel kavramlar

### Placement group

Placement group, aynı reklam formatı için farklı ekran veya kullanım senaryolarına ait placement ID'lerini gruplar.

Örnek:

```text
Manager group 0: default
Manager group 1: onboarding
```

Her platform wrapper'ında aynı group'lar aynı sırada tanımlanmalıdır:

```text
AdManager:  [default, onboarding]
AdMob:      [default, onboarding]
AppLovin:   [default, onboarding]
Unity Ads:  [default, onboarding]
```

Aşağıdaki konfigürasyon geçersizdir:

```text
AdManager: [default, onboarding]
AdMob:     [onboarding, default]
```

Kütüphane geçersiz index, eksik group veya isim/sıra uyuşmazlığında açıklayıcı `IllegalArgumentException` üretir.

Group index'ini isimden almak için:

```kotlin
val onboardingGroupIndex =
    adManager.getPlacementGroupIndexByName("onboarding")
```

Birden fazla group kullanılıyorsa her wrapper'a her group için ayrı `AdPlacementGroupModel` ekleyin. Bütün reklam çağrılarında doğru `placementGroupIndex` değerini açıkça gönderin. Otomatik preload yalnızca index `0` için çalışır.

### `AdPlatformModel` format flag'leri

Bir wrapper, `AdPlatformModel` içine eklenir. Boolean flag'ler platformun hangi format sıralamalarında kullanılacağını belirler:

```kotlin
AdPlatformModel(
    platformInstance = wrapper,
    showInterstitial = true,
    showBanner = true,
    showRewarded = true,
    showMrec = true,
    showNative = false
)
```

Constructor çağrılarında positional boolean yerine her zaman named argument kullanın.

## Önerilen tam kurulum

`AdManager` genellikle `Application` içinde bir kez oluşturulur.

```kotlin
import android.app.Application
import android.util.Log
import com.helikanonlib.admanager.*
import com.helikanonlib.admanager.adplatforms.AdmobAdWrapper
import com.helikanonlib.admanager.adplatforms.ApplovinAdWrapper
import com.helikanonlib.admanager.adplatforms.UnityAdsAdWrapper

class MyApplication : Application() {

    lateinit var adManager: AdManager
        private set

    override fun onCreate() {
        super.onCreate()

        adManager = createAdManager()

        adManager.initializePlatforms(
            context = applicationContext,
            onInitializeComplete = { result ->
                when (result.status) {
                    AdInitializationStatus.SUCCESS,
                    AdInitializationStatus.PARTIAL_SUCCESS -> {
                        // Activity hazır olduğunda ihtiyaç duyulan reklamlar açıkça yüklenmelidir.
                    }

                    AdInitializationStatus.FAILURE -> {
                        Log.e("Ads", "No platform initialized: ${result.message}")
                    }

                    AdInitializationStatus.DISABLED -> {
                        Log.d("Ads", "Ads are disabled")
                    }
                }
            },
            onPlatformInitializeComplete = { result ->
                Log.d("Ads", "${result.platform}: ${result.isSuccessful}")
            }
        )
    }

    private fun createAdManager(): AdManager {
        val groupName = "default"

        val admob = AdmobAdWrapper(
            appId = "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"
        ).apply {
            placementGroups.add(
                AdPlacementGroupModel(
                    groupName = groupName,
                    interstitial = "ADMOB_INTERSTITIAL_ID",
                    rewarded = "ADMOB_REWARDED_ID",
                    banner = "ADMOB_BANNER_ID",
                    mrec = "ADMOB_MREC_ID",
                    native = "ADMOB_NATIVE_ID",
                    nativeMedium = "ADMOB_NATIVE_MEDIUM_ID",
                    appOpenAd = "ADMOB_APP_OPEN_ID"
                )
            )
        }

        val applovin = ApplovinAdWrapper(
            appId = "APPLOVIN_SDK_KEY"
        ).apply {
            placementGroups.add(
                AdPlacementGroupModel(
                    groupName = groupName,
                    interstitial = "APPLOVIN_INTERSTITIAL_ID",
                    rewarded = "APPLOVIN_REWARDED_ID",
                    banner = "APPLOVIN_BANNER_ID",
                    mrec = "APPLOVIN_MREC_ID",
                    native = "APPLOVIN_NATIVE_ID",
                    nativeMedium = "APPLOVIN_NATIVE_MEDIUM_ID",
                    appOpenAd = "APPLOVIN_APP_OPEN_ID"
                )
            )
        }

        val unity = UnityAdsAdWrapper(
            appId = "UNITY_GAME_ID"
        ).apply {
            placementGroups.add(
                AdPlacementGroupModel(
                    groupName = groupName,
                    interstitial = "Interstitial_Android",
                    rewarded = "Rewarded_Android",
                    banner = "Banner_Android",
                    mrec = "BannerMrec_Android"
                )
            )
        }

        return AdManager().apply {
            showAds = true
            testMode = BuildConfig.DEBUG
            deviceId = "TEST_DEVICE_ID"

            autoLoadForInterstitial = true
            autoLoadForRewarded = true
            autoLoadDelay = 10L

            interstitialMinElapsedSecondsToNextShow = 40
            rewardedMinElapsedSecondsToNextShow = 40
            randomInterval = 10

            isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode = false
            isEnableShowLoadingViewForInterstitial = true

            setPlacementGroups(listOf(groupName))
            setAdPlatforms(
                listOf(
                    AdPlatformModel(
                        platformInstance = admob,
                        showInterstitial = true,
                        showBanner = true,
                        showRewarded = true,
                        showMrec = true,
                        showNative = true
                    ),
                    AdPlatformModel(
                        platformInstance = applovin,
                        showInterstitial = true,
                        showBanner = true,
                        showRewarded = true,
                        showMrec = true,
                        showNative = true
                    ),
                    AdPlatformModel(
                        platformInstance = unity,
                        showInterstitial = true,
                        showBanner = true,
                        showRewarded = true,
                        showMrec = true,
                        showNative = false
                    )
                )
            )
        }
    }
}
```

Süre değerleri negatif olamaz. Negatif değer verilirse `IllegalArgumentException` üretilir.

Başlıca konfigürasyon alanları:

| Alan | Varsayılan | Açıklama |
|---|---:|---|
| `showAds` | `true` | Bütün ana reklam işlemlerini açar veya kapatır. |
| `testMode` | `false` | Platform wrapper'larını test modunda initialize eder. |
| `deviceId` | Boş string | Test cihazı kimliği. |
| `autoLoadForInterstitial` | `false` | Yüklenmiş Interstitial reklamı gösterme ve kapanış/hata sonrası reload davranışını açar. İlk yükleme açıkça yapılır. |
| `autoLoadForRewarded` | `true` | Yüklenmiş Rewarded reklamı gösterme ve kapanış/hata sonrası reload davranışını açar. İlk yükleme açıkça yapılır. |
| `autoLoadDelay` | `10` saniye | Kapanış/hata sonrası autoload reload işleminin gecikmesi. |
| `randomInterval` | `40` saniye | Interstitial zaman stratejisine eklenen random aralık. |
| `interstitialMinElapsedSecondsToNextShow` | `40` saniye | Interstitial zaman stratejisinin minimum süresi. |
| `rewardedMinElapsedSecondsToNextShow` | `40` saniye | Rewarded için saklanan minimum gösterim süresi ayarı. |
| `isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode` | `false` | Autoload gösterim hatasında isteğe bağlı load-and-show fallback davranışını açar. |
| `isEnableShowLoadingViewForInterstitial` | `true` | Interstitial load-and-show sırasında kütüphane loading view'ini açar. |

## Platform initialization

Önerilen initialization API'si:

```kotlin
adManager.initializePlatforms(
    context = applicationContext,
    onInitializeComplete = { result ->
        Log.d("Ads", "Status=${result.status}")
        Log.d("Ads", "Ready=${result.initializedPlatforms}")
        Log.d("Ads", "Failed=${result.failedPlatforms}")
    },
    onPlatformInitializeComplete = { platformResult ->
        Log.d(
            "Ads",
            "${platformResult.platform} success=${platformResult.isSuccessful}"
        )
    }
)
```

`AdInitializationStatus` değerleri:

| Değer | Anlamı |
|---|---|
| `SUCCESS` | Bütün platformlar initialize oldu. |
| `PARTIAL_SUCCESS` | En az bir platform başarılı, en az bir platform başarısız oldu. |
| `FAILURE` | Hiçbir platform initialize olmadı veya platform tanımlanmadı. |
| `DISABLED` | `showAds=false`; initialization yapılmadı. |

Initialization tamamlandıktan ve bir `Activity` hazır olduktan sonra ihtiyaç duyduğunuz reklamları açıkça yükleyin:

```kotlin
adManager.loadInterstitial(activity, placementGroupIndex = 0)
adManager.loadRewarded(activity, placementGroupIndex = 0)
```

`AdManager` başlangıçta otomatik preload yapmaz. Yalnızca kullanacağınız formatları ve placement group'ları yükleyerek gereksiz reklam isteklerini önleyebilirsiniz.

## Platform sırasını belirleme

Sıralama, soldan sağa öncelik sırasıdır:

```text
applovin,admob
```

önce AppLovin'i, ardından AdMob'u dener.

### Typed API

```kotlin
val result = adManager.setAdPlatformOrder(
    0,
    AdFormatEnum.INTERSTITIAL,
    AdPlatformTypeEnum.APPLOVIN,
    AdPlatformTypeEnum.ADMOB
)

if (!result.isSuccessful) {
    Log.e("Ads", result.message.orEmpty())
}
```

### Firebase Remote Config string API'si

Firebase'den gelen format ve platform sırası string olarak doğrudan doğrulanabilir:

```kotlin
val result = adManager.setAdPlatformSortByAdFormatStr(
    placementGroupIndex = 0,
    adFormatName = firebaseFormat,       // Örnek: "interstitial"
    adPlatformsStr = firebaseOrder       // Örnek: "applovin,admob"
)

if (!result.isSuccessful) {
    // Hatalı Remote Config değerini raporla.
    Log.e("AdsRemoteConfig", result.message.orEmpty())
}
```

String API şu kontrolleri yapar:

- Reklam formatı biliniyor mu?
- Platform listesinde boş değer var mı?
- Bilinmeyen platform var mı?
- Aynı platform tekrar edilmiş mi?
- Platform `AdManager` içine eklenmiş mi?
- Platform ilgili reklam formatı için etkin mi?
- Placement group konfigürasyonu bütün wrapper'larla uyumlu mu?

Doğrulama başarısız olursa mevcut/default sıralama değiştirilmez.

Desteklenen sıralama formatları:

- `INTERSTITIAL`
- `BANNER`
- `REWARDED`
- `MREC`
- `NATIVE`
- `NATIVE_MEDIUM`

## Listener sözleşmesi

### Load listener

```kotlin
val loadListener = object : AdPlatformLoadListener() {
    override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
        // Bir platform reklamı başarıyla yükledi.
    }

    override fun onPlatformError(error: AdPlatformError) {
        // Tek bir reklam platformunun gönderdiği hata.
    }

    override fun onError(error: AdManagerError) {
        // AdManager seviyesindeki terminal sonuç.
        // Örnek: bütün platformlar denendi ve hiçbiri yükleyemedi.
    }
}
```

### Show listener

```kotlin
val showListener = object : AdPlatformShowListener() {
    override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {}
    override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {}
    override fun onClosed(adPlatformEnum: AdPlatformTypeEnum) {}

    override fun onRewarded(
        type: String?,
        amount: Int?,
        adPlatformEnum: AdPlatformTypeEnum
    ) {
        // Rewarded ödülünü burada verin.
    }

    override fun onPlatformError(error: AdPlatformError) {
        // Platformun kendi gösterim hatası.
    }

    override fun onError(error: AdManagerError) {
        // Manager seviyesindeki terminal gösterim sonucu.
    }
}
```

### Platform hatası ve manager hatası arasındaki fark

`onPlatformError` bir SDK/platform denemesinin neden başarısız olduğunu bildirir. `onError`, operasyonun AdManager tarafından tamamlanamadığını bildirir.

Örnek akış:

```text
AppLovin başarısız -> onPlatformError(APPLOVIN)
AdMob başarısız   -> onPlatformError(ADMOB)
Platform kalmadı  -> onError(AdManagerError)
```

`AdManagerError.platformErrors`, operasyon boyunca toplanan platform hatalarını içerir.

Global ve metoda gönderilen local listener birlikte çalışır. Aynı listener nesnesi hem global hem local olarak verilirse callback yalnızca bir kez gönderilir.

> Ana `AdManager` listener callback'lerinin geldiği thread reklam SDK'sına göre değişebilir. Callback içinde UI güncellenecekse main thread'e geçin. `AppOpenAdManager` callback'leri main thread üzerinde normalize eder.

### Global listener'lar

```kotlin
adManager.globalInterstitialLoadListener = loadListener
adManager.globalRewardedLoadListener = loadListener
adManager.globalInterstitialShowListener = showListener
adManager.globalRewardedShowListener = showListener
```

Global listener genellikle `Application` içinde bir kez atanır. Metotlara ayrıca local listener verilebilir.

## `showAds=false` davranışı

`showAds=false` olduğunda load/show metotları sessizce beklemez. İlgili listener'a `AdManagerError` gönderir:

```text
Ads are disabled because showAds is false
```

Boolean state/show metotları `false`, Native reklam sayımı `0` döndürür. `initializePlatforms` sonucu `DISABLED` olur.

## Interstitial reklamlar

### Autoload kullanımı

```kotlin
adManager.autoLoadForInterstitial = true
adManager.loadInterstitial(activity, placementGroupIndex = 0)

adManager.showInterstitial(
    activity = activity,
    shownWhere = "home_continue",
    listener = showListener,
    placementGroupIndex = 0
)
```

İlk reklam açıkça yüklenmelidir. Autoload açıkken reklam kapandıktan veya gösterim hatası oluştuktan sonra `autoLoadDelay` kadar beklenerek placement group `0` için sonraki reklam yüklenir.

### Autoload kapalıyken

```kotlin
adManager.autoLoadForInterstitial = false

adManager.showInterstitial(
    activity = activity,
    shownWhere = "editor_export",
    listener = showListener,
    placementGroupIndex = 0
)
```

Autoload kapalıysa `showInterstitial` yükleme ve ardından gösterme akışını başlatır.

Doğrudan yükle ve göster:

```kotlin
adManager.loadAndShowInterstitial(
    activity = activity,
    shownWhere = "editor_export",
    listener = showListener,
    placementGroupIndex = 0
)
```

Sadece yükleme:

```kotlin
adManager.loadInterstitial(
    activity = activity,
    listener = loadListener,
    parallel = false,
    placementGroupIndex = 0
)
```

- `parallel=false`: Platformları sırasıyla dener, ilk başarıda durur.
- `parallel=true`: Hazır olmayan bütün uygun platformlarda load başlatır.

Yüklü reklam kontrolü:

```kotlin
val loaded = adManager.hasLoadedInterstitial(
    platform = null,
    placementGroupIndex = 0
)
```

### Zaman stratejisi

```kotlin
when (
    adManager.showInterstitialForTimeStrategy(
        activity = activity,
        shownWhere = "screen_open",
        listener = showListener,
        placementGroupIndex = 0
    )
) {
    AdShowRequestResult.SHOW_REQUESTED -> {
        // Zaman kontrolü geçti; asenkron gösterim akışı başlatıldı.
    }

    AdShowRequestResult.THROTTLED -> {
        // Minimum + random bekleme süresi henüz dolmadı.
    }

    AdShowRequestResult.DISABLED -> {
        // showAds=false.
    }
}
```

`SHOW_REQUESTED`, reklamın kesin gösterildiği anlamına gelmez. Gerçek sonuç `onDisplayed` veya `onError` üzerinden alınır.

Bekleme eşiği her çağrıda şu şekilde hesaplanır:

```text
interstitialMinElapsedSecondsToNextShow + random(0..randomInterval)
```

## Rewarded reklamlar

```kotlin
adManager.autoLoadForRewarded = true
adManager.loadRewarded(activity, placementGroupIndex = 0)

adManager.showRewarded(
    activity = activity,
    listener = object : AdPlatformShowListener() {
        override fun onRewarded(
            type: String?,
            amount: Int?,
            adPlatformEnum: AdPlatformTypeEnum
        ) {
            grantReward()
        }

        override fun onError(error: AdManagerError) {
            showRewardUnavailableMessage()
        }
    },
    placementGroupIndex = 0
)
```

İlk reklam açıkça yüklenmelidir. Autoload açıkken reklam kapandıktan veya gösterim hatası oluştuktan sonra `autoLoadDelay` kadar beklenerek placement group `0` için sonraki reklam yüklenir.

Autoload kapalıysa `showRewarded` load-and-show akışını başlatır. Doğrudan kullanılabilecek diğer metotlar:

```kotlin
adManager.loadRewarded(activity, loadListener, placementGroupIndex = 0)
adManager.loadAndShowRewarded(activity, showListener, placementGroupIndex = 0)
adManager.hasLoadedRewarded(placementGroupIndex = 0)
```

Ödülü yalnızca `onRewarded` callback'i geldiğinde verin. `onDisplayed` ödül kazanıldığı anlamına gelmez.

## Banner reklamlar

Container tipi `RelativeLayout` olmalıdır:

```xml
<RelativeLayout
    android:id="@+id/bannerContainer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
adManager.showBanner(
    activity = activity,
    containerView = binding.bannerContainer,
    listener = showListener,
    placementGroupIndex = 0
)
```

Manager sıralamadaki ilk uygun platformu dener; platform hatası alırsa sıradaki platforma geçer.

## MREC reklamlar

MREC container tipi de `RelativeLayout` olmalıdır:

```kotlin
adManager.showMrec(
    activity = activity,
    containerView = binding.mrecContainer,
    listener = showListener,
    placementGroupIndex = 0
)
```

## Native reklamlar

Küçük Native reklam yükleme:

```kotlin
adManager.loadSmallNativeAds(
    activity = activity,
    count = 3,
    listener = loadListener,
    placementGroupIndex = 0
)
```

Orta Native reklam yükleme:

```kotlin
adManager.loadMediumNativeAds(
    activity = activity,
    count = 3,
    listener = loadListener,
    placementGroupIndex = 0
)
```

Genel API:

```kotlin
adManager.loadNativeAds(
    activity = activity,
    nativeAdFormat = AdFormatEnum.NATIVE,
    count = 3,
    listener = loadListener,
    placementGroupIndex = 0
)

val shown = adManager.showNative(
    activity = activity,
    nativeAdFormat = AdFormatEnum.NATIVE,
    containerView = binding.nativeContainer,
    listener = showListener,
    placementGroupIndex = 0
)
```

`showNative` dönüşündeki `true`, manager'ın pool içinde yüklü reklam bulup wrapper'a gösterim isteği gönderdiğini belirtir; görselin kesin render edildiği anlamına gelmez. Kesin gösterim için listener sonucunu takip edin.

Pool kontrolü:

```kotlin
val hasAd = adManager.hasLoadedNativeAds(
    activity,
    AdFormatEnum.NATIVE,
    placementGroupIndex = 0
)

val count = adManager.getLoadedNativeAdsCount(
    activity,
    AdFormatEnum.NATIVE,
    placementGroupIndex = 0
)
```

## Belirli bir platformu kullanma

Normalde `platform=null` bırakılır ve tanımlanan sıra kullanılır. Belirli bir platform zorlanacaksa:

```kotlin
val admob = adManager.getAdPlatformByType(AdPlatformTypeEnum.ADMOB)

adManager.showInterstitial(
    activity = activity,
    listener = showListener,
    platform = admob,
    placementGroupIndex = 0
)
```

Platform bulunamazsa `getAdPlatformByType` null döner.

## App Open reklamları

App Open reklamları ana `AdManager` platform initialization'ı tamamlandıktan sonra yüklenmelidir.

```kotlin
val appOpenAdManager = AppOpenAdManager(
    application = this,
    placements = mapOf(
        AdPlatformTypeEnum.ADMOB to "ADMOB_APP_OPEN_ID",
        AdPlatformTypeEnum.APPLOVIN to "APPLOVIN_APP_OPEN_ID"
    ),
    showOrderStr = "applovin,admob",
    globalShowListener = showListener,
    globalLoadListener = loadListener
)

appOpenAdManager.minElapsedSecondsToNextShow = 30
appOpenAdManager.minElapsedSecondsAfterFullScreenAd = 30
appOpenAdManager.excludedActivities.add(PaymentActivity::class.java.simpleName)
```

Initialization sonucundan sonra:

```kotlin
if (
    initializationResult.status == AdInitializationStatus.SUCCESS ||
    initializationResult.status == AdInitializationStatus.PARTIAL_SUCCESS
) {
    appOpenAdManager.load()
}
```

Manuel gösterim:

```kotlin
val result = appOpenAdManager.show(activity)
```

Interval ve excluded activity kontrolleriyle gösterim:

```kotlin
val result = appOpenAdManager.showIntervalElapsed(activity)
```

`show(activity)`, App Open'ın kendi gösterim intervalini ve excluded activity kontrolünü uygulamaz; fullscreen reklam sonrası cooldown kontrolü yine uygulanır. Uygulama foreground olduğunda otomatik gösterim için `showIntervalElapsed(activity)` kullanın.

Her iki gösterim metodu da interstitial veya rewarded reklamın üzerinde çalışan Google Mobile Ads ve desteklenen mediation SDK'larının fullscreen Activity'lerini güvenlik amacıyla reddeder. Bu durumda sonuç `FULL_SCREEN_AD_ACTIVE` olur. Reklam kapandıktan sonra `minElapsedSecondsAfterFullScreenAd` (varsayılan 5 saniye) dolana kadar App Open gösterilmez ve `FULL_SCREEN_AD_INTERVAL_NOT_ELAPSED` döner.

`AppOpenAdShowResult` değerleri:

| Değer | Anlamı |
|---|---|
| `SHOW_REQUESTED` | SDK gösterim isteği başlatıldı. Gerçek sonuç listener'dan gelir. |
| `QUEUED_ON_MAIN_THREAD` | Çağrı background thread'den geldi; main thread'e kuyruğa alındı. |
| `DISABLED` | App Open manager devre dışı. |
| `SHOWING_PAUSED` | Yalnızca gösterimler duraklatılmış. |
| `FULL_SCREEN_AD_ACTIVE` | Interstitial veya Rewarded gibi başka bir fullscreen reklam aktif. |
| `FULL_SCREEN_AD_INTERVAL_NOT_ELAPSED` | Son Interstitial veya Rewarded kapanışından sonraki minimum süre dolmadı. |
| `ALREADY_SHOWING` | App Open reklamı zaten gösteriliyor. |
| `INVALID_ACTIVITY` | Activity finishing veya destroyed. |
| `ACTIVITY_EXCLUDED` | Activity exclude listesinde. |
| `INTERVAL_NOT_ELAPSED` | Minimum gösterim süresi dolmadı. |
| `AD_NOT_READY` | Hazır reklam yok; manager load isteği başlattı. |
| `NO_CONFIGURED_PLATFORM` | Gösterim sırasında kullanılabilir App Open placement yok. |
| `REQUEST_FAILED` | SDK gösterim isteğini başlatamadı. |

Kontrol metotları:

```kotlin
appOpenAdManager.disable()       // Load ve show tamamen kapanır.
appOpenAdManager.enable()        // Manager yeniden açılır; otomatik load yapmaz.
appOpenAdManager.pauseShowing()  // Load açık kalır, yalnızca show engellenir.
appOpenAdManager.resumeShowing()
```

App Open foreground lifecycle örneği için [AppOpenLifecycleController.kt](app/src/main/java/com/helikanonlibsample/admanager/AppOpenLifecycleController.kt) dosyasına bakın.

Bu controller örneğini kullanmak için uygulama modülünde process lifecycle dependency'si bulunmalıdır:

```kotlin
dependencies {
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
}
```

Ana `AdManager`, fullscreen Interstitial/Rewarded gösterirken App Open reklamının bunun üzerine açılmasını engeller.

## Activity ve manager yaşam döngüsü

Activity lifecycle event'lerini wrapper'lara iletmek gerekiyorsa:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    adManager.onCreate(this)
}

override fun onResume() {
    super.onResume()
    adManager.onResume(this)
}

override fun onPause() {
    adManager.onPause(this)
    super.onPause()
}

override fun onStop() {
    adManager.onStop(this)
    super.onStop()
}

override fun onDestroy() {
    adManager.onDestroyActivity(this)
    super.onDestroy()
}
```

Banner ve MREC kaynaklarını açıkça temizlemek için:

```kotlin
adManager.destroyBannersAndMrecs(activity)
```

Manager artık hiç kullanılmayacaksa:

```kotlin
adManager.destroy(activity)
```

`destroy(activity)` bütün platformları, autoload handler'larını, loading view'i ve fullscreen display lease'lerini temizleyen terminal cleanup işlemidir. Activity her kapandığında application-scope manager üzerinde çağrılmamalıdır; bunun yerine `onDestroyActivity(activity)` kullanın.

## Test mode

```kotlin
adManager.testMode = BuildConfig.DEBUG
adManager.deviceId = "TEST_DEVICE_ID"
```

Manager oluşturulduktan sonra Activity ile test modunu açmak da mümkündür:

```kotlin
adManager.enableTestMode(activity, "TEST_DEVICE_ID")
```

Production build'de test reklam ayarlarını kapatın ve her reklam platformunun test mode politikasına uyun.

## Loading view

Interstitial load-and-show akışındaki varsayılan loading view:

```kotlin
adManager.isEnableShowLoadingViewForInterstitial = true
```

Kapatmak için:

```kotlin
adManager.isEnableShowLoadingViewForInterstitial = false
```

View Activity bazında oluşturulur ve işlem tamamlandığında temizlenir.

## Yaygın hatalar

### Manager ve wrapper placement group'larının uyuşmaması

Yanlış:

```kotlin
adManager.setPlacementGroups(listOf("default", "second"))
// Wrapper içinde yalnızca "default" var.
```

Doğru: Her aktif wrapper aynı group isimlerini aynı sırada içermelidir.

### Format flag'i kapalı platformu sıralamaya eklemek

```kotlin
AdPlatformModel(
    platformInstance = admob,
    showNative = false
)

adManager.setAdPlatformSortByAdFormatStr(0, "native", "admob")
```

Bu konfigürasyon `AdPlatformOrderResult(isSuccessful=false)` döndürür.

### Firebase sonucunu kontrol etmemek

Yanlış:

```kotlin
adManager.setAdPlatformSortByAdFormatStr(0, format, order)
```

Doğru:

```kotlin
val result = adManager.setAdPlatformSortByAdFormatStr(0, format, order)
if (!result.isSuccessful) reportConfigurationError(result.message)
```

### Rewarded ödülünü yanlış callback'te vermek

Ödülü `onDisplayed` veya `onClosed` içinde değil, yalnızca `onRewarded` içinde verin.

### `SHOW_REQUESTED` değerini kesin gösterim sanmak

Hem `AdShowRequestResult.SHOW_REQUESTED` hem `AppOpenAdShowResult.SHOW_REQUESTED`, yalnızca gösterim isteğinin başlatıldığını belirtir. Kesin gösterim `onDisplayed` callback'iyle doğrulanır.

## Java kullanımı

Public API'lerin uygun bölümlerinde `@JvmOverloads` bulunur. Temel Java konfigürasyonu:

```java
AdManager adManager = new AdManager();
adManager.setShowAds(true);
adManager.setAutoLoadForInterstitial(true);
adManager.setAutoLoadDelay(10L);
adManager.setPlacementGroups(
    java.util.Collections.singletonList("default")
);
adManager.addAdPlatform(
    new AdPlatformModel(
        admobWrapper,
        true,  // Interstitial
        true,  // Banner
        true,  // Rewarded
        true,  // MREC
        false  // Native
    )
);
```

Tam Java örneği için [JavaSampleActivity.java](app/src/main/java/com/helikanonlibsample/admanager/JavaSampleActivity.java) dosyasına bakın.

## Yapay zekâ ile kod üretimi için kurallar

Bir yapay zekâ modeli bu kütüphaneyle kod yazarken aşağıdaki kurallara uymalıdır:

1. `AdManager` application scope'ta bir kez oluşturulmalıdır.
2. Önce `setPlacementGroups`, sonra `setAdPlatforms` çağrılmalıdır.
3. Her wrapper'ın `placementGroups` listesi manager ile aynı isim ve sırada olmalıdır.
4. `AdPlatformModel` oluştururken boolean flag'ler Kotlin'de named argument ile yazılmalıdır.
5. Platform initialization tamamlanmadan load/show başlatılmamalıdır.
6. İlk Interstitial ve Rewarded reklamlar initialization sonrasında ve Activity hazırken `loadInterstitial` / `loadRewarded` ile açıkça yüklenmelidir.
7. Autoload kapanış/hata sonrasında yalnızca placement group `0` için reload yapar; diğer group'lar gerektiğinde açıkça load edilmelidir.
8. Firebase sıralama sonucu olan `AdPlatformOrderResult.isSuccessful` kontrol edilmelidir.
9. `onPlatformError`, tek SDK/platform hatasıdır; `onError`, manager seviyesindeki terminal sonuçtur.
10. Rewarded ödülü yalnızca `onRewarded` içinde verilmelidir.
11. `SHOW_REQUESTED`, kesin gösterim değildir; `onDisplayed` beklenmelidir.
12. Activity UI'si listener içinde değiştirilecekse gerekirse main thread'e geçilmelidir.
13. `destroy(activity)` yalnızca manager tamamen kapatılırken kullanılmalıdır.
14. App Open foreground otomasyonu `showIntervalElapsed(activity)` kullanmalıdır.
15. App Open, ana manager initialization sonucundan sonra yüklenmelidir.
16. Interstitial, Rewarded ve App Open aynı anda gösterilmeye çalışılmamalıdır; kütüphanenin fullscreen gate sonucuna uyulmalıdır.

Yapay zekâya verilebilecek kısa görev bağlamı:

```text
Use com.helikanonlib.admanager.
Create one application-scoped AdManager.
Configure placement groups before platforms.
Keep identical placement group names/order in every wrapper.
Use named AdPlatformModel flags.
Initialize platforms before loading ads.
Treat onPlatformError as a per-platform diagnostic and onError as the terminal manager result.
Treat SHOW_REQUESTED as asynchronous request acceptance, not confirmed display.
Grant rewarded value only in onRewarded.
Check AdPlatformOrderResult when applying Firebase Remote Config order.
Call destroy only for final manager shutdown.
```

## Migration

Hata callback API'sinin eski sürümden taşınması için [MIGRATION.md](MIGRATION.md) dosyasına bakın.

## Çalışan örnekler

- Kotlin tam kurulum: [MyApplication.kt](app/src/main/java/com/helikanonlibsample/admanager/MyApplication.kt)
- Kotlin reklam çağrıları: [MainActivity.kt](app/src/main/java/com/helikanonlibsample/admanager/MainActivity.kt)
- Java kullanım örneği: [JavaSampleActivity.java](app/src/main/java/com/helikanonlibsample/admanager/JavaSampleActivity.java)
- App Open foreground lifecycle: [AppOpenLifecycleController.kt](app/src/main/java/com/helikanonlibsample/admanager/AppOpenLifecycleController.kt)
