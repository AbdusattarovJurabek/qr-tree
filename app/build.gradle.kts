plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.devtools.ksp") }
android {
 namespace = "uz.kochatzor"; compileSdk = 36
 defaultConfig { applicationId = "uz.kochatzor"; minSdk = 24; targetSdk = 36; versionCode = 1; versionName = "1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17; isCoreLibraryDesugaringEnabled = true }

}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.10.01"))
 implementation("androidx.activity:activity-compose:1.11.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
 implementation("androidx.room:room-runtime:2.8.3")
 implementation("androidx.room:room-ktx:2.8.3")
 ksp("androidx.room:room-compiler:2.8.3")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("androidx.camera:camera-camera2:1.5.1")
 implementation("androidx.camera:camera-lifecycle:1.5.1")
 implementation("androidx.camera:camera-view:1.5.1")
 implementation("com.google.mlkit:barcode-scanning:17.3.0")
 implementation("com.google.zxing:core:3.5.3")
 implementation("androidx.print:print:1.1.0")
 coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
 androidTestImplementation("androidx.test:core-ktx:1.7.0")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
