package io.jentz.winter.compilertest;

import javax.inject.Inject;

public class NoArgumentInjectConstructorWithSetterInjection {
    NoArgumentInjectConstructor field0;
    OneArgumentInjectConstructor field1;

    @Inject
    public NoArgumentInjectConstructorWithSetterInjection() {
    }

    @Inject
    public void setField0(NoArgumentInjectConstructor field0) {
        this.field0 = field0;
    }

    @Inject
    public void field1(OneArgumentInjectConstructor field1) {
        this.field1 = field1;
    }
}
