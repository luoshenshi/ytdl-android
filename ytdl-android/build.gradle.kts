plugins {
    alias(libs.plugins.android.library)
    id("com.vanniktech.maven.publish") version "0.36.0"
}

android {
    namespace = "io.github.luoshenshi"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

mavenPublishing {
    coordinates("io.github.luoshenshi", "ytdl-android", "1.2")

    pom {
        name.set("Ytdl-Android")
        description.set("Ytdl-Android is a lightweight Android library designed for fetching and processing YouTube video information.")
        inceptionYear.set("2026")
        url.set("https://github.com/luoshenshi/ytdl-android/")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("luoshenshi")
                name.set("Luo Shenshi")
                url.set("https://github.com/luoshenshi/")
            }
        }

        scm {
            url.set("https://github.com/luoshenshi/ytdl-android/")
            connection.set("scm:git:git://github.com/luoshenshi/ytdl-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/luoshenshi/ytdl-android.git")
        }
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.slf4j.simple)
}