# AdManager hata callback API'si migration rehberi

Bu doküman, eski `AdErrorMode` tabanlı listener API'sinden yeni ayrıştırılmış hata callback API'sine geçişi açıklar.

## Değişikliğin amacı

Eski API'de platform hataları ve AdManager'ın bütün platformları denedikten sonra ürettiği terminal hata aynı `onError(...)` metodu üzerinden bildiriliyordu. Hatanın kaynağını anlamak için `AdErrorMode.PLATFORM` ve `AdErrorMode.MANAGER` kontrolü yapmak gerekiyordu.

Yeni API'de iki durum ayrı callback'lere bölündü:

- `onPlatformError(AdPlatformError)`: Tek bir reklam platformundan gelen hata.
- `onError(AdManagerError)`: AdManager'ın işlemi tamamlayamadığını bildiren terminal hata.

Bu değişiklik geriye uyumlu değildir. Eski listener implementasyonlarının güncellenmesi gerekir.

## Listener imzaları

### Eski API

```kotlin
abstract class AdPlatformLoadListener {
    open fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onError(
        errorMode: AdErrorMode?,
        errorMessage: String?,
        adPlatformEnum: AdPlatformTypeEnum?
    ) {}
}

abstract class AdPlatformShowListener {
    open fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onRewarded(
        type: String? = null,
        amount: Int? = null,
        adPlatformEnum: AdPlatformTypeEnum?
    ) {}
    open fun onError(
        errorMode: AdErrorMode?,
        errorMessage: String?,
        adPlatformEnum: AdPlatformTypeEnum?
    ) {}
}
```

### Yeni API

```kotlin
abstract class AdPlatformLoadListener {
    open fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {}
    open fun onPlatformError(error: AdPlatformError) {}
    open fun onError(error: AdManagerError) {}
}

abstract class AdPlatformShowListener {
    open fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {}
    open fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {}
    open fun onClosed(adPlatformEnum: AdPlatformTypeEnum) {}
    open fun onRewarded(
        type: String? = null,
        amount: Int? = null,
        adPlatformEnum: AdPlatformTypeEnum
    ) {}
    open fun onPlatformError(error: AdPlatformError) {}
    open fun onError(error: AdManagerError) {}
}
```

Başarılı callback'lerdeki `AdPlatformTypeEnum` parametreleri artık nullable değildir.

## Yeni hata modelleri

### AdPlatformError

Bir reklam SDK'sından veya platform wrapper'ından gelen hatayı temsil eder.

```kotlin
data class AdPlatformError(
    val format: AdFormatEnum,
    val platform: AdPlatformTypeEnum,
    val placementGroupIndex: Int,
    val message: String,
    val cause: Throwable? = null
)
```

- `format`: Hatanın oluştuğu reklam türü.
- `platform`: Hatayı üreten reklam platformu.
- `placementGroupIndex`: Kullanılan placement group index'i.
- `message`: Platform veya wrapper tarafından üretilen hata mesajı.
- `cause`: Varsa orijinal exception.

### AdManagerError

İstenen reklam işleminin AdManager tarafından tamamlanamadığını temsil eder.

```kotlin
data class AdManagerError(
    val format: AdFormatEnum,
    val placementGroupIndex: Int,
    val attemptedPlatforms: List<AdPlatformTypeEnum> = emptyList(),
    val platformErrors: List<AdPlatformError> = emptyList(),
    val message: String
)
```

- `attemptedPlatforms`: İşlem sırasında denenmiş platformlar.
- `platformErrors`: Denenen platformlardan toplanan hatalar.
- `message`: AdManager seviyesindeki sonuç mesajı.

Konfigürasyonun boş olması veya gösterilecek yüklü reklam bulunmaması gibi durumlarda `onError` doğrudan çağrılabilir. Böyle bir durumda `platformErrors` boş olabilir.

## Callback davranışı

Örneğin sıralama `ADMOB, APPLOVIN, UNITYADS` ise ve bütün platformlar yükleme sırasında hata verirse callback akışı şöyledir:

