package io.jentz.winter.compilertest.membersonly;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import io.jentz.winter.compilertest.injectconstructor.FiveArgumentsInjectConstructor;
import io.jentz.winter.compilertest.injectconstructor.NoArgumentInjectConstructor;
import io.jentz.winter.compilertest.injectconstructor.OneArgumentInjectConstructor;

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
