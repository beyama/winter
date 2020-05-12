package io.jentz.winter.compiler

import com.google.common.truth.Truth.assert_
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects.forResource
import com.google.testing.compile.JavaSourceSubjectFactory
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.junit.jupiter.api.Test


@KotlinPoetMetadataPreview
class MembersInjectorTest : BaseProcessorTest() {

    @Test
    fun `should generate injector for class with string field`() {
        assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(forResource("WithInjectedField.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("WithInjectedField_WinterMembersInjector")
    }

    @Test
    fun `should invoke superclass injector`() {
        Compiler.javac()
            .withProcessors(WinterProcessor())
            .compile(
                forResource("WithInjectedField.java"),
                forResource("WithInjectedFieldExtended.java")
            )

        generatesSource("WithInjectedFieldExtended_WinterMembersInjector")
    }

    @Test
    fun `should generate injector for field with generics type`() {
        assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(forResource("WithInjectedGenericFields.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("WithInjectedGenericFields_WinterMembersInjector")
    }

    @Test
    fun `should generate injector for javax Provider and Lazy fields`() {
        assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(forResource("WithInjectedProviderAndLazyFields.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("WithInjectedProviderAndLazyFields_WinterMembersInjector")
    }

}
