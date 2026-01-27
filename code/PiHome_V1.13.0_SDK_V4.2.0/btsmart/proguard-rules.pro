# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-optimizationpasses 5
#关闭混淆优化
-dontoptimize
#配置不混淆类型
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
# 指定混淆是采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不做更改
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#重命名抛出异常时的文件名称
-renamesourcefileattribute SourceFile
#抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable

#butterknife 混淆
-keep class butterknife.* { *; }
-dontwarn butterknife.internal.*
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

#avLoading 混淆
-keep class com.wang.avi.* { *; }
-keep class com.wang.avi.indicators.* { *; }

# 枚举类不能被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
# native方法不混淆
-keepclasseswithmembernames class *{
 native <methods>;
}
-keepattributes EnclosingMethod
# 这指定了继承Serizalizable的类的如下成员不被移除混淆
-keepclassmembers class * implements java.io.Serializable {
   *;
}

-keepclassmembers class * implements android.os.Parcelable {
   # 保持Parcelable不被混淆
    public static final android.os.Parcelable$Creator *;
}

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}