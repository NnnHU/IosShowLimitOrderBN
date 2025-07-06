# 添加混淆规则
-keep class com.pythonn.androidshowlimitorderbn.data.model.** { *; }
-keep class com.pythonn.androidshowlimitorderbn.data.remote.** { *; }

# Retrofit和Gson规则
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }