package io.jentz.winter

import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ServiceEvaluatorTest {

    private val graph = emptyGraph()

    private val plugin: Plugin = mock()

    private val plugins = Plugins(plugin)

    private val evaluator = ServiceEvaluator(graph, plugins)

    @Test
    fun `should call new instance and return result`() {
        evaluator
            .evaluate(BoundTestService(evaluator) { "FOO" }, emptyGraph())
            .shouldBe("FOO")
    }

    @Test
    fun `should throw DependencyResolutionException if service throws an EntryNotFoundException`() {
        val exception = EntryNotFoundException(erased<List<*>>(), "")
        val b = BoundTestService(evaluator, erased(QualifierB), throwOnNewInstance = { exception })
        val a = BoundTestService(evaluator, erased(QualifierA), b)

        shouldThrow<DependencyResolutionException> {
            evaluator.evaluate(a, emptyGraph())
        }.run {
            key.shouldBe(erased<String>(QualifierB))
            message.shouldBe("Error while resolving dependency with key: " +
                    "TypeKey(java.lang.String, qualifier = qualifier(b), isOptional = false) " +
                    "reason: could not find dependency with key " +
                    "TypeKey(java.util.List, qualifier = null, isOptional = false)")
            cause.shouldBeSameInstanceAs(exception)
        }
    }

    @Test
    fun `should throw DependencyResolutionException if service throws an exception`() {
        val exception = Exception()
        val b = BoundTestService(evaluator, erased(QualifierB),
            throwOnNewInstance = { exception })
        val a = BoundTestService(evaluator, erased(QualifierA), b)

        shouldThrow<DependencyResolutionException> {
            evaluator.evaluate(a, emptyGraph())
        }.run {
            key.shouldBe(erased<String>(QualifierB))
            message.shouldBe(
                "Factory of dependency with key " +
                        "TypeKey(java.lang.String, qualifier = qualifier(b), isOptional = false) " +
                        "threw an exception on invocation.")
            cause.shouldBeSameInstanceAs(exception)
        }
    }

    @Test
    fun `should check for cyclic dependencies`() {
        val d = BoundTestService(evaluator, erased(QualifierD))
        val c = BoundTestService(evaluator, erased(QualifierC), d)
        val b = BoundTestService(evaluator, erased(QualifierB), c)
        val a = BoundTestService(evaluator, erased(QualifierA), b)
        d.dependency = b

        shouldThrow<CyclicDependencyException> {
            evaluator.evaluate(a, emptyGraph())
        }.message.shouldBe(
            "Cyclic dependency found: " +
                    "`TypeKey(java.lang.String, qualifier = qualifier(b), isOptional = false)` " +
                    "is dependent of itself.\n" +
                    "Dependency chain: " +
                    "TypeKey(java.lang.String, qualifier = qualifier(b), isOptional = false) -> " +
                    "TypeKey(java.lang.String, qualifier = qualifier(c), isOptional = false) -> " +
                    "TypeKey(java.lang.String, qualifier = qualifier(d), isOptional = false) => " +
                    "TypeKey(java.lang.String, qualifier = qualifier(b), isOptional = false)"
        )
    }

    @Test
    fun `should check for direct cyclic dependencies`() {
        val a = BoundTestService(evaluator, erased(QualifierA))
        a.dependency = a

        shouldThrow<CyclicDependencyException> {
            evaluator.evaluate(a, emptyGraph())
        }.message.shouldBe(
            "Cyclic dependency found: " +
                    "`TypeKey(java.lang.String, qualifier = qualifier(a), isOptional = false)` " +
                    "is directly dependent of itself.\n" +
                    "Dependency chain: " +
                    "TypeKey(java.lang.String, qualifier = qualifier(a), isOptional = false) => " +
                    "TypeKey(java.lang.String, qualifier = qualifier(a), isOptional = false)"
        )
    }

    @Test
    fun `should call service and plugin post-construct callbacks`() {
        val service = BoundTestService(evaluator) { "FOO" }
        evaluator
            .evaluate(service, graph)
            .shouldBe("FOO")

        service.postConstructCalled.shouldHaveSize(1)
        service.postConstructCalled.first().shouldBe("FOO")

        verify(plugin).postConstruct(graph, Prototype, "FOO")
    }

}
