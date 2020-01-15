package test;

import io.jentz.winter.ClassTypeKey;
import io.jentz.winter.Component;
import io.jentz.winter.GenericClassTypeKey;
import io.jentz.winter.Graph;
import io.jentz.winter.TypeKey;
import io.jentz.winter.inject.Factory;
import io.jentz.winter.inject.InterOp;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import kotlin.LazyKt;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class InjectConstructorWithProviderAndLazyArguments_WinterFactory implements Factory<InjectConstructorWithProviderAndLazyArguments> {
    @Override
    public InjectConstructorWithProviderAndLazyArguments invoke(final Graph graph) {
        return new InjectConstructorWithProviderAndLazyArguments(
                () -> graph.instanceByKey(new ClassTypeKey<String>(String.class, "string")),
                LazyKt.lazy(() -> graph.instanceOrNullByKey(new GenericClassTypeKey<List<String>>("stringList") {}))
        );
    }

    @Override
    public TypeKey<InjectConstructorWithProviderAndLazyArguments> register(
            final Component.Builder builder, final boolean override) {
        TypeKey<test.InjectConstructorWithProviderAndLazyArguments> key = new ClassTypeKey<InjectConstructorWithProviderAndLazyArguments>(InjectConstructorWithProviderAndLazyArguments.class, null);
        InterOp.prototype(builder, key, override, this);
        return key;
    }
}
