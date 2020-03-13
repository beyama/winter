package test

import io.jentz.winter.Graph
import io.jentz.winter.inject.MembersInjector
import javax.annotation.Generated
import kotlin.String

@Generated(
  value = ["io.jentz.winter.compiler.WinterProcessor"],
  date = "2019-02-10T14:52Z"
)
class WithInjectedField_WinterMembersInjector : MembersInjector<WithInjectedField> {
  override fun inject(graph: Graph, target: WithInjectedField) {
    target.field0 = graph.instanceOrNull<String>()
  }
}
