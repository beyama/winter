package io.jentz.winter

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test

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
        val exception = EntryNotFoundException(typeKey<List<*>>(), "")
        val b = BoundTestService(evaluator, typeKey("b"), throwOnNewInstance = { exception })
        val a = BoundTestService(evaluator, typeKey("a"), b)

        shouldThrow<DependencyResolutionException> {
            evaluator.evaluate(a, emptyGraph())
        }.run {
            key.shouldBe(typeKey<String>("b"))
            message.shouldBe("Error while resolving dependency with key: " +
                    "ClassTypeKey(class java.lang.String qualifier = b) " +
                    "reason: could not find dependency with key " +
                    "ClassTypeKey(interface java.util.List qualifier = null)")
            cause.shouldBeSameInstanceAs(exception)
        }
    }

    @Test
    fun `should throw DependencyResolutionException if service throws an exception`() {
        val exception = Exception()
        val b = BoundTestService(evaluator, typeKey("b"),
            throwOnNewInstance = { exception })
        val a = BoundTestService(evaluator, typeKey("a"), b)

        shouldThrow<DependencyResolutionException> {
            evaluator.evaluate(a, emptyGraph())
        }.run {
            key.shouldBe(typeKey<String>("b"))
            message.shouldBe(
                "Factory of dependency with key " +
                        "ClassTypeKey(class java.lang.String qualifier = b) " +
                        "threw an exception on invocation.")
            cause.shouldBeSameInstanceAs(exception)
        }
    }

    @Test
    fun `should check for cyclic dependencies`() {
        val d = BoundTestService(evaluator, typeKey("d"))
        val c = BoundTestService(evaluator, typeKey("c"), d)
        val b = BoundTestService(evaluator, typeKey("b"), c)
        val a = BoundTestService(evaluator, typeKey("a"), b)
        d.dependency = b

        shouldThrow<CyclicDependencyException> {
            evaluator.evaluate(a, emptyGraph())
        }.message.shouldBe(
            "Cyclic dependency found: " +
                    "`ClassTypeKey(class java.lang.String qualifier = b)` " +
                    "is dependent of itself.\n" +
                    "Dependency chain: " +
                    "ClassTypeKey(class java.lang.String qualifier = b) -> " +
                    "ClassTypeKey(class java.lang.String qualifier = c) -> " +
                    "ClassTypeKey(class java.lang.String qualifier = d) => " +
                    "ClassTypeKey(class java.lang.String qualifier = b)"
        )
    }

    @Test
    fun `should check for direct cyclic dependencies`() {
        val a = BoundTestService(evaluator, typeKey("a"))
        a.dependency = a

        shouldThrow<CyclicDependencyException> {
            evaluator.evaluate(a, emptyGraph())
        }.message.shouldBe(
            "Cyclic dependency found: " +
                    "`ClassTypeKey(class java.lang.String qualifier = a)` " +
                    "is directly dependent of itself.\n" +
                    "Dependency chain: " +
                    "ClassTypeKey(class java.lang.String qualifier = a) => " +
                    "ClassTypeKey(class java.lang.String qualifier = a)"
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

        verify(plugin).postConstruct(graph, Scope.Prototype, "FOO")
    }

}
