plugins { id("com.android.application");id("org.jetbrains.kotlin.android");id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace="com.mazhar.mediapocket";compileSdk=35
 defaultConfig { applicationId="com.mazhar.mediapocket";minSdk=29;targetSdk=35;versionCode=1;versionName="1.0";ndk { abiFilters.add("arm64-v8a") } }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
 buildFeatures { compose=true }
 packaging { jniLibs { useLegacyPackaging=true };resources.excludes.addAll(listOf("META-INF/DEPENDENCIES","META-INF/LICENSE*","META-INF/NOTICE*")) }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.runtime:runtime-livedata")
 implementation("androidx.work:work-runtime-ktx:2.10.0")
 implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
 implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("org.jsoup:jsoup:1.18.3")
 testImplementation("junit:junit:4.13.2")
}
