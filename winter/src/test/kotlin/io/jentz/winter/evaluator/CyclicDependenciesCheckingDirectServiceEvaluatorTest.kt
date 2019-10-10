package io.jentz.winter.evaluator

class CyclicDependenciesCheckingDirectServiceEvaluatorTest : AbstractCyclicServiceEvaluatorTest() {
    override val evaluator = CyclicDependenciesCheckingDirectServiceEvaluator()
}
