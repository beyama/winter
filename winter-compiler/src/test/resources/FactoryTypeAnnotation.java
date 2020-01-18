package test;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.jentz.winter.inject.FactoryType;

@FactoryType(AtomicBoolean.class)
class FactoryTypeAnnotation extends AtomicBoolean {
    @Inject public FactoryTypeAnnotation() {}
}