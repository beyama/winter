package io.jentz.winter.compilertest;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import kotlin.Lazy;

public class InjectConstructorWithProviderAndLazyArguments {
    @Inject
    public InjectConstructorWithProviderAndLazyArguments(
            @Named("stringList") Provider<List<String>> arg0,
            @Named("stringList") Lazy<List<String>> arg1
    ) {
    }
}
