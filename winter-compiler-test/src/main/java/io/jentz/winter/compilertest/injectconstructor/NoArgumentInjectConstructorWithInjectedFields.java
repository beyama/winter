package io.jentz.winter.compilertest.injectconstructor;

import javax.inject.Inject;

public class NoArgumentInjectConstructorWithInjectedFields {
    @Inject
    NoArgumentInjectConstructor field0;

    @Inject
    OneArgumentInjectConstructor field1;

    @Inject
    public NoArgumentInjectConstructorWithInjectedFields() {
    }
}
