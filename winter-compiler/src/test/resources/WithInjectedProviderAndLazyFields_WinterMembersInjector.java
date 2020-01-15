package test;

import io.jentz.winter.ClassTypeKey;
import io.jentz.winter.GenericClassTypeKey;
import io.jentz.winter.Graph;
import io.jentz.winter.inject.MembersInjector;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import kotlin.LazyKt;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class WithInjectedProviderAndLazyFields_WinterMembersInjector implements MembersInjector<WithInjectedProviderAndLazyFields> {
    @Override
    public void inject(final Graph graph, final WithInjectedProviderAndLazyFields target) {
        target.field0 = graph.instanceOrNullByKey(new ClassTypeKey<Object>(Object.class, null));
        target.field1 = () -> graph.instanceOrNullByKey(new GenericClassTypeKey<List<String>>("stringList") {});
        target.field2 = LazyKt.lazy(() -> graph.instanceOrNullByKey(new GenericClassTypeKey<List<String>>("stringList") {}));
    }
}
