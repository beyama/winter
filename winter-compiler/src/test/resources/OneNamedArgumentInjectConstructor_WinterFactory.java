package test;

import io.jentz.winter.ClassTypeKey;
import io.jentz.winter.Component;
import io.jentz.winter.Graph;
import io.jentz.winter.TypeKey;
import io.jentz.winter.inject.Factory;
import io.jentz.winter.inject.InterOp;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class OneNamedArgumentInjectConstructor_WinterFactory implements Factory<OneNamedArgumentInjectConstructor> {
    @Override
    public OneNamedArgumentInjectConstructor invoke(final Graph graph) {
        return new OneNamedArgumentInjectConstructor(graph.instanceOrNullByKey(new ClassTypeKey<>(String.class, null)));
    }

    @Override
    public TypeKey<OneNamedArgumentInjectConstructor> register(final Component.Builder builder,
                                                               final boolean override) {
        TypeKey<test.OneNamedArgumentInjectConstructor> key = new ClassTypeKey<>(OneNamedArgumentInjectConstructor.class, null);
        InterOp.prototype(builder, key, override, this);
        return key;
    }
}
