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
class NoArgumentInjectConstructor_WinterFactory : Factory<NoArgumentInjectConstructor> {
  override fun invoke(graph: Graph): NoArgumentInjectConstructor = NoArgumentInjectConstructor()

  override fun register(builder: Component.Builder, override: Boolean):
      TypeKey<NoArgumentInjectConstructor> {
    builder.checkComponentQualifier(ApplicationScope::class)
    return builder.singleton(override = override, factory = this)
  }
}
