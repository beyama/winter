package io.jentz.winter.compiler

import com.google.common.truth.Truth.assert_
import com.google.testing.compile.JavaFileObjects.forResource
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.junit.jupiter.api.Test

@KotlinPoetMetadataPreview
class FactoryTest : BaseProcessorTest() {

    @Test
    fun `should generate factory for class with no argument constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("NoArgumentInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("NoArgumentInjectConstructor_WinterFactory")
    }

    @Test
    fun `should generate factory for class with one argument constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("OneArgumentInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("OneArgumentInjectConstructor_WinterFactory")
    }

    @Test
    fun `should generate factory for class with one named argument constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("OneNamedArgumentInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("OneNamedArgumentInjectConstructor_WinterFactory")
    }

    @Test
    fun `should generate factory for class with members injector`() {
        assert_()
            .about(javaSource())
            .that(forResource("WithInjectedField.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("WithInjectedField_WinterFactory")
    }

    @Test
    fun `should generate factory for named singleton inject constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("NamedSingletonInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("NamedSingletonInjectConstructor_WinterFactory")
    }

    @Test
    fun `should generate factory that registers as prototype if class is annotated with @Prototype`() {
        assert_()
            .about(javaSource())
            .that(forResource("PrototypeAnnotation.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("PrototypeAnnotation_WinterFactory")
    }

    @Test
    fun `should generate factory that registers as eagerSingleton if class is annotated with @EagerSingleton`() {
        assert_()
            .about(javaSource())
            .that(forResource("EagerSingletonAnnotation.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("EagerSingletonAnnotation_WinterFactory")
    }

    @Test
    fun `should fail if class is annotated with @Prototype and @EagerSingleton`() {
        assert_()
            .about(javaSource())
            .that(forResource("PrototypeAndEagerSingletonAnnotation.java"))
            .processedWith(WinterProcessor())
            .failsToCompile()
            .withErrorContaining("Class can either be annotated with EagerSingleton or Prototype but not both.")
    }

    @Test
    fun `should fail if annotated constructor is private`() {
        assert_()
            .about(javaSource())
            .that(forResource("PrivateNoArgumentInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .failsToCompile()
            .withErrorContaining(
                "Cannot inject a private constructor."
            )
    }

    @Test
    fun `should generate factory for class with lazy and provider arguments constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("InjectConstructorWithProviderAndLazyArguments.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("InjectConstructorWithProviderAndLazyArguments_WinterFactory")
    }

    @Test
    fun `should generate factory with different type when annotated with FactoryType`() {
        assert_()
            .about(javaSource())
            .that(forResource("FactoryTypeAnnotation.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()

        generatesSource("FactoryTypeAnnotation_WinterFactory")
    }

    @Test
    fun `should fail if factory type is declared in FactoryType and InjectConstructor annotation`() {
        assert_()
            .about(javaSource())
            .that(forResource("FactoryTypeAnnotationAndTypeInInjectConstructorAnnotation.java"))
            .processedWith(WinterProcessor())
            .failsToCompile()
            .withErrorContaining("Factory type can be declared via InjectConstructor or FactoryType annotation but not both.")
    }

}
