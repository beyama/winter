package io.jentz.winter.compiler

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class MembersInjectorTest : BaseProcessorTest() {

    @Test
    fun `should generate injector for class with string field`() {
        compiler()
            .compileSuccessful("WithInjectedField.java")

        generatedFile("WithInjectedField_WinterMembersInjector").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Graph
        |import io.jentz.winter.MembersInjector
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class WithInjectedField_WinterMembersInjector : MembersInjector<WithInjectedField> {
        |
        |    override fun invoke(graph: Graph, target: WithInjectedField) {
        |        target.field0 = graph.instanceOrNull<String>()
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should invoke superclass injector`() {
        compiler()
            .compileSuccessful("WithInjectedField.java", "WithInjectedFieldExtended.java")

        generatedFile("WithInjectedFieldExtended_WinterMembersInjector").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Graph
        |import io.jentz.winter.MembersInjector
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class WithInjectedFieldExtended_WinterMembersInjector : MembersInjector<WithInjectedFieldExtended> {
        |
        |    override fun invoke(graph: Graph, target: WithInjectedFieldExtended) {
        |        io.jentz.winter.compilertest.WithInjectedField_WinterMembersInjector().invoke(graph, target)
        |        target.field1 = graph.instanceOrNull<String>()
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should generate injector for field with generics type`() {
        compiler()
            .compileSuccessful("WithInjectedGenericFields.java")

        generatedFile("WithInjectedGenericFields_WinterMembersInjector").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Graph
        |import io.jentz.winter.MembersInjector
        |import java.util.List
        |import java.util.Map
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class WithInjectedGenericFields_WinterMembersInjector : MembersInjector<WithInjectedGenericFields> {
        |
        |    override fun invoke(graph: Graph, target: WithInjectedGenericFields) {
        |        target.field0 = graph.instanceOrNull<Map<String, Integer>>(generics = true)
        |        target.field1 = graph.instanceOrNull<List<Integer>>(generics = true)
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should generate injector for javax Provider and Lazy fields`() {
        compiler()
            .compileSuccessful("WithInjectedProviderAndLazyFields.java")

        generatedFile("WithInjectedProviderAndLazyFields_WinterMembersInjector").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Graph
        |import io.jentz.winter.MembersInjector
        |import java.util.List
        |import javax.annotation.Generated
        |import javax.inject.Provider
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class WithInjectedProviderAndLazyFields_WinterMembersInjector : MembersInjector<WithInjectedProviderAndLazyFields> {
        |
        |    override fun invoke(graph: Graph, target: WithInjectedProviderAndLazyFields) {
        |        target.field0 = graph.instanceOrNull<Any>()
        |        target.field1 = Provider { graph.instanceOrNull<List<String>>("stringList", generics = true) }
        |        target.field2 = lazy { graph.instanceOrNull<List<String>>("stringList", generics = true) }
        |    }
        |
        |}
        |
        """.trimMargin())
    }

}
