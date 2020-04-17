package test

import io.jentz.winter.Graph
import io.jentz.winter.inject.MembersInjector
import javax.annotation.Generated
import kotlin.Any
import kotlin.String
import kotlin.collections.List

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class WithInjectedProviderAndLazyFields_WinterMembersInjector :
    MembersInjector<WithInjectedProviderAndLazyFields> {
  override fun inject(graph: Graph, target: WithInjectedProviderAndLazyFields) {
    target.field0 = graph.instanceOrNull<Any>()
    target.field1 = { graph.instanceOrNull<List<String>>(qualifier = "stringList", generics = true)
        }
    target.field2 = graph.providerOrNull<List<String>>(qualifier = "stringList", generics = true)
    target.field3 = lazy { graph.instanceOrNull<List<String>>(qualifier = "stringList", generics =
        true) }
  }
}
