package test;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import kotlin.Lazy;

public class InjectConstructorWithProviderAndLazyArguments {
    @Inject
    public InjectConstructorWithProviderAndLazyArguments(
            @NotNull  @Named("string") Provider<String> arg0,
            @Named("stringList") Lazy<List<String>> arg1
    ) {
    }
}
