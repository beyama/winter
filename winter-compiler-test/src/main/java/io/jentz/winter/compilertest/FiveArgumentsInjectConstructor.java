package io.jentz.winter.compilertest;

import javax.inject.Inject;

public class FiveArgumentsInjectConstructor {
    final NoArgumentInjectConstructor arg0;
    final NoArgumentInjectConstructor arg1;
    final OneArgumentInjectConstructor arg2;
    final OneArgumentInjectConstructor arg3;
    final OneArgumentInjectConstructor arg4;

    @Inject
    public FiveArgumentsInjectConstructor(NoArgumentInjectConstructor arg0, NoArgumentInjectConstructor arg1,
                                          OneArgumentInjectConstructor arg2, OneArgumentInjectConstructor arg3,
                                          OneArgumentInjectConstructor arg4) {
        this.arg0 = arg0;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
    }
}
