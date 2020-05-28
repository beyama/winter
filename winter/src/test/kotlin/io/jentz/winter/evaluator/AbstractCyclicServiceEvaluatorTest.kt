package io.jentz.winter.evaluator

import io.jentz.winter.CyclicDependencyException
import io.jentz.winter.emptyGraph
import io.jentz.winter.typeKey
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test

abstract class AbstractCyclicServiceEvaluatorTest : AbstractServiceEvaluatorTest() {

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

}