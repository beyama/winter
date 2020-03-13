package test

import io.jentz.winter.Graph
import io.jentz.winter.inject.MembersInjector
import javax.annotation.Generated
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class WithInjectedGenericFields_WinterMembersInjector : MembersInjector<WithInjectedGenericFields> {
  override fun inject(graph: Graph, target: WithInjectedGenericFields) {
    target.field0 = graph.instance<Map<String, Int>>(generics = true)
    target.field1 = graph.instanceOrNull<List<Int>>(generics = true)
  }
}
