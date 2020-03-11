package test;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PrivateNoArgumentInjectConstructor {
    @Inject
    private PrivateNoArgumentInjectConstructor() {
    }
}
