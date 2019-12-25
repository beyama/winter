package io.jentz.winter.compilertest;

import javax.inject.Inject;

public class WithInjectedField {

    @Inject
    public WithInjectedField() {}

    @Inject String field0;
}