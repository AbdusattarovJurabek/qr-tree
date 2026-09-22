# Room: entities/DAOs are referenced by generated code; keep annotations and schema classes safe.
-keep class uz.kochatzor.data.** { *; }
-keepclassmembers class uz.kochatzor.data.** { *; }

# ML Kit bundled barcode model loads classes by name at runtime.
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# ZXing QR encoder — no reflection, but keep public API stable against shrinking edge cases.
-keep class com.google.zxing.** { *; }

# org.json is part of the Android platform, never shrink/obfuscate it.
-keep class org.json.** { *; }

-dontwarn org.json.**
