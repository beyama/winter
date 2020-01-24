package io.jentz.winter.compiler

import kotlinx.metadata.Flag
import kotlinx.metadata.KmProperty

val KmProperty.hasAccessibleSetter: Boolean
    get() = Flag.IS_PUBLIC(setterFlags) || Flag.IS_INTERNAL(setterFlags)

val KmProperty.isNullable: Boolean get() = Flag.Type.IS_NULLABLE(returnType.flags)
