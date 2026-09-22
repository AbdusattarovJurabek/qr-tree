# Ko‘chatzor — offline Android ilova

Xonadonlarga ekilgan mevali ko‘chatlarni hisobga olish uchun o‘zbek lotin yozuvidagi Android ilova.

## Build va ishga tushirish

Android Studio’da shu papkani oching. Gradle JDK sifatida **JDK 17** tanlang. SDK Manager orqali **Android SDK Platform 36** va **Build Tools 35.0.0** o‘rnating.

```sh
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug
./gradlew testDebugUnitTest
# Ishlab turgan emulator yoki USB orqali ulangan qurilma uchun:
./gradlew connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Min SDK 24 (Android 7), target/compile SDK 36. Birinchi build uchun Gradle va Maven kutubxonalarini internetdan yuklash kerak; o‘rnatilgan ilovaga internet kerak emas. `local.properties` shaxsiy SDK yo‘lini saqlaydi, uni boshqa kompyuterda Android Studio qayta yaratadi.

## Funksiyalar

- Bosh sahifa: bugungi, haftalik va jami yozuvlar; jami ko‘chatlar va turlar kesimidagi hisob.
- Viloyat → tuman/shahar → MFY tanlash. MFY qidiruvi Room’da faqat shu tuman bo‘yicha, 100 talik sahifalarda bajariladi. Lotin apostroflari va harf kattaligi normallashtiriladi; kirill matni qo‘llanadi, lotin-kirill transliteratsiyasi bajarilmaydi.
- Barcha 10 ta soha maydoni, majburiy maydon va son/sana/telefon tekshiruvi. Yer maydoni uchun nuqta yoki vergul qabul qilinadi.
- Saqlash, qayta tahrirlash, tasdiq bilan mantiqiy o‘chirish; manzilni saqlab qolgan holda yangi yozuv.
- Matn, viloyat, tuman, MFY, ko‘chat turi va sana filtrlari birgalikda ishlaydi. Sana filtri **yozuv kiritilgan vaqtga** tegishli, ekish sanasiga emas. Hafta dushanbadan boshlanadi; vaqt zonasi telefonnikidir. Maxsus oraliqning oxirgi kuni to‘liq qo‘shiladi.
- QR yaratish, galereyaga PNG saqlash, FileProvider orqali ulashish. Skaner natijani faqat ilova ichida ko‘rsatadi; URL ochmaydi.
- Filtrlangan yoki barcha yozuvlarni haqiqiy XLSX formatida, har qatorga QR rasmi bilan eksport. Tizim fayl tanlagichi orqali saqlash yoki Telegram/Gmail va boshqa ilovalarga ulashish.
- Tizim, yorug‘ va to‘q mavzular. Forma `SavedStateHandle` bilan configuration change va Android tiklaydigan process recreation’da saqlanadi.

## Arxitektura va paketlar

Kotlin, Jetpack Compose, Material 3; MVVM va Coroutines/Flow. `KochatzorApp` oddiy dependency container, `AppViewModel` UI holati va amallarni boshqaradi.

- `data/Database.kt`: Room entity/DAO, alohida reference va survey bazalari, asset importer.
- `domain/Repository.kt`: `SurveyRepository` interfeysi, lokal realizatsiya va sana/filter modeli.
- `ui/`: dashboard, forma, kartalar, detail, QR, CameraX skaner, sozlamalar.
- `util/Qr.kt`: to‘rtta maydonli payload, qat’iy parser va ZXing renderer.
- `util/Xlsx.kt`: Android bilan mos, ZIP/XML asosidagi minimal OOXML writer.
- `util/Files.kt`: MediaStore va FileProvider.

Room chaqiruvlari suspend/Flow orqali, asset import va fayl/QR/Excel amallari background dispatcher’da ishlaydi. Bazani main thread’da ochishga ruxsat berilmagan.

## Bazalar va reference ro‘yxatini almashtirish

`surveys.db` foydalanuvchi yozuvlarini, `reference.db` esa viloyat/tuman/MFY katalogini saqlaydi. Survey yozuvlari UUID, `createdAt`, `updatedAt`, `syncStatus`, `isDeleted` maydonlariga ega. Yangi yozuv `LOCAL`, tahrir va o‘chirish `PENDING`; `SYNCED` kelajakdagi server tasdig‘i uchun ajratilgan. O‘chirish tombstone sifatida saqlanadi va ro‘yxat, statistika, eksportdan chiqariladi.

`app/src/main/assets/reference.json` — **faqat sinov katalogi**: 2 viloyat, 3 tuman/shahar, 4 MFY. Rasmiy to‘liq O‘zbekiston ro‘yxati emas. Uni shu JSON sxemasidagi rasmiy ma’lumotga almashtiring; IDlar barqaror va ota-bola havolalari to‘g‘ri bo‘lsin. Asset SHA-256 o‘zgarganda reference jadvallari bitta tranzaksiyada yangilanadi. Foydalanuvchi bazasi o‘zgarmaydi. Survey ichidagi manzil nomlari tarixiy snapshot sifatida saqlanadi.

Room sxemalari `app/schemas/` ichida eksport qilinadi. Sxema o‘zgartirilganda versiyani oshirib, `Databases.migrations` ichiga aniq `Migration(old, new)` qo‘shing; destructive migration yoqilmagan.

## QR va saqlash

ZXing Core 3.5.3 UTF-8, xatolarni tuzatish M darajasi va oq chegaralar bilan QR yaratadi. Payload faqat:

```text
FIO=Kadirov Abduxaxxor Kurbanbayevich
TUR=Olma
NAV=Golden
PAYVANDTAG=
YIL=2026
```

Telefon, maydon, son, to‘liq sana, manzil (viloyat/tuman/MFY), manba, UUID QRga kiritilmaydi; faqat ekish yilining o‘zi (YIL) kiritiladi. Parser `TUMAN`/`MFY`/`KOCHAT` kabi eski formatdagi kalitlarni ham o‘qiy oladi (eski chop etilgan QR kodlar bilan moslik uchun), lekin tanilmagan yoki takrorlangan maydon bo‘lsa butun QR rad etiladi. CameraX 1.5.1 va APKga **bundled** ML Kit Barcode Scanning 17.3.0 ishlatiladi; modelni keyin internetdan yuklash talab qilinmaydi. Asos: [ML Kit Android hujjati](https://developers.google.com/ml-kit/vision/barcode-scanning/android).

Android 10+ da MediaStore `Pictures/Kochatzor` va `IS_PENDING` orqali saqlanadi, storage permission so‘ralmaydi. Android 7–9 da faqat galereyaga saqlash bosilganda `WRITE_EXTERNAL_STORAGE` so‘raladi (`maxSdkVersion=28`). Kamera ruxsati faqat skanerda so‘raladi. INTERNET va ACCESS_NETWORK_STATE hatto dependency manifestlaridan ham chiqarib tashlanadi.

## Excel

Apache POI yoki desktop grafik APIlariga bog‘liqlik yo‘q. Writer `.xlsx` OOXML paketini `ZipOutputStream` bilan yaratadi: workbook, worksheet, styles, drawing va relationship XML fayllari hamda `xl/media/qrN.png` rasmlari. QR oxirgi **N ustunida** `oneCellAnchor` orqali taxminan 100×100 px o‘lchamda ko‘rsatiladi.

Header qalin, 14 ustun (shu jumladan Payvandtag), border, matn o‘rash, mos kengliklar, QR uchun 84 pt qator balandligi, muzlatilgan header va Excel autofilter bor. Son/maydonlar raqamli kataklar, telefon va matnlar inline string: `=` bilan boshlangan matn ham formula bo‘lib bajarilmaydi. Rasmlar ketma-ket yoziladi, barcha bitmaplar bir paytda xotirada saqlanmaydi. Export boshlanganda filtrga mos bazadan snapshot olinadi.

## Keyinchalik serverga ulash

Boshlash nuqtasi: `domain/Repository.kt` dagi `SurveyRepository` va `KochatzorApp.repository`. Yangi remote data source va API mijozini shu repository realizatsiyasiga qo‘shing. UI Room Flow’ni kuzatishda davom etsin. `pendingSync()` lokal/o‘zgargan/o‘chirilgan yozuvlarni qaytaradi.

Server UUID bo‘yicha idempotent upsert/delete qabul qilishi kerak. Sync worker jo‘natgan `updatedAt` versiyasi o‘zgarmagan bo‘lsagina server tasdig‘idan keyin `SYNCED` qiling; parallel tahrirni yo‘qotmang. Konflikt, autentifikatsiya, retry/backoff va WorkManager rejasini shu qatlamda qo‘shing. Hozir tarmoq yoki soxta server realizatsiyasi yo‘q.

## Ma’lumotlar hayot sikli

Ma’lumot shu telefonda qoladi. Ilova o‘chirilganda lokal baza ham o‘chadi; muhim ma’lumotlarni Excel sifatida tashqariga saqlang. Excel eksporti hisobotdir; bazaga qayta import qilish yoki qurilmalararo backup bu versiya doirasiga kirmaydi.

Bu kompyuterda tekshirilgan JDK: `/Users/jurabek/Documents/Codex/2026-09-14/files-pasted-by-the-user-menga/work/jdk/Contents/Home`. Android Studio Gradle JDK sozlamasida shu JDKni tanlash mumkin.

## Tekshiruvlar

`assembleDebug` va `testDebugUnitTest` muvaffaqiyatli bajarildi (5 ta unit test). Emulatorda 5 ta offline integratsiya testi: Room kombinatsiyalangan filtrlari va soft delete, reference qidiruvi, ZXing Unicode round-trip, bundled ML Kit QR o‘qishi, MediaStore PNG saqlash. Haqiqiy XLSX openpyxl bilan tuzilma/rasmlar bo‘yicha tekshirildi va Microsoft Excel’da tiklash dialogisiz ochildi; ikkala QR rasm ko‘rindi. Android lint’da xato yo‘q; kutubxonalarning yangi versiyalari haqidagi tavsiya ogohlantirishlari mavjud.

ARM64 native kutubxonalarining ELF segmentlari 16 KB page size bilan mos. Real telefon kamerasi va Android 7–9 galereya ruxsat oqimi alohida jismoniy qurilmada sinovdan o‘tkazilmagan.

Forma → saqlash → QR bo‘yicha `EntryUiTest` bu muhitda to‘liq o‘tmadi: Android 17 emulatori UI Automatorga ilova o‘rniga System UI ekranini qaytardi (`Missing: Kiritish`). Shu sabab butun UI oqimi tasdiqlangan deb hisoblanmaydi. Test kodi loyihada qoldirilgan; uni qulfi ochilgan qurilmada qayta bajarish kerak.
