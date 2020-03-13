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
class WithInjectedField_WinterFactory : Factory<WithInjectedField> {
  override fun invoke(graph: Graph): WithInjectedField {
    val instance = WithInjectedField()
    WithInjectedField_WinterMembersInjector().inject(graph, instance)
    return instance
  }

  override fun register(builder: Component.Builder, override: Boolean): TypeKey<WithInjectedField> =
      builder.prototype(override = override, factory = this)
}
