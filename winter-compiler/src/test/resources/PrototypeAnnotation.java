package io.jentz.winter.compilertest;

import javax.inject.Inject;

import io.jentz.winter.inject.ApplicationScope;
import io.jentz.winter.inject.InjectConstructor;
import io.jentz.winter.inject.Prototype;

@Prototype
@ApplicationScope
@InjectConstructor
public class PrototypeAnnotation {
}
