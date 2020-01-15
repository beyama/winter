package test;

import io.jentz.winter.ClassTypeKey;
import io.jentz.winter.Component;
import io.jentz.winter.Graph;
import io.jentz.winter.TypeKey;
import io.jentz.winter.inject.Factory;
import io.jentz.winter.inject.InterOp;
import java.lang.Override;
import javax.annotation.Generated;
import javax.inject.Singleton;
import kotlin.jvm.JvmClassMappingKt;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class NamedSingletonInjectConstructor_WinterFactory implements Factory<NamedSingletonInjectConstructor> {
    @Override
    public NamedSingletonInjectConstructor invoke(final Graph graph) {
        return new NamedSingletonInjectConstructor();
    }

    @Override
    public TypeKey<NamedSingletonInjectConstructor> register(final Component.Builder builder,
                                                             final boolean override) {
        builder.checkComponentQualifier(JvmClassMappingKt.getKotlinClass(Singleton.class));
        TypeKey<test.NamedSingletonInjectConstructor> key = new ClassTypeKey<NamedSingletonInjectConstructor>(NamedSingletonInjectConstructor.class, "variant1");
        InterOp.singleton(builder, key, override, this);
        return key;
    }
}
