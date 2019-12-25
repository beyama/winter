package io.jentz.winter.compilertest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("variant1")
public class NamedSingletonInjectConstructor {
    @Inject
    public NamedSingletonInjectConstructor() {
    }
}
