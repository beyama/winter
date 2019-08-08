package io.jentz.winter.compilertest.injectconstructor;

import javax.inject.Inject;

import io.jentz.winter.compilertest.CustomScope;

@CustomScope
public class CustomScopedWithNoArgumentInjectConstructor {
    @Inject
    public CustomScopedWithNoArgumentInjectConstructor() {
    }
}
