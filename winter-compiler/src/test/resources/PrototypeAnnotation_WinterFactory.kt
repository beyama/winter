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
class PrototypeAnnotation_WinterFactory : Factory<PrototypeAnnotation> {
  override fun invoke(graph: Graph): PrototypeAnnotation = PrototypeAnnotation()

  override fun register(builder: Component.Builder, override: Boolean):
      TypeKey<PrototypeAnnotation> {
    builder.checkComponentQualifier(ApplicationScope::class)
    return builder.prototype(override = override, factory = this)
  }
}
