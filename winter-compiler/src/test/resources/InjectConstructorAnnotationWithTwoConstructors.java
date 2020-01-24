package test;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.jentz.winter.inject.InjectConstructor;

@InjectConstructor
public class InjectConstructorAnnotationWithTwoConstructors {
    public InjectConstructorAnnotationWithTwoConstructors() {
    }
    public InjectConstructorAnnotationWithTwoConstructors(String arg) {
    }
}
