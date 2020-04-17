package test

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.inject.Factory
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.Generated
import kotlin.Boolean

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class FactoryTypeAnnotation_WinterFactory : Factory<AtomicBoolean> {
  override fun invoke(graph: Graph): AtomicBoolean = FactoryTypeAnnotation()

  override fun register(builder: Component.Builder, override: Boolean): TypeKey<AtomicBoolean> =
      builder.prototype(override = override, factory = this)
}
