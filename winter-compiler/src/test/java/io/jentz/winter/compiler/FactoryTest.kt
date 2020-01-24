package io.jentz.winter.compiler

import com.google.common.truth.Truth.assert_
import com.google.testing.compile.JavaFileObjects.forResource
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import org.junit.jupiter.api.Test

class FactoryTest : BaseProcessorTest() {

    @Test
    fun `should generate factory for class with no argument constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("NoArgumentInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("NoArgumentInjectConstructor_WinterFactory.java"))
    }

    @Test
    fun `should generate factory for class with one argument constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("OneArgumentInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("OneArgumentInjectConstructor_WinterFactory.java"))
    }

    @Test
    fun `should generate factory for class with one named argument constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("OneNamedArgumentInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("OneNamedArgumentInjectConstructor_WinterFactory.java"))
    }

    @Test
    fun `should generate factory for class with members injector`() {
        assert_()
            .about(javaSource())
            .that(forResource("WithInjectedField.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("WithInjectedField_WinterFactory.java"))
    }

    @Test
    fun `should generate factory for named singleton inject constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("NamedSingletonInjectConstructor.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("NamedSingletonInjectConstructor_WinterFactory.java"))
    }

    @Test
    fun `should generate factory that registers as prototype if class is annotated with @Prototype`() {
        assert_()
            .about(javaSource())
            .that(forResource("PrototypeAnnotation.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("PrototypeAnnotation_WinterFactory.java"))
    }

    @Test
    fun `should generate factory that registers as eagerSingleton if class is annotated with @EagerSingleton`() {
        assert_()
            .about(javaSource())
            .that(forResource("EagerSingletonAnnotation.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("EagerSingletonAnnotation_WinterFactory.java"))
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
    fun `should generate factory for class with lazy and provider arguments constructor`() {
        assert_()
            .about(javaSource())
            .that(forResource("InjectConstructorWithProviderAndLazyArguments.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("InjectConstructorWithProviderAndLazyArguments_WinterFactory.java"))
    }

    @Test
    fun `should generate factory with different type when annotated with FactoryType`() {
        assert_()
            .about(javaSource())
            .that(forResource("FactoryTypeAnnotation.java"))
            .processedWith(WinterProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(forResource("FactoryTypeAnnotation_WinterFactory.java"))
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
