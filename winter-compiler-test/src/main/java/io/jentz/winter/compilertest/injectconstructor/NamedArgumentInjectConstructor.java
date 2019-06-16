package io.jentz.winter.compilertest.injectconstructor;

import javax.inject.Inject;
import javax.inject.Named;

public class NamedArgumentInjectConstructor {
    final String message;

    @Inject
    public NamedArgumentInjectConstructor(@Named("message") String message) {
        this.message = message;
    }
}
