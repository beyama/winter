package io.jentz.winter.evaluator

import io.jentz.winter.DependencyResolutionException
import io.jentz.winter.EntryNotFoundException
import io.jentz.winter.typeKey
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test

class DirectServiceEvaluatorTest {

    private val evaluator = DirectServiceEvaluator()

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

}
