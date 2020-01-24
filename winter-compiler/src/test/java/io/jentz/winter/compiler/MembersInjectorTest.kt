package io.jentz.winter.compiler

import com.google.common.truth.Truth.assert_
import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects.forResource
import com.google.testing.compile.JavaSourceSubjectFactory
import org.junit.jupiter.api.Test


class MembersInjectorTest : BaseProcessorTest() {

    @Test
    fun `should generate injector for class with string field`() {
        assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(forResource("WithInjectedField.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("WithInjectedField_WinterMembersInjector.java"))
    }

    @Test
    fun `should invoke superclass injector`() {
        val compilation = Compiler.javac()
            .withProcessors(WinterProcessor())
            .compile(
                forResource("WithInjectedField.java"),
                forResource("WithInjectedFieldExtended.java")
            )

        assertThat(compilation)
            .generatedSourceFile("test.WithInjectedFieldExtended_WinterMembersInjector")
            .hasSourceEquivalentTo(
                forResource("WithInjectedFieldExtended_WinterMembersInjector.java")
            )
    }

    @Test
    fun `should generate injector for field with generics type`() {
        assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(forResource("WithInjectedGenericFields.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("WithInjectedGenericFields_WinterMembersInjector.java"))
    }

    @Test
    fun `should generate injector for javax Provider and Lazy fields`() {
        assert_()
            .about(JavaSourceSubjectFactory.javaSource())
            .that(forResource("WithInjectedProviderAndLazyFields.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("WithInjectedProviderAndLazyFields_WinterMembersInjector.java"))
    }

}
