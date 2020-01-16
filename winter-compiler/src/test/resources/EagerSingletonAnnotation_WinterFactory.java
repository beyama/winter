package test;

import io.jentz.winter.ClassTypeKey;
import io.jentz.winter.Component;
import io.jentz.winter.Graph;
import io.jentz.winter.TypeKey;
import io.jentz.winter.inject.Factory;
import io.jentz.winter.inject.InterOp;
import java.lang.Override;
import javax.annotation.Generated;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class EagerSingletonAnnotation_WinterFactory implements Factory<EagerSingletonAnnotation> {
    @Override
    public EagerSingletonAnnotation invoke(final Graph graph) {
        return new EagerSingletonAnnotation();
    }

    @Override
    public TypeKey<EagerSingletonAnnotation> register(final Component.Builder builder,
                                                      final boolean override) {
        TypeKey<test.EagerSingletonAnnotation> key = new ClassTypeKey<>(EagerSingletonAnnotation.class, null);
        InterOp.eagerSingleton(builder, key, override, this);
        return key;
    }
}
