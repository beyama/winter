package io.jentz.winter.compiler

import com.squareup.kotlinpoet.FileSpec

interface SourceWriter {
    fun write(fileSpec: FileSpec)
}