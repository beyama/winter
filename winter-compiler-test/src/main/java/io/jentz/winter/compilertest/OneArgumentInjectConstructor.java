package io.jentz.winter.compilertest;

import javax.inject.Inject;

public class OneArgumentInjectConstructor {
    final NoArgumentInjectConstructor arg;

    @Inject
    public OneArgumentInjectConstructor(NoArgumentInjectConstructor arg) {
        this.arg = arg;
    }
}
