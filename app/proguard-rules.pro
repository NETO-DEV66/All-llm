-keepclasseswithmembernames class com.netodev66.alllm.runtime.NativeSpawnProcessKt$NativeSpawn {
    native <methods>;
}
-keep class com.github.luben.zstd.** { *; }

# Tink (pulled in by androidx.security-crypto) references annotations that are
# compile-only and absent from the Android runtime. Firebase used to supply them
# transitively; after its removal R8 must be told to ignore them.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
