package test

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.inject.Factory
import javax.annotation.Generated
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class InjectConstructorWithProviderAndLazyArguments_WinterFactory :
    Factory<InjectConstructorWithProviderAndLazyArguments> {
  override fun invoke(graph: Graph): InjectConstructorWithProviderAndLazyArguments =
      InjectConstructorWithProviderAndLazyArguments(
      { graph.instance<String>(qualifier = "string") },
      lazy { graph.instanceOrNull<List<String>>(qualifier = "stringList", generics = true) }
  )

  override fun register(builder: Component.Builder, override: Boolean):
      TypeKey<InjectConstructorWithProviderAndLazyArguments> = builder.prototype(override =
      override, factory = this)
}
