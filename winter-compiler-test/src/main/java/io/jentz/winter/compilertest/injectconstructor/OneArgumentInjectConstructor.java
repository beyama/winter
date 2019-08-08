package io.jentz.winter.compilertest.injectconstructor;

import javax.inject.Inject;

public class OneArgumentInjectConstructor {
    final NoArgumentInjectConstructor arg;

    @Inject
    public OneArgumentInjectConstructor(NoArgumentInjectConstructor arg) {
        this.arg = arg;
    }
}
