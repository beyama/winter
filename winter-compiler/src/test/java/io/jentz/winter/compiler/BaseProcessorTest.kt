package io.jentz.winter.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import io.jentz.winter.junit5.WinterEachExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension

@KotlinPoetMetadataPreview
abstract class BaseProcessorTest {

    protected val writer = TestSourceWriter()

    @JvmField
    @RegisterExtension
    val extension = WinterEachExtension {
        application = DI
        extend { constant<SourceWriter>(writer, override = true) }
    }

    @BeforeEach
    fun beforeEach() {
        currentDateFixed = ISO8601_FORMAT.parse("2019-02-10T14:52Z")
        writer.sources.clear()
    }

    @AfterEach
    fun afterEach() {
        currentDateFixed = null
    }

    fun generatesSource(name: String) {
        val sourceFile = "$name.kt"
        val generatedSource = writer.sources[name]
        val expectedSource = javaClass.classLoader?.getResource(sourceFile)?.readText()

        assertNotNull(expectedSource, "Resource with name `$sourceFile` not found.")
        assertNotNull(generatedSource, "Expected `$sourceFile` to be generated but was not.")
        assertEquals(expectedSource, generatedSource)
    }

    class TestSourceWriter : SourceWriter {
        val sources: MutableMap<String, String> = mutableMapOf()

        override fun write(fileSpec: FileSpec) {
            sources[fileSpec.name] = fileSpec.toString()
        }
    }

}
