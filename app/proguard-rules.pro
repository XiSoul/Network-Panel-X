# Network Panel X — R8 混淆/收缩规则
# 目标：业务代码全混淆，框架（Compose/OkHttp/Cronet/Kotlin）保持可运行。
# 原则：宁可少混淆框架，也不要让 release 包在真机上崩。

# ---------- 基础属性（缺一个就可能运行时找不到类/注解） ----------
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes MethodParameters

# 不打印警告（三方库常见的 optional 依赖缺失）
-dontwarn **

# ---------- Kotlin ----------
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlin.**
-keep class kotlin.coroutines.Continuation { *; }

# ---------- Jetpack Compose ----------
# Compose 运行时重度依赖反射（ComposableMethodKt / AndroidComposeView / ViewLayer），
# 这里必须放开，否则混淆后启动白屏或 UI 不渲染。
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.ui.graphics.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ---------- AndroidX / Lifecycle / Security ----------
-keep class androidx.lifecycle.** { *; }
-keep class androidx.security.crypto.** { *; }
-keep class androidx.activity.** { *; }
-dontwarn androidx.**

# ---------- OkHttp / Okio ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ---------- Cronet (play-services-cronet) ----------
-keep class org.chromium.** { *; }
-keep class com.google.android.gms.cronet.** { *; }
-dontwarn org.chromium.**
-dontwarn com.google.android.gms.**

# ---------- 反射/枚举/序列化常见崩点 ----------
-keepclassmembers enum * { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------- 保留行号，崩溃堆栈配合 mapping.txt 才能还原 ----------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- 保留 App 入口 ----------
-keep class com.example.networkpanelx.MainActivity { *; }
-keep class com.example.networkpanelx.*Application { *; }
