package io.jentz.winter.compiler

import io.jentz.winter.compiler.kotlinbuilder.KotlinFile
import java.io.File

class SourceFileWriter(
    private val generatedSourcesDirectory: String
) : SourceWriter {

    override fun write(kotlinFile: KotlinFile) {
        val file = File(generatedSourcesDirectory)
        kotlinFile.writeTo(file.toPath())
    }
}