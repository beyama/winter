package test;

import io.jentz.winter.ClassTypeKey;
import io.jentz.winter.Component;
import io.jentz.winter.Graph;
import io.jentz.winter.TypeKey;
import io.jentz.winter.inject.ApplicationScope;
import io.jentz.winter.inject.Factory;
import io.jentz.winter.inject.InterOp;
import java.lang.Override;
import javax.annotation.Generated;
import kotlin.jvm.JvmClassMappingKt;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class NoArgumentInjectConstructor_WinterFactory implements Factory<NoArgumentInjectConstructor> {
    @Override
    public NoArgumentInjectConstructor invoke(final Graph graph) {
        return new NoArgumentInjectConstructor();
    }

    @Override
    public TypeKey<NoArgumentInjectConstructor> register(final Component.Builder builder,
                                                         final boolean override) {
        builder.checkComponentQualifier(JvmClassMappingKt.getKotlinClass(ApplicationScope.class));
        TypeKey<test.NoArgumentInjectConstructor> key = new ClassTypeKey<>(NoArgumentInjectConstructor.class, null);
        InterOp.singleton(builder, key, override, this);
        return key;
    }
}
