package io.jentz.winter.androidx

import io.jentz.winter.Qualifier
import io.jentz.winter.qualifier

val ApplicationScope = Qualifier.App
val ViewModelScope = qualifier("view model scope")
val ActivityScope = qualifier("activity scope")
val ScreenScope = qualifier("screen scope")