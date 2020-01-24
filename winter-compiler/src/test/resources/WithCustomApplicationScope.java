package io.jentz.winter.compilertest;

import javax.inject.Inject;

import io.jentz.winter.inject.ApplicationScope;

@ApplicationScope
public class WithCustomApplicationScope {
    @Inject
    public WithCustomApplicationScope() {
    }
}
