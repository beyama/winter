package io.jentz.winter.compiler

import io.jentz.winter.compiler.kotlinbuilder.KotlinFile
import javax.annotation.processing.Filer
import javax.tools.StandardLocation

class SourceFileWriter(
    private val filer: Filer
) : SourceWriter {

    override fun write(kotlinFile: KotlinFile) {
        val sourceFile = filer.createResource(
            StandardLocation.SOURCE_OUTPUT,
            kotlinFile.packageName,
            "${kotlinFile.fileName}.kt",
            kotlinFile.originatingElement
        )
        try {
            sourceFile.openWriter().use { it.write(kotlinFile.code) }
        } catch (e: Exception) {
            try {
                sourceFile.delete()
            } catch (ignored: Exception) {
            }
            throw e
        }
    }
}
