package io.jentz.winter.compilertest;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NoArgumentInjectConstructor {
    @Inject
    public NoArgumentInjectConstructor() {
    }
}
