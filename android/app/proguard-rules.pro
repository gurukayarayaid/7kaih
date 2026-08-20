# Aturan ProGuard untuk Aplikasi 7 KAIH (minify nonaktif secara bawaan)

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
