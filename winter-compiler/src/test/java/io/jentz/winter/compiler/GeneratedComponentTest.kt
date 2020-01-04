package io.jentz.winter.compiler

import com.google.testing.compile.JavaFileObjects.forResource
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class GeneratedComponentTest : BaseProcessorTest() {

    private val noArgumentInjectConstructor = forResource("NoArgumentInjectConstructor.java")

    @Test
    fun `should generate component if package name is configured`() {
        compilerWithOptions(ARG_GENERATED_COMPONENT)
            .compile(noArgumentInjectConstructor)

        generatedFile(GENERATED_COMPONENT).shouldNotBeNull()
    }

    @Test
    fun `should skip component generation if package name is not configured`() {
        compiler().compile(noArgumentInjectConstructor)

        generatedFile(GENERATED_COMPONENT).shouldBeNull()
    }

    @Test
    fun `should generate component for class without scope`() {
        compilerWithOptions(ARG_GENERATED_COMPONENT)
            .compileSuccessful("OneArgumentInjectConstructor.java")

        generatedFile(GENERATED_COMPONENT).shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component
        |import io.jentz.winter.component
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |val generatedComponent: Component = component {
        |
        |    io.jentz.winter.compilertest.OneArgumentInjectConstructor_WinterFactory().register(this)
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should register class with scope annotation in subcomponent`() {
        compilerWithOptions(ARG_GENERATED_COMPONENT)
            .compileSuccessful("WithCustomApplicationScope.java")

        generatedFile(GENERATED_COMPONENT).shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component
        |import io.jentz.winter.component
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |val generatedComponent: Component = component {
        |
        |    subcomponent(io.jentz.winter.compiler.ApplicationScope::class) {
        |
        |        io.jentz.winter.compilertest.WithCustomApplicationScope_WinterFactory().register(this)
        |
        |    }
        |
        |}

        """.trimMargin()
        )
    }

    @Test
    fun `should register class with root scope annotation in root component`() {
        compilerWithOptions(
            ARG_GENERATED_COMPONENT,
            "$OPTION_ROOT_SCOPE_ANNOTATION=io.jentz.winter.compiler.ApplicationScope"
        ).compileSuccessful("WithCustomApplicationScope.java")

        generatedFile(GENERATED_COMPONENT).shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Component
        |import io.jentz.winter.component
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |val generatedComponent: Component = component {
        |
        |    io.jentz.winter.compilertest.WithCustomApplicationScope_WinterFactory().register(this)
        |
        |}

        """.trimMargin()
        )
    }

}
