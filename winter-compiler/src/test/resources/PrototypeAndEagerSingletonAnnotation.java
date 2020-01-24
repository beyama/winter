package test;

import javax.inject.Inject;

import io.jentz.winter.inject.ApplicationScope;
import io.jentz.winter.inject.EagerSingleton;
import io.jentz.winter.inject.InjectConstructor;
import io.jentz.winter.inject.Prototype;

@Prototype
@EagerSingleton
@InjectConstructor
public class PrototypeAndEagerSingletonAnnotation {
}
