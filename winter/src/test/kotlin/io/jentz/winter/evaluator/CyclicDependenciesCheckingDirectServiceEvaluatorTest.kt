package io.jentz.winter.evaluator

import io.jentz.winter.*
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test

class CyclicDependenciesCheckingDirectServiceEvaluatorTest {

    private val evaluator = CyclicDependenciesCheckingDirectServiceEvaluator()

    @Test
    fun `should call new instance and return result`() {
        evaluator
            .evaluate(BoundTestService(evaluator) { it.toUpperCase() }, "foo")
            .shouldBe("FOO")
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
            message.shouldBe("Factory of dependency with key ClassTypeKey(class java.lang.String qualifier = b) threw an exception on invocation.")
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
