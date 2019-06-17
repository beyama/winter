package io.jentz.winter.compiler

import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects.forSourceString
import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.Winter
import io.jentz.winter.compiler.kotlinbuilder.KotlinFile
import io.jentz.winter.plugin.SimplePlugin
import io.kotlintest.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.tools.JavaFileObject

class WinterProcessorTest {

    private val writer = object : SourceWriter {

        val files = mutableMapOf<String, String>()

        override fun write(kotlinFile: KotlinFile) {
            files[kotlinFile.fileName] = kotlinFile.code
        }
    }

    private val plugin = object : SimplePlugin() {
        override fun initializingComponent(parentGraph: Graph?, builder: ComponentBuilder) {
            builder.constant<SourceWriter>(writer, override = true)
        }
    }

    private val noArgumentInjectConstructor = forSourceString(
            "NoArgumentInjectConstructor",
            """
            |package io.jentz.winter.compilertest;
            |
            |import javax.inject.Inject;
            |
            |public class NoArgumentInjectConstructor {
            |    @Inject
            |    public NoArgumentInjectConstructor() {
            |    }
            |}
            |""".trimMargin()
    )

    private val GENERATED_COMPONENT = "generatedComponent"

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
                .compile(noArgumentInjectConstructor)

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
                .compile(noArgumentInjectConstructor)

        assertThat(compilation)
                .hadWarningContaining(
                        "Package to generate component to is not configured. " +
                                "Set option `$OPTION_GENERATED_COMPONENT_PACKAGE`."
                )
    }

    @Test
    fun `should generate component for no argument inject constructor class`() {
        defaultCompiler().compileSuccessful(noArgumentInjectConstructor)

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
        |    prototype<NoArgumentInjectConstructor> {
        |        NoArgumentInjectConstructor()
        |    }
        |
        |}

        """.trimMargin()
        )
    }

    @Test
    fun `should generate component for one argument inject constructor class`() {
        defaultCompiler().compileSuccessful("OneArgumentInjectConstructor",
                """
                |package io.jentz.winter.compilertest;
                |
                |import javax.inject.Inject;
                |
                |public class OneArgumentInjectConstructor {
                |    @Inject
                |    public OneArgumentInjectConstructor(String arg) {
                |    }
                |}

                """.trimMargin())

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
        |    prototype<OneArgumentInjectConstructor> {
        |        OneArgumentInjectConstructor(instanceOrNull<String>())
        |    }
        |
        |}
        |""".trimMargin())
    }

    @Test
    fun `should generate injector for field with generics type`() {
        defaultCompiler().compileSuccessful("WithInjectedGenericField",
                """
                |package io.jentz.winter.compilertest;
                |
                |import java.util.List;
                |import java.util.Map;
                |import javax.inject.Inject;
                |
                |public class WithInjectedGenericField {
                |    @Inject Map<String, Integer> field0;
                |    @Inject List<Integer> field1;
                |}
                """.trimMargin())

        generatedFile("WinterMembersInjector_WithInjectedGenericField").shouldBe("""
        |package io.jentz.winter.compilertest
        |
        |import io.jentz.winter.Graph
        |import io.jentz.winter.MembersInjector
        |import java.util.List
        |import java.util.Map
        |import javax.annotation.Generated
        |
        |@Generated(
        |    value = ["io.jentz.winter.compiler.WinterProcessor"],
        |    date = "2019-02-10T14:52Z"
        |)
        |class WinterMembersInjector_WithInjectedGenericField : MembersInjector<WithInjectedGenericField> {
        |
        |    override fun injectMembers(graph: Graph, target: WithInjectedGenericField) {
        |        target.field0 = graph.instanceOrNull<Map<String, Integer>>(generics = true)
        |        target.field1 = graph.instanceOrNull<List<Integer>>(generics = true)
        |    }
        |
        |}
        |""".trimMargin())

        generatedFile("generatedComponent").shouldBe("""
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
        |    membersInjector<WithInjectedGenericField> {
        |        WinterMembersInjector_WithInjectedGenericField()
        |    }
        |
        |}
        |
        """.trimMargin())
    }

    @Test
    fun `should register class annotated with root scope in root component`() {
        defaultCompiler(
                "-A$OPTION_ROOT_SCOPE_ANNOTATION=io.jentz.winter.compiler.ApplicationScope"
        ).compileSuccessful("NoArgumentInjectConstructor",
                """
                |package io.jentz.winter.compilertest;
                |
                |import javax.inject.Inject;
                |import io.jentz.winter.compiler.ApplicationScope;
                |
                |@ApplicationScope
                |public class NoArgumentInjectConstructor {
                |    @Inject
                |    public NoArgumentInjectConstructor() {
                |    }
                |}
                |""".trimMargin())

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
        |    singleton<NoArgumentInjectConstructor> {
        |        NoArgumentInjectConstructor()
        |    }
        |
        |}

        """.trimMargin()
        )
    }

    private fun generatedFile(name: String) = writer.files[name]

    private fun compiler() = javac().withProcessors(WinterProcessor())

    private fun defaultCompiler(vararg options: String) =
            compiler().withOptions(validOptions + options)

    private fun Compiler.compileSuccessful(filename: String, code: String) {
        compileSuccessful(forSourceString(filename, code))
    }

    private fun Compiler.compileSuccessful(file: JavaFileObject) {
        assertThat(compile(file)).succeeded()
    }

}
