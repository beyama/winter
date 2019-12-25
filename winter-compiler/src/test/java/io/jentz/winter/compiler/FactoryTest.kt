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
        |import io.jentz.winter.Factory
        |import io.jentz.winter.Graph
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class NoArgumentInjectConstructor_WinterFactory : Factory<Graph, NoArgumentInjectConstructor> {
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
        |import io.jentz.winter.Factory
        |import io.jentz.winter.Graph
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class OneArgumentInjectConstructor_WinterFactory : Factory<Graph, OneArgumentInjectConstructor> {
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
        |import io.jentz.winter.Factory
        |import io.jentz.winter.Graph
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class OneNamedArgumentInjectConstructor_WinterFactory : Factory<Graph, OneNamedArgumentInjectConstructor> {
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
        |import io.jentz.winter.Factory
        |import io.jentz.winter.Graph
        |import java.util.List
        |import javax.annotation.Generated
        |import javax.inject.Provider
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class InjectConstructorWithProviderAndLazyArguments_WinterFactory : Factory<Graph, InjectConstructorWithProviderAndLazyArguments> {
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
        |import io.jentz.winter.Factory
        |import io.jentz.winter.Graph
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class WithInjectedField_WinterFactory : Factory<Graph, WithInjectedField> {
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

}
