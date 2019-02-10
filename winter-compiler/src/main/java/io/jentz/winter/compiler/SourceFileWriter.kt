package io.jentz.winter.compiler

import com.squareup.kotlinpoet.FileSpec
import java.io.File

class SourceFileWriter(
    private val generatedSourcesDirectory: String
) : SourceWriter {

    override fun write(fileSpec: FileSpec) {
        val file = File(generatedSourcesDirectory)
        fileSpec.writeTo(file)
    }
}