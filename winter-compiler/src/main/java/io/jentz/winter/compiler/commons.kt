package io.jentz.winter.compiler

import com.squareup.javapoet.ClassName
import java.text.SimpleDateFormat
import java.util.*

val GENERATED_ANNOTATION_LEGACY_INTERFACE_NAME: ClassName =
    ClassName.get("javax.annotation", "Generated")

val GENERATED_ANNOTATION_JDK9_INTERFACE_NAME: ClassName =
    ClassName.get("javax.annotation.processing", "Generated")

val ISO8601_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

val notNullAnnotations = setOf(
    "org.jetbrains.annotations.NotNull",
    "javax.validation.constraints.NotNull",
    "edu.umd.cs.findbugs.annotations.NonNull",
    "javax.annotation.Nonnull",
    "lombok.NonNull",
    "android.support.annotation.NonNull",
    "org.eclipse.jdt.annotation.NonNull"
)
