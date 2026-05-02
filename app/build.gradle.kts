plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.camyuran.camyunews"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.camyuran.camyunews"
        minSdk = 26
        targetSdk = 35
        versionCode = 26050305
        versionName = "26.05.03.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Windows AGP バグ回避: スペースを含むパスで -Djava.library.path がクォートされない問題
    sourceSets {
        getByName("test") {
            jniLibs.setSrcDirs(listOf("C:/TmpJniLibs/test"))
        }
        getByName("testDebug") {
            jniLibs.setSrcDirs(listOf("C:/TmpJniLibs/testDebug"))
        }
        getByName("testRelease") {
            jniLibs.setSrcDirs(listOf("C:/TmpJniLibs/testRelease"))
        }
    }

    applicationVariants.all {
        val appName = "CamyuNews"
        val version = versionName
        val buildTypeName = buildType.name
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "$appName-$version-$buildTypeName.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    // OkHttp
    implementation(libs.okhttp)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    // Glance (Widget)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    // Security
    implementation(libs.androidx.security.crypto)
    // Gemini AI
    implementation(libs.generativeai)
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    // Chrome Custom Tabs
    implementation(libs.androidx.browser)
    // デバッグ
    debugImplementation(libs.androidx.compose.ui.tooling)
    // kxml2: RssParser が KXmlParser を直接使用（android.util.Xml 排除）
    implementation("net.sf.kxml:kxml2:2.3.0")
    // ユニットテスト
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
}
