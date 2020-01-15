package test;

import io.jentz.winter.GenericClassTypeKey;
import io.jentz.winter.Graph;
import io.jentz.winter.inject.MembersInjector;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class WithInjectedGenericFields_WinterMembersInjector implements MembersInjector<WithInjectedGenericFields> {
    @Override
    public void inject(final Graph graph, final WithInjectedGenericFields target) {
        target.field0 = graph.instanceByKey(new GenericClassTypeKey<Map<String, Integer>>(null) {});
        target.field1 = graph.instanceOrNullByKey(new GenericClassTypeKey<List<Integer>>(null) {});
    }
}
