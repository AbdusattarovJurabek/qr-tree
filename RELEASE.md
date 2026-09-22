# Play Marketga chiqarish

## 1. Imzolash kaliti (keystore) — faqat bir marta

Bu kalitni **hech qachon yo‘qotmang va commit qilmang** — Play App Signing yoqilgan bo‘lsa ham, upload key yo‘qolsa Google’ga murojaat qilib tiklashga to‘g‘ri keladi.

Terminalda (parolni o‘zingiz kiritasiz, u hech qayerga yozilmaydi):

```sh
keytool -genkeypair -v -keystore ~/kochatzor-upload.jks -alias kochatzor -keyalg RSA -keysize 2048 -validity 10000
```

So‘ralgan savollarga (ism, tashkilot, shahar, mamlakat kodi — masalan `UZ`) javob bering. Parolni ishonchli joyda saqlang (parol menejeri).

Loyiha ildizida `keystore.properties` fayl yarating (bu fayl `.gitignore`da, hech qachon Git’ga tushmaydi):

```properties
storeFile=/Users/jurabek/kochatzor-upload.jks
storePassword=***
keyAlias=kochatzor
keyPassword=***
```

Shundan keyin `./gradlew bundleRelease` avtomatik shu kalit bilan imzolaydi.

## 2. AAB (App Bundle) yig‘ish

Play Market APK emas, **.aab** talab qiladi:

```sh
export JAVA_HOME=/path/to/jdk-17
./gradlew bundleRelease
```

Natija: `app/build/outputs/bundle/release/app-release.aab`

Faqat lokal sinov uchun imzolangan APK kerak bo‘lsa:

```sh
./gradlew assembleRelease
```

## 3. Har bir yangi versiya uchun

`app/build.gradle.kts` dagi `defaultConfig`da:

```kotlin
versionCode = 2   // har safar +1
versionName = "1.1"  // foydalanuvchiga ko‘rinadigan raqam
```

## 4. Play Console’da tayyor turgan narsalar

- **Ikonka (512×512):** `store/ic_launcher_512.png`
- **Store listing matnlari (nom, tavsif, kategoriya):** `store/listing.md`
- **Maxfiylik siyosati matni:** `store/privacy-policy.md` — buni biror bepul static-hosting’ga (GitHub Pages, Google Sites) joylashtirib, havolasini Play Console → App content → Privacy policy’ga qo‘ying (bu majburiy maydon, matnni loyihada saqlash yetarli emas).

## 5. Play Console’da hali qo‘lda to‘ldirish kerak bo‘lgan narsalar

- **Skrinshotlar** — kamida 2 ta telefon skrinshoti (ilovani ishga tushirib, o‘zingiz olishingiz kerak).
- **Feature graphic** (1024×500) — marketing banner, ilova kodiga aloqasi yo‘q.
- **Content rating so‘rovnomasi** — `store/listing.md`dagi eslatmaga asoslanib javob bering (reklama/kontent yo‘q, Everyone bo‘lishi kerak).
- **Data safety formasi** — `store/listing.md`dagi javoblardan foydalaning ("No data collected").
- **Target audience va Ads** — reklama yo‘q, deb belgilang.

## 6. Texnik holat (tekshirildi)

- `compileSdk`/`targetSdk` = 36 — Play Console’ning eng so‘nggi talablariga mos.
- `./gradlew assembleRelease` — R8 minifikatsiya va resurs siqish bilan muvaffaqiyatli o‘tdi (ML Kit, ZXing, Room uchun `app/proguard-rules.pro` qo‘shildi).
- Adaptiv ikonka (`mipmap-anydpi-v26`) va eski qurilmalar uchun fallback qo‘shildi.
- `INTERNET`/`ACCESS_NETWORK_STATE` ruxsatlari yo‘q — Data safety formasini eng qulay to‘ldirish mumkin.
