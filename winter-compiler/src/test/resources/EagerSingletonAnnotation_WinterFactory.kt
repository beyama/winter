package test

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.inject.Factory
import javax.annotation.Generated
import kotlin.Boolean

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class EagerSingletonAnnotation_WinterFactory : Factory<EagerSingletonAnnotation> {
  override fun invoke(graph: Graph): EagerSingletonAnnotation = EagerSingletonAnnotation()

  override fun register(builder: Component.Builder, override: Boolean):
      TypeKey<EagerSingletonAnnotation> = builder.eagerSingleton(override = override, factory =
      this)
}
