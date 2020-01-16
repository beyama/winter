package test;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import kotlin.Lazy;

public class WithInjectedProviderAndLazyFields {
    @Inject
    Object field0;

    @Inject
    @Named("stringList")
    Provider<List<String>> field1;

    @Inject
    @Named("stringList")
    Lazy<List<String>> field2;
}