```text
onPlatformError(ADMOB)
onPlatformError(APPLOVIN)
onPlatformError(UNITYADS)
onError(
    attemptedPlatforms = [ADMOB, APPLOVIN, UNITYADS],
    platformErrors = [ADMOB error, APPLOVIN error, UNITYADS error]
)
```

Platformlardan biri başarılı olursa `onLoaded(...)` çağrılır ve tüm platformların başarısız olduğunu belirten terminal `onError(...)` çağrılmaz.

Paralel yüklemede platform callback'lerinin geliş sırası garanti edilmez. `attemptedPlatforms` reklam sıralamasını, `platformErrors` ise callback'lerin alınma sırasını yansıtır.

Gösterim sırasında seçilmiş platform hata verirse önce `onPlatformError(...)`, ardından işlem tamamlanamadığı için `onError(...)` çağrılabilir.

## Kotlin migration örneği

### Eski kullanım

```kotlin
adManager.loadInterstitial(this, object : AdPlatformLoadListener() {
    override fun onError(
        errorMode: AdErrorMode?,
        errorMessage: String?,
        adPlatformEnum: AdPlatformTypeEnum?
    ) {
        when (errorMode) {
            AdErrorMode.PLATFORM -> {
                logPlatformError(adPlatformEnum, errorMessage)
            }

            AdErrorMode.MANAGER -> {
                showNoAdAvailableMessage()
            }

            null -> Unit
        }
    }
})
```

### Yeni kullanım

```kotlin
adManager.loadInterstitial(this, object : AdPlatformLoadListener() {
    override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
        logAdLoaded(adPlatformEnum)
    }

    override fun onPlatformError(error: AdPlatformError) {
        logPlatformError(error.platform, error.message)
        error.cause?.let(::reportException)
    }

    override fun onError(error: AdManagerError) {
        logManagerError(
            format = error.format,
            attemptedPlatforms = error.attemptedPlatforms,
            platformErrors = error.platformErrors,
            message = error.message
        )
        showNoAdAvailableMessage()
    }
})
```

Show listener'ları da aynı şekilde taşınır:

```kotlin
adManager.showRewarded(this, object : AdPlatformShowListener() {
    override fun onRewarded(
        type: String?,
        amount: Int?,
        adPlatformEnum: AdPlatformTypeEnum
    ) {
        grantReward(type, amount)
    }

    override fun onPlatformError(error: AdPlatformError) {
        logPlatformError(error.platform, error.message)
    }

    override fun onError(error: AdManagerError) {
        showRewardedUnavailableMessage()
    }
})
```

## Java migration örneği

```java
adManager.loadInterstitial(
    activity,
    new AdPlatformLoadListener() {
        @Override
        public void onLoaded(@NonNull AdPlatformTypeEnum platform) {
            logAdLoaded(platform);
        }

        @Override
        public void onPlatformError(@NonNull AdPlatformError error) {
            logPlatformError(error.getPlatform(), error.getMessage());
        }

        @Override
        public void onError(@NonNull AdManagerError error) {
            showNoAdAvailableMessage();
        }
    }
);
```

Java tarafında projenin nullability annotation tercihine göre `@NonNull` import'u eklenebilir veya annotation'lar kaldırılabilir.

## Global ve lokal listener davranışı

Interstitial ve rewarded için tanımlanan global listener'lar çalışmaya devam eder:

```kotlin
adManager.globalInterstitialLoadListener = loadListener
adManager.globalInterstitialShowListener = showListener
adManager.globalRewardedLoadListener = loadListener
adManager.globalRewardedShowListener = showListener
```

App Open tarafında:

```kotlin
appOpenAdManager.globalLoadListener = loadListener
appOpenAdManager.globalShowListener = showListener
```

Bir metoda ayrıca lokal listener gönderilmişse hem global hem lokal listener tetiklenir. Aynı listener nesnesi hem global hem lokal olarak verilirse callback yalnızca bir kez çağrılır.

```kotlin
val listener = object : AdPlatformLoadListener() {
    override fun onError(error: AdManagerError) {
        // Bir kez çağrılır.
    }
}

adManager.globalInterstitialLoadListener = listener
adManager.loadInterstitial(activity, listener)
```

