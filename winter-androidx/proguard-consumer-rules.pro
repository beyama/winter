# Do not obfuscate classes with Inject annotated constructors
-keepclasseswithmembernames class * { @javax.inject.Inject <init>(...); }

# Do not obfuscate names of classes with InjectConstructor annotation
-keepnames @io.jentz.winter.inject.InjectConstructor class *

# Do not obfuscate classes with Injected Fields
-keepclasseswithmembernames class * { @javax.inject.Inject <fields>; }

# Do not obfuscate classes with Injected Methods
-keepclasseswithmembernames class * { @javax.inject.Inject <methods>; }

# Do not obfuscate classes with inject delegates
-keepclasseswithmembernames class * { io.jentz.winter.delegate.* *; }

# Do not remove constructors, methods and fields annotated with Inject
-keepclassmembers class * {
	@javax.inject.Inject <init>(...);
	@javax.inject.Inject <init>();
	@javax.inject.Inject <fields>;
	public <init>(...);
    io.jentz.winter.delegate.* *;
}

# Keep Winter generated factories and members injectors
-keep class * implements io.jentz.winter.inject.Factory { *; }
-keep class * implements io.jentz.winter.inject.MembersInjector { *; }