package io.jentz.winter.compilertest;

import javax.inject.Inject;

@CustomScope
public class CustomScopedWithNoArgumentInjectConstructor {
    @Inject
    public CustomScopedWithNoArgumentInjectConstructor() {
    }
}
