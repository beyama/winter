package io.jentz.winter.compiler

import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.JavaFileObjects.forResource
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class InjectConstructorAnnotationTest : BaseProcessorTest() {

    @Test
    fun `should fail if class contains more than one constructor`() {
        val compilation = compiler()
            .compile(forResource("InjectConstructorAnnotationWithTwoConstructors.java"))

        assertThat(compilation)
            .hadErrorContaining(
                "Class `io.jentz.winter.compilertest.InjectConstructorAnnotationWithTwoConstructors` " +
                        "is annotated with InjectConstructor and therefore must not have more than one constructor."
            )
    }

    @Test
    fun `should fail if class contains an Inject annotated constructor`() {
        val compilation = compiler()
            .compile(forResource("InjectConstructorAnnotationWithInjectConstructor.java"))

        assertThat(compilation)
            .hadErrorContaining(
                "Class `io.jentz.winter.compilertest.InjectConstructorAnnotationWithInjectConstructor` " +
                        "is annotated with InjectConstructor and therefore must not have a constructor with Inject annotation."
            )
    }

    @Test
    fun `should generate factory`() {
        compiler()
            .compileSuccessful("InjectConstructorAnnotation.java")

        generatedFile("InjectConstructorAnnotation_WinterFactory").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component.Builder
        |import io.jentz.winter.Graph
        |import io.jentz.winter.inject.Factory
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class InjectConstructorAnnotation_WinterFactory : Factory<InjectConstructorAnnotation> {
        |
        |    override fun register(builder: Builder) {
        |        builder.prototype(factory = this)
        |    }
        |
        |    override fun invoke(graph: Graph): InjectConstructorAnnotation {
        |        return InjectConstructorAnnotation(graph.instanceOrNull<String>())
        |    }
        |
        |}
        |""".trimMargin())
    }

}