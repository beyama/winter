package io.jentz.winter.compilertest;

import javax.inject.Inject;

import io.jentz.winter.compiler.CustomScope;
import io.jentz.winter.inject.ApplicationScope;

@CustomScope
public class WithCustomScope {
    @Inject
    public WithCustomScope() {
    }
}
