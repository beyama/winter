package io.jentz.winter.compiler.kotlinbuilder

import com.squareup.kotlinpoet.ClassName
import io.jentz.winter.compiler.GENERATED_ANNOTATION_NAME
import io.jentz.winter.compiler.ISO8601_FORMAT
import io.jentz.winter.compiler.WinterProcessor
import io.jentz.winter.compiler.now
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

private val SPACES_PER_INDENTATION_LEVEL = 4

typealias KotlinBuilderBlock = KotlinBuilder.() -> Unit

typealias KotlinFileBuilderBlock = KotlinFileBuilder.() -> Unit

data class KotlinCode(
        val imports: Set<ClassName>,
        val code: String
)

data class KotlinFile(
        val packageName: String,
        val fileName: String,
        val imports: Set<ClassName>,
        val code: String
) {

    fun writeTo(directory: Path) {
        require(Files.notExists(directory) || Files.isDirectory(directory)) {
            "path $directory exists but is not a directory."
        }
        var outputDirectory = directory
        if (packageName.isNotEmpty()) {
            for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
                outputDirectory = outputDirectory.resolve(packageComponent)
            }
        }

        Files.createDirectories(outputDirectory)

        val outputPath = outputDirectory.resolve("$fileName.kt")
        OutputStreamWriter(Files.newOutputStream(outputPath), StandardCharsets.UTF_8).use { writer ->
            writer.write(code)
            writer.close()
        }
    }

}


abstract class KotlinBuilder {

    protected val imports: MutableSet<ClassName> = mutableSetOf()
    private val builder = StringBuilder()
    private var level = 0

    fun append(text: String) {
        builder.append(text)
    }

    fun line(text: String = "") {
        if (text.isEmpty()) {
            builder.appendln()
            return
        }
        text.lineSequence().forEach {
            when {
                it.isBlank() -> {
                    if (it.length < level * SPACES_PER_INDENTATION_LEVEL) {
                        appendIndent()
                        newLine()
                    } else {
                        builder.appendln(it)
                    }
                }
                else -> {
                    appendIndent()
                    builder.appendln(it)
                }
            }
        }
        return
    }

    fun lines(text: String) = line(text)

    fun newLine() {
        builder.appendln()
    }

    fun newLineAndIndent() {
        newLine()
        appendIndent()
    }

    fun import(className: ClassName) {
        imports += className
    }

    fun import(imports: Set<ClassName>) {
        this.imports.addAll(imports)
    }

    fun block(block: KotlinBuilderBlock) {
        level += 1
        block(this)
        level -= 1
    }

    fun block(text: String, block: KotlinBuilderBlock) {
        line("$text {")
        block(block)
        line("}")
    }

    fun code(code: KotlinCode) {
        import(code.imports)
        lines(code.code)
    }

    fun appendCode(code: KotlinCode) {
        import(code.imports)
        append(code.code)
    }

    fun objectBlock(extends: String, block: KotlinBuilderBlock) {
        block("object : $extends", block)
    }

    fun appendIndent() {
        repeat(level * SPACES_PER_INDENTATION_LEVEL) { builder.append(' ') }
    }

    override fun toString(): String = builder.toString()
}

class KotlinCodeBuilder : KotlinBuilder() {
    fun build(): KotlinCode = KotlinCode(imports.toSet(), toString())
}

private val DEFAULT_NAMESPACES = listOf("java.lang", "kotlin")

class KotlinFileBuilder(
        val packageName: String,
        val fileName: String
) : KotlinBuilder() {

    fun build(): KotlinFile {
        val builder = this
        val code = buildString {
            appendln("package $packageName")
            appendln()
            imports
                    .sortedBy { it.toString() }
                    .filterNot { DEFAULT_NAMESPACES.contains(it.packageName) }
                    .forEach {
                        appendln("import $it")
                    }
            appendln()
            append(builder.toString())
            newLine()
        }
        return KotlinFile(packageName, fileName, imports, code)
    }

}

fun buildKotlinCode(block: KotlinBuilderBlock): KotlinCode =
        KotlinCodeBuilder().apply(block).build()

fun buildKotlinFile(packageName: String, fileName: String, block: KotlinFileBuilderBlock): KotlinFile =
        KotlinFileBuilder(packageName, fileName).apply(block).build()

fun KotlinBuilder.generatedAnnotation(isAnnotationAvailable: Boolean) {
    val processorName = WinterProcessor::class.java.name
    if (isAnnotationAvailable) {
        import(GENERATED_ANNOTATION_NAME)
        line("@${GENERATED_ANNOTATION_NAME.simpleName}(")
        line("    value = [\"$processorName\"],")
        line("    date = \"${ISO8601_FORMAT.format(now())}\"")
        line(")")
    } else {
        line("// Generated by $processorName at ${ISO8601_FORMAT.format(now())}\n")
    }
}
