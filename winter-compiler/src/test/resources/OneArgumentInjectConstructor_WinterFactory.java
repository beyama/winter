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
public final class OneArgumentInjectConstructor_WinterFactory implements Factory<OneArgumentInjectConstructor> {
    @Override
    public OneArgumentInjectConstructor invoke(final Graph graph) {
        return new OneArgumentInjectConstructor(graph.instanceOrNullByKey(new ClassTypeKey<String>(String.class, null)));
    }

    @Override
    public TypeKey<OneArgumentInjectConstructor> register(final Component.Builder builder,
                                                          final boolean override) {
        TypeKey<test.OneArgumentInjectConstructor> key = new ClassTypeKey<OneArgumentInjectConstructor>(OneArgumentInjectConstructor.class, null);
        InterOp.prototype(builder, key, override, this);
        return key;
    }
}
