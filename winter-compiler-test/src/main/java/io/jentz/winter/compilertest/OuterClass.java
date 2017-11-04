package io.jentz.winter.compilertest;

import javax.inject.Inject;

public class OuterClass {
    static public class InnerClassWithInjectConstructorAndInjectedFields {
        @Inject
        NoArgumentInjectConstructor field0;

        @Inject
        OneArgumentInjectConstructor field1;

        @Inject
        public InnerClassWithInjectConstructorAndInjectedFields() {
        }
    }
}
