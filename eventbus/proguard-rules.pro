# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\DevTools\AndroidSdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#保持注解不被移除
-keepattributes *Annotation*
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
#EventBus总线，订阅者的回调方法不被移除
-keepclassmembers class ** {
    @org.greenrobot.eventbus.annotation.Subscribe public <methods>;
}
#EventBus总线调用自定义的静态方法，类名和方法名不被移除和重命名
-keep @org.greenrobot.eventbus.annotation.Service public class ** {
   public static  <methods>;
}
#EventBus打开activity，默认混淆文件会保证activity类不会被重命名

#Gson解析
-keepattributes Signature
-keep class sun.misc.Unsafe {*;}
-keep class com.google.gson.stream.** {*;}