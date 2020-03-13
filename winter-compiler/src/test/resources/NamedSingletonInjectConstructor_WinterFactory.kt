package test

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.inject.Factory
import javax.annotation.Generated
import kotlin.Boolean

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class NamedSingletonInjectConstructor_WinterFactory : Factory<NamedSingletonInjectConstructor> {
  override fun invoke(graph: Graph): NamedSingletonInjectConstructor =
      NamedSingletonInjectConstructor()

  override fun register(builder: Component.Builder, override: Boolean):
      TypeKey<NamedSingletonInjectConstructor> {
    builder.checkComponentQualifier(ApplicationScope::class)
    return builder.singleton(qualifier = "variant1", override = override, factory = this)
  }
}
