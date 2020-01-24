package test;

import io.jentz.winter.ClassTypeKey;
import io.jentz.winter.Component;
import io.jentz.winter.Graph;
import io.jentz.winter.TypeKey;
import io.jentz.winter.inject.Factory;
import io.jentz.winter.inject.InterOp;
import java.lang.Override;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Generated;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class FactoryTypeAnnotation_WinterFactory implements Factory<AtomicBoolean> {
    @Override
    public AtomicBoolean invoke(final Graph graph) {
        return new FactoryTypeAnnotation();
    }

    @Override
    public TypeKey<AtomicBoolean> register(final Component.Builder builder, final boolean override) {
        TypeKey<java.util.concurrent.atomic.AtomicBoolean> key = new ClassTypeKey<>(AtomicBoolean.class, null);
        InterOp.prototype(builder, key, override, this);
        return key;
    }
}
