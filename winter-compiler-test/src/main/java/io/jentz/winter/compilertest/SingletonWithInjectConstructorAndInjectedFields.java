package io.jentz.winter.compilertest;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SingletonWithInjectConstructorAndInjectedFields {
    @Inject
    NoArgumentInjectConstructor field0;

    @Inject
    OneArgumentInjectConstructor field1;

    @Inject
    public SingletonWithInjectConstructorAndInjectedFields() {
    }
}
