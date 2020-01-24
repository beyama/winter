package test;

import javax.inject.Inject;
import javax.inject.Named;

public class OneNamedArgumentInjectConstructor {
    @Inject
    @Named("a name")
    public OneNamedArgumentInjectConstructor(String arg) {
    }
}
