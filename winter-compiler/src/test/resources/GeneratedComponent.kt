package test

import io.jentz.winter.Component
import io.jentz.winter.compiler.CustomScope
import io.jentz.winter.component
import io.jentz.winter.inject.ApplicationScope
import javax.annotation.Generated

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
val generatedComponent: Component = component("generated") {
  subcomponent(ApplicationScope::class) {
    InjectConstructorAnnotation_WinterFactory().register(this, false)
    PrototypeAnnotation_WinterFactory().register(this, false)
    NamedSingletonInjectConstructor_WinterFactory().register(this, false)
  }
  subcomponent(CustomScope::class) {
    WithCustomScope_WinterFactory().register(this, false)
  }
}

