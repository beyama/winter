package io.jentz.winter.compiler

import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects.forResource
import io.jentz.winter.compiler.kotlinbuilder.KotlinFile
import io.jentz.winter.junit5.WinterEachExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import javax.tools.JavaFileObject

abstract class BaseProcessorTest {

    private val writer = object : SourceWriter {

        val files = mutableMapOf<String, String>()

        override fun write(kotlinFile: KotlinFile) {
            files[kotlinFile.fileName] = kotlinFile.code
        }
    }

    @JvmField
    @RegisterExtension
    val extension = WinterEachExtension {
        extend { constant<SourceWriter>(writer, override = true) }
    }

    @BeforeEach
    fun beforeEach() {
        currentDateFixed = ISO8601_FORMAT.parse("2019-02-10T14:52Z")
    }

    @AfterEach
    fun afterEach() {
        writer.files.clear()
        currentDateFixed = null
    }

    protected fun generatedFile(name: String) = writer.files[name]

    protected fun compiler(): Compiler = javac().withProcessors(WinterProcessor())

    protected fun compilerWithOptions(vararg options: String): Compiler =
        compiler().withOptions(options.map { "-A$it" })

    protected fun Compiler.compileSuccessful(vararg resourceNames: String) {
        compileSuccessful(*resourceNames.map(::forResource).toTypedArray())
    }

    protected fun Compiler.compileSuccessful(vararg files: JavaFileObject) {
        assertThat(compile(*files)).succeeded()
    }

}
