package io.jentz.winter.compilertest;

import javax.inject.Inject;

import io.jentz.winter.inject.InjectConstructor;

@InjectConstructor
public class InjectConstructorAnnotationWithInjectConstructor {
    @Inject
    public InjectConstructorAnnotationWithInjectConstructor() {
    }
}
