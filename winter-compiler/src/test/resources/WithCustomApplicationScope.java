package io.jentz.winter.compilertest;

import javax.inject.Inject;

import io.jentz.winter.compiler.ApplicationScope;

@ApplicationScope
public class WithCustomApplicationScope {
    @Inject
    public WithCustomApplicationScope() {
    }
}
