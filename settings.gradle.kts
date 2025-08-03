pluginManagement {
    repositories {
        // ✅ ضروري لتحميل إضافات Google مثل google-services
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()        // ✅ لتحميل مكتبات Google/Firebase
        mavenCentral()  // ✅ مكتبات Compose/Kotlin
        maven { url = uri("https://jitpack.io") }  // ✅ للـ MPAndroidChart
    }
}

rootProject.name = "SmartPay"
include(":app")
