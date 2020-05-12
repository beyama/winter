package test

import io.jentz.winter.Graph
import io.jentz.winter.inject.MembersInjector
import javax.annotation.Generated
import kotlin.String

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class WithInjectedFieldExtended_WinterMembersInjector : MembersInjector<WithInjectedFieldExtended> {
  override fun inject(graph: Graph, target: WithInjectedFieldExtended) {
    WithInjectedField_WinterMembersInjector().inject(graph, target)
    target.field1 = graph.instanceOrNull<String>()
  }
}
