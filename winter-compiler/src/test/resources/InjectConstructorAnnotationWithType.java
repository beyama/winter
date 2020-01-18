package test;

import java.util.concurrent.atomic.AtomicBoolean;

import io.jentz.winter.inject.InjectConstructor;

@InjectConstructor(AtomicBoolean.class)
class InjectConstructorAnnotationWithType extends AtomicBoolean {
}