@file:Suppress("MayBeConstant", "unused")

package nerd.tuxmobil.fahrplan.congress

object Versions {
    val androidGradle = "3.1.1"
    val assertjAndroid = "1.2.0"
    val emailIntentBuilder = "1.0.0"
    val espresso = "3.0.2"
    val gradleVersions = "0.20.0"
    val junit = "4.12"
    val kotlin = "1.2.51"
    val mockito = "2.22.0"
    val okhttp = "3.11.0"
    val snackengage = "0.15"
    val sonarQubeGradle = "2.6.2"
    val supportLibrary = "26.1.0"
    val testRules = "1.0.2"
    val threeTenBp = "1.3.7"
    val tracedroid = "1.4"
}

object Android {
    val buildToolsVersion = "28.0.3"
    val compileSdkVersion = 26
    val minSdkVersion = 14
    val targetSdkVersion = 26
}

object GradlePlugins {
    val androidGradle = "com.android.tools.build:gradle:${Versions.androidGradle}"
    val gradleVersions = "com.github.ben-manes:gradle-versions-plugin:${Versions.gradleVersions}"
    val sonarQubeGradle = "org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:${Versions.sonarQubeGradle}"
    val kotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
}

object Libs {
    val assertjAndroid = "com.squareup.assertj:assertj-android:${Versions.assertjAndroid}"
    val emailIntentBuilder = "de.cketti.mailto:email-intent-builder:${Versions.emailIntentBuilder}"
    val espresso = "com.android.support.test.espresso:espresso-core:${Versions.espresso}"
    val junit = "junit:junit:${Versions.junit}"
    val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    val mockitoCore = "org.mockito:mockito-core:${Versions.mockito}"
    val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    val okhttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}"
    val snackengagePlayrate = "com.github.ligi.snackengage:snackengage-playrate:${Versions.snackengage}"
    val supportLibraryAnnotations = "com.android.support:support-annotations:${Versions.supportLibrary}"
    val supportLibraryAppcompatV7 = "com.android.support:appcompat-v7:${Versions.supportLibrary}"
    val supportLibraryDesign = "com.android.support:design:${Versions.supportLibrary}"
    val testRules = "com.android.support.test:rules:${Versions.testRules}"
    val threeTenBp = "org.threeten:threetenbp:${Versions.threeTenBp}"
    val tracedroid = "org.ligi:tracedroid:${Versions.tracedroid}"
}
