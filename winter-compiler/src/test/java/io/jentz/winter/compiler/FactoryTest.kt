package io.jentz.winter.compiler

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class FactoryTest : BaseProcessorTest() {

    @Test
    fun `should generate factory for class with no argument constructor`() {
        compiler()
            .compileSuccessful("NoArgumentInjectConstructor.java")

        generatedFile("NoArgumentInjectConstructor_WinterFactory").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component.Builder
        |import io.jentz.winter.Graph
        |import io.jentz.winter.TypeKey
        |import io.jentz.winter.inject.Factory
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class NoArgumentInjectConstructor_WinterFactory : Factory<NoArgumentInjectConstructor> {
        |
        |    override fun register(builder: Builder): TypeKey<NoArgumentInjectConstructor> {
        |        return builder.singleton(factory = this)
        |    }
        |
        |    override fun invoke(graph: Graph): NoArgumentInjectConstructor {
        |        return NoArgumentInjectConstructor()
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should generate factory for class with one argument constructor`() {
        compiler()
            .compileSuccessful("OneArgumentInjectConstructor.java")

        generatedFile("OneArgumentInjectConstructor_WinterFactory").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component.Builder
        |import io.jentz.winter.Graph
        |import io.jentz.winter.TypeKey
        |import io.jentz.winter.inject.Factory
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class OneArgumentInjectConstructor_WinterFactory : Factory<OneArgumentInjectConstructor> {
        |
        |    override fun register(builder: Builder): TypeKey<OneArgumentInjectConstructor> {
        |        return builder.prototype(factory = this)
        |    }
        |
        |    override fun invoke(graph: Graph): OneArgumentInjectConstructor {
        |        return OneArgumentInjectConstructor(graph.instanceOrNull<String>())
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should generate factory for class with one named argument constructor`() {
        compiler()
            .compileSuccessful("OneNamedArgumentInjectConstructor.java")

        generatedFile("OneNamedArgumentInjectConstructor_WinterFactory").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component.Builder
        |import io.jentz.winter.Graph
        |import io.jentz.winter.TypeKey
        |import io.jentz.winter.inject.Factory
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class OneNamedArgumentInjectConstructor_WinterFactory : Factory<OneNamedArgumentInjectConstructor> {
        |
        |    override fun register(builder: Builder): TypeKey<OneNamedArgumentInjectConstructor> {
        |        return builder.prototype(factory = this)
        |    }
        |
        |    override fun invoke(graph: Graph): OneNamedArgumentInjectConstructor {
        |        return OneNamedArgumentInjectConstructor(graph.instanceOrNull<String>("a name"))
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should generate factory for class with lazy and provider arguments constructor`() {
        compiler()
            .compileSuccessful("InjectConstructorWithProviderAndLazyArguments.java")

        generatedFile("InjectConstructorWithProviderAndLazyArguments_WinterFactory").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component.Builder
        |import io.jentz.winter.Graph
        |import io.jentz.winter.TypeKey
        |import io.jentz.winter.inject.Factory
        |import java.util.List
        |import javax.annotation.Generated
        |import javax.inject.Provider
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class InjectConstructorWithProviderAndLazyArguments_WinterFactory : Factory<InjectConstructorWithProviderAndLazyArguments> {
        |
        |    override fun register(builder: Builder): TypeKey<InjectConstructorWithProviderAndLazyArguments> {
        |        return builder.prototype(factory = this)
        |    }
        |
        |    override fun invoke(graph: Graph): InjectConstructorWithProviderAndLazyArguments {
        |        return InjectConstructorWithProviderAndLazyArguments(
        |            Provider { graph.instanceOrNull<List<String>>("stringList", generics = true) },
        |            lazy { graph.instanceOrNull<List<String>>("stringList", generics = true) }
        |        )
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should generate factory for class with members injector`() {
        compiler()
            .compileSuccessful("WithInjectedField.java")

        generatedFile("WithInjectedField_WinterFactory").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component.Builder
        |import io.jentz.winter.Graph
        |import io.jentz.winter.TypeKey
        |import io.jentz.winter.inject.Factory
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class WithInjectedField_WinterFactory : Factory<WithInjectedField> {
        |
        |    override fun register(builder: Builder): TypeKey<WithInjectedField> {
        |        return builder.prototype(factory = this)
        |    }
        |
        |    override fun invoke(graph: Graph): WithInjectedField {
        |        val instance = WithInjectedField()
        |        io.jentz.winter.compilertest.WithInjectedField_WinterMembersInjector().invoke(graph, instance)
        |        return instance
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should generate factory for named singleton inject constructor`() {
        compilerWithOptions(ARG_GENERATED_COMPONENT)
            .compileSuccessful("NamedSingletonInjectConstructor.java")

        generatedFile("NamedSingletonInjectConstructor_WinterFactory").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component.Builder
        |import io.jentz.winter.Graph
        |import io.jentz.winter.TypeKey
        |import io.jentz.winter.inject.Factory
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class NamedSingletonInjectConstructor_WinterFactory : Factory<NamedSingletonInjectConstructor> {
        |
        |    override fun register(builder: Builder): TypeKey<NamedSingletonInjectConstructor> {
        |        return builder.singleton(qualifier = "variant1", factory = this)
        |    }
        |
        |    override fun invoke(graph: Graph): NamedSingletonInjectConstructor {
        |        return NamedSingletonInjectConstructor()
        |    }
        |
        |}
        |""".trimMargin())
    }

}
