package io.jentz.winter.compiler

import io.jentz.winter.compiler.kotlinbuilder.KotlinFile

interface SourceWriter {
    fun write(kotlinFile: KotlinFile)
}
