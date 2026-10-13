# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *

# Moshi 反射序列化的数据类（关键）
#
# AppModule 里用的是 KotlinJsonAdapterFactory（反射式），不是 codegen，
# 所以 JSON 的 key 直接取构造参数名。R8 默认会把这些名字混淆成 a/b/c，
# 结果：
#   - ExpenseParseResult 字段全对不上 -> release 版 AI 解析必然失败
#   - ExportData 字段被改名       -> release 导出的备份与 debug 版互不兼容
# 这两个问题在 debug 版完全看不出来，只在 release 版复现。
-keep class com.example.expensetracker.domain.model.ExpenseParseResult { *; }
-keep class com.example.expensetracker.data.repository.ExportData { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# DTO classes (DeepSeek API)
-keep class com.example.expensetracker.data.remote.dto.** { *; }

# Entity classes
-keep class com.example.expensetracker.data.local.entity.** { *; }

# androidx.security:security-crypto 依赖的 Tink 引用了 errorprone 注解，
# 这些类仅存在于编译期、不在运行时 classpath 上。
# AGP 8 默认把"缺失类"当硬错误，不 -dontwarn 会导致 R8 直接失败。
-dontwarn com.google.errorprone.annotations.**

# ONNX Runtime（离屏语音识别引擎）
#
# ORT 的 native 层（libonnxruntime4j_jni.so）不通过 JNI 注册表，而是用
# FindClass / GetMethodID 按**字符串类名和字段名**回查 Java 侧。R8 默认会
# 把它们当普通类改名：
#   ai.onnxruntime.OnnxJavaType             -> b.a
#   ai.onnxruntime.OrtEnvironment$OrtEnvCloser -> b.b
#   ai.onnxruntime.OrtException             -> b.d
#   ai.onnxruntime.OrtException$OrtErrorCode -> b.c
#   ai.onnxruntime.OrtLoggingLevel          -> b.e
#   ai.onnxruntime.OrtProvider              -> b.f
#   ai.onnxruntime.OrtSession$Result        -> b.g
#   ai.onnxruntime.OrtUtil                  -> b.h
#   ai.onnxruntime.TensorInfo$OnnxTensorType -> b.i
#   ai.onnxruntime.platform.Fp16Conversions -> c.a
# 改名后 native 找不到这些类，进程直接原生崩溃（SIGABRT），
# logcat 里**看不到任何 Java 异常**，只有 app 突然消失。
# 症状：release 版一点语音识别就闪退，debug 版完全正常。
-keep class ai.onnxruntime.** { *; }
-keep interface ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

