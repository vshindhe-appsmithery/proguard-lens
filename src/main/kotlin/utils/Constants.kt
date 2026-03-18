package dev.appsmithery.utils

object Constants {

    val defaultAndroidComponents = setOf(
        "android/app/Activity",
        "androidx/appcompat/app/AppCompatActivity",
        "android/app/Service",
        "android/content/BroadcastReceiver",
        "android/content/ContentProvider",
        "androidx/fragment/app/Fragment",
        "androidx/startup/Initializer"
    )

    enum class KeepRules {
        public_api,
        keep_rule,
        android_component,
        default_android_component,
    }

}