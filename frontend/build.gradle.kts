// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // 기존에 있는 다른 plugins 줄은 그대로 유지하고 아래 줄만 추가
    id("com.google.gms.google-services") version "4.4.2" apply false
}