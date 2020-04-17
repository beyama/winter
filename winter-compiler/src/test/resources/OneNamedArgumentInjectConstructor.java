package test;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;

public class OneNamedArgumentInjectConstructor {
    @Inject
    public OneNamedArgumentInjectConstructor(@Named("a name") @NotNull String arg) {
    }
}
