package test;

import io.jentz.winter.CommonKt;
import io.jentz.winter.Component;
import io.jentz.winter.compiler.CustomScope;
import io.jentz.winter.compilertest.WithCustomScope_WinterFactory;
import io.jentz.winter.inject.ApplicationScope;
import javax.annotation.Generated;
import kotlin.Unit;
import kotlin.jvm.JvmClassMappingKt;

@Generated(
        value = "io.jentz.winter.compiler.WinterProcessor",
        date = "2019-02-10T14:52Z"
)
public final class GeneratedComponent {
    public static final Component generatedComponent;

    static {
        generatedComponent = CommonKt.component ("generated", builder -> {
            builder.subcomponent(JvmClassMappingKt.getKotlinClass(ApplicationScope.class), false, false, subBuilder -> {
                new InjectConstructorAnnotation_WinterFactory().register(subBuilder, false);
                new PrototypeAnnotation_WinterFactory().register(subBuilder, false);
                new NamedSingletonInjectConstructor_WinterFactory().register(subBuilder, false);
                return Unit.INSTANCE;
            });
            builder.subcomponent(JvmClassMappingKt.getKotlinClass(CustomScope.class), false, false, subBuilder -> {
                new WithCustomScope_WinterFactory().register(subBuilder, false);
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    }
}
