package io.jentz.winter.compiler

import com.google.common.truth.Truth.assert_
import com.google.testing.compile.JavaFileObjects.forResource
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import org.junit.jupiter.api.Test


class InjectConstructorAnnotationTest : BaseProcessorTest() {

    @Test
    fun `should fail if class contains more than one constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("InjectConstructorAnnotationWithTwoConstructors.java"))
            .processedWith(WinterProcessor())
            .failsToCompile()
            .withErrorContaining(
                "Class `test.InjectConstructorAnnotationWithTwoConstructors` " +
                        "is annotated with InjectConstructor and therefore must have exactly one non-private constructor."
            )
    }

    @Test
    fun `should fail if class contains an Inject annotated constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("InjectConstructorAnnotationWithInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .failsToCompile()
            .withErrorContaining(
                "Class `test.InjectConstructorAnnotationWithInjectConstructor` " +
                        "is annotated with InjectConstructor and therefore must not have a constructor with Inject annotation."
            )
    }

    @Test
    fun `should generate factory`() {
        assert_()
            .about(javaSource())
            .that(forResource("InjectConstructorAnnotation.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("InjectConstructorAnnotation_WinterFactory.java"))
    }

    @Test
    fun `should register factory with type from annotation`() {
        assert_()
            .about(javaSource())
            .that(forResource("InjectConstructorAnnotationWithType.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("InjectConstructorAnnotationWithType_WinterFactory.java"))
    }

}