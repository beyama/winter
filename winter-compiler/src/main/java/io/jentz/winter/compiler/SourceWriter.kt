package io.jentz.winter.compiler

import com.squareup.kotlinpoet.FileSpec
import javax.annotation.processing.Filer

interface SourceWriter {
    fun write(fileSpec: FileSpec)
}

class FilerSourceWriter(private val filer: Filer): SourceWriter {
    override fun write(fileSpec: FileSpec) {
        fileSpec.writeTo(filer)
    }
}