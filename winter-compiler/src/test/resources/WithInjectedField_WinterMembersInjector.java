package test;

import io.jentz.winter.ClassTypeKey;
import io.jentz.winter.Graph;
import io.jentz.winter.inject.MembersInjector;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class WithInjectedField_WinterMembersInjector implements MembersInjector<WithInjectedField> {
    @Override
    public void inject(final Graph graph, final WithInjectedField target) {
        target.field0 = graph.instanceOrNullByKey(new ClassTypeKey<String>(String.class, null));
    }
}
