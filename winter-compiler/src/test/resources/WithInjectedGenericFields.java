package io.jentz.winter.compilertest;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class WithInjectedGenericFields {
    @Inject Map<String, Integer> field0;
    @Inject List<Integer> field1;
}