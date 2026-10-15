import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)       // Kotlin 2.0+ Compose 编译器插件
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// 发布签名配置从 keystore.properties 读取（该文件已在 .gitignore 中，密钥不入库）。
// 文件不存在时下面的 signingConfig 不会创建，assembleRelease 仍可用，只是产物未签名——
// 这样别人克隆仓库后依然能构建。
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.expensetracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.expensetracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        // 设置页要读 BuildConfig.VERSION_NAME 显示版本号，
        // 否则只能硬编码，发版时必然忘记同步
        buildConfig = true
    }

    // Kotlin 2.0+ Compose 编译器由 kotlin-compose 插件管理，不需要 composeOptions

    // ONNX 模型文件不二次压缩（已经高度优化）
    aaptOptions {
        noCompress += listOf("onnx")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room schema 导出路径 (供版本迁移使用)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.graphics)
    debugImplementation(libs.compose.ui.tooling)

    // Activity + Navigation
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp + Moshi
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Vico Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Core
    implementation(libs.core.ktx)

    // Security
    implementation(libs.security.crypto)

    // ONNX Runtime Android (FunASR Paraformer 推理引擎, 完整算子)
    implementation(libs.onnxruntime.android)

    // JTransforms (FFT 用于 Fbank 音频特征提取)
    implementation(libs.jtransforms)
}
