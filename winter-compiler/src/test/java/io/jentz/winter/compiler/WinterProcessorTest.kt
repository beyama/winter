package io.jentz.winter.compiler

import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import com.squareup.kotlinpoet.FileSpec
import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.Winter
import io.jentz.winter.plugin.SimplePlugin
import io.kotlintest.matchers.string.shouldNotBeBlank
import io.kotlintest.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class WinterProcessorTest {

    private val writer = object : SourceWriter {

        val files = mutableMapOf<String, String>()

        override fun write(fileSpec: FileSpec) {
            files[fileSpec.name] = fileSpec.toString()
        }
    }

    private val plugin = object : SimplePlugin() {
        override fun initializingComponent(parentGraph: Graph?, builder: ComponentBuilder) {
            builder.constant<SourceWriter>(writer, override = true)
        }
    }


    val javaFile1 = JavaFileObjects.forSourceString(
        "NoArgumentInjectConstructor",
        """
        package io.jentz.winter.compilertest;

        import javax.inject.Inject;

        public class NoArgumentInjectConstructor {
            @Inject
            public NoArgumentInjectConstructor() {
            }
        }
        """.trimIndent()
    )

    private val validOptions = listOf(
        "-A$OPTION_KAPT_KOTLIN_GENERATED=/tmp",
        "-A$OPTION_GENERATED_COMPONENT_PACKAGE=io.jentz.winter.compilertest"
    )

    @BeforeEach
    fun beforeEach() {
        Winter.plugins.register(plugin)
        currentDateFixed = ISO8601_FORMAT.parse("2019-02-10T14:52Z")
    }

    @AfterEach
    fun afterEach() {
        Winter.plugins.unregister(plugin)
        writer.files.clear()
        currentDateFixed = null
    }

    @Test
    fun `should warn if kapt directory is not set`() {
        val compilation = javac()
            .withProcessors(WinterProcessor())
            .compile(javaFile1)

        assertThat(compilation)
            .hadWarningContaining("Skipping annotation processing: Kapt generated sources directory is not set.")
    }

    @Test
    fun `should warn if package for generated component is not set`() {

        val compilation = javac()
            .withProcessors(WinterProcessor())
            .withOptions(
                "-A$OPTION_KAPT_KOTLIN_GENERATED=/tmp"
            )
            .compile(javaFile1)

        assertThat(compilation)
            .hadWarningContaining(
                "Package to generate component to is not configured. " +
                        "Set option `$OPTION_GENERATED_COMPONENT_PACKAGE`."
            )
    }

    @Test
    fun `should generate factory for no argument inject constructor`() {

        val compilation = javac()
            .withProcessors(WinterProcessor())
            .withOptions(validOptions)
            .compile(javaFile1)

        assertThat(compilation)
            .succeeded()

        writer.files["WinterFactory_NoArgumentInjectConstructor"].shouldBe(
            """
            package io.jentz.winter.compilertest

            import io.jentz.winter.Factory
            import io.jentz.winter.Graph
            import javax.annotation.Generated

            @Generated(
                    value = ["io.jentz.winter.compiler.WinterProcessor"],
                    date = "2019-02-10T14:52Z"
            )
            class WinterFactory_NoArgumentInjectConstructor : Factory<Graph, NoArgumentInjectConstructor> {
                override fun invoke(graph: Graph): NoArgumentInjectConstructor = NoArgumentInjectConstructor()
            }

            """.trimIndent()
        )
    }

    @Test
    fun `should generate component for no argument inject constructor class`() {

        val compilation = javac()
            .withProcessors(WinterProcessor())
            .withOptions(validOptions)
            .compile(javaFile1)

        assertThat(compilation)
            .succeeded()

        writer.files["generatedComponent"].shouldBe(
            """
            package io.jentz.winter.compilertest

            import io.jentz.winter.Component
            import io.jentz.winter.component
            import javax.annotation.Generated

            @Generated(
                    value = ["io.jentz.winter.compiler.WinterProcessor"],
                    date = "2019-02-10T14:52Z"
            )
            val generatedComponent: Component = component {
                        prototype<NoArgumentInjectConstructor> {
                            `WinterFactory_NoArgumentInjectConstructor`().invoke(this)
                        }
                    }


            """.trimIndent()
        )
    }

}
