package io.jentz.winter.compilertest.membersonly;

import javax.inject.Inject;

import kotlin.Lazy;

public class LazyInjection {

    @Inject Lazy<Integer> field;

    @Inject
    public LazyInjection() {
    }
}
