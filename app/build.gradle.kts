plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myfirstapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.myfirstapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 基础组件（注意：和上面libs.appcompat重复了，保留一组即可，这里保留你的手动配置）
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 图片加载利器 Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Glide注解处理器（补充）

    // 网络请求核心库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ========== 新增 Room 相关依赖 ==========
    // Room 核心库（最新稳定版）
    implementation("androidx.room:room-runtime:2.6.1")
    // Room 注解处理器（Java项目用这个）
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    // 可选：Room 数据绑定（如果需要）
    implementation("androidx.room:room-ktx:2.6.1")

    // Lombok核心库
    implementation("org.projectlombok:lombok:1.18.30")
    // 注解处理器（Java项目）
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}