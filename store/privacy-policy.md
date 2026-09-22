# Ko‘chatzor — Maxfiylik siyosati

**Amal qilish sanasi:** 2026-09-22

Ko‘chatzor (Ko'chatzor / QrTree) — to‘liq oflayn ishlaydigan Android ilova. Ilova hech qanday ma’lumotni serverga yubormaydi, chunki unda internetga ulanish funksiyasi umuman yo‘q (`INTERNET` va `ACCESS_NETWORK_STATE` ruxsatlari ilova manifestidan va barcha kutubxonalardan olib tashlangan).

## Biz nimani yig‘amiz

Hech narsa. Ilova:

- Hech qanday tahliliy (analytics), reklama yoki kuzatuv (tracking) kutubxonasidan foydalanmaydi.
- Hech qanday ma’lumotni uzoq serverga yoki uchinchi tomonga yubormaydi.
- Hisob yaratishni yoki tizimga kirishni talab qilmaydi.

## Foydalanuvchi kiritadigan ma’lumotlar

Ilova orqali kiritilgan barcha ma’lumotlar (xonadon egasining F.I.Sh., telefon raqami, manzil, ko‘chat ma’lumotlari) **faqat foydalanuvchi qurilmasining lokal bazasida** saqlanadi. Bu ma’lumotlar:

- Hech qachon qurilmadan tashqariga avtomatik yuborilmaydi.
- Faqat foydalanuvchi o‘zi tanlab, tizim fayl tanlagichi yoki ulashish menyusi orqali (masalan, Excel fayl sifatida) boshqa ilova yoki joyga jo‘natganda tashqariga chiqadi.
- Ilova o‘chirilganda qurilmadagi lokal baza bilan birga butunlay o‘chib ketadi.

## So‘raladigan ruxsatlar

- **Kamera** — faqat QR kod skanerlash vaqtida ishlatiladi, rasm yoki video hech qayerga saqlanmaydi yoki yuborilmaydi.
- **Xotiraga yozish (faqat Android 7–9)** — foydalanuvchi QR rasmni galereyaga saqlashni tanlaganda so‘raladi. Android 10 va undan yuqorisida bu ruxsat umuman so‘ralmaydi.

## Uchinchi tomon xizmatlari

Ilova hech qanday uchinchi tomon serveriga ulanmaydi. QR skanerlash (ML Kit Barcode Scanning) va QR yaratish (ZXing) kutubxonalari APK ichiga o‘rnatilgan (bundled) holda ishlaydi — internetdan model yuklab olinmaydi.

## Bog‘lanish

Savollar bo‘lsa: `jorabekaql07@gmail.com`

---
*Bu sahifani GitHub Pages, Google Sites yoki boshqa bepul static-hosting xizmatida joylashtirib, havolasini Google Play Console → App content → Privacy policy bo‘limiga qo‘shing.*
