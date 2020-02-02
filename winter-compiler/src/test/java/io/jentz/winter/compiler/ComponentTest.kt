package io.jentz.winter.compiler

import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import org.junit.jupiter.api.Test

class ComponentTest : BaseProcessorTest() {

    @Test
    fun `should generate component in configured package`() {
        val compilation = Compiler.javac()
            .withProcessors(WinterProcessor())
            .withOptions("-A$OPTION_GENERATED_COMPONENT_PACKAGE=test")
            .compile(
                JavaFileObjects.forResource("InjectConstructorAnnotation.java"),
                JavaFileObjects.forResource("NamedSingletonInjectConstructor.java"),
                JavaFileObjects.forResource("WithCustomScope.java"),
                JavaFileObjects.forResource("PrototypeAnnotation.java")
            )

        CompilationSubject.assertThat(compilation)
            .generatedSourceFile("test.GeneratedComponent")
            .hasSourceEquivalentTo(
                JavaFileObjects.forResource("GeneratedComponent.java")
            )
    }

}