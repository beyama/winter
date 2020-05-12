package test

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.inject.Factory
import javax.annotation.Generated
import kotlin.Boolean
import kotlin.String

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class OneNamedArgumentInjectConstructor_WinterFactory : Factory<OneNamedArgumentInjectConstructor> {
  override fun invoke(graph: Graph): OneNamedArgumentInjectConstructor =
      OneNamedArgumentInjectConstructor(graph.instance<String>(qualifier = "a name"))

  override fun register(builder: Component.Builder, override: Boolean):
      TypeKey<OneNamedArgumentInjectConstructor> = builder.prototype(override = override, factory =
      this)
}
