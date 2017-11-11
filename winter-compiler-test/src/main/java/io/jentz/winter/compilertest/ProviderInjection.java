package io.jentz.winter.compilertest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class ProviderInjection {
    final Provider<NoArgumentInjectConstructor> constructorInjected;
    final Provider<String> namedConstructorInjected;

    @Inject
    Provider<OneArgumentInjectConstructor> fieldInjected;

    @Named("field")
    @Inject
    Provider<String> namedFieldInjected;

    Provider<FiveArgumentsInjectConstructor> setterInjected;
    Provider<String> namedSetterInjected;

    @Inject
    public ProviderInjection(Provider<NoArgumentInjectConstructor> constructorInjected,
                             @Named("constructor") Provider<String> namedConstructorInjected) {
        this.constructorInjected = constructorInjected;
        this.namedConstructorInjected = namedConstructorInjected;
    }

    @Inject
    public void setSetterInjected(Provider<FiveArgumentsInjectConstructor> setterInjected) {
        this.setterInjected = setterInjected;
    }

    @Named("setter")
    @Inject
    public void setNamedSetterInjected(Provider<String> namedSetterInjected) {
        this.namedSetterInjected = namedSetterInjected;
    }
}
