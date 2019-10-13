package io.jentz.winter.evaluator

import io.jentz.winter.CyclicDependencyException
import io.jentz.winter.compoundTypeKey
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test

abstract class AbstractCyclicServiceEvaluatorTest : AbstractServiceEvaluatorTest() {

    @Test
    fun `should check for cyclic dependencies`() {
        val d = BoundTestService(evaluator, compoundTypeKey("d"))
        val c = BoundTestService(evaluator, compoundTypeKey("c"), d)
        val b = BoundTestService(evaluator, compoundTypeKey("b"), c)
        val a = BoundTestService(evaluator, compoundTypeKey("a"), b)
        d.dependency = b

        shouldThrow<CyclicDependencyException> {
            evaluator.evaluate(a, "")
        }.message.shouldBe(
            "Cyclic dependency found: " +
                    "`CompoundClassTypeKey(class java.lang.String class java.lang.String qualifier = b)` " +
                    "is dependent of itself.\n" +
                    "Dependency chain: " +
                    "CompoundClassTypeKey(class java.lang.String class java.lang.String qualifier = b) -> " +
                    "CompoundClassTypeKey(class java.lang.String class java.lang.String qualifier = c) -> " +
                    "CompoundClassTypeKey(class java.lang.String class java.lang.String qualifier = d) => " +
                    "CompoundClassTypeKey(class java.lang.String class java.lang.String qualifier = b)"
        )
    }

    @Test
    fun `should check for direct cyclic dependencies`() {
        val a = BoundTestService(evaluator, compoundTypeKey("a"))
        a.dependency = a

        shouldThrow<CyclicDependencyException> {
            evaluator.evaluate(a, "")
        }.message.shouldBe(
            "Cyclic dependency found: " +
                    "`CompoundClassTypeKey(class java.lang.String class java.lang.String qualifier = a)` " +
                    "is directly dependent of itself.\n" +
                    "Dependency chain: " +
                    "CompoundClassTypeKey(class java.lang.String class java.lang.String qualifier = a) => " +
                    "CompoundClassTypeKey(class java.lang.String class java.lang.String qualifier = a)"
        )
    }

}