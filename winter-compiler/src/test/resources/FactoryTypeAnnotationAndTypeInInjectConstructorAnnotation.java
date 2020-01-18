package test;

import java.util.concurrent.atomic.AtomicBoolean;

import io.jentz.winter.inject.FactoryType;
import io.jentz.winter.inject.InjectConstructor;

@FactoryType(AtomicBoolean.class)
@InjectConstructor(AtomicBoolean.class)
class FactoryTypeAnnotationAndTypeInInjectConstructorAnnotation extends AtomicBoolean {
}