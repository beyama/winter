package io.jentz.winter

import io.jentz.winter.plugin.Plugins
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ServiceEvaluatorTest {

    private val graph = emptyGraph()

    private val plugin = RecordingPlugin()

    private val plugins = Plugins(plugin)

    private val evaluator = ServiceEvaluator(graph, plugins)

    private val stringName = String::class.displayName

    private val listName = List::class.displayName

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
                    "TypeKey($stringName, qualifier = qualifier(b), isOptional = false) " +
                    "reason: could not find dependency with key " +
                    "TypeKey($listName, qualifier = null, isOptional = false)")
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
                        "TypeKey($stringName, qualifier = qualifier(b), isOptional = false) " +
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
                    "`TypeKey($stringName, qualifier = qualifier(b), isOptional = false)` " +
                    "is dependent of itself.\n" +
                    "Dependency chain: " +
                    "TypeKey($stringName, qualifier = qualifier(b), isOptional = false) -> " +
                    "TypeKey($stringName, qualifier = qualifier(c), isOptional = false) -> " +
                    "TypeKey($stringName, qualifier = qualifier(d), isOptional = false) => " +
                    "TypeKey($stringName, qualifier = qualifier(b), isOptional = false)"
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
                    "`TypeKey($stringName, qualifier = qualifier(a), isOptional = false)` " +
                    "is directly dependent of itself.\n" +
                    "Dependency chain: " +
                    "TypeKey($stringName, qualifier = qualifier(a), isOptional = false) => " +
                    "TypeKey($stringName, qualifier = qualifier(a), isOptional = false)"
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

        plugin.postConstruct.shouldContainExactly(PostConstruct(graph, Prototype, "FOO"))
    }

}
