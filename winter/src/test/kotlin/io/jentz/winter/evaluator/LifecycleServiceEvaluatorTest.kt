package io.jentz.winter.evaluator

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.jentz.winter.*
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.eclipse.jgit.merge.StrategySimpleTwoWayInCore
import org.junit.jupiter.api.Test

class LifecycleServiceEvaluatorTest {

    private val graph = emptyGraph()

    private val plugin: Plugin = mock()

    private val plugins = Plugins(plugin)

    private val evaluator = LifecycleServiceEvaluator(graph, plugins, true)

    @Test
    fun `should call new instance and return result`() {
        evaluator
            .evaluate(BoundTestService(evaluator) { it.toUpperCase() }, "foo")
            .shouldBe("FOO")
    }

    @Test
    fun `should call service and plugin post-construct callbacks`() {
        val service = BoundTestService(evaluator) { it.toUpperCase() }
        evaluator
            .evaluate(service, "foo")
            .shouldBe("FOO")

        service.postConstructCalled.shouldHaveSize(1)
        service.postConstructCalled.first().shouldBe("foo" to "FOO")

        verify(plugin).postConstruct(graph, Scope.Prototype, "foo", "FOO")
    }

    @Test
    fun `should throw DependencyResolutionException if service throws an EntryNotFoundException`() {
        val exception = EntryNotFoundException(typeKey<List<*>>(), "")
        val b = BoundTestService(evaluator, typeKey<String>("b"),
            throwOnNewInstance = { exception })
        val a = BoundTestService(evaluator, typeKey<String>("a"), b)

        shouldThrow<DependencyResolutionException> {
            evaluator.evaluate(a, "")
        }.run {
            key.shouldBe(typeKey<String>("b"))
            message.shouldBe("Error while resolving dependency with key: " +
                    "ClassTypeKey(class java.lang.String qualifier = b) reason: could not find dependency with key " +
                    "ClassTypeKey(interface java.util.List qualifier = null)")
            cause.shouldBeSameInstanceAs(exception)
        }
    }

    @Test
    fun `should throw DependencyResolutionException if service throws an exception`() {
        val exception = Exception()
        val b = BoundTestService(evaluator, typeKey<String>("b"),
            throwOnNewInstance = { exception })
        val a = BoundTestService(evaluator, typeKey<String>("a"), b)

        shouldThrow<DependencyResolutionException> {
            evaluator.evaluate(a, "")
        }.run {
            key.shouldBe(typeKey<String>("b"))
            message.shouldBe("Factory of dependency with key " +
                    "ClassTypeKey(class java.lang.String qualifier = b) threw an exception on invocation.")
            cause.shouldBeSameInstanceAs(exception)
        }
    }

    @Test
    fun `should check for cyclic dependencies`() {
        val d = BoundTestService(evaluator, typeKey<String>("d"))
        val c = BoundTestService(evaluator, typeKey<String>("c"), d)
        val b = BoundTestService(evaluator, typeKey<String>("b"), c)
        val a = BoundTestService(evaluator, typeKey<String>("a"), b)
        d.dependency = b

        shouldThrow<CyclicDependencyException> {
            evaluator.evaluate(a, "")
        }.message.shouldBe(
            "Cyclic dependency found: `ClassTypeKey(class java.lang.String qualifier = b)` is dependent of itself.\n" +
                    "Dependency chain: " +
                    "ClassTypeKey(class java.lang.String qualifier = b) -> " +
                    "ClassTypeKey(class java.lang.String qualifier = c) -> " +
                    "ClassTypeKey(class java.lang.String qualifier = d) => " +
                    "ClassTypeKey(class java.lang.String qualifier = b)"
        )
    }

    @Test
    fun `should check for direct cyclic dependencies`() {
        val a = BoundTestService(evaluator, typeKey<String>("a"))
        a.dependency = a

        shouldThrow<CyclicDependencyException> {
            evaluator.evaluate(a, "")
        }.message.shouldBe(
            "Cyclic dependency found: `ClassTypeKey(class java.lang.String qualifier = a)` is directly dependent of itself.\n" +
                    "Dependency chain: " +
                    "ClassTypeKey(class java.lang.String qualifier = a) => " +
                    "ClassTypeKey(class java.lang.String qualifier = a)"
        )
    }

}