## App Open değişiklikleri

`AdFormatEnum` içerisine `APP_OPEN` eklendi. App Open yükleme ve gösterim hataları da diğer reklam formatlarıyla aynı modelleri kullanır.

- Platform yükleme/gösterim hataları `onPlatformError(AdPlatformError)` ile gelir.
- Bütün App Open platformları yükleme sırasında başarısız olursa `onError(AdManagerError)` çağrılır.
- App Open devre dışı bırakıldığında bekleyen bir birleşik yükleme isteği için tek terminal manager hatası gönderilir.

## Etkilenen reklam türleri

Yeni callback sözleşmesi aşağıdaki akışlarda uygulanır:

- Interstitial load/show
- Rewarded load/show
- Banner show
- MREC show
- Native ve Native Medium load
- App Open load/show

## Migration kontrol listesi

1. Bütün `AdErrorMode` import ve kullanımlarını kaldırın.
2. Eski üç parametreli `onError(...)` override'larını kaldırın.
3. Platforma özel işlemleri `onPlatformError(AdPlatformError)` içine taşıyın.
4. Tüm denemelerin başarısız olması veya manager seviyesindeki terminal durumları `onError(AdManagerError)` içinde yönetin.
5. Nullable `AdPlatformTypeEnum?` callback parametrelerini `AdPlatformTypeEnum` olarak değiştirin.
6. Hata raporlama sisteminize `format`, `placementGroupIndex`, `attemptedPlatforms` ve `platformErrors` alanlarını ekleyin.
7. Paralel yükleme kullanıyorsanız platform callback sırasına bağlı kodları kaldırın.
8. Kotlin ve Java consumer modüllerini yeniden derleyerek eski listener imzası kalmadığını doğrulayın.

## Initialization API değişikliği

Platform initialization işlemi artık yalnızca bütün SDK'lar başarılı olduğunda çalışan parametresiz bir callback yerine yapılandırılmış bir sonuç döndürür.

Eski kullanım:

```kotlin
adManager.initializePlatforms(
    context,
    onAllInitializeComplete = {
        startAds()
    },
    onPlatformInitializeComplete = { platform ->
        logInitialized(platform)
    }
)
```

Yeni kullanım:

```kotlin
adManager.initializePlatforms(
    context,
    onInitializeComplete = { result ->
        when (result.status) {
            AdInitializationStatus.SUCCESS -> startAds()
            AdInitializationStatus.PARTIAL_SUCCESS -> {
                logFailedPlatforms(result.failedPlatforms)
                startAvailableAds()
            }
            AdInitializationStatus.FAILURE -> showInitializationError()
            AdInitializationStatus.DISABLED -> continueWithoutAds()
        }
    },
    onPlatformInitializeComplete = { result ->
        logPlatformInitialization(result.platform, result.isSuccessful)
    }
)
```

Davranış değişiklikleri:

- `showAds == false` ise terminal callback `DISABLED` sonucu ile çağrılır.
- Hiç platform yapılandırılmamışsa terminal callback `FAILURE` sonucu ile çağrılır.
- Bütün platformlar başarılıysa sonuç `SUCCESS` olur.
- Bazı platformlar başarılı, bazıları başarısızsa sonuç `PARTIAL_SUCCESS` olur.
- Bütün platformlar başarısızsa sonuç `FAILURE` olur.
- `onPlatformInitializeComplete`, platform enum'u yerine `AdPlatformInitializationResult` alır.
- Wrapper'ın gönderdiği `Boolean` başarı sonucu terminal sonucun hesaplanmasında kullanılır.
- SDK callback'leri farklı thread'lerden gelse bile sonuç listesi güvenli şekilde güncellenir.

`isAllPlatformsSdksInitialized(context)` ve `isPlatformSdkInitialized(context, platform)` metotlarındaki kullanılmayan `context` parametresi kaldırıldı:

```kotlin
adManager.isAllPlatformsSdksInitialized()
adManager.isPlatformSdkInitialized(platform)
```
