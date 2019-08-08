package io.jentz.winter.compilertest.membersonly;

import javax.inject.Inject;

import io.jentz.winter.compilertest.injectconstructor.NoArgumentInjectConstructor;
import io.jentz.winter.compilertest.injectconstructor.OneArgumentInjectConstructor;

public class OnlyInjectedMembers {
    @Inject
    NoArgumentInjectConstructor field0;

    private OneArgumentInjectConstructor field1;

    public OneArgumentInjectConstructor getField1() {
        return field1;
    }

    @Inject
    public void setField1(OneArgumentInjectConstructor field1) {
        this.field1 = field1;
    }
}